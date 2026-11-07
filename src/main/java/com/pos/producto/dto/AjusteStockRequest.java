package com.pos.producto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AjusteStockRequest {

    // Positivo suma al stock, negativo resta. El cero no cambia nada y se rechaza.
    @NotNull(message = "La cantidad es obligatoria")
    private Integer cantidad;

    // Uno de los motivos de MotivoAjuste (INGRESO_COMPRA, DEVOLUCION_CLIENTE, DANO, VENCIMIENTO, PERDIDA,
    // CONTEO_FISICO). Que sea válido y coherente con el signo de la cantidad lo valida el servicio.
    @NotBlank(message = "El tipo de motivo es obligatorio")
    private String tipoMotivo;

    // Observación: explica el ajuste con palabras.
    @NotBlank(message = "El motivo es obligatorio")
    @Size(min = 5, max = 255, message = "El motivo debe tener entre 5 y 255 caracteres")
    private String motivo;
}
