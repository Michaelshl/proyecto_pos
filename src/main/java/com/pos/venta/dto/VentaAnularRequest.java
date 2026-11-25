package com.pos.venta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VentaAnularRequest {

    // Por qué se anula la venta (RF-029): queda guardado junto con quién y cuándo la anuló.
    @NotBlank(message = "El motivo es obligatorio")
    @Size(min = 5, max = 500, message = "El motivo debe tener entre 5 y 500 caracteres")
    private String motivo;
}
