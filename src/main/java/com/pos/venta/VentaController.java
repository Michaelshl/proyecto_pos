package com.pos.venta;

import com.pos.venta.dto.LineaVentaRequest;
import com.pos.venta.dto.VentaAnularRequest;
import com.pos.venta.dto.VentaCrearRequest;
import com.pos.venta.dto.VentaListaResponse;
import com.pos.venta.dto.VentaResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

// Ventas (Sprint 3). Las usan el cajero y el administrador. El usuario es el del token: JwtAuthFilter deja la
// cédula como nombre de la autenticación. El cajero solo ve y modifica sus ventas; el administrador, todas.
@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    // RF-025: crea el borrador (cliente opcional; sin cliente es Consumidor final) y devuelve su número (B-000001).
    @PostMapping
    public ResponseEntity<VentaResponse> crear(@Valid @RequestBody(required = false) VentaCrearRequest request,
                                               Authentication authentication) {
        VentaCrearRequest cuerpo = request != null ? request : new VentaCrearRequest();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ventaService.crear(cuerpo, authentication.getName(), esAdmin(authentication)));
    }

    // Buscar ventas registradas o anuladas: ?texto= (últimos dígitos de la factura o documento del cliente),
    // ?desde= y ?hasta= (fechas AAAA-MM-DD), ?producto= (código o nombre). El administrador también filtra por
    // ?estado= y ?cajero=. Base del RF-031.
    @GetMapping
    public ResponseEntity<VentaListaResponse> buscar(
            @RequestParam(required = false) String texto,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) String producto,
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String cajero,
            Authentication authentication) {
        return ResponseEntity.ok(ventaService.buscar(texto, desde, hasta, producto, estado, cajero,
                authentication.getName(), esAdmin(authentication)));
    }

    // RF-030: consultar una venta por su número.
    @GetMapping("/{numero}")
    public ResponseEntity<VentaResponse> consultar(@PathVariable String numero, Authentication authentication) {
        return ResponseEntity.ok(ventaService.consultar(numero, authentication.getName(), esAdmin(authentication)));
    }

    // RF-032: la factura de una venta registrada o anulada.
    @GetMapping("/{numero}/factura")
    public ResponseEntity<VentaResponse> factura(@PathVariable String numero, Authentication authentication) {
        return ResponseEntity.ok(ventaService.factura(numero, authentication.getName(), esAdmin(authentication)));
    }

    // RF-029: anular una venta registrada, con motivo. Solo el administrador (SecurityConfig).
    @PostMapping("/{numero}/anular")
    public ResponseEntity<VentaResponse> anular(@PathVariable String numero,
                                                @Valid @RequestBody VentaAnularRequest request,
                                                Authentication authentication) {
        return ResponseEntity.ok(ventaService.anular(numero, request.getMotivo(), authentication.getName()));
    }

    // Cancelar el borrador: no queda nada guardado.
    @DeleteMapping("/{numero}")
    public ResponseEntity<Void> cancelar(@PathVariable String numero, Authentication authentication) {
        ventaService.cancelar(numero, authentication.getName(), esAdmin(authentication));
        return ResponseEntity.noContent().build();
    }

    // RF-026: agregar un producto al borrador.
    @PostMapping("/{numero}/productos")
    public ResponseEntity<VentaResponse> agregarProducto(@PathVariable String numero,
                                                         @Valid @RequestBody LineaVentaRequest request,
                                                         Authentication authentication) {
        return ResponseEntity.ok(ventaService.agregarProducto(numero, request,
                authentication.getName(), esAdmin(authentication)));
    }

    // RF-027: quitar una línea del borrador.
    @DeleteMapping("/{numero}/productos/{linea}")
    public ResponseEntity<VentaResponse> eliminarLinea(@PathVariable String numero,
                                                       @PathVariable int linea,
                                                       Authentication authentication) {
        return ResponseEntity.ok(ventaService.eliminarLinea(numero, linea,
                authentication.getName(), esAdmin(authentication)));
    }

    // RF-025: registrar la venta: descuenta el stock y le asigna su número de factura (F-000001).
    @PostMapping("/{numero}/registrar")
    public ResponseEntity<VentaResponse> registrar(@PathVariable String numero, Authentication authentication) {
        return ResponseEntity.ok(ventaService.registrar(numero, authentication.getName(), esAdmin(authentication)));
    }

    private static boolean esAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
