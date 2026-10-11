package com.pos.producto;

import com.pos.categoria.Categoria;
import com.pos.categoria.CategoriaRepository;
import com.pos.producto.dto.AjusteStockRequest;
import com.pos.producto.dto.InactivarProductoRequest;
import com.pos.producto.dto.ProductoRequest;
import com.pos.producto.dto.ProductoResponse;
import com.pos.producto.dto.ProductoUpdateRequest;
import com.pos.producto.dto.StockResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock private ProductoRepository productoRepository;
    @Mock private AjusteStockRepository ajusteStockRepository;
    @Mock private CategoriaRepository categoriaRepository;

    private ProductoService service;

    @BeforeEach
    void setUp() {
        service = new ProductoService(productoRepository, ajusteStockRepository, categoriaRepository);
    }

    private Categoria categoria(String nombre) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        return c;
    }

    private ProductoRequest solicitud(String codigo) {
        ProductoRequest r = new ProductoRequest();
        r.setCodigo(codigo);
        r.setNombre("Coca-Cola 1.5L");
        r.setDescripcion("Gaseosa");
        r.setPrecioVenta(new BigDecimal("5500.00"));
        r.setStockInicial(20);
        r.setStockMinimo(5);
        r.setCategoria("Bebidas");
        return r;
    }

    private Producto producto(String codigo, int stock, int minimo, boolean activo) {
        Producto p = new Producto();
        p.setCodigo(codigo);
        p.setNombre("Coca-Cola 1.5L");
        p.setPrecioVenta(new BigDecimal("5500.00"));
        p.setStock(stock);
        p.setStockMinimo(minimo);
        p.setActivo(activo);
        p.setCategoria(categoria("Bebidas"));
        return p;
    }

    private int estado(ResponseStatusException e) {
        return HttpStatus.valueOf(e.getStatusCode().value()).value();
    }

    // ---- RF-013 crear ----

    @Test
    void crearGuardaElCodigoSinEspaciosComoLlaveYNaceActivo() {
        when(productoRepository.existsById("P001")).thenReturn(false);
        when(categoriaRepository.findById("Bebidas")).thenReturn(Optional.of(categoria("Bebidas")));

        ProductoResponse r = service.crear(solicitud("  P001  "));

        assertEquals("P001", r.getCodigo());
        assertEquals(20, r.getStock());
        assertTrue(r.getActivo());
        verify(productoRepository).save(argThat(p -> p.getCodigo().equals("P001")));
    }

    @Test
    void crearConCodigoRepetidoDa400() {
        when(productoRepository.existsById("P001")).thenReturn(true);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.crear(solicitud("P001")));

        assertEquals(400, estado(e));
        verify(productoRepository, never()).save(any());
    }

    @Test
    void crearConCategoriaInexistenteDa400() {
        when(productoRepository.existsById("P001")).thenReturn(false);
        when(categoriaRepository.findById("Bebidas")).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.crear(solicitud("P001")));

        assertEquals(400, estado(e));
        verify(productoRepository, never()).save(any());
    }

    @Test
    void crearRespetaElEstadoInactivoSiSeEnvia() {
        when(productoRepository.existsById("P001")).thenReturn(false);
        when(categoriaRepository.findById("Bebidas")).thenReturn(Optional.of(categoria("Bebidas")));
        ProductoRequest r = solicitud("P001");
        r.setActivo(false);

        assertFalse(service.crear(r).getActivo());
    }

    // ---- RF-014 actualizar ----

    @Test
    void actualizarCambiaLosDatosPeroNoElStockNiElCodigo() {
        Producto existente = producto("P001", 20, 5, true);
        when(productoRepository.findById("P001")).thenReturn(Optional.of(existente));
        when(categoriaRepository.findById("Lacteos")).thenReturn(Optional.of(categoria("Lacteos")));
        ProductoUpdateRequest u = new ProductoUpdateRequest();
        u.setNombre("Leche 1L");
        u.setPrecioVenta(new BigDecimal("4200.00"));
        u.setStockMinimo(8);
        u.setCategoria("Lacteos");

        ProductoResponse r = service.actualizar("P001", u);

        assertEquals("P001", r.getCodigo());
        assertEquals("Leche 1L", r.getNombre());
        assertEquals("Lacteos", r.getCategoria());
        assertEquals(20, r.getStock());
        assertEquals(8, r.getStockMinimo());
    }

    @Test
    void actualizarProductoInexistenteDa404() {
        when(productoRepository.findById("X")).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.actualizar("X", new ProductoUpdateRequest()));

        assertEquals(404, estado(e));
    }

    // ---- RF-015 / RF-016 consultar y listar ----

    @Test
    void consultarInexistenteDa404() {
        when(productoRepository.findById("X")).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.consultar("X"));

        assertEquals(404, estado(e));
    }

    @Test
    void listarPasaLosFiltrosYDevuelveElTotal() {
        when(productoRepository.filtrar("Bebidas", true, "col"))
                .thenReturn(List.of(producto("P001", 20, 5, true), producto("P002", 3, 5, true)));

        var r = service.listar("Bebidas", true, "col");

        assertEquals(2, r.getTotal());
        assertEquals(2, r.getProductos().size());
    }

    @Test
    void listarConFiltrosVaciosLosTrataComoNulos() {
        when(productoRepository.filtrar(null, null, null)).thenReturn(List.of());

        var r = service.listar("  ", null, "%_");

        assertEquals(0, r.getTotal());
        verify(productoRepository).filtrar(null, null, null);
    }

    // ---- RF-017 inactivar ----

    @Test
    void inactivarGuardaElMotivoYMarcaInactivo() {
        Producto p = producto("P001", 20, 5, true);
        when(productoRepository.findById("P001")).thenReturn(Optional.of(p));
        InactivarProductoRequest req = new InactivarProductoRequest();
        req.setMotivo("  Descontinuado  ");

        ProductoResponse r = service.inactivar("P001", req);

        assertFalse(r.getActivo());
        assertEquals("Descontinuado", r.getMotivoInactivacion());
    }

    @Test
    void inactivarSinMotivoDa400() {
        InactivarProductoRequest req = new InactivarProductoRequest();
        req.setMotivo("   ");

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.inactivar("P001", req));

        assertEquals(400, estado(e));
        verify(productoRepository, never()).save(any());
    }

    @Test
    void inactivarUnProductoYaInactivoDa400() {
        when(productoRepository.findById("P001")).thenReturn(Optional.of(producto("P001", 1, 1, false)));
        InactivarProductoRequest req = new InactivarProductoRequest();
        req.setMotivo("otra vez");

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.inactivar("P001", req));

        assertEquals(400, estado(e));
    }

    @Test
    void inactivarInexistenteDa404() {
        when(productoRepository.findById("X")).thenReturn(Optional.empty());
        InactivarProductoRequest req = new InactivarProductoRequest();
        req.setMotivo("x");

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.inactivar("X", req));

        assertEquals(404, estado(e));
    }

    // ---- RF-018 consultar stock ----

    @Test
    void consultarStockAvisaCuandoElStockEsMenorOIgualAlMinimo() {
        when(productoRepository.findById("P001")).thenReturn(Optional.of(producto("P001", 5, 5, true)));
        when(productoRepository.findById("P002")).thenReturn(Optional.of(producto("P002", 6, 5, true)));

        assertTrue(service.consultarStock("P001").isAlerta());
        assertFalse(service.consultarStock("P002").isAlerta());
    }

    // ---- RF-019 ajustar stock ----

    private AjusteStockRequest ajuste(int cantidad, String motivo) {
        AjusteStockRequest r = new AjusteStockRequest();
        r.setCantidad(cantidad);
        r.setMotivo(motivo);
        return r;
    }

    @Test
    void ajustarSumaAlStockYRegistraElMovimientoConElSiguienteConsecutivo() {
        when(productoRepository.findById("P001")).thenReturn(Optional.of(producto("P001", 10, 5, true)));
        when(ajusteStockRepository.ultimoConsecutivo("P001")).thenReturn(3);

        StockResponse r = service.ajustarStock("P001", ajuste(15, "  Compra al proveedor  "), "1001");

        assertEquals(25, r.getStock());
        ArgumentCaptor<AjusteStock> captor = ArgumentCaptor.forClass(AjusteStock.class);
        verify(ajusteStockRepository).save(captor.capture());
        AjusteStock guardado = captor.getValue();
        assertEquals("P001", guardado.getId().getProductoCodigo());
        assertEquals(4, guardado.getId().getConsecutivo());
        assertEquals("1001", guardado.getUsuarioCedula());
        assertEquals(15, guardado.getCantidad());
        assertEquals("Compra al proveedor", guardado.getMotivo());
        assertNotNull(guardado.getFecha());
    }

    @Test
    void ajustarNegativoRestaYPrendeLaAlertaSiLlegaAlMinimo() {
        when(productoRepository.findById("P001")).thenReturn(Optional.of(producto("P001", 10, 5, true)));
        when(ajusteStockRepository.ultimoConsecutivo("P001")).thenReturn(0);

        StockResponse r = service.ajustarStock("P001", ajuste(-5, "Producto vencido"), "1001");

        assertEquals(5, r.getStock());
        assertTrue(r.isAlerta());
        verify(ajusteStockRepository).save(argThat(a -> a.getId().getConsecutivo() == 1));
    }

    @Test
    void ajustarQueDejaElStockEnNegativoDa400SinGuardarNada() {
        when(productoRepository.findById("P001")).thenReturn(Optional.of(producto("P001", 3, 5, true)));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.ajustarStock("P001", ajuste(-4, "Merma"), "1001"));

        assertEquals(400, estado(e));
        verify(productoRepository, never()).save(any());
        verify(ajusteStockRepository, never()).save(any());
    }

    @Test
    void ajustarPermiteDejarElStockExactamenteEnCero() {
        when(productoRepository.findById("P001")).thenReturn(Optional.of(producto("P001", 3, 0, true)));
        when(ajusteStockRepository.ultimoConsecutivo("P001")).thenReturn(0);

        assertEquals(0, service.ajustarStock("P001", ajuste(-3, "Inventario"), "1001").getStock());
    }

    @Test
    void ajustarConCantidadCeroDa400() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.ajustarStock("P001", ajuste(0, "Nada"), "1001"));

        assertEquals(400, estado(e));
        verify(productoRepository, never()).findById(anyString());
    }

    @Test
    void ajustarProductoInexistenteDa404() {
        when(productoRepository.findById("X")).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.ajustarStock("X", ajuste(1, "Alta"), "1001"));

        assertEquals(404, estado(e));
    }

    // ---- autocompletado ----

    @Test
    void sugerirConMenosDeDosCaracteresNoConsulta() {
        assertTrue(service.sugerir("a").isEmpty());
        assertTrue(service.sugerir(null).isEmpty());
        verifyNoInteractions(productoRepository);
    }

    @Test
    void sugerirQuitaLosComodinesDelLikeAntesDeConsultar() {
        when(productoRepository.sugerir(eq("co"), any(Pageable.class)))
                .thenReturn(List.of(producto("P001", 1, 1, true)));

        var r = service.sugerir("%c_o");

        assertEquals(1, r.size());
        assertEquals("P001", r.get(0).getCodigo());
    }
}
