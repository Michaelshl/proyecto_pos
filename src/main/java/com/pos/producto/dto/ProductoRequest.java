package com.pos.producto.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductoRequest {

    // El código es un SKU numérico (como un código de barras): solo dígitos, entre 4 y 14 (cubre los
    // códigos internos y los EAN-8, EAN-13 y GTIN-14). Es String para no perder ceros a la izquierda.
    @NotBlank(message = "El código es obligatorio")
    @Pattern(regexp = "\\d{4,14}", message = "El código (SKU) debe tener entre 4 y 14 dígitos, solo números")
    private String codigo;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    private String nombre;

    @Size(max = 255, message = "La descripción no puede superar 255 caracteres")
    private String descripcion;

    @NotNull(message = "El precio de venta es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio de venta debe ser mayor que cero")
    private BigDecimal precioVenta;

    @NotNull(message = "El stock inicial es obligatorio")
    @Min(value = 0, message = "El stock inicial no puede ser negativo")
    private Integer stockInicial;

    @NotNull(message = "El stock mínimo es obligatorio")
    @Min(value = 0, message = "El stock mínimo no puede ser negativo")
    private Integer stockMinimo;

    @NotBlank(message = "La categoría es obligatoria")
    private String categoria;

    // Estado inicial (RF-013). Si no se envía, el producto nace activo.
    private Boolean activo;
}
