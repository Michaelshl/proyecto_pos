package com.pos.producto;

import com.pos.auditoria.Auditoria;
import com.pos.categoria.Categoria;
import com.pos.categoria.CategoriaRepository;
import com.pos.producto.dto.AjusteStockRequest;
import com.pos.producto.dto.InactivarProductoRequest;
import com.pos.producto.dto.ProductoListaResponse;
import com.pos.producto.dto.ProductoRequest;
import com.pos.producto.dto.ProductoResponse;
import com.pos.producto.dto.ProductoSugerencia;
import com.pos.producto.dto.ProductoUpdateRequest;
import com.pos.producto.dto.StockResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class ProductoService {

    private static final int MAX_SUGERENCIAS = 8;

    // Un ajuste es "grande" si mueve más del 20 % del stock actual o 50 unidades o más: se registra como
    // alerta (WARN) en el log de auditoría. Toda salida de stock también deja alerta.
    private static final int AJUSTE_GRANDE_PORCENTAJE = 20;
    private static final int AJUSTE_GRANDE_UNIDADES = 50;

    private final ProductoRepository productoRepository;
    private final AjusteStockRepository ajusteStockRepository;
    private final CategoriaRepository categoriaRepository;

    public ProductoService(ProductoRepository productoRepository,
                           AjusteStockRepository ajusteStockRepository,
                           CategoriaRepository categoriaRepository) {
        this.productoRepository = productoRepository;
        this.ajusteStockRepository = ajusteStockRepository;
        this.categoriaRepository = categoriaRepository;
    }

    // RF-013
    public ProductoResponse crear(ProductoRequest request) {
        String codigo = request.getCodigo().trim();
        if (productoRepository.existsById(codigo)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ya existe un producto con ese código");
        }

        Producto producto = new Producto();
        producto.setCodigo(codigo);
        producto.setNombre(request.getNombre().trim());
        producto.setDescripcion(request.getDescripcion());
        producto.setPrecioVenta(request.getPrecioVenta());
        producto.setStock(request.getStockInicial());
        producto.setStockMinimo(request.getStockMinimo());
        producto.setActivo(request.getActivo() == null || request.getActivo());
        producto.setCategoria(buscarCategoria(request.getCategoria()));
        productoRepository.save(producto);
        Auditoria.accion("Producto creado", "código " + producto.getCodigo());
        return toResponse(producto);
    }

    // RF-014
    @Transactional
    public ProductoResponse actualizar(String codigo, ProductoUpdateRequest request) {
        Producto producto = obtener(codigo);
        producto.setNombre(request.getNombre().trim());
        producto.setDescripcion(request.getDescripcion());
        producto.setPrecioVenta(request.getPrecioVenta());
        producto.setStockMinimo(request.getStockMinimo());
        producto.setCategoria(buscarCategoria(request.getCategoria()));
        productoRepository.save(producto);
        Auditoria.accion("Producto actualizado", "código " + producto.getCodigo());
        return toResponse(producto);
    }

    // RF-015 (ficha por código). Con soloActivos (el cajero) un producto inactivo responde igual que uno que
    // no existe: el cajero no sabe que está inactivo.
    public ProductoResponse consultar(String codigo, boolean soloActivos) {
        Producto producto = obtener(codigo);
        if (soloActivos && !Boolean.TRUE.equals(producto.getActivo())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado");
        }
        return toResponse(producto);
    }

    // RF-015 (por nombre) y RF-016: todos los filtros son opcionales. Con soloActivos (el cajero) solo se ven
    // los productos activos y el filtro de estado se ignora: el cajero no elige entre activos e inactivos.
    public ProductoListaResponse listar(String categoria, Boolean activo, String nombre, boolean soloActivos) {
        Boolean filtroActivo = soloActivos ? Boolean.TRUE : activo;
        List<ProductoResponse> productos = productoRepository
                .filtrar(vacioANulo(categoria), filtroActivo, vacioANulo(limpiarComodines(nombre)))
                .stream()
                .map(this::toResponse)
                .toList();
        return new ProductoListaResponse(productos.size(), productos);
    }

    // RF-017: soft delete, igual que con los usuarios. Un producto inactivo no podrá venderse (Sprint 3).
    @Transactional
    public ProductoResponse inactivar(String codigo, InactivarProductoRequest request) {
        if (request.getMotivo() == null || request.getMotivo().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El motivo es obligatorio");
        }
        String motivo = request.getMotivo().trim();
        if (motivo.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El motivo no puede superar 500 caracteres");
        }

        Producto producto = obtener(codigo);
        if (Boolean.FALSE.equals(producto.getActivo())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El producto ya está inactivo");
        }
        producto.setActivo(false);
        producto.setMotivoInactivacion(motivo);
        productoRepository.save(producto);
        Auditoria.accion("Producto inactivado", "código " + producto.getCodigo() + ", motivo: " + motivo);
        return toResponse(producto);
    }

    // RF-018
    public StockResponse consultarStock(String codigo) {
        Producto p = obtener(codigo);
        return new StockResponse(p.getCodigo(), p.getNombre(), p.getStock(), p.getStockMinimo(),
                p.getStock() <= p.getStockMinimo());
    }

    // RF-019: cambia el stock y deja el movimiento en el historial, todo en una transacción.
    // Si otro ajuste o venta tocó el producto al mismo tiempo, @Version hace fallar este con 409.
    @Transactional
    public StockResponse ajustarStock(String codigo, AjusteStockRequest request, String cedulaUsuario) {
        if (request.getCantidad() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La cantidad del ajuste no puede ser cero");
        }

        MotivoAjuste tipo = MotivoAjuste.desde(request.getTipoMotivo())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El tipo de motivo no es válido. Use: " + Arrays.toString(MotivoAjuste.values())));
        if (!tipo.admite(request.getCantidad())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, mensajeSentido(tipo));
        }

        Producto producto = obtener(codigo);
        int stockAnterior = producto.getStock();
        int nuevoStock = stockAnterior + request.getCantidad();
        if (nuevoStock < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El ajuste dejaría el stock en negativo (stock actual: " + producto.getStock() + ")");
        }
        producto.setStock(nuevoStock);
        productoRepository.save(producto);

        AjusteStock ajuste = new AjusteStock();
        ajuste.setId(new AjusteStockId(producto.getCodigo(),
                ajusteStockRepository.ultimoConsecutivo(producto.getCodigo()) + 1));
        ajuste.setUsuarioCedula(cedulaUsuario);
        ajuste.setCantidad(request.getCantidad());
        ajuste.setTipoMotivo(tipo.name());
        ajuste.setMotivo(request.getMotivo().trim());
        ajuste.setFecha(LocalDateTime.now());
        ajusteStockRepository.save(ajuste);

        String detalle = "producto " + producto.getCodigo() + ", cantidad " + request.getCantidad()
                + ", stock nuevo " + nuevoStock + ", tipo " + tipo.name() + ", motivo: " + ajuste.getMotivo();
        Auditoria.accion("Ajuste de stock", detalle);
        // Las salidas y los ajustes grandes son lo que más conviene revisar: quedan también como alerta.
        if (request.getCantidad() < 0 || esAjusteGrande(stockAnterior, request.getCantidad())) {
            Auditoria.alerta("ALERTA ajuste de stock " + (request.getCantidad() < 0 ? "(salida)" : "(grande)"),
                    "usuario " + Auditoria.limpiar(cedulaUsuario) + ", " + detalle);
        }

        return new StockResponse(producto.getCodigo(), producto.getNombre(), nuevoStock,
                producto.getStockMinimo(), nuevoStock <= producto.getStockMinimo());
    }

    private static String mensajeSentido(MotivoAjuste tipo) {
        return switch (tipo.getSentido()) {
            case ENTRADA -> "El motivo «" + tipo.getDescripcion() + "» solo sirve para sumar stock (cantidad positiva)";
            case SALIDA -> "El motivo «" + tipo.getDescripcion() + "» solo sirve para restar stock (cantidad negativa)";
            case AMBOS -> "La cantidad del ajuste no puede ser cero";
        };
    }

    // Visible en el paquete para poder probarlo directamente.
    static boolean esAjusteGrande(int stockAnterior, int cantidad) {
        int magnitud = Math.abs(cantidad);
        return magnitud >= AJUSTE_GRANDE_UNIDADES
                || (stockAnterior > 0 && magnitud * 100 > stockAnterior * AJUSTE_GRANDE_PORCENTAJE);
    }

    // Lista corta para el autocompletado. Con menos de 2 caracteres no consulta.
    public List<ProductoSugerencia> sugerir(String texto, boolean soloActivos) {
        String limpio = limpiarComodines(texto);
        if (limpio.length() < 2) {
            return List.of();
        }
        return productoRepository.sugerir(limpio, soloActivos, PageRequest.of(0, MAX_SUGERENCIAS)).stream()
                .map(p -> new ProductoSugerencia(p.getCodigo(), p.getNombre()))
                .toList();
    }

    private Producto obtener(String codigo) {
        return productoRepository.findById(codigo.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Producto no encontrado"));
    }

    private Categoria buscarCategoria(String nombre) {
        return categoriaRepository.findById(nombre.trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría no existe"));
    }

    // Se quitan % y _ para que el texto escrito no funcione como comodín del LIKE.
    private static String limpiarComodines(String texto) {
        return texto == null ? "" : texto.replaceAll("[%_\\\\]", "").trim();
    }

    private static String vacioANulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private ProductoResponse toResponse(Producto p) {
        return ProductoResponse.builder()
                .codigo(p.getCodigo())
                .nombre(p.getNombre())
                .descripcion(p.getDescripcion())
                .precioVenta(p.getPrecioVenta())
                .stock(p.getStock())
                .stockMinimo(p.getStockMinimo())
                .categoria(p.getCategoria().getNombre())
                .activo(p.getActivo())
                .motivoInactivacion(p.getMotivoInactivacion())
                .build();
    }
}
