package com.pos.usuario;

import com.pos.rol.Rol;
import com.pos.rol.RolRepository;
import com.pos.usuario.dto.InactivarRequest;
import com.pos.usuario.dto.UsuarioRequest;
import com.pos.usuario.dto.UsuarioResponse;
import com.pos.usuario.dto.UsuarioUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private RolRepository rolRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private UsuarioService service;
    private Rol cajero;
    private Usuario existente;

    @BeforeEach
    void setUp() {
        service = new UsuarioService(usuarioRepository, rolRepository, encoder);

        cajero = new Rol();
        cajero.setNombre("CAJERO");

        existente = new Usuario();
        existente.setCedula("1001");
        existente.setNombre("Ana");
        existente.setApellido("Lopez");
        existente.setEmail("ana@pos.com");
        existente.setActivo(true);
        existente.setRol(cajero);
    }

    private UsuarioRequest solicitud() {
        UsuarioRequest r = new UsuarioRequest();
        r.setNombre("Juan");
        r.setApellido("Perez");
        r.setCedula("1020304050");
        r.setEmail("juan@pos.com");
        r.setPassword("password123");
        r.setRol("cajero");
        return r;
    }

    private HttpStatus estadoDe(ResponseStatusException e) {
        return HttpStatus.valueOf(e.getStatusCode().value());
    }

    @Test
    void crearGuardaConCedulaComoLlaveYClaveCifrada() {
        when(usuarioRepository.existsByEmail("juan@pos.com")).thenReturn(false);
        when(usuarioRepository.existsById("1020304050")).thenReturn(false);
        when(rolRepository.findById("CAJERO")).thenReturn(Optional.of(cajero));

        UsuarioResponse resp = service.crear(solicitud());

        assertEquals("1020304050", resp.getCedula());
        assertEquals("CAJERO", resp.getRol());
        assertTrue(resp.getActivo());
        verify(usuarioRepository).save(argThat(u ->
                "1020304050".equals(u.getCedula())
                        && !"password123".equals(u.getPassword())
                        && encoder.matches("password123", u.getPassword())));
    }

    @Test
    void crearConCedulaRepetidaDa400() {
        when(usuarioRepository.existsByEmail(any())).thenReturn(false);
        when(usuarioRepository.existsById("1020304050")).thenReturn(true);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.crear(solicitud()));

        assertEquals(HttpStatus.BAD_REQUEST, estadoDe(e));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void crearConEmailRepetidoDa400() {
        when(usuarioRepository.existsByEmail("juan@pos.com")).thenReturn(true);

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.crear(solicitud()));

        assertEquals(HttpStatus.BAD_REQUEST, estadoDe(e));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void crearConRolInexistenteDa400() {
        when(usuarioRepository.existsByEmail(any())).thenReturn(false);
        when(usuarioRepository.existsById(any())).thenReturn(false);
        when(rolRepository.findById("CAJERO")).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.crear(solicitud()));

        assertEquals(HttpStatus.BAD_REQUEST, estadoDe(e));
    }

    @Test
    void actualizarCambiaNombreApellidoYEmailPeroNoLaCedula() {
        when(usuarioRepository.findById("1001")).thenReturn(Optional.of(existente));
        when(usuarioRepository.existsByEmail("nuevo@pos.com")).thenReturn(false);
        UsuarioUpdateRequest r = new UsuarioUpdateRequest();
        r.setNombre("Ana Maria");
        r.setApellido("Gomez");
        r.setEmail("nuevo@pos.com");

        UsuarioResponse resp = service.actualizar("1001", r);

        assertEquals("1001", resp.getCedula());
        assertEquals("Ana Maria", resp.getNombre());
        assertEquals("nuevo@pos.com", resp.getEmail());
    }

    @Test
    void actualizarConEmailDeOtroUsuarioDa400() {
        when(usuarioRepository.findById("1001")).thenReturn(Optional.of(existente));
        when(usuarioRepository.existsByEmail("otro@pos.com")).thenReturn(true);
        UsuarioUpdateRequest r = new UsuarioUpdateRequest();
        r.setEmail("otro@pos.com");

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.actualizar("1001", r));

        assertEquals(HttpStatus.BAD_REQUEST, estadoDe(e));
    }

    @Test
    void actualizarUsuarioInexistenteDa404() {
        when(usuarioRepository.findById("0000")).thenReturn(Optional.empty());
        UsuarioUpdateRequest r = new UsuarioUpdateRequest();
        r.setEmail("x@pos.com");

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.actualizar("0000", r));

        assertEquals(HttpStatus.NOT_FOUND, estadoDe(e));
    }

    @Test
    void inactivarGuardaElMotivoYMarcaInactivo() {
        when(usuarioRepository.findById("1001")).thenReturn(Optional.of(existente));
        InactivarRequest r = new InactivarRequest();
        r.setMotivo("  Renuncia  ");

        service.inactivar("1001", r);

        assertFalse(existente.getActivo());
        assertEquals("Renuncia", existente.getMotivoInactivacion());
        verify(usuarioRepository).save(existente);
    }

    @Test
    void inactivarSinMotivoDa400() {
        InactivarRequest r = new InactivarRequest();
        r.setMotivo("   ");

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.inactivar("1001", r));

        assertEquals(HttpStatus.BAD_REQUEST, estadoDe(e));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void buscarPorCedulaUsaLaLlavePrimaria() {
        when(usuarioRepository.findById("1001")).thenReturn(Optional.of(existente));

        assertEquals("1001", service.buscar("1001", null).getCedula());
    }

    @Test
    void buscarPorEmailFunciona() {
        when(usuarioRepository.findByEmail("ana@pos.com")).thenReturn(Optional.of(existente));

        assertEquals("ana@pos.com", service.buscar(null, "ana@pos.com").getEmail());
    }

    @Test
    void buscarSinCriteriosDa400() {
        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.buscar(" ", null));

        assertEquals(HttpStatus.BAD_REQUEST, estadoDe(e));
    }

    @Test
    void buscarSinResultadosDa404() {
        when(usuarioRepository.findById("7777")).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.buscar("7777", null));

        assertEquals(HttpStatus.NOT_FOUND, estadoDe(e));
    }

    @Test
    void asignarRolCambiaElRolDelUsuario() {
        Rol admin = new Rol();
        admin.setNombre("ADMIN");
        when(usuarioRepository.findById("1001")).thenReturn(Optional.of(existente));
        when(rolRepository.findById("ADMIN")).thenReturn(Optional.of(admin));

        UsuarioResponse resp = service.asignarRol("1001", "admin");

        assertEquals("ADMIN", resp.getRol());
    }

    @Test
    void asignarRolInexistenteDa400() {
        when(usuarioRepository.findById("1001")).thenReturn(Optional.of(existente));
        when(rolRepository.findById("NOEXISTE")).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> service.asignarRol("1001", "NOEXISTE"));

        assertEquals(HttpStatus.BAD_REQUEST, estadoDe(e));
    }
}
