package com.pos.venta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Encabezado de una venta (RF-025). Sus líneas están en detalle_venta. La llave es el número de factura,
// que genera la aplicación a partir de la tabla consecutivos.
@Data
@Entity
@Table(name = "ventas")
public class Venta {

    // Un borrador lleva un número propio (B-000001); la factura (F-000001) solo se asigna al registrar la venta.
    @Id
    @Column(name = "numero_factura", length = 20)
    private String numeroFactura;

    // Fecha de creación del borrador; la venta registrada lleva la fecha del momento en que se registró.
    @Column(nullable = false)
    private LocalDateTime fecha;

    // Valores de EstadoVenta.
    @Column(length = 20, nullable = false)
    private String estado;

    // Suma de cantidad x precio_unitario de las líneas.
    @Column(precision = 12, scale = 2, nullable = false)
    private BigDecimal total;

    @Column(name = "motivo_anulacion", length = 500)
    private String motivoAnulacion;

    @Column(name = "fecha_anulacion")
    private LocalDateTime fechaAnulacion;

    @Column(name = "anulado_por", length = 20)
    private String anuladoPor;

    @Column(name = "cajero_cedula", length = 20, nullable = false)
    private String cajeroCedula;

    // Opcional: una venta sin cliente se muestra como "Consumidor final".
    @Column(name = "cliente_documento", length = 20)
    private String clienteDocumento;

    // Nota de pedido: una venta que se entrega o se despacha; exige un cliente registrado.
    @Column(name = "es_pedido", nullable = false, columnDefinition = "TINYINT(1)")
    private Boolean esPedido = false;

    // Celular del contacto de pedido cuando la nota de pedido es de alguien que no es cliente registrado.
    @Column(name = "contacto_telefono", length = 20)
    private String contactoTelefono;
}
