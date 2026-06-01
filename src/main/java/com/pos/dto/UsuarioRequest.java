package com.pos.dto;

import lombok.Data;

@Data
public class UsuarioRequest {
    private String nombre;
    private String apellido;
    private String username;
    private String email;
    private String password;  // llega en texto plano; el servicio la encripta con BCrypt antes de guardar
    private Long rolId;       // ID del rol a asignar (debe existir en la tabla roles)
}
