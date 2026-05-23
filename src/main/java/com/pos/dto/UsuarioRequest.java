package com.pos.dto;

import lombok.Data;

@Data
public class UsuarioRequest {
    private String nombre;
    private String apellido;
    private String username;
    private String email;
    private String password;
    private Long rolId;
}