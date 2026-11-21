package com.pos.venta.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

// Resultado de buscar ventas. `total` y `totalIngresos` son de TODAS las ventas que cumplen la búsqueda (los calcula la
// base de datos); `ventas` trae solo las más recientes y `truncado` avisa cuando hay más de las que se muestran.
@Data
@AllArgsConstructor
public class VentaListaResponse {
    private long total;
    private BigDecimal totalIngresos;
    private boolean truncado;
    private List<VentaResumenResponse> ventas;
}
