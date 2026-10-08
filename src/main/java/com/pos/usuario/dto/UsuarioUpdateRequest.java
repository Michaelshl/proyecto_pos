package com.pos.usuario.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

// Solo permite cambiar nombre, apellido y email.
// username, password y rol se omiten deliberadamente:
// - username: cambiarlo rompería tokens JWT activos que lo contienen como subject
// - password: tiene su propio flujo de cambio de contraseña (no implementado aún)
// - rol: cambiar roles requiere un proceso más controlado (no se hace en edición simple)
@Data
public class UsuarioUpdateRequest {
    private String nombre;
    private String apellido;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El email no tiene un formato válido")
    private String email;
}
