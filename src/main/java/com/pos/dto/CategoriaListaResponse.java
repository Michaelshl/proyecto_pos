package com.pos.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class CategoriaListaResponse {
    private int total;
    private List<CategoriaResponse> categorias;
}
