package com.pos.cliente.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

// RF-023: la lista de clientes y cuántos son (también sirve a la búsqueda por nombre del RF-022).
@Data
@AllArgsConstructor
public class ClienteListaResponse {
    private int total;
    private List<ClienteResponse> clientes;
}
