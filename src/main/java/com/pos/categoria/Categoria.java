package com.pos.categoria;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "categorias")
public class Categoria {

    // El nombre de la categoría es la llave primaria.
    @Id
    @Column(length = 100)
    private String nombre;

    private String descripcion;
}
