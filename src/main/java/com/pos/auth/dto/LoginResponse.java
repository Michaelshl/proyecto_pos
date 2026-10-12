package com.pos.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;

    // Nombre y apellido de quien inició sesión (o su cédula si no tiene): la interfaz lo muestra en el encabezado.
    private String nombre;
}
