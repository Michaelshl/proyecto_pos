package com.pos.dto;

import lombok.Data;

@Data
public class UsuarioUpdateRequest {
    private String nombre;
    private String apellido;
    private String email;
}