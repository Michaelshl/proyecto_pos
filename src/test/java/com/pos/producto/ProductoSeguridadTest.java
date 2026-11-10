package com.pos.producto;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Quién puede qué en /api/productos: el cajero solo consulta y lista; el administrador hace todo.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductoSeguridadTest {

    private static final String JSON_VACIO = "{}";

    @Autowired private MockMvc mockMvc;

    @Test
    void sinTokenDa401() throws Exception {
        mockMvc.perform(get("/api/productos")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/productos/P001")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/productos/sugerencias").param("q", "co"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void cajeroPuedeListarConsultarYUsarSugerencias() throws Exception {
        mockMvc.perform(get("/api/productos")).andExpect(status().isOk());
        mockMvc.perform(get("/api/productos/sugerencias").param("q", "co")).andExpect(status().isOk());
        mockMvc.perform(get("/api/productos/NOEXISTE")).andExpect(status().isNotFound());   // pasó la seguridad
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void cajeroNoPuedeCrearActualizarInactivarNiAjustar() throws Exception {
        mockMvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(JSON_VACIO))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/productos/P001").contentType(MediaType.APPLICATION_JSON).content(JSON_VACIO))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/productos/P001/inactivar").contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_VACIO))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/productos/P001/ajustes").contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_VACIO))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "CAJERO")
    void cajeroNoPuedeConsultarElStock() throws Exception {
        mockMvc.perform(get("/api/productos/P001/stock")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPuedeListarYConsultarElStock() throws Exception {
        mockMvc.perform(get("/api/productos")).andExpect(status().isOk());
        mockMvc.perform(get("/api/productos/NOEXISTE/stock")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void crearConJsonInvalidoDa400() throws Exception {
        mockMvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(JSON_VACIO))
                .andExpect(status().isBadRequest());
    }

    private String producto(String codigo) {
        return "{\"codigo\":\"" + codigo + "\",\"nombre\":\"Producto de prueba\",\"precioVenta\":1000,"
                + "\"stockInicial\":5,\"stockMinimo\":2,\"categoria\":\"CategoriaSku\"}";
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void elCodigoDebeSerUnSkuNumerico() throws Exception {
        mockMvc.perform(post("/api/categorias").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"CategoriaSku\",\"descripcion\":\"x\"}"));

        // Con letras, con guion, con espacios, muy corto o muy largo: 400 con el mensaje del SKU.
        for (String malo : new String[]{"BEB-001", "P001", "1234 5678", "123", "123456789012345"}) {
            mockMvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(producto(malo)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value(containsString("SKU")));
        }

        // Entre 4 y 14 dígitos, conservando los ceros a la izquierda.
        mockMvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(producto("0001234")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("0001234"));
        mockMvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(producto("7701234567890")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(producto("1234")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(producto("12345678901234")))
                .andExpect(status().isCreated());
    }

    // Observación del profesor: el administrador no ajusta el stock "a su gusto". Cada ajuste lleva un tipo de
    // motivo de una lista cerrada y el tipo tiene que ser coherente con el signo de la cantidad.
    @Test
    @WithMockUser(roles = "ADMIN")
    void elAjusteExigeUnTipoDeMotivoCoherenteConLaCantidad() throws Exception {
        mockMvc.perform(post("/api/categorias").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"CategoriaSku\",\"descripcion\":\"x\"}"));
        mockMvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content(producto("0009001")))
                .andExpect(status().isCreated());

        // El televisor: sacar unidades declarándolas como ingreso por compra.
        mockMvc.perform(post("/api/productos/0009001/ajustes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":-1,\"tipoMotivo\":\"INGRESO_COMPRA\",\"motivo\":\"Compra de televisor\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("solo sirve para sumar")));
        // Sin tipo de motivo, o con uno inventado.
        mockMvc.perform(post("/api/productos/0009001/ajustes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":1,\"motivo\":\"Sin tipo de motivo\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/productos/0009001/ajustes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":1,\"tipoMotivo\":\"REGALO\",\"motivo\":\"Porque sí\"}"))
                .andExpect(status().isBadRequest());
        // Observación demasiado corta.
        mockMvc.perform(post("/api/productos/0009001/ajustes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":1,\"tipoMotivo\":\"INGRESO_COMPRA\",\"motivo\":\"ok\"}"))
                .andExpect(status().isBadRequest());

        // Coherentes: una entrada por compra y una salida por daño.
        mockMvc.perform(post("/api/productos/0009001/ajustes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":3,\"tipoMotivo\":\"INGRESO_COMPRA\",\"motivo\":\"Compra al proveedor\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(8));
        mockMvc.perform(post("/api/productos/0009001/ajustes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cantidad\":-2,\"tipoMotivo\":\"DANO\",\"motivo\":\"Se cayó de la estantería\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock").value(6));
    }

    // Observación del profesor: el cajero no ve los productos inactivos ni puede elegir el estado; solo
    // filtra por categoría y nombre. El administrador sigue viéndolos todos.
    @Test
    void elCajeroSoloVeLosProductosActivosYElAdminTodos() throws Exception {
        mockMvc.perform(post("/api/categorias").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"CategoriaSku\",\"descripcion\":\"x\"}"));
        mockMvc.perform(post("/api/productos").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(producto("0008001"))).andExpect(status().isCreated());
        mockMvc.perform(post("/api/productos").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content(producto("0008002"))).andExpect(status().isCreated());
        mockMvc.perform(patch("/api/productos/0008002/inactivar").with(user("admin").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Descontinuado\"}"))
                .andExpect(status().isOk());

        // Listado: el cajero solo ve el activo, aunque pida los inactivos a propósito.
        mockMvc.perform(get("/api/productos").with(user("cajero").roles("CAJERO")).param("categoria", "CategoriaSku"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productos[?(@.codigo=='0008001')]").isNotEmpty())
                .andExpect(jsonPath("$.productos[?(@.codigo=='0008002')]").isEmpty());
        mockMvc.perform(get("/api/productos").with(user("cajero").roles("CAJERO"))
                        .param("categoria", "CategoriaSku").param("activo", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productos[?(@.codigo=='0008002')]").isEmpty())
                .andExpect(jsonPath("$.productos[?(@.codigo=='0008001')]").isNotEmpty());
        // Buscar por código en el listado: el cajero encuentra el activo y no el inactivo.
        mockMvc.perform(get("/api/productos").with(user("cajero").roles("CAJERO")).param("nombre", "0008001"))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.productos[0].codigo").value("0008001"));
        mockMvc.perform(get("/api/productos").with(user("cajero").roles("CAJERO")).param("nombre", "0008002"))
                .andExpect(jsonPath("$.total").value(0));
        // Consulta por código: el inactivo responde como si no existiera.
        mockMvc.perform(get("/api/productos/0008001").with(user("cajero").roles("CAJERO"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/productos/0008002").with(user("cajero").roles("CAJERO")))
                .andExpect(status().isNotFound());
        // Autocompletado: tampoco aparece.
        mockMvc.perform(get("/api/productos/sugerencias").with(user("cajero").roles("CAJERO")).param("q", "00080"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.codigo=='0008002')]").isEmpty());

        // El administrador ve los dos, filtra por estado y consulta el inactivo.
        mockMvc.perform(get("/api/productos").with(user("admin").roles("ADMIN")).param("categoria", "CategoriaSku"))
                .andExpect(jsonPath("$.productos[?(@.codigo=='0008002')]").isNotEmpty());
        mockMvc.perform(get("/api/productos").with(user("admin").roles("ADMIN"))
                        .param("categoria", "CategoriaSku").param("activo", "false"))
                .andExpect(jsonPath("$.productos[?(@.codigo=='0008001')]").isEmpty())
                .andExpect(jsonPath("$.productos[?(@.codigo=='0008002')]").isNotEmpty());
        mockMvc.perform(get("/api/productos/0008002").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void ajusteConJsonInvalidoDa400() throws Exception {
        mockMvc.perform(post("/api/productos/P001/ajustes").contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_VACIO))
                .andExpect(status().isBadRequest());
    }
}
