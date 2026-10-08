package com.pos.usuario;

import com.pos.rol.Rol;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "usuarios")
public class Usuario {

    // La cédula es la llave primaria: es un dato propio del usuario, único e inmutable.
    // Es String (no número) para no perder ceros iniciales.
    @Id
    @Column(length = 20)
    private String cedula;

    private String nombre;

    private String apellido;

    @Column(unique = true)
    private String email;

    // Aquí se guarda el hash BCrypt, NUNCA la contraseña en texto plano
    private String password;

    @Column(columnDefinition = "TINYINT(1)")
    private Boolean activo;

    // Se guarda al inactivar al usuario y se muestra cuando intenta iniciar sesión.
    @Column(length = 500)
    private String motivoInactivacion;

    // Relación muchos-a-uno: muchos usuarios pueden tener el mismo rol.
    // @JoinColumn indica que en la tabla "usuarios" habrá una columna "rol_nombre"
    // que es clave foránea hacia la llave primaria (nombre) de la tabla "roles"
    @ManyToOne
    @JoinColumn(name = "rol_nombre")
    private Rol rol;
}
