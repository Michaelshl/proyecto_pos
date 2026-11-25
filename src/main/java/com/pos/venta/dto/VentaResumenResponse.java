package com.pos.venta.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Una fila de la búsqueda de ventas.
@Data
@Builder
public class VentaResumenResponse {
    private String numeroFactura;
    private LocalDateTime fecha;
    private String estado;
    private BigDecimal total;
    private String cajeroCedula;
    // Para el cajero: el documento enmascarado y sin nombre (no se revela quién es el cliente hasta abrir la venta).
    private String clienteDocumento;
    private String clienteNombre;
    private Boolean esPedido;
    private Boolean consumidorFinal;
    // Nota de pedido de un contacto (no cliente registrado): clienteDocumento trae su celular.
    private Boolean contactoPedido;
}
