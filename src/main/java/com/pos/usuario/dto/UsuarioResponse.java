package com.pos.usuario.dto;

import com.pos.usuario.Usuario;
import lombok.Builder;
import lombok.Data;

// Nunca devolvemos el objeto Usuario directamente porque contiene el hash de la contraseña.
// Este DTO expone solo lo que el cliente necesita ver.
@Data
@Builder
public class UsuarioResponse {
    private String cedula;
    private String nombre;
    private String apellido;
    private String email;
    private Boolean activo;
    private String rol;
}
