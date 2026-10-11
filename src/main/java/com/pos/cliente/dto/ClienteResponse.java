package com.pos.cliente.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClienteResponse {
    private String tipoDocumento;
    private String numeroDocumento;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private String direccion;
}
