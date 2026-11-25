package com.pos.venta;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

// Contador de números de factura (y más adelante de recibos). Una fila por tipo.
@Data
@Entity
@Table(name = "consecutivos")
public class Consecutivo {

    @Id
    @Column(length = 20)
    private String tipo;

    @Column(name = "ultimo_valor", nullable = false)
    private Long ultimoValor;
}
