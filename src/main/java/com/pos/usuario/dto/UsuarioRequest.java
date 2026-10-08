package com.pos.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UsuarioRequest {
    private String nombre;
    private String apellido;

    @NotBlank(message = "La cédula es obligatoria")
    @Pattern(regexp = "\\d{6,12}", message = "La cédula debe tener entre 6 y 12 dígitos")
    private String cedula;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    private String password;  // llega en texto plano; el servicio la encripta con BCrypt antes de guardar

    @NotBlank(message = "El rol es obligatorio")
    private String rol;       // nombre del rol a asignar (debe existir en la tabla roles)
}
