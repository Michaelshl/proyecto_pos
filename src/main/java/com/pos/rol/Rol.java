package com.pos.rol;

import jakarta.persistence.*;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@Table(name = "roles")
public class Rol {

    // El nombre del rol (ADMIN, CAJERO...) es la llave primaria. Se guarda en mayúsculas.
    @Id
    @Column(length = 50)
    private String nombre;

    private String descripcion;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rol_permisos", joinColumns = @JoinColumn(name = "rol_nombre"))
    @Column(name = "permiso")
    private List<String> permisos = new ArrayList<>();
}
