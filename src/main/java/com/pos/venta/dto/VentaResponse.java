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
    // Nota de pedido de alguien que no es cliente registrado: se factura a Consumidor final y aparte van los datos de
    // entrega (contactoNombre, contactoDocumento y clienteCorreo, clienteTelefono y clienteDireccion). El cajero ve
    // la cédula, el correo, el teléfono y la dirección enmascarados.
    private Boolean contactoPedido;
    private String contactoNombre;
    private String contactoDocumento;
    private String motivoAnulacion;
    // Solo en una venta anulada: cuándo y quién (cédula del administrador).
    private LocalDateTime fechaAnulacion;
    private String anuladoPor;
    private List<LineaVentaResponse> lineas;
    // Avisos que no impiden la operación (por ejemplo, stock insuficiente al agregar un producto, RF-026).
    private List<String> advertencias;
}
