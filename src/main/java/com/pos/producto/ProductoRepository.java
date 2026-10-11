package com.pos.producto;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductoRepository extends JpaRepository<Producto, String> {

    // Listado con filtros opcionales (RF-015, RF-016): categoría, estado y parte del nombre.
    @Query("""
            select p from Producto p
            where (:categoria is null or p.categoria.nombre = :categoria)
              and (:activo is null or p.activo = :activo)
              and (:nombre is null or lower(p.nombre) like lower(concat('%', :nombre, '%')))
            order by p.nombre
            """)
    List<Producto> filtrar(@Param("categoria") String categoria,
                           @Param("activo") Boolean activo,
                           @Param("nombre") String nombre);

    // Autocompletado: el código empieza por el texto, o el nombre lo contiene.
    @Query("""
            select p from Producto p
            where p.codigo like concat(:texto, '%')
               or lower(p.nombre) like lower(concat('%', :texto, '%'))
            order by p.nombre
            """)
    List<Producto> sugerir(@Param("texto") String texto, Pageable limite);
}
