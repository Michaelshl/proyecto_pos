package com.pos.venta.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Data;

// RF-025: el cliente es opcional (una venta puede hacerse sin cliente). Los productos se agregan después.
@Data
public class VentaCrearRequest {

    // Sin documento, la venta es de "Consumidor final".
    @Size(max = 20, message = "El documento del cliente no puede superar 20 caracteres")
    private String clienteDocumento;

    // Nota de pedido: exige un cliente registrado o, si no lo es, un contacto de pedido (celular, nombre, correo y
    // dirección).
    private Boolean notaPedido;

    // Solo se usa en una nota de pedido sin cliente registrado.
    @Valid
    private ContactoPedidoRequest contacto;
}
