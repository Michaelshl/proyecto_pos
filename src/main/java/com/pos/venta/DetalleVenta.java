package com.pos.venta;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;

// Una línea de la venta (RF-026 y RF-027): un producto, su cantidad y el precio que tenía al venderse.
// El subtotal no se guarda: es cantidad x precio_unitario.
@Data
@Entity
@Table(name = "detalle_venta")
public class DetalleVenta {

    @EmbeddedId
    private DetalleVentaId id;

    @Column(name = "producto_codigo", length = 30, nullable = false)
    private String productoCodigo;

    @Column(nullable = false)
    private Integer cantidad;

    // Precio del momento: si el producto cambia de precio después, la venta anterior no se altera.
    @Column(name = "precio_unitario", precision = 12, scale = 2, nullable = false)
    private BigDecimal precioUnitario;
}
