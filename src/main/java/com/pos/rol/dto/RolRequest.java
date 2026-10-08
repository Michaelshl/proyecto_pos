package com.pos.rol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class RolRequest {
    @NotBlank
    private String nombre;
    @NotBlank
    private String descripcion;
    @NotEmpty
    private List<@NotBlank String> permisos;
}
