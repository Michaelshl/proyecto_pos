package com.pos.cliente;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClienteRepository extends JpaRepository<Cliente, String> {

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
