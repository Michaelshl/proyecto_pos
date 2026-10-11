package com.pos.producto;

import com.pos.producto.dto.AjusteStockRequest;
import com.pos.producto.dto.InactivarProductoRequest;
import com.pos.producto.dto.ProductoListaResponse;
import com.pos.producto.dto.ProductoRequest;
import com.pos.producto.dto.ProductoResponse;
import com.pos.producto.dto.ProductoSugerencia;
import com.pos.producto.dto.ProductoUpdateRequest;
import com.pos.producto.dto.StockResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    private final ProductoService productoService;

    public ProductoController(ProductoService productoService) {
        this.productoService = productoService;
    }

    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productoService.crear(request));
    }

    @GetMapping
    public ResponseEntity<ProductoListaResponse> listar(@RequestParam(required = false) String categoria,
                                                        @RequestParam(required = false) Boolean activo,
                                                        @RequestParam(required = false) String nombre) {
        return ResponseEntity.ok(productoService.listar(categoria, activo, nombre));
    }

    // Apoyo del autocompletado: pocas coincidencias con código y nombre.
    @GetMapping("/sugerencias")
    public ResponseEntity<List<ProductoSugerencia>> sugerencias(@RequestParam String q) {
        return ResponseEntity.ok(productoService.sugerir(q));
    }

    @GetMapping("/{codigo}")
    public ResponseEntity<ProductoResponse> consultar(@PathVariable String codigo) {
        return ResponseEntity.ok(productoService.consultar(codigo));
    }

    @PutMapping("/{codigo}")
    public ResponseEntity<ProductoResponse> actualizar(@PathVariable String codigo,
                                                       @Valid @RequestBody ProductoUpdateRequest request) {
        return ResponseEntity.ok(productoService.actualizar(codigo, request));
    }

    @PatchMapping("/{codigo}/inactivar")
    public ResponseEntity<ProductoResponse> inactivar(@PathVariable String codigo,
                                                      @RequestBody InactivarProductoRequest request) {
        return ResponseEntity.ok(productoService.inactivar(codigo, request));
    }

    @GetMapping("/{codigo}/stock")
    public ResponseEntity<StockResponse> consultarStock(@PathVariable String codigo) {
        return ResponseEntity.ok(productoService.consultarStock(codigo));
    }

    // El usuario que ajusta es el del token: JwtAuthFilter deja la cédula como nombre de la autenticación.
    @PostMapping("/{codigo}/ajustes")
    public ResponseEntity<StockResponse> ajustarStock(@PathVariable String codigo,
                                                      @Valid @RequestBody AjusteStockRequest request,
                                                      Authentication authentication) {
        return ResponseEntity.ok(productoService.ajustarStock(codigo, request, authentication.getName()));
    }
}
