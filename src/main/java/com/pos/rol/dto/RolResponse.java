package com.pos.rol.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RolResponse {
    private Long id;
    private String nombre;
    private String descripcion;
    private List<String> permisos;
}
