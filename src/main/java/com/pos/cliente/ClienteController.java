package com.pos.cliente;

import com.pos.cliente.dto.ClienteRequest;
import com.pos.cliente.dto.ClienteResponse;
import com.pos.cliente.dto.ClienteSugerencia;
import com.pos.cliente.dto.ClienteUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @PostMapping
    public ResponseEntity<ClienteResponse> crear(@Valid @RequestBody ClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.crear(request));
    }

    // Apoyo del autocompletado: pocas coincidencias con documento, nombre y apellido.
    @GetMapping("/sugerencias")
    public ResponseEntity<List<ClienteSugerencia>> sugerencias(@RequestParam String q) {
        return ResponseEntity.ok(clienteService.sugerir(q));
    }

    @GetMapping("/{numeroDocumento}")
    public ResponseEntity<ClienteResponse> consultar(@PathVariable String numeroDocumento) {
        return ResponseEntity.ok(clienteService.consultar(numeroDocumento));
    }

    @PutMapping("/{numeroDocumento}")
    public ResponseEntity<ClienteResponse> actualizar(@PathVariable String numeroDocumento,
                                                      @Valid @RequestBody ClienteUpdateRequest request) {
        return ResponseEntity.ok(clienteService.actualizar(numeroDocumento, request));
    }
}
