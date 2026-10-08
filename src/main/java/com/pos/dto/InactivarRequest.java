package com.pos.dto;

import lombok.Data;

// El motivo es obligatorio: fuerza a justificar por qué se bloquea al usuario.
// Se guarda en el usuario y se muestra si intenta iniciar sesión estando inactivo.
@Data
public class InactivarRequest {
    private String motivo;
}
