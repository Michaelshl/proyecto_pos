package com.pos.usuario;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, String> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    // Autocompletado: la cédula empieza por el texto, o el nombre / apellido lo contienen.
    @Query("""
            select u from Usuario u
            where u.cedula like concat(:texto, '%')
               or lower(u.nombre) like lower(concat('%', :texto, '%'))
               or lower(u.apellido) like lower(concat('%', :texto, '%'))
            order by u.apellido, u.nombre
            """)
    List<Usuario> sugerir(@Param("texto") String texto, Pageable limite);
}
