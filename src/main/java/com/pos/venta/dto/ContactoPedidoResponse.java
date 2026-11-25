package com.pos.venta.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

// El contacto guardado. El cajero recibe el celular, el correo y la dirección enmascarados; el administrador, completos.
@Data
@AllArgsConstructor
public class ContactoPedidoResponse {
    private String documento;
    private String telefono;
    private String nombre;
    private String correo;
    private String direccion;
}
