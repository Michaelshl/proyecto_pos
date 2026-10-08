package com.pos.rol;

import com.pos.rol.dto.RolRequest;
import com.pos.rol.dto.RolResponse;
import com.pos.rol.RolService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RolController {

    private final RolService rolService;

    public RolController(RolService rolService) {
        this.rolService = rolService;
    }

    @GetMapping
    public ResponseEntity<List<RolResponse>> listar() {
        return ResponseEntity.ok(rolService.listar());
    }

    @PostMapping
    public ResponseEntity<RolResponse> crear(@Valid @RequestBody RolRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(rolService.crear(request));
    }
}
