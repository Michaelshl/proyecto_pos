package com.pos.venta.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

// RF-026: agregar un producto (por su código) a una venta en proceso.
@Data
public class LineaVentaRequest {

    @NotBlank(message = "El código del producto es obligatorio")
    private String codigo;

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    @Max(value = 100000, message = "La cantidad no puede superar 100000")
    private Integer cantidad;
}
