package com.pos.cliente.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

// Lo mínimo para reconocer al cliente en la lista de autocompletado.
@Data
@AllArgsConstructor
public class ClienteSugerencia {
    private String numeroDocumento;
    private String nombre;
    private String apellido;
}
