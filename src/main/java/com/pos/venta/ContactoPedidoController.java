package com.pos.venta;

import com.pos.venta.dto.ContactoPedidoResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Contactos de pedido: quien hace una nota de pedido sin ser cliente registrado. El cajero escribe la cédula y
// ve el contacto con el celular, el correo y la dirección enmascarados; el administrador, completos.
@RestController
@RequestMapping("/api/contactos-pedido")
public class ContactoPedidoController {

    private final VentaService ventaService;

    public ContactoPedidoController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    @GetMapping("/{documento}")
    public ResponseEntity<ContactoPedidoResponse> consultar(@PathVariable String documento,
                                                            Authentication authentication) {
        boolean esAdmin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(ventaService.consultarContacto(documento, esAdmin));
    }
}
