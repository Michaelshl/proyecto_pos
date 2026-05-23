package com.pos.controller;

import com.pos.dto.InactivarRequest;
import com.pos.dto.UsuarioRequest;
import com.pos.dto.UsuarioResponse;
import com.pos.dto.UsuarioUpdateRequest;
import com.pos.service.UsuarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@RequestBody UsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.crear(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResponse> actualizar(@PathVariable Long id,
                                                      @RequestBody UsuarioUpdateRequest request) {
        return ResponseEntity.ok(usuarioService.actualizar(id, request));
    }

    @PatchMapping("/{id}/inactivar")
    public ResponseEntity<String> inactivar(@PathVariable Long id,
                                            @RequestBody InactivarRequest request) {
        return ResponseEntity.ok(usuarioService.inactivar(id, request));
    }
}