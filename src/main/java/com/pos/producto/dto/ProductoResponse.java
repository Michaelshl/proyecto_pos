package com.pos.producto.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ProductoResponse {
    private String codigo;
    private String nombre;
    private String descripcion;
    private BigDecimal precioVenta;
    private Integer stock;
    private Integer stockMinimo;
    private String categoria;
    private Boolean activo;
    private String motivoInactivacion;
}
