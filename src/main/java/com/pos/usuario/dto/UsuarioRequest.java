package com.pos.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UsuarioRequest {
    private String nombre;
    private String apellido;

    @NotBlank(message = "El username es obligatorio")
    private String username;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;  // llega en texto plano; el servicio la encripta con BCrypt antes de guardar

    @NotNull(message = "El rol es obligatorio")
    private Long rolId;       // ID del rol a asignar (debe existir en la tabla roles)
}
