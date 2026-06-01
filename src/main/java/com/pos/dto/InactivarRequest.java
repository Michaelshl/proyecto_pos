package com.pos.dto;

import lombok.Data;

// El motivo es obligatorio: fuerza a justificar por qué se bloquea al usuario.
// En esta versión solo se valida; en el futuro debería persistirse como registro de auditoría.
@Data
public class InactivarRequest {
    private String motivo;
}
