package com.pos.venta;

import com.pos.cliente.Cliente;
import com.pos.cliente.ClienteRepository;
import com.pos.producto.Producto;
import com.pos.producto.ProductoRepository;
import com.pos.venta.dto.ContactoPedidoRequest;
import com.pos.venta.dto.ContactoPedidoResponse;
import com.pos.venta.dto.LineaVentaRequest;
import com.pos.venta.dto.VentaCrearRequest;
import com.pos.venta.dto.VentaListaResponse;
import com.pos.venta.dto.VentaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VentaServiceTest {

    @Mock private VentaRepository ventaRepository;
    @Mock private DetalleVentaRepository detalleRepository;
    @Mock private ConsecutivoRepository consecutivoRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private ContactoPedidoRepository contactoRepository;

    private VentaService service;

    @BeforeEach
    void setUp() {
        service = new VentaService(ventaRepository, detalleRepository, consecutivoRepository,
                productoRepository, clienteRepository, contactoRepository);
    }

    // ---- datos de prueba ----

    private Venta venta(String numero, String estado, String cajero) {
        Venta v = new Venta();
        v.setNumeroFactura(numero);
        v.setFecha(LocalDateTime.now());
        v.setEstado(estado);
        v.setTotal(BigDecimal.ZERO);
        v.setCajeroCedula(cajero);
        v.setEsPedido(false);
        return v;
    }

    private Venta borrador(String cajero) {
        return venta("B-000001", "EN_PROCESO", cajero);
    }

    private Producto producto(String codigo, int stock, boolean activo, String precio) {
        Producto p = new Producto();
        p.setCodigo(codigo);
        p.setNombre("Producto " + codigo);
        p.setPrecioVenta(new BigDecimal(precio));
        p.setStock(stock);
        p.setStockMinimo(1);
        p.setActivo(activo);
        return p;
    }

    private DetalleVenta linea(String numero, int n, String codigo, int cantidad, String precio) {
        DetalleVenta d = new DetalleVenta();
        d.setId(new DetalleVentaId(numero, n));
        d.setProductoCodigo(codigo);
        d.setCantidad(cantidad);
        d.setPrecioUnitario(new BigDecimal(precio));
        return d;
    }

    private Cliente cliente(String documento) {
        Cliente c = new Cliente();
        c.setNumeroDocumento(documento);
        c.setTipoDocumento("CC");
        c.setNombre("Ana");
        c.setApellido("Torres");
        c.setEmail("ana.torres@correo.test");
        c.setTelefono("3150245878");
        c.setDireccion("Calle 10 # 5-20, Cali");
        return c;
    }

    private ContactoPedido contacto(String telefono) {
        ContactoPedido c = new ContactoPedido();
        c.setTelefono(telefono);
        c.setNombre("Carlos Pérez");
        c.setCorreo("carlos.perez@correo.test");
        c.setDireccion("Carrera 8 # 12-30, Yumbo");
        return c;
    }

    private ContactoPedidoRequest datosDeContacto(String telefono, String nombre, String correo, String direccion) {
        ContactoPedidoRequest r = new ContactoPedidoRequest();
        r.setTelefono(telefono);
        r.setNombre(nombre);
        r.setCorreo(correo);
        r.setDireccion(direccion);
        return r;
    }

    private LineaVentaRequest pedido(String codigo, int cantidad) {
        LineaVentaRequest r = new LineaVentaRequest();
        r.setCodigo(codigo);
        r.setCantidad(cantidad);
        return r;
    }

    private Consecutivo consecutivo(String tipo, long ultimo) {
        Consecutivo c = new Consecutivo();
        c.setTipo(tipo);
        c.setUltimoValor(ultimo);
        return c;
    }

    private int estado(ResponseStatusException e) {
        return HttpStatus.valueOf(e.getStatusCode().value()).value();
    }

    // ---- RF-025 crear el borrador ----

    @Test
    void crearGeneraUnNumeroDeBorradorYNoGastaUnNumeroDeFactura() {
        Consecutivo c = consecutivo("BORRADOR", 41);
        when(consecutivoRepository.bloquear("BORRADOR")).thenReturn(Optional.of(c));

        VentaResponse r = service.crear(new VentaCrearRequest(), "1001", false);

        assertEquals("B-000042", r.getNumeroFactura());
        assertEquals("EN_PROCESO", r.getEstado());
        assertEquals(42L, c.getUltimoValor());
        verify(consecutivoRepository, never()).bloquear("FACTURA");
        ArgumentCaptor<Venta> captor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepository).save(captor.capture());
        assertEquals("1001", captor.getValue().getCajeroCedula());
        assertNull(captor.getValue().getClienteDocumento());
        assertFalse(captor.getValue().getEsPedido());
    }

    @Test
    void sinClienteLaVentaEsDeConsumidorFinal() {
        when(consecutivoRepository.bloquear("BORRADOR")).thenReturn(Optional.empty());
        VentaCrearRequest request = new VentaCrearRequest();
        request.setClienteDocumento("   ");

        VentaResponse r = service.crear(request, "1001", false);

        assertEquals("B-000001", r.getNumeroFactura());
        assertTrue(r.getConsumidorFinal());
        assertEquals("Consumidor final", r.getClienteNombre());
        assertEquals("222222222222", r.getClienteDocumento());
        assertNull(r.getClienteCorreo());
        verifyNoInteractions(clienteRepository);
    }

    @Test
    void crearConClienteInexistenteDa400SinGastarUnNumero() {
        when(clienteRepository.existsById("999")).thenReturn(false);
        VentaCrearRequest request = new VentaCrearRequest();
        request.setClienteDocumento("999");

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.crear(request, "1001", false));

        assertEquals(400, estado(e));
        verifyNoInteractions(consecutivoRepository);
        verify(ventaRepository, never()).save(any());
    }

    @Test
    void unaNotaDePedidoSinClienteRegistradoDa400() {
        VentaCrearRequest request = new VentaCrearRequest();
        request.setNotaPedido(true);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.crear(request, "1001", false));

        assertEquals(400, estado(e));
        verifyNoInteractions(consecutivoRepository);
    }

    @Test
    void unaNotaDePedidoSeGuardaMarcadaYConClienteRegistrado() {
        when(clienteRepository.existsById("1010101010")).thenReturn(true);
        when(clienteRepository.findById("1010101010")).thenReturn(Optional.of(cliente("1010101010")));
        when(consecutivoRepository.bloquear("BORRADOR")).thenReturn(Optional.of(consecutivo("BORRADOR", 0)));
        VentaCrearRequest request = new VentaCrearRequest();
        request.setClienteDocumento("1010101010");
        request.setNotaPedido(true);

        VentaResponse r = service.crear(request, "1001", false);

        assertTrue(r.getEsPedido());
        ArgumentCaptor<Venta> captor = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepository).save(captor.capture());
        assertTrue(captor.getValue().getEsPedido());
    }

    // ---- nota de pedido de un contacto (no es cliente registrado) ----

    @Test
    void unContactoNuevoSeGuardaConSusDatosYLaVentaApuntaASuCelular() {
        when(contactoRepository.existsById("3201112222")).thenReturn(false);
        when(consecutivoRepository.bloquear("BORRADOR")).thenReturn(Optional.of(consecutivo("BORRADOR", 0)));
        VentaCrearRequest request = new VentaCrearRequest();
        request.setNotaPedido(true);
        request.setContacto(datosDeContacto(" 3201112222 ", "Carlos Pérez", "carlos.perez@correo.test", "Carrera 8 # 12-30, Yumbo"));
        when(contactoRepository.findById("3201112222")).thenReturn(Optional.of(contacto("3201112222")));

        VentaResponse r = service.crear(request, "1001", false);

        ArgumentCaptor<ContactoPedido> guardado = ArgumentCaptor.forClass(ContactoPedido.class);
        verify(contactoRepository).save(guardado.capture());
        assertEquals("3201112222", guardado.getValue().getTelefono());
        assertEquals("Carlos Pérez", guardado.getValue().getNombre());
        ArgumentCaptor<Venta> venta = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepository).save(venta.capture());
        assertEquals("3201112222", venta.getValue().getContactoTelefono());
        assertNull(venta.getValue().getClienteDocumento());
        // El cajero ve los datos del contacto enmascarados.
        assertTrue(r.getContactoPedido());
        assertFalse(r.getConsumidorFinal());
        assertEquals("Carlos Pérez", r.getClienteNombre());
        assertEquals("ca****ez@correo.test", r.getClienteCorreo());
        assertEquals("320*******", r.getClienteTelefono());
        assertEquals("Carrera***", r.getClienteDireccion());
    }

    @Test
    void unContactoYaRegistradoSeReutilizaSinPedirSusDatos() {
        when(contactoRepository.existsById("3201112222")).thenReturn(true);
        when(contactoRepository.findById("3201112222")).thenReturn(Optional.of(contacto("3201112222")));
        when(consecutivoRepository.bloquear("BORRADOR")).thenReturn(Optional.of(consecutivo("BORRADOR", 0)));
        VentaCrearRequest request = new VentaCrearRequest();
        request.setNotaPedido(true);
        request.setContacto(datosDeContacto("3201112222", null, null, null));

        VentaResponse r = service.crear(request, "1001", false);

        verify(contactoRepository, never()).save(any());
        assertEquals("Carlos Pérez", r.getClienteNombre());
    }

    @Test
    void unContactoNuevoIncompletoDa400SinGuardarNiGastarNumero() {
        when(contactoRepository.existsById("3201112222")).thenReturn(false);
        VentaCrearRequest request = new VentaCrearRequest();
        request.setNotaPedido(true);
        request.setContacto(datosDeContacto("3201112222", "Carlos", "  ", "Calle 1"));   // falta el correo

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.crear(request, "1001", false));

        assertEquals(400, estado(e));
        verify(contactoRepository, never()).save(any());
        verifyNoInteractions(consecutivoRepository);
    }

    @Test
    void conClienteRegistradoElContactoSeIgnora() {
        when(clienteRepository.existsById("1010101010")).thenReturn(true);
        when(clienteRepository.findById("1010101010")).thenReturn(Optional.of(cliente("1010101010")));
        when(consecutivoRepository.bloquear("BORRADOR")).thenReturn(Optional.of(consecutivo("BORRADOR", 0)));
        VentaCrearRequest request = new VentaCrearRequest();
        request.setClienteDocumento("1010101010");
        request.setNotaPedido(true);
        request.setContacto(datosDeContacto("3201112222", "Otro", "otro@correo.test", "Calle 9"));

        VentaResponse r = service.crear(request, "1001", false);

        verifyNoInteractions(contactoRepository);
        assertEquals("Ana Torres", r.getClienteNombre());
        assertFalse(r.getContactoPedido());
    }

    @Test
    void elCajeroConsultaUnContactoConCorreoYDireccionEnmascaradosYElAdminCompletos() {
        when(contactoRepository.findById("3201112222")).thenReturn(Optional.of(contacto("3201112222")));

        ContactoPedidoResponse cajero = service.consultarContacto("3201112222", false);
        assertEquals("Carlos Pérez", cajero.getNombre());
        assertEquals("ca****ez@correo.test", cajero.getCorreo());
        assertEquals("Carrera***", cajero.getDireccion());

        ContactoPedidoResponse admin = service.consultarContacto("3201112222", true);
        assertEquals("carlos.perez@correo.test", admin.getCorreo());
        assertEquals("Carrera 8 # 12-30, Yumbo", admin.getDireccion());
    }

    @Test
    void consultarUnContactoInexistenteDa404() {
        when(contactoRepository.findById("999")).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.consultarContacto("999", false));

        assertEquals(404, estado(e));
    }

    // ---- datos de contacto: enmascarados para el cajero, completos para el administrador ----

    @Test
    void elCajeroVeLosDatosDelClienteEnmascaradosYElAdminCompletos() {
        Venta v = borrador("1001");
        v.setClienteDocumento("1010101010");
        v.setEsPedido(true);
        when(ventaRepository.findById("B-000001")).thenReturn(Optional.of(v));
        when(clienteRepository.findById("1010101010")).thenReturn(Optional.of(cliente("1010101010")));
        when(detalleRepository.findByIdNumeroFacturaOrderByIdLinea("B-000001")).thenReturn(List.of());

        VentaResponse cajero = service.consultar("B-000001", "1001", false);
        assertEquals("Ana Torres", cajero.getClienteNombre());
        assertEquals("an****es@correo.test", cajero.getClienteCorreo());
        assertEquals("315*******", cajero.getClienteTelefono());
        assertEquals("Calle 10***", cajero.getClienteDireccion());

        VentaResponse admin = service.consultar("B-000001", "9999", true);
        assertEquals("ana.torres@correo.test", admin.getClienteCorreo());
        assertEquals("3150245878", admin.getClienteTelefono());
        assertEquals("Calle 10 # 5-20, Cali", admin.getClienteDireccion());
    }

    @Test
    void sinNotaDePedidoNoSeEnviaTelefonoNiDireccion() {
        Venta v = borrador("1001");
        v.setClienteDocumento("1010101010");
        when(ventaRepository.findById("B-000001")).thenReturn(Optional.of(v));
        when(clienteRepository.findById("1010101010")).thenReturn(Optional.of(cliente("1010101010")));
        when(detalleRepository.findByIdNumeroFacturaOrderByIdLinea("B-000001")).thenReturn(List.of());

        VentaResponse admin = service.consultar("B-000001", "9999", true);

        assertNotNull(admin.getClienteCorreo());
        assertNull(admin.getClienteTelefono());
        assertNull(admin.getClienteDireccion());
    }

    @Test
    void unCajeroNoConsultaLaVentaDeOtro() {
        when(ventaRepository.findById("F-000001")).thenReturn(Optional.of(venta("F-000001", "REGISTRADA", "1001")));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.consultar("F-000001", "2002", false));

        assertEquals(404, estado(e));
    }

    @Test
    void unaVentaInexistenteDa404() {
        when(ventaRepository.findById("F-999999")).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.consultar("F-999999", "1001", true));

        assertEquals(404, estado(e));
    }

    // ---- RF-026 agregar producto ----

    @Test
    void agregarUnProductoNuevoGuardaLaLineaConElPrecioDelMomento() {
        when(ventaRepository.findById("B-000001")).thenReturn(Optional.of(borrador("1001")));
        when(productoRepository.findById("P001")).thenReturn(Optional.of(producto("P001", 10, true, "5500.00")));
        when(detalleRepository.findByIdNumeroFacturaOrderByIdLinea("B-000001")).thenReturn(List.of());
        when(detalleRepository.ultimaLinea("B-000001")).thenReturn(0);

        VentaResponse r = service.agregarProducto("B-000001", pedido("P001", 2), "1001", false);

        ArgumentCaptor<DetalleVenta> captor = ArgumentCaptor.forClass(DetalleVenta.class);
        verify(detalleRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getId().getLinea());
        assertEquals(2, captor.getValue().getCantidad());
        assertEquals(0, new BigDecimal("5500.00").compareTo(captor.getValue().getPrecioUnitario()));
        assertEquals(0, new BigDecimal("11000.00").compareTo(r.getTotal()));
        assertTrue(r.getAdvertencias().isEmpty());
    }

    @Test
    void agregarElMismoProductoSumaLaCantidadEnLaMismaLinea() {
        DetalleVenta existente = linea("B-000001", 1, "P001", 2, "5500.00");
        when(ventaRepository.findById("B-000001")).thenReturn(Optional.of(borrador("1001")));
        when(productoRepository.findById("P001")).thenReturn(Optional.of(producto("P001", 10, true, "6000.00")));
        when(detalleRepository.findByIdNumeroFacturaOrderByIdLinea("B-000001")).thenReturn(List.of(existente));

        VentaResponse r = service.agregarProducto("B-000001", pedido("P001", 3), "1001", false);

        assertEquals(5, existente.getCantidad());
        // La línea conserva el precio con el que se agregó, aunque el producto ya cueste otra cosa.
        assertEquals(0, new BigDecimal("27500.00").compareTo(r.getTotal()));
        verify(detalleRepository, never()).ultimaLinea(anyString());
        assertEquals(1, r.getLineas().size());
    }

    @Test
    void agregarMasDeLoQueHayAvisaPeroNoImpideAgregar() {
        when(ventaRepository.findById("B-000001")).thenReturn(Optional.of(borrador("1001")));
        when(productoRepository.findById("P001")).thenReturn(Optional.of(producto("P001", 3, true, "5500.00")));
        when(detalleRepository.findByIdNumeroFacturaOrderByIdLinea("B-000001")).thenReturn(List.of());
        when(detalleRepository.ultimaLinea("B-000001")).thenReturn(0);

        VentaResponse r = service.agregarProducto("B-000001", pedido("P001", 5), "1001", false);

        assertEquals(1, r.getAdvertencias().size());
        assertTrue(r.getAdvertencias().get(0).contains("Stock insuficiente"));
        verify(detalleRepository).save(any(DetalleVenta.class));
    }

    @Test
    void agregarUnProductoInactivoDa400() {
        when(ventaRepository.findById("B-000001")).thenReturn(Optional.of(borrador("1001")));
        when(productoRepository.findById("P001")).thenReturn(Optional.of(producto("P001", 10, false, "5500.00")));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.agregarProducto("B-000001", pedido("P001", 1), "1001", false));

        assertEquals(400, estado(e));
        verify(detalleRepository, never()).save(any());
    }

    @Test
    void agregarUnProductoInexistenteDa404() {
        when(ventaRepository.findById("B-000001")).thenReturn(Optional.of(borrador("1001")));
        when(productoRepository.findById("X")).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.agregarProducto("B-000001", pedido("X", 1), "1001", false));

        assertEquals(404, estado(e));
    }

    @Test
    void soloQuienCreoElBorradorOUnAdminPuedeModificarlo() {
        when(ventaRepository.findById("B-000001")).thenReturn(Optional.of(borrador("1001")));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.agregarProducto("B-000001", pedido("P001", 1), "2002", false));
        assertEquals(403, estado(e));

        // Un administrador sí puede: llega hasta buscar el producto (que aquí no existe).
        when(productoRepository.findById("P001")).thenReturn(Optional.empty());
        ResponseStatusException e2 = assertThrows(ResponseStatusException.class,
                () -> service.agregarProducto("B-000001", pedido("P001", 1), "2002", true));
        assertEquals(404, estado(e2));
    }

    @Test
    void unaVentaYaRegistradaNoSeModifica() {
        when(ventaRepository.findById("F-000001")).thenReturn(Optional.of(venta("F-000001", "REGISTRADA", "1001")));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.agregarProducto("F-000001", pedido("P001", 1), "1001", false));

        assertEquals(400, estado(e));
        verifyNoInteractions(productoRepository);
    }

    // ---- RF-027 quitar una línea ----

    @Test
    void quitarUnaLineaRecalculaElTotal() {
        DetalleVenta quitada = linea("B-000001", 1, "P001", 2, "5500.00");
        DetalleVenta queda = linea("B-000001", 2, "P002", 1, "3000.00");
        when(ventaRepository.findById("B-000001")).thenReturn(Optional.of(borrador("1001")));
        when(detalleRepository.findById(new DetalleVentaId("B-000001", 1))).thenReturn(Optional.of(quitada));
        when(detalleRepository.findByIdNumeroFacturaOrderByIdLinea("B-000001")).thenReturn(List.of(queda));

        VentaResponse r = service.eliminarLinea("B-000001", 1, "1001", false);

        verify(detalleRepository).delete(quitada);
        assertEquals(0, new BigDecimal("3000.00").compareTo(r.getTotal()));
        assertEquals(1, r.getLineas().size());
    }

    @Test
    void quitarUnaLineaInexistenteDa404() {
        when(ventaRepository.findById("B-000001")).thenReturn(Optional.of(borrador("1001")));
        when(detalleRepository.findById(new DetalleVentaId("B-000001", 9))).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.eliminarLinea("B-000001", 9, "1001", false));

        assertEquals(404, estado(e));
        verify(detalleRepository, never()).delete(any());
    }

    // ---- cancelar el borrador ----

    @Test
    void cancelarBorraLasLineasYElBorradorEnEseOrden() {
        Venta v = borrador("1001");
        List<DetalleVenta> lineas = List.of(linea("B-000001", 1, "P001", 2, "5500.00"));
        when(ventaRepository.findById("B-000001")).thenReturn(Optional.of(v));
        when(detalleRepository.findByIdNumeroFacturaOrderByIdLinea("B-000001")).thenReturn(lineas);

        service.cancelar("B-000001", "1001", false);

        InOrder orden = inOrder(detalleRepository, ventaRepository);
        orden.verify(detalleRepository).deleteAll(lineas);
        orden.verify(ventaRepository).delete(v);
        verifyNoInteractions(productoRepository);   // un borrador no tocó el stock
    }

    @Test
    void soloSePuedeCancelarUnBorradorPropio() {
        when(ventaRepository.findById("B-000001")).thenReturn(Optional.of(borrador("1001")));
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.cancelar("B-000001", "2002", false));
        assertEquals(403, estado(e));

        when(ventaRepository.findById("F-000001")).thenReturn(Optional.of(venta("F-000001", "REGISTRADA", "1001")));
        ResponseStatusException e2 = assertThrows(ResponseStatusException.class,
                () -> service.cancelar("F-000001", "1001", false));
        assertEquals(400, estado(e2));
        verify(ventaRepository, never()).delete(any());
    }

    // ---- RF-025 registrar ----

    @Test
    void registrarAsignaElNumeroDeFacturaDescuentaElStockYBorraElBorrador() {
        Venta b = borrador("1001");
        b.setClienteDocumento("1010101010");
        List<DetalleVenta> lineas = List.of(linea("B-000001", 1, "P001", 2, "5500.00"),
                linea("B-000001", 2, "P002", 1, "3000.00"));
        Producto p1 = producto("P001", 10, true, "5500.00");
        Producto p2 = producto("P002", 5, true, "3000.00");
        when(ventaRepository.findById("B-000001")).thenReturn(Optional.of(b));
        when(detalleRepository.findByIdNumeroFacturaOrderByIdLinea("B-000001")).thenReturn(lineas);
        when(productoRepository.findById("P001")).thenReturn(Optional.of(p1));
        when(productoRepository.findById("P002")).thenReturn(Optional.of(p2));
        when(consecutivoRepository.bloquear("FACTURA")).thenReturn(Optional.of(consecutivo("FACTURA", 6)));
        when(detalleRepository.save(any(DetalleVenta.class))).thenAnswer(i -> i.getArgument(0));

        VentaResponse r = service.registrar("B-000001", "1001", false);

        assertEquals("F-000007", r.getNumeroFactura());
        assertEquals("REGISTRADA", r.getEstado());
        assertEquals(0, new BigDecimal("14000.00").compareTo(r.getTotal()));
        assertEquals(8, p1.getStock());
        assertEquals(4, p2.getStock());
        ArgumentCaptor<Venta> venta = ArgumentCaptor.forClass(Venta.class);
        verify(ventaRepository).save(venta.capture());
        assertEquals("F-000007", venta.getValue().getNumeroFactura());
        assertEquals("1010101010", venta.getValue().getClienteDocumento());
        // Las líneas se copian con el número definitivo y el borrador desaparece (líneas antes que la venta).
        ArgumentCaptor<DetalleVenta> copias = ArgumentCaptor.forClass(DetalleVenta.class);
        verify(detalleRepository, times(2)).save(copias.capture());
        assertEquals(List.of("F-000007", "F-000007"),
                copias.getAllValues().stream().map(d -> d.getId().getNumeroFactura()).toList());
        assertEquals(List.of(1, 2), copias.getAllValues().stream().map(d -> d.getId().getLinea()).toList());
        InOrder orden = inOrder(detalleRepository, ventaRepository);
        orden.verify(detalleRepository).deleteAll(lineas);
        orden.verify(ventaRepository).delete(b);
        verify(consecutivoRepository, never()).bloquear("BORRADOR");
    }

    @Test
    void registrarUnaVentaSinProductosDa400() {
        when(ventaRepository.findById("B-000001")).thenReturn(Optional.of(borrador("1001")));
        when(detalleRepository.findByIdNumeroFacturaOrderByIdLinea("B-000001")).thenReturn(List.of());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.registrar("B-000001", "1001", false));

        assertEquals(400, estado(e));
        verifyNoInteractions(consecutivoRepository);
    }

    @Test
    void registrarSinStockDa400NoGastaNumeroDeFacturaNiBorraElBorrador() {
        Venta b = borrador("1001");
        when(ventaRepository.findById("B-000001")).thenReturn(Optional.of(b));
        when(detalleRepository.findByIdNumeroFacturaOrderByIdLinea("B-000001"))
                .thenReturn(List.of(linea("B-000001", 1, "P001", 5, "5500.00")));
        when(productoRepository.findById("P001")).thenReturn(Optional.of(producto("P001", 3, true, "5500.00")));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.registrar("B-000001", "1001", false));

        assertEquals(400, estado(e));
        assertEquals("EN_PROCESO", b.getEstado());
        verify(productoRepository, never()).save(any());
        verify(ventaRepository, never()).save(any());
        verify(ventaRepository, never()).delete(any());
        verifyNoInteractions(consecutivoRepository);
    }

    @Test
    void registrarConUnProductoInactivoDa400() {
        when(ventaRepository.findById("B-000001")).thenReturn(Optional.of(borrador("1001")));
        when(detalleRepository.findByIdNumeroFacturaOrderByIdLinea("B-000001"))
                .thenReturn(List.of(linea("B-000001", 1, "P001", 1, "5500.00")));
        when(productoRepository.findById("P001")).thenReturn(Optional.of(producto("P001", 10, false, "5500.00")));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.registrar("B-000001", "1001", false));

        assertEquals(400, estado(e));
        verify(productoRepository, never()).save(any());
    }

    // ---- buscar ventas ----

    private Venta registrada(String numero, String cajero, String total, String estado) {
        Venta v = venta(numero, estado, cajero);
        v.setTotal(new BigDecimal(total));
        return v;
    }

    private void sinResultados() {
        when(ventaRepository.buscar(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(List.of());
    }

    @Test
    void elCajeroSoloBuscaEntreSusVentasAunqueIndiqueOtroCajero() {
        sinResultados();

        service.buscar("0003", null, null, null, null, "9999", "1001", false);

        ArgumentCaptor<String> cajero = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(ventaRepository).buscar(texto.capture(), any(), any(), any(), any(), cajero.capture(), any(Pageable.class));
        assertEquals("1001", cajero.getValue());
        assertEquals("0003", texto.getValue());
        // Los totales usan los mismos filtros que la lista.
        verify(ventaRepository).contar(eq("0003"), any(), any(), any(), any(), eq("1001"));
        verify(ventaRepository).sumarIngresos(eq("0003"), any(), any(), any(), any(), eq("1001"));
    }

    @Test
    void elAdminFiltraPorCajeroYEstadoYVeTodasSiNoIndicaCajero() {
        sinResultados();

        service.buscar(null, null, null, null, "ANULADA", "2002", "1001", true);
        service.buscar(null, null, null, null, null, "  ", "1001", true);

        ArgumentCaptor<String> estado = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> cajero = ArgumentCaptor.forClass(String.class);
        verify(ventaRepository, times(2)).buscar(any(), any(), any(), any(), estado.capture(), cajero.capture(), any(Pageable.class));
        assertEquals("ANULADA", estado.getAllValues().get(0));
        assertEquals("2002", cajero.getAllValues().get(0));
        assertNull(cajero.getAllValues().get(1));
    }

    @Test
    void laFechaFinalEsInclusivaYQuitaLosComodinesDelTexto() {
        sinResultados();

        service.buscar("%12_", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), "arr%", null, null, "1001", true);

        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<LocalDateTime> desde = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> hasta = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<String> producto = ArgumentCaptor.forClass(String.class);
        verify(ventaRepository).buscar(texto.capture(), desde.capture(), hasta.capture(), producto.capture(),
                any(), any(), any(Pageable.class));
        assertEquals("12", texto.getValue());
        assertEquals("arr", producto.getValue());
        assertEquals(LocalDateTime.of(2026, 11, 1, 0, 0), desde.getValue());
        assertEquals(LocalDateTime.of(2026, 12, 1, 0, 0), hasta.getValue());   // hasta el 30 incluido
    }

    @Test
    void buscarValidaElEstadoYElRangoDeFechas() {
        assertEquals(400, estado(assertThrows(ResponseStatusException.class,
                () -> service.buscar(null, null, null, null, "EN_PROCESO", null, "1001", true))));
        assertEquals(400, estado(assertThrows(ResponseStatusException.class,
                () -> service.buscar(null, LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 1), null, null, null,
                        "1001", true))));
        verifyNoInteractions(ventaRepository);
    }

    // Pregunta del usuario: ¿qué pasa con un rango enorme (por ejemplo, desde 2003)? El cajero no puede pedir más
    // de un año; el administrador sí (la lista igual se limita a 200 y los totales los suma la base de datos).
    @Test
    void elCajeroNoPuedeBuscarEnUnRangoDeMasDeUnAnoPeroElAdminSi() {
        sinResultados();
        LocalDate antiguo = LocalDate.of(2003, 12, 6);

        assertEquals(400, estado(assertThrows(ResponseStatusException.class,
                () -> service.buscar(null, antiguo, LocalDate.now(), null, null, null, "1001", false))));
        // Solo "desde": se cuenta hasta hoy.
        assertEquals(400, estado(assertThrows(ResponseStatusException.class,
                () -> service.buscar(null, antiguo, null, null, null, null, "1001", false))));
        verifyNoInteractions(ventaRepository);

        assertDoesNotThrow(() -> service.buscar(null, antiguo, LocalDate.now(), null, null, null, "1001", true));
        // Un año exacto sí se permite al cajero.
        assertDoesNotThrow(() -> service.buscar(null, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31), null, null, null,
                "1001", false));
    }

    @Test
    void losTotalesSonDeTodasLasVentasQueCumplenYNoSoloDeLasMostradas() {
        when(ventaRepository.contar(any(), any(), any(), any(), any(), any())).thenReturn(3000L);
        when(ventaRepository.sumarIngresos(any(), any(), any(), any(), any(), any())).thenReturn(new BigDecimal("86000.00"));
        List<Venta> mostradas = new ArrayList<>();
        for (int i = 0; i < VentaService.MAX_RESULTADOS; i++) {
            mostradas.add(registrada("F-" + i, "1001", "1000.00", "REGISTRADA"));
        }
        when(ventaRepository.buscar(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(mostradas);

        VentaListaResponse r = service.buscar(null, null, null, null, null, null, "1001", true);

        assertEquals(3000L, r.getTotal());
        assertEquals(0, new BigDecimal("86000.00").compareTo(r.getTotalIngresos()));
        assertEquals(VentaService.MAX_RESULTADOS, r.getVentas().size());
        assertTrue(r.isTruncado());
    }

    @Test
    void sinVentasLosIngresosSonCero() {
        sinResultados();

        VentaListaResponse r = service.buscar(null, null, null, null, null, null, "1001", false);

        assertEquals(0L, r.getTotal());
        assertEquals(0, BigDecimal.ZERO.compareTo(r.getTotalIngresos()));
        assertFalse(r.isTruncado());
    }

    // Pedido del usuario: al buscar por "1010" no debe aparecer quién es el cliente; el cajero lo confirma abriendo la
    // venta. La lista le muestra solo el documento enmascarado.
    @Test
    void laListaNoRevelaAlCajeroQuienEsElClienteYAlAdminSi() {
        Venta v = registrada("F-000002", "1001", "48000.00", "REGISTRADA");
        v.setClienteDocumento("1010101010");
        when(ventaRepository.contar(any(), any(), any(), any(), any(), any())).thenReturn(1L);
        when(ventaRepository.buscar(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(List.of(v));
        when(clienteRepository.findAllById(any())).thenReturn(List.of(cliente("1010101010")));

        var cajero = service.buscar("1010", null, null, null, null, null, "1001", false).getVentas().get(0);
        assertNull(cajero.getClienteNombre());
        assertEquals("10******10", cajero.getClienteDocumento());
        assertFalse(cajero.getConsumidorFinal());

        var admin = service.buscar("1010", null, null, null, null, null, "1001", true).getVentas().get(0);
        assertEquals("Ana Torres", admin.getClienteNombre());
        assertEquals("1010101010", admin.getClienteDocumento());
    }

    @Test
    void elConsumidorFinalSiSeMuestraEnLaListaDelCajero() {
        when(ventaRepository.contar(any(), any(), any(), any(), any(), any())).thenReturn(1L);
        when(ventaRepository.buscar(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(registrada("F-000003", "1001", "38000.00", "REGISTRADA")));

        var fila = service.buscar(null, null, null, null, null, null, "1001", false).getVentas().get(0);

        assertEquals("Consumidor final", fila.getClienteNombre());
        assertEquals("222222222222", fila.getClienteDocumento());
        assertTrue(fila.getConsumidorFinal());
    }

    @Test
    void unaNotaDePedidoDeUnContactoSeMuestraEnmascaradaAlCajero() {
        Venta v = registrada("F-000004", "1001", "9000.00", "REGISTRADA");
        v.setEsPedido(true);
        v.setContactoTelefono("3201112222");
        when(ventaRepository.contar(any(), any(), any(), any(), any(), any())).thenReturn(1L);
        when(ventaRepository.buscar(any(), any(), any(), any(), any(), any(), any(Pageable.class))).thenReturn(List.of(v));
        when(contactoRepository.findAllById(any())).thenReturn(List.of(contacto("3201112222")));

        var cajero = service.buscar(null, null, null, null, null, null, "1001", false).getVentas().get(0);
        assertTrue(cajero.getContactoPedido());
        assertNull(cajero.getClienteNombre());
        assertEquals("320*******", cajero.getClienteDocumento());

        var admin = service.buscar(null, null, null, null, null, null, "1001", true).getVentas().get(0);
        assertEquals("Carlos Pérez", admin.getClienteNombre());
        assertEquals("3201112222", admin.getClienteDocumento());
    }

    // ---- limpiar borradores ----

    @Test
    void limpiarBorradoresBorraPrimeroLasLineasYLuegoLosBorradores() {
        LocalDateTime limite = LocalDateTime.now();
        when(ventaRepository.borrarBorradoresAnterioresA(limite)).thenReturn(2);

        int borrados = service.limpiarBorradores(limite);

        assertEquals(2, borrados);
        InOrder orden = inOrder(detalleRepository, ventaRepository);
        orden.verify(detalleRepository).borrarLineasDeBorradoresAnterioresA(limite);
        orden.verify(ventaRepository).borrarBorradoresAnterioresA(limite);
    }
}
