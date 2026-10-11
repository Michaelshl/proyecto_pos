package com.pos.producto.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class StockResponse {
    private String codigo;
    private String nombre;
    private Integer stock;
    private Integer stockMinimo;
    // true cuando el stock es menor o igual al mínimo (RF-018).
    private boolean alerta;
}
