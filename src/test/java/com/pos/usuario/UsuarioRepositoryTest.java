package com.pos.usuario;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Prueba la consulta del autocompletado contra una base real en memoria (H2).
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UsuarioRepositoryTest {

    @Autowired private UsuarioRepository repository;

    @BeforeEach
    void datos() {
        repository.save(usuario("1001", "Ana", "Lopez", "ana@pos.com"));
        repository.save(usuario("1002", "Luis", "Anaya", "luis@pos.com"));
        repository.save(usuario("2500", "Maria", "Ramirez", "maria@pos.com"));
    }

    private Usuario usuario(String cedula, String nombre, String apellido, String email) {
        Usuario u = new Usuario();
        u.setCedula(cedula);
        u.setNombre(nombre);
        u.setApellido(apellido);
        u.setEmail(email);
        u.setPassword("hash");
        u.setActivo(true);
        return u;
    }

    private List<String> cedulas(String texto) {
        return repository.sugerir(texto, PageRequest.of(0, 8)).stream()
                .map(Usuario::getCedula).toList();
    }

    @Test
    void encuentraPorInicioDeCedula() {
        assertEquals(List.of("1001", "1002"), cedulas("100").stream().sorted().toList());
    }

    @Test
    void laCedulaSeBuscaDesdeElInicioNoPorElMedio() {
        assertEquals(List.of(), cedulas("500"));
    }

    @Test
    void encuentraPorParteDelNombreOApellidoSinImportarMayusculas() {
        // "ana" está en el nombre de Ana y en el apellido de Anaya.
        assertEquals(List.of("1002", "1001"), cedulas("ANA"));
    }

    @Test
    void respetaElLimiteDeResultados() {
        assertEquals(1, repository.sugerir("100", PageRequest.of(0, 1)).size());
    }
}
