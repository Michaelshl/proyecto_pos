package com.pos.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoriaRequest {
    @NotBlank
    private String nombre;
    @NotBlank
    private String descripcion;
}
