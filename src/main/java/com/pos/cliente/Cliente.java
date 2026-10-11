package com.pos.cliente;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "clientes")
public class Cliente {

    // El número de documento es la llave primaria: dato propio del cliente, único e inmutable.
    // Es String (no número) para no perder ceros iniciales ni el dígito de verificación de un NIT.
    @Id
    @Column(name = "numero_documento", length = 20)
    private String numeroDocumento;

    // CC, CE, TI, NIT o PP. Es una columna normal: dos tipos distintos con el mismo número son el mismo cliente.
    @Column(name = "tipo_documento", length = 20, nullable = false)
    private String tipoDocumento;

    @Column(length = 100, nullable = false)
    private String nombre;

    @Column(length = 100, nullable = false)
    private String apellido;

    @Column(length = 150)
    private String email;

    @Column(length = 20)
    private String telefono;

    private String direccion;
}
