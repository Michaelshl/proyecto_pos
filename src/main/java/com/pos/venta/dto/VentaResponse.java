package com.pos.venta.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class VentaResponse {
    private String numeroFactura;
    private LocalDateTime fecha;
    private String estado;
    private BigDecimal total;
    private String cajeroCedula;
    private String clienteDocumento;
    private String clienteNombre;
    // Sin cliente: la venta es de "Consumidor final" (documento 222222222222).
    private Boolean consumidorFinal;
    // Correo donde llegan las facturas del cliente. El cajero lo ve enmascarado; el administrador, completo.
    private String clienteCorreo;
    private Boolean esPedido;
    // Solo en una nota de pedido (y también enmascarados para el cajero).
    private String clienteTelefono;
    private String clienteDireccion;
    // Nota de pedido de alguien que no es cliente registrado: sus datos son los del contacto de pedido
    // (clienteNombre, clienteCorreo, clienteTelefono y clienteDireccion, con la misma máscara para el cajero).
    private Boolean contactoPedido;
    private String motivoAnulacion;
    private List<LineaVentaResponse> lineas;
    // Avisos que no impiden la operación (por ejemplo, stock insuficiente al agregar un producto, RF-026).
    private List<String> advertencias;
}
