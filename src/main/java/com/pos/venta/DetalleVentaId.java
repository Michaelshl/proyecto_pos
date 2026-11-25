package com.pos.venta;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// Llave compuesta de detalle_venta: una línea no tiene un dato propio que la identifique sola, así que se
// identifica por factura + número de línea (1, 2, 3... dentro de cada factura).
@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class DetalleVentaId implements Serializable {

    @Column(name = "numero_factura", length = 20)
    private String numeroFactura;

    private Integer linea;
}
