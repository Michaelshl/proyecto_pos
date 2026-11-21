package com.pos.venta;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// El recorrido completo de una venta contra la base en memoria (H2): borrador, agregar, quitar, cancelar,
// registrar (número de factura y stock), datos enmascarados, búsqueda y limpieza de borradores.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VentaIntegracionTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private VentaService ventaService;

    private static final RequestPostProcessor ADMIN = user("admin").roles("ADMIN");
    private static final RequestPostProcessor CAJERO = user("caja1").roles("CAJERO");
    private static final RequestPostProcessor OTRO_CAJERO = user("caja2").roles("CAJERO");

    private void crearProducto(String codigo, int stock, String precio) throws Exception {
        mockMvc.perform(post("/api/productos").with(ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"" + codigo + "\",\"nombre\":\"Producto " + codigo + "\",\"precioVenta\":"
                                + precio + ",\"stockInicial\":" + stock + ",\"stockMinimo\":1,\"categoria\":\"CategoriaVenta\"}"))
                .andExpect(status().isCreated());
    }

    private void prepararCatalogo() throws Exception {
        mockMvc.perform(post("/api/categorias").with(ADMIN).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"CategoriaVenta\",\"descripcion\":\"x\"}"));
    }

    private void crearCliente(String documento) throws Exception {
        mockMvc.perform(post("/api/clientes").with(ADMIN).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipoDocumento\":\"CC\",\"numeroDocumento\":\"" + documento + "\",\"nombre\":\"Ana\","
                                + "\"apellido\":\"Torres\",\"email\":\"ana.torres@correo.test\",\"telefono\":\"3150245878\","
                                + "\"direccion\":\"Calle 10 # 5-20, Cali\"}"))
                .andExpect(status().isCreated());
    }

    private String crearBorrador(RequestPostProcessor usuario, String cuerpo) throws Exception {
        String respuesta = mockMvc.perform(post("/api/ventas").with(usuario).contentType(MediaType.APPLICATION_JSON)
                        .content(cuerpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("EN_PROCESO"))
                .andExpect(jsonPath("$.numeroFactura").value(startsWith("B-")))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.numeroFactura");
    }

    private String crearBorrador(RequestPostProcessor usuario) throws Exception {
        return crearBorrador(usuario, "{}");
    }

    private void agregar(String numero, RequestPostProcessor usuario, String codigo, int cantidad, int esperado)
            throws Exception {
        mockMvc.perform(post("/api/ventas/" + numero + "/productos").with(usuario)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"" + codigo + "\",\"cantidad\":" + cantidad + "}"))
                .andExpect(status().is(esperado));
    }

    // Registra el borrador y devuelve el número de factura definitivo (F-...).
    private String registrar(String borrador, RequestPostProcessor usuario) throws Exception {
        String respuesta = mockMvc.perform(post("/api/ventas/" + borrador + "/registrar").with(usuario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("REGISTRADA"))
                .andExpect(jsonPath("$.numeroFactura").value(startsWith("F-")))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(respuesta, "$.numeroFactura");
    }

    private int stockDe(String codigo) throws Exception {
        String cuerpo = mockMvc.perform(get("/api/productos/" + codigo).with(ADMIN)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(cuerpo, "$.stock");
    }

    private long numero(String factura) {
        return Long.parseLong(factura.substring(2));
    }

    @Test
    void sinTokenDa401() throws Exception {
        mockMvc.perform(post("/api/ventas").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/ventas/F-000001")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/ventas")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/ventas/B-000001")).andExpect(status().isUnauthorized());
    }

    @Test
    void elRecorridoCompletoDescuentaElStockSoloAlRegistrarYElBorradorDesaparece() throws Exception {
        prepararCatalogo();
        crearProducto("0007001", 10, "5500");
        crearProducto("0007002", 20, "3000");

        String borrador = crearBorrador(CAJERO);
        agregar(borrador, CAJERO, "0007001", 2, 200);
        agregar(borrador, CAJERO, "0007002", 1, 200);
        agregar(borrador, CAJERO, "0007001", 1, 200);   // suma a la misma línea
        mockMvc.perform(get("/api/ventas/" + borrador).with(CAJERO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas.length()").value(2))
                .andExpect(jsonPath("$.lineas[0].cantidad").value(3))
                .andExpect(jsonPath("$.lineas[0].subtotal").value(16500.0))
                .andExpect(jsonPath("$.total").value(19500.0));
        assertEquals(10, stockDe("0007001"));   // el borrador no toca el stock

        mockMvc.perform(delete("/api/ventas/" + borrador + "/productos/2").with(CAJERO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas.length()").value(1))
                .andExpect(jsonPath("$.total").value(16500.0));

        String factura = registrar(borrador, CAJERO);
        assertEquals(7, stockDe("0007001"));
        assertEquals(20, stockDe("0007002"));

        // La factura tiene sus líneas y el borrador ya no existe.
        mockMvc.perform(get("/api/ventas/" + factura).with(CAJERO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineas.length()").value(1))
                .andExpect(jsonPath("$.lineas[0].productoCodigo").value("0007001"))
                .andExpect(jsonPath("$.total").value(16500.0));
        mockMvc.perform(get("/api/ventas/" + borrador).with(CAJERO)).andExpect(status().isNotFound());

        // Una venta registrada ya no se modifica, ni se cancela, ni se registra otra vez.
        mockMvc.perform(post("/api/ventas/" + factura + "/registrar").with(CAJERO)).andExpect(status().isBadRequest());
        agregar(factura, CAJERO, "0007002", 1, 400);
        mockMvc.perform(delete("/api/ventas/" + factura).with(CAJERO)).andExpect(status().isBadRequest());
    }

    @Test
    void elNumeroDeFacturaSoloSeGastaAlRegistrarYNoQuedanHuecos() throws Exception {
        prepararCatalogo();
        crearProducto("0007051", 50, "1000");

        String primero = crearBorrador(CAJERO);
        agregar(primero, CAJERO, "0007051", 1, 200);
        String facturaA = registrar(primero, CAJERO);

        // Un borrador cancelado y otro abandonado no consumen números de factura.
        String cancelado = crearBorrador(CAJERO);
        mockMvc.perform(delete("/api/ventas/" + cancelado).with(CAJERO)).andExpect(status().isNoContent());
        crearBorrador(CAJERO);

        String segundo = crearBorrador(CAJERO);
        agregar(segundo, CAJERO, "0007051", 1, 200);
        String facturaB = registrar(segundo, CAJERO);

        assertEquals(numero(facturaA) + 1, numero(facturaB));
    }

    @Test
    void cancelarBorraElBorradorYNoTocaElStock() throws Exception {
        prepararCatalogo();
        crearProducto("0007061", 10, "1000");

        String borrador = crearBorrador(CAJERO);
        agregar(borrador, CAJERO, "0007061", 3, 200);
        mockMvc.perform(delete("/api/ventas/" + borrador).with(CAJERO)).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/ventas/" + borrador).with(CAJERO)).andExpect(status().isNotFound());
        assertEquals(10, stockDe("0007061"));
        // Otro cajero no puede cancelar el borrador ajeno.
        String ajeno = crearBorrador(CAJERO);
        mockMvc.perform(delete("/api/ventas/" + ajeno).with(OTRO_CAJERO)).andExpect(status().isForbidden());
    }

    @Test
    void siUnProductoNoTieneStockNoSeRegistraNadaYElStockQuedaIgual() throws Exception {
        prepararCatalogo();
        crearProducto("0007011", 10, "5500");
        crearProducto("0007012", 2, "3000");

        String borrador = crearBorrador(CAJERO);
        agregar(borrador, CAJERO, "0007011", 4, 200);
        mockMvc.perform(post("/api/ventas/" + borrador + "/productos").with(CAJERO)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"0007012\",\"cantidad\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.advertencias[0]").value(containsString("Stock insuficiente")));

        mockMvc.perform(post("/api/ventas/" + borrador + "/registrar").with(CAJERO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Stock insuficiente")));
        assertEquals(10, stockDe("0007011"));
        assertEquals(2, stockDe("0007012"));
        // Sigue siendo un borrador y se puede corregir.
        mockMvc.perform(get("/api/ventas/" + borrador).with(CAJERO)).andExpect(jsonPath("$.estado").value("EN_PROCESO"));
    }

    @Test
    void unProductoInactivoNoEntraAUnaVenta() throws Exception {
        prepararCatalogo();
        crearProducto("0007021", 10, "5500");
        mockMvc.perform(patch("/api/productos/0007021/inactivar").with(ADMIN).contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"Descontinuado\"}")).andExpect(status().isOk());

        String borrador = crearBorrador(CAJERO);
        agregar(borrador, CAJERO, "0007021", 1, 400);
        agregar(borrador, CAJERO, "NO-EXISTE", 1, 404);
    }

    @Test
    void sinClienteLaVentaEsDeConsumidorFinal() throws Exception {
        mockMvc.perform(post("/api/ventas").with(CAJERO).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.consumidorFinal").value(true))
                .andExpect(jsonPath("$.clienteNombre").value("Consumidor final"))
                .andExpect(jsonPath("$.clienteDocumento").value("222222222222"));
    }

    @Test
    void elCajeroVeLosDatosEnmascaradosEnUnaNotaDePedidoYElAdminCompletos() throws Exception {
        crearCliente("7100001");

        String borrador = crearBorrador(CAJERO, "{\"clienteDocumento\":\"7100001\",\"notaPedido\":true}");

        mockMvc.perform(get("/api/ventas/" + borrador).with(CAJERO))
                .andExpect(jsonPath("$.clienteNombre").value("Ana Torres"))
                .andExpect(jsonPath("$.clienteCorreo").value("an****es@correo.test"))
                .andExpect(jsonPath("$.clienteTelefono").value("315*******"))
                .andExpect(jsonPath("$.clienteDireccion").value("Calle 10***"))
                .andExpect(jsonPath("$.esPedido").value(true));
        mockMvc.perform(get("/api/ventas/" + borrador).with(ADMIN))
                .andExpect(jsonPath("$.clienteCorreo").value("ana.torres@correo.test"))
                .andExpect(jsonPath("$.clienteTelefono").value("3150245878"))
                .andExpect(jsonPath("$.clienteDireccion").value("Calle 10 # 5-20, Cali"));
    }

    @Test
    void unaNotaDePedidoNecesitaUnClienteRegistradoYElClienteDebeExistir() throws Exception {
        mockMvc.perform(post("/api/ventas").with(CAJERO).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notaPedido\":true}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/ventas").with(CAJERO).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteDocumento\":\"NO-EXISTE\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void soloQuienCreoElBorradorOUnAdminPuedeModificarloYUnCajeroNoVeLaVentaDeOtro() throws Exception {
        prepararCatalogo();
        crearProducto("0007031", 10, "5500");

        String borrador = crearBorrador(CAJERO);
        agregar(borrador, OTRO_CAJERO, "0007031", 1, 403);
        agregar(borrador, ADMIN, "0007031", 1, 200);
        // El borrador ajeno responde como si no existiera; el administrador sí lo ve.
        mockMvc.perform(get("/api/ventas/" + borrador).with(OTRO_CAJERO)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/ventas/" + borrador).with(ADMIN)).andExpect(status().isOk());
    }

    @Test
    void registrarSinProductosYConsultarInexistenteFallan() throws Exception {
        String borrador = crearBorrador(CAJERO);
        mockMvc.perform(post("/api/ventas/" + borrador + "/registrar").with(CAJERO)).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/ventas/F-999999").with(CAJERO)).andExpect(status().isNotFound());
    }

    @Test
    void laCantidadDebeSerPositiva() throws Exception {
        String borrador = crearBorrador(CAJERO);
        mockMvc.perform(post("/api/ventas/" + borrador + "/productos").with(CAJERO)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"codigo\":\"X\",\"cantidad\":0}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/ventas/" + borrador + "/productos").with(CAJERO)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ---- nota de pedido de un contacto (no es cliente registrado) ----

    private static final String CONTACTO_NUEVO = "{\"notaPedido\":true,\"contacto\":{\"telefono\":\"3201112222\","
            + "\"nombre\":\"Carlos Pérez\",\"correo\":\"carlos.perez@correo.test\",\"direccion\":\"Carrera 8 # 12-30, Yumbo\"}}";

    @Test
    void unContactoDePedidoSeRegistraSeReutilizaYSusDatosSalenEnmascaradosParaElCajero() throws Exception {
        prepararCatalogo();
        crearProducto("0007121", 20, "2500");

        // Primer pedido: el contacto es nuevo y se guarda.
        String borrador = crearBorrador(CAJERO, CONTACTO_NUEVO);
        mockMvc.perform(get("/api/ventas/" + borrador).with(CAJERO))
                .andExpect(jsonPath("$.contactoPedido").value(true))
                .andExpect(jsonPath("$.consumidorFinal").value(false))
                .andExpect(jsonPath("$.clienteNombre").value("Carlos Pérez"))
                .andExpect(jsonPath("$.clienteCorreo").value("ca****ez@correo.test"))
                .andExpect(jsonPath("$.clienteTelefono").value("320*******"))
                .andExpect(jsonPath("$.clienteDireccion").value("Carrera***"));
        mockMvc.perform(get("/api/ventas/" + borrador).with(ADMIN))
                .andExpect(jsonPath("$.clienteCorreo").value("carlos.perez@correo.test"))
                .andExpect(jsonPath("$.clienteTelefono").value("3201112222"))
                .andExpect(jsonPath("$.clienteDireccion").value("Carrera 8 # 12-30, Yumbo"));
        agregar(borrador, CAJERO, "0007121", 2, 200);
        String factura = registrar(borrador, CAJERO);
        mockMvc.perform(get("/api/ventas/" + factura).with(CAJERO))
                .andExpect(jsonPath("$.contactoPedido").value(true))
                .andExpect(jsonPath("$.clienteNombre").value("Carlos Pérez"));

        // El contacto queda guardado: el cajero lo consulta por su celular (enmascarado) y el admin completo.
        mockMvc.perform(get("/api/contactos-pedido/3201112222").with(CAJERO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Carlos Pérez"))
                .andExpect(jsonPath("$.correo").value("ca****ez@correo.test"))
                .andExpect(jsonPath("$.direccion").value("Carrera***"));
        mockMvc.perform(get("/api/contactos-pedido/3201112222").with(ADMIN))
                .andExpect(jsonPath("$.correo").value("carlos.perez@correo.test"));
        mockMvc.perform(get("/api/contactos-pedido/3999999999").with(CAJERO)).andExpect(status().isNotFound());

        // Segundo pedido: basta el celular, se reutiliza el contacto.
        crearBorrador(CAJERO, "{\"notaPedido\":true,\"contacto\":{\"telefono\":\"3201112222\"}}");

        // Se puede buscar por el celular: el cajero ve el celular enmascarado y no el nombre; el admin, todo.
        mockMvc.perform(get("/api/ventas").with(CAJERO).param("texto", "3201112222"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.ventas[0].contactoPedido").value(true))
                .andExpect(jsonPath("$.ventas[0].clienteDocumento").value("320*******"))
                .andExpect(jsonPath("$.ventas[0].clienteNombre").doesNotExist());
        mockMvc.perform(get("/api/ventas").with(ADMIN).param("texto", "3201112222"))
                .andExpect(jsonPath("$.ventas[0].clienteNombre").value("Carlos Pérez"))
                .andExpect(jsonPath("$.ventas[0].clienteDocumento").value("3201112222"));
    }

    @Test
    void unContactoNuevoIncompletoYUnaNotaDePedidoSinNingunDatoDan400() throws Exception {
        mockMvc.perform(post("/api/ventas").with(CAJERO).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notaPedido\":true,\"contacto\":{\"telefono\":\"3105550000\",\"nombre\":\"Luis\"}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("obligatorios")));
        mockMvc.perform(post("/api/ventas").with(CAJERO).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notaPedido\":true,\"contacto\":{\"telefono\":\"abc\"}}"))
                .andExpect(status().isBadRequest());
        // El contacto que no se pudo crear no quedó guardado.
        mockMvc.perform(get("/api/contactos-pedido/3105550000").with(CAJERO)).andExpect(status().isNotFound());
    }

    // ---- buscar ----

    @Test
    void buscarPorUltimosDigitosDocumentoFechaYProductoRespetandoQuienEsCadaCajero() throws Exception {
        prepararCatalogo();
        crearProducto("0007091", 50, "2000");
        crearProducto("0007092", 50, "3000");
        crearCliente("7100091");

        String b1 = crearBorrador(CAJERO, "{\"clienteDocumento\":\"7100091\"}");
        agregar(b1, CAJERO, "0007091", 2, 200);
        String facturaCaja1 = registrar(b1, CAJERO);
        String b2 = crearBorrador(OTRO_CAJERO);
        agregar(b2, OTRO_CAJERO, "0007092", 1, 200);
        String facturaCaja2 = registrar(b2, OTRO_CAJERO);

        String hoy = LocalDate.now().toString();
        String ultimos4 = facturaCaja1.substring(facturaCaja1.length() - 4);

        // Últimos 4 dígitos de la factura: el cajero la encuentra porque es suya...
        mockMvc.perform(get("/api/ventas").with(CAJERO).param("texto", ultimos4))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ventas[?(@.numeroFactura=='" + facturaCaja1 + "')]").isNotEmpty());
        // ...pero no encuentra la del otro cajero, ni siquiera con su número completo.
        mockMvc.perform(get("/api/ventas").with(CAJERO).param("texto", facturaCaja2))
                .andExpect(jsonPath("$.total").value(0));
        // El administrador ve las dos.
        mockMvc.perform(get("/api/ventas").with(ADMIN).param("texto", facturaCaja2))
                .andExpect(jsonPath("$.ventas[0].numeroFactura").value(facturaCaja2));

        // Por documento del cliente: la lista no dice quién es (documento enmascarado); el admin sí lo ve.
        mockMvc.perform(get("/api/ventas").with(CAJERO).param("texto", "7100091"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.ventas[0].clienteDocumento").value("71******91"))
                .andExpect(jsonPath("$.ventas[0].clienteNombre").doesNotExist());
        mockMvc.perform(get("/api/ventas").with(ADMIN).param("texto", "7100091"))
                .andExpect(jsonPath("$.ventas[0].clienteNombre").value("Ana Torres"));
        // Y por consumidor final.
        mockMvc.perform(get("/api/ventas").with(OTRO_CAJERO).param("producto", "0007092"))
                .andExpect(jsonPath("$.ventas[0].clienteNombre").value("Consumidor final"))
                .andExpect(jsonPath("$.ventas[0].clienteDocumento").value("222222222222"));

        // Por producto (código o parte del nombre) y por fecha.
        mockMvc.perform(get("/api/ventas").with(CAJERO).param("producto", "0007091").param("desde", hoy).param("hasta", hoy))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.totalIngresos").value(4000.0));
        mockMvc.perform(get("/api/ventas").with(CAJERO).param("producto", "Producto 0007091"))
                .andExpect(jsonPath("$.total").value(1));
        mockMvc.perform(get("/api/ventas").with(CAJERO).param("producto", "0007091")
                        .param("desde", LocalDate.now().plusDays(1).toString()))
                .andExpect(jsonPath("$.total").value(0));
        mockMvc.perform(get("/api/ventas").with(CAJERO).param("desde", "2026-12-01").param("hasta", "2026-11-01"))
                .andExpect(status().isBadRequest());
    }

    // Pedido del usuario: al buscar por parte de un documento la lista no dice quién es el cliente; se confirma al abrir.
    @Test
    void laListaDeResultadosNoRevelaQuienEsElClienteAlCajero() throws Exception {
        prepararCatalogo();
        crearProducto("0007131", 20, "2000");
        crearCliente("7100131");
        String borrador = crearBorrador(CAJERO, "{\"clienteDocumento\":\"7100131\"}");
        agregar(borrador, CAJERO, "0007131", 1, 200);
        String factura = registrar(borrador, CAJERO);

        mockMvc.perform(get("/api/ventas").with(CAJERO).param("texto", "7100131"))
                .andExpect(jsonPath("$.ventas[0].numeroFactura").value(factura))
                .andExpect(jsonPath("$.ventas[0].clienteNombre").doesNotExist())
                .andExpect(jsonPath("$.ventas[0].clienteDocumento").value("71******31"));
        mockMvc.perform(get("/api/ventas").with(ADMIN).param("texto", "7100131"))
                .andExpect(jsonPath("$.ventas[0].clienteNombre").value("Ana Torres"));
        // Al abrir la venta sí aparece el cliente (con su contacto enmascarado para el cajero).
        mockMvc.perform(get("/api/ventas/" + factura).with(CAJERO))
                .andExpect(jsonPath("$.clienteNombre").value("Ana Torres"))
                .andExpect(jsonPath("$.clienteCorreo").value("an****es@correo.test"));
    }

    // Pregunta del usuario: ¿y un rango de fechas enorme? El cajero no puede pedir más de un año.
    @Test
    void elCajeroNoPuedeBuscarEnUnRangoDeMasDeUnAno() throws Exception {
        mockMvc.perform(get("/api/ventas").with(CAJERO).param("desde", "2003-12-06"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("un año")));
        mockMvc.perform(get("/api/ventas").with(CAJERO).param("desde", "2003-12-06").param("hasta", LocalDate.now().toString()))
                .andExpect(status().isBadRequest());
        // El administrador sí puede; la lista se limita y los totales los suma la base de datos.
        mockMvc.perform(get("/api/ventas").with(ADMIN).param("desde", "2003-12-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").isNumber());
    }

    @Test
    void buscarNuncaMuestraBorradoresYElAdminFiltraPorCajeroYEstado() throws Exception {
        prepararCatalogo();
        crearProducto("0007101", 50, "1500");
        String borrador = crearBorrador(CAJERO);
        agregar(borrador, CAJERO, "0007101", 1, 200);

        mockMvc.perform(get("/api/ventas").with(ADMIN).param("producto", "0007101"))
                .andExpect(jsonPath("$.total").value(0));   // un borrador no aparece

        String factura = registrar(borrador, CAJERO);
        mockMvc.perform(get("/api/ventas").with(ADMIN).param("producto", "0007101").param("cajero", "caja1")
                        .param("estado", "REGISTRADA"))
                .andExpect(jsonPath("$.ventas[0].numeroFactura").value(factura));
        mockMvc.perform(get("/api/ventas").with(ADMIN).param("producto", "0007101").param("cajero", "caja2"))
                .andExpect(jsonPath("$.total").value(0));
        mockMvc.perform(get("/api/ventas").with(ADMIN).param("estado", "EN_PROCESO"))
                .andExpect(status().isBadRequest());
    }

    // ---- borradores abandonados ----

    @Test
    void limpiarBorradoresBorraLosQueNadieTerminoPeroConservaLasVentasRegistradas() throws Exception {
        prepararCatalogo();
        crearProducto("0007111", 50, "1500");
        String abandonado = crearBorrador(CAJERO);
        agregar(abandonado, CAJERO, "0007111", 2, 200);
        String paraRegistrar = crearBorrador(CAJERO);
        agregar(paraRegistrar, CAJERO, "0007111", 1, 200);
        String factura = registrar(paraRegistrar, CAJERO);

        // Al arrancar se borran todos los borradores (límite en el futuro).
        ventaService.limpiarBorradores(LocalDateTime.now().plusDays(1));

        mockMvc.perform(get("/api/ventas/" + abandonado).with(CAJERO)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/ventas/" + factura).with(CAJERO)).andExpect(status().isOk());
        assertEquals(49, stockDe("0007111"));   // el borrador abandonado nunca tocó el stock
    }

    @Test
    void limpiarBorradoresRespetaLosRecientes() throws Exception {
        String reciente = crearBorrador(CAJERO);

        // Con un límite en el pasado no se borra el borrador recién creado.
        ventaService.limpiarBorradores(LocalDateTime.now().minusMinutes(30));

        mockMvc.perform(get("/api/ventas/" + reciente).with(CAJERO)).andExpect(status().isOk());
    }
}
