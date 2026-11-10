package com.pos.cliente;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClienteRepository extends JpaRepository<Cliente, String> {

    // Listado (RF-023) y búsqueda por documento o nombre (RF-022). Sin texto devuelve todos los clientes.
    // Se ordena por apellido (o por razón social, si es una empresa) y luego por nombre.
    @Query("""
            select c from Cliente c
            where :texto is null
               or c.numeroDocumento like concat(:texto, '%')
               or lower(c.nombre) like lower(concat('%', :texto, '%'))
               or lower(c.apellido) like lower(concat('%', :texto, '%'))
            order by coalesce(c.apellido, c.nombre), c.nombre
            """)
    List<Cliente> buscar(@Param("texto") String texto);

    // Autocompletado: el documento empieza por el texto, o el nombre / apellido lo contienen.
    @Query("""
            select c from Cliente c
            where c.numeroDocumento like concat(:texto, '%')
               or lower(c.nombre) like lower(concat('%', :texto, '%'))
               or lower(c.apellido) like lower(concat('%', :texto, '%'))
            order by c.apellido, c.nombre
            """)
    List<Cliente> sugerir(@Param("texto") String texto, Pageable limite);
}
