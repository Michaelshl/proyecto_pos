package com.pos.usuario.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

// Lo mínimo para reconocer a la persona en la lista de autocompletado:
// cédula, primer nombre y primer apellido. No lleva correo, rol ni estado.
@Data
@AllArgsConstructor
public class UsuarioSugerencia {
    private String cedula;
    private String nombre;
    private String apellido;
}
