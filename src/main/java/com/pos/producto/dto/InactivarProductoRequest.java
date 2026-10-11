package com.pos.producto.dto;

import lombok.Data;

// El motivo es obligatorio (RF-017); se guarda en el producto.
@Data
public class InactivarProductoRequest {
    private String motivo;
}
