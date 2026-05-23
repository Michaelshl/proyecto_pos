package com.pos.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UsuarioResponse {
    private Long id;
    private String nombre;
    private String apellido;
    private String username;
    private String email;
    private Boolean activo;
    private String rol;
}