package com.pos.producto;

import com.pos.categoria.Categoria;
import com.pos.categoria.CategoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Prueba las consultas de productos, la llave compuesta de ajustes y el bloqueo optimista
// contra una base real en memoria (H2).
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ProductoRepositoryTest {

    @Autowired private ProductoRepository productos;
    @Autowired private AjusteStockRepository ajustes;
    @Autowired private CategoriaRepository categorias;

    @BeforeEach
    void datos() {
        categorias.save(categoria("Bebidas"));
        categorias.save(categoria("Lacteos"));
        productos.save(producto("P001", "Coca-Cola 1.5L", "Bebidas", true));
        productos.save(producto("P002", "Jugo de naranja", "Bebidas", false));
        productos.save(producto("L100", "Leche entera", "Lacteos", true));
        productos.flush();
    }

    private Categoria categoria(String nombre) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        return c;
    }

    private Producto producto(String codigo, String nombre, String categoria, boolean activo) {
        Producto p = new Producto();
        p.setCodigo(codigo);
        p.setNombre(nombre);
        p.setPrecioVenta(new BigDecimal("1000.00"));
        p.setStock(10);
        p.setStockMinimo(2);
        p.setActivo(activo);
        p.setCategoria(categorias.findById(categoria).orElseThrow());
        return p;
    }

    private List<String> codigos(List<Producto> lista) {
        return lista.stream().map(Producto::getCodigo).toList();
    }

    // ---- filtrar (RF-015, RF-016) ----

    @Test
    void sinFiltrosDevuelveTodosOrdenadosPorNombre() {
        assertEquals(List.of("P001", "P002", "L100"), codigos(productos.filtrar(null, null, null)));
    }

    @Test
    void filtraPorCategoria() {
        assertEquals(List.of("L100"), codigos(productos.filtrar("Lacteos", null, null)));
    }

    @Test
    void filtraPorEstado() {
        assertEquals(List.of("P001", "L100"), codigos(productos.filtrar(null, true, null)));
        assertEquals(List.of("P002"), codigos(productos.filtrar(null, false, null)));
    }

    @Test
    void filtraPorParteDelNombreSinImportarMayusculas() {
        assertEquals(List.of("P001"), codigos(productos.filtrar(null, null, "COCA")));
    }

    @Test
    void combinaLosTresFiltros() {
        assertEquals(List.of("P001"), codigos(productos.filtrar("Bebidas", true, "cola")));
        assertTrue(productos.filtrar("Lacteos", false, null).isEmpty());
    }

    // ---- sugerir ----

    @Test
    void sugiereporCodigoQueEmpiezaPorElTexto() {
        assertEquals(List.of("L100"), codigos(productos.sugerir("L1", PageRequest.of(0, 8))));
    }

    @Test
    void sugierePorNombreQueContieneElTexto() {
        assertEquals(List.of("P002"), codigos(productos.sugerir("naranja", PageRequest.of(0, 8))));
    }

    @Test
    void sugerirRespetaElLimite() {
        assertEquals(1, productos.sugerir("P0", PageRequest.of(0, 1)).size());
    }

    // ---- ajustes_stock (llave compuesta) ----

    private AjusteStock ajuste(String codigo, int consecutivo) {
        AjusteStock a = new AjusteStock();
        a.setId(new AjusteStockId(codigo, consecutivo));
        a.setUsuarioCedula("1001");
        a.setCantidad(5);
        a.setMotivo("Compra");
        a.setFecha(LocalDateTime.now());
        return a;
    }

    @Test
    void ultimoConsecutivoEsCeroSiElProductoNoTieneAjustes() {
        assertEquals(0, ajustes.ultimoConsecutivo("P001"));
    }

    @Test
    void ultimoConsecutivoEsIndependientePorProducto() {
        ajustes.save(ajuste("P001", 1));
        ajustes.save(ajuste("P001", 2));
        ajustes.save(ajuste("L100", 1));
        ajustes.flush();

        assertEquals(2, ajustes.ultimoConsecutivo("P001"));
        assertEquals(1, ajustes.ultimoConsecutivo("L100"));
        assertEquals(0, ajustes.ultimoConsecutivo("P002"));
    }

    @Test
    void laLlaveCompuestaPermiteElMismoConsecutivoEnProductosDistintos() {
        ajustes.save(ajuste("P001", 1));
        ajustes.save(ajuste("L100", 1));
        ajustes.flush();

        assertEquals(2, ajustes.count());
        assertTrue(ajustes.existsById(new AjusteStockId("L100", 1)));
    }

    // ---- bloqueo optimista ----

    @Test
    void laVersionAvanzaConCadaCambio() {
        Producto p = productos.findById("P001").orElseThrow();
        Long antes = p.getVersion();
        p.setStock(p.getStock() + 1);
        productos.saveAndFlush(p);

        assertEquals(antes + 1, p.getVersion());
    }

    @Test
    void guardarUnaCopiaDesactualizadaFallaPorBloqueoOptimista() {
        Producto vieja = productos.findById("P001").orElseThrow();
        Producto copia = new Producto();
        copia.setCodigo(vieja.getCodigo());
        copia.setNombre(vieja.getNombre());
        copia.setPrecioVenta(vieja.getPrecioVenta());
        copia.setStock(99);
        copia.setStockMinimo(vieja.getStockMinimo());
        copia.setActivo(true);
        copia.setCategoria(vieja.getCategoria());
        copia.setVersion(vieja.getVersion() + 5);   // versión que no corresponde a la guardada

        assertThrows(ObjectOptimisticLockingFailureException.class, () -> productos.saveAndFlush(copia));
    }
}
