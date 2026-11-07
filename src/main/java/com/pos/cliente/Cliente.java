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

    // CC, CE, TI, NIT, PP o PPT (permiso por protección temporal). Es una columna normal: dos tipos distintos con el mismo número son el mismo cliente.
    @Column(name = "tipo_documento", length = 20, nullable = false)
    private String tipoDocumento;

    // Nombre de la persona o, si el tipo de documento es NIT, la razón social de la empresa.
    @Column(length = 150, nullable = false)
    private String nombre;

    // Solo las personas tienen apellido: para un NIT es siempre NULL.
    @Column(length = 100)
    private String apellido;

    // Obligatorios al crear y al actualizar (los valida el DTO). La columna admite NULL solo por los clientes
    // que se crearon antes de que fueran obligatorios; a esos se les exige completarlos al actualizarlos.
    @Column(length = 150)
    private String email;

    @Column(length = 20)
    private String telefono;

    private String direccion;
}
