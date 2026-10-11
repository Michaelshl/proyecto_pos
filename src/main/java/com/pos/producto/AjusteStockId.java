package com.pos.producto;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// Llave compuesta de ajustes_stock: el ajuste no tiene un dato propio que lo identifique solo,
// así que se identifica por producto + consecutivo (1, 2, 3... dentro de cada producto).
@Data
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class AjusteStockId implements Serializable {

    @Column(name = "producto_codigo", length = 30)
    private String productoCodigo;

    private Integer consecutivo;
}
