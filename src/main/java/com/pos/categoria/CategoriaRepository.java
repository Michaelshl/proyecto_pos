package com.pos.categoria;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, String> {

    boolean existsByNombreIgnoreCase(String nombre);
}
