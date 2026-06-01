package com.pos.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;

    private String apellido;

    @Column(unique = true)
    private String username;

    @Column(unique = true)
    private String email;

    // Aquí se guarda el hash BCrypt, NUNCA la contraseña en texto plano
    private String password;

    private Boolean activo;

    // Relación muchos-a-uno: muchos usuarios pueden tener el mismo rol.
    // @JoinColumn indica que en la tabla "usuarios" habrá una columna "rol_id"
    // que es clave foránea hacia la tabla "roles"
    @ManyToOne
    @JoinColumn(name = "rol_id")
    private Rol rol;
}
