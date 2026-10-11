package com.pos.producto.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

// Lo mínimo para reconocer el producto en la lista de autocompletado.
@Data
@AllArgsConstructor
public class ProductoSugerencia {
    private String codigo;
    private String nombre;
}
