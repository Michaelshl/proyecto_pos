package com.pos.cliente;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// Prueba la consulta del autocompletado y el uso del documento como llave contra H2.
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class ClienteRepositoryTest {

    @Autowired private ClienteRepository repository;

    @BeforeEach
    void datos() {
        repository.save(cliente("1001", "CC", "Ana", "Lopez"));
        repository.save(cliente("1002", "CE", "Luis", "Anaya"));
        repository.save(cliente("900123456-7", "NIT", "Maria", "Ramirez"));
        repository.flush();
    }

    private Cliente cliente(String numero, String tipo, String nombre, String apellido) {
        Cliente c = new Cliente();
        c.setNumeroDocumento(numero);
        c.setTipoDocumento(tipo);
        c.setNombre(nombre);
        c.setApellido(apellido);
        return c;
    }

    private List<String> numeros(String texto) {
        return repository.sugerir(texto, PageRequest.of(0, 8)).stream()
                .map(Cliente::getNumeroDocumento).toList();
    }

    @Test
    void sugierePorDocumentoQueEmpiezaPorElTexto() {
        assertEquals(List.of("1001", "1002"), numeros("100").stream().sorted().toList());
        assertEquals(List.of("900123456-7"), numeros("9001"));
    }

    @Test
    void sugierePorNombreOApellidoQueLoContiene() {
        // "ana" está en el nombre de Ana y en el apellido de Anaya; se ordena por apellido: Anaya, Lopez.
        assertEquals(List.of("1002", "1001"), numeros("ana"));
    }

    @Test
    void sugerirNoDistingueMayusculas() {
        assertEquals(List.of("900123456-7"), numeros("RAMIREZ"));
    }

    @Test
    void sugerirRespetaElLimite() {
        assertEquals(1, repository.sugerir("100", PageRequest.of(0, 1)).size());
    }

    @Test
    void sinCoincidenciasDevuelveListaVacia() {
        assertTrue(numeros("zzz").isEmpty());
    }

    @Test
    void elDocumentoEsLaLlave() {
        assertTrue(repository.existsById("1001"));
        assertFalse(repository.existsById("9999"));
        assertEquals("NIT", repository.findById("900123456-7").orElseThrow().getTipoDocumento());
    }

    @Test
    void guardarOtraVezElMismoDocumentoActualizaEnLugarDeDuplicar() {
        repository.saveAndFlush(cliente("1001", "CE", "Ana Maria", "Lopez"));

        assertEquals(3, repository.count());
        assertEquals("Ana Maria", repository.findById("1001").orElseThrow().getNombre());
    }
}
