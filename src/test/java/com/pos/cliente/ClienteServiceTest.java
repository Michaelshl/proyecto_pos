package com.pos.cliente;

import com.pos.cliente.dto.ClienteRequest;
import com.pos.cliente.dto.ClienteResponse;
import com.pos.cliente.dto.ClienteUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    @Mock private ClienteRepository clienteRepository;

    private ClienteService service;

    @BeforeEach
    void setUp() {
        service = new ClienteService(clienteRepository);
    }

    private ClienteRequest solicitud(String numero) {
        ClienteRequest r = new ClienteRequest();
        r.setTipoDocumento("CC");
        r.setNumeroDocumento(numero);
        r.setNombre("Ana");
        r.setApellido("Lopez");
        r.setEmail("ana@correo.com");
        r.setTelefono("3001234567");
        r.setDireccion("Calle 1 # 2-3");
        return r;
    }

    private ClienteUpdateRequest cambio() {
        ClienteUpdateRequest u = new ClienteUpdateRequest();
        u.setTipoDocumento("CE");
        u.setNombre("  Ana María ");
        u.setApellido("Lopez");
        u.setEmail("nueva@correo.com");
        u.setTelefono("3109876543");
        u.setDireccion("Carrera 5 # 6-7");
        return u;
    }

    private Cliente existente(String numero) {
        Cliente c = new Cliente();
        c.setNumeroDocumento(numero);
        c.setTipoDocumento("CC");
        c.setNombre("Ana");
        c.setApellido("Lopez");
        c.setEmail("ana@correo.com");
        return c;
    }

    private int estado(ResponseStatusException e) {
        return HttpStatus.valueOf(e.getStatusCode().value()).value();
    }

    // ---- RF-020 crear ----

    @Test
    void crearGuardaElNumeroSinEspaciosComoLlave() {
        when(clienteRepository.existsById("1001")).thenReturn(false);

        ClienteResponse r = service.crear(solicitud("  1001  "));

        assertEquals("1001", r.getNumeroDocumento());
        assertEquals("CC", r.getTipoDocumento());
        assertEquals("Ana", r.getNombre());
        verify(clienteRepository).save(argThat(c -> c.getNumeroDocumento().equals("1001")));
    }

    @Test
    void crearConNumeroRepetidoDa400SinGuardar() {
        when(clienteRepository.existsById("1001")).thenReturn(true);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.crear(solicitud("1001")));

        assertEquals(400, estado(e));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    void crearGuardaLosOpcionalesVaciosComoNulos() {
        when(clienteRepository.existsById("1001")).thenReturn(false);
        ClienteRequest r = solicitud("1001");
        r.setEmail("  ");
        r.setTelefono("");
        r.setDireccion(null);

        ClienteResponse res = service.crear(r);

        assertNull(res.getEmail());
        assertNull(res.getTelefono());
        assertNull(res.getDireccion());
    }

    // ---- RF-021 actualizar ----

    @Test
    void actualizarCambiaLosDatosPeroNoElNumeroDeDocumento() {
        when(clienteRepository.findById("1001")).thenReturn(Optional.of(existente("1001")));

        ClienteResponse r = service.actualizar("1001", cambio());

        assertEquals("1001", r.getNumeroDocumento());
        assertEquals("CE", r.getTipoDocumento());
        assertEquals("Ana María", r.getNombre());
        assertEquals("nueva@correo.com", r.getEmail());
        assertEquals("Carrera 5 # 6-7", r.getDireccion());
        verify(clienteRepository).save(argThat(c -> c.getNumeroDocumento().equals("1001")));
    }

    @Test
    void actualizarClienteInexistenteDa404() {
        when(clienteRepository.findById("X")).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.actualizar("X", cambio()));

        assertEquals(404, estado(e));
        verify(clienteRepository, never()).save(any());
    }

    // ---- consultar ----

    @Test
    void consultarDevuelveLaFicha() {
        when(clienteRepository.findById("1001")).thenReturn(Optional.of(existente("1001")));

        assertEquals("Ana", service.consultar("1001").getNombre());
    }

    @Test
    void consultarInexistenteDa404() {
        when(clienteRepository.findById("X")).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class, () -> service.consultar("X"));

        assertEquals(404, estado(e));
    }

    // ---- autocompletado ----

    @Test
    void sugerirConMenosDeDosCaracteresNoConsulta() {
        assertTrue(service.sugerir("a").isEmpty());
        assertTrue(service.sugerir(null).isEmpty());
        verifyNoInteractions(clienteRepository);
    }

    @Test
    void sugerirQuitaLosComodinesDelLikeAntesDeConsultar() {
        when(clienteRepository.sugerir(eq("an"), any(Pageable.class))).thenReturn(List.of(existente("1001")));

        var r = service.sugerir("%a_n");

        assertEquals(1, r.size());
        assertEquals("1001", r.get(0).getNumeroDocumento());
        assertEquals("Lopez", r.get(0).getApellido());
    }
}
