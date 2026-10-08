package com.pos.categoria.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CategoriaResponse {
    private String nombre;
    private String descripcion;
}
