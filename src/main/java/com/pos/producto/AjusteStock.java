package com.pos.producto;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

// Historial de cada ajuste de stock (RF-019): quién, cuánto, por qué y cuándo.
@Data
@Entity
@Table(name = "ajustes_stock")
public class AjusteStock {

    @EmbeddedId
    private AjusteStockId id;

    @Column(name = "usuario_cedula", length = 20, nullable = false)
    private String usuarioCedula;

    // Positivo suma al stock, negativo resta.
    private Integer cantidad;

    @Column(length = 255, nullable = false)
    private String motivo;

    private LocalDateTime fecha;
}
