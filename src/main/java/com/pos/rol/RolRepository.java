package com.pos.rol;

import org.springframework.data.jpa.repository.JpaRepository;

// La llave primaria es el nombre del rol, así que findById/existsById ya sirven para buscarlo.
public interface RolRepository extends JpaRepository<Rol, String> {
}
