package com.pos.venta.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class LineaVentaResponse {
    private Integer linea;
    private String productoCodigo;
    private String productoNombre;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    // cantidad x precio_unitario (se calcula, no se guarda).
    private BigDecimal subtotal;
}
