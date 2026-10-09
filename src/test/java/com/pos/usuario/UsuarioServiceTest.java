package com.pos.usuario;

import com.pos.rol.Rol;
import com.pos.rol.RolRepository;
import com.pos.usuario.dto.InactivarRequest;
import com.pos.usuario.dto.UsuarioRequest;
import com.pos.usuario.dto.UsuarioResponse;
import com.pos.usuario.dto.UsuarioSugerencia;
import com.pos.usuario.dto.UsuarioUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    void sugerirDevuelveCedulaPrimerNombreYPrimerApellido() {
        Usuario u = new Usuario();
        u.setCedula("1001");
        u.setNombre("Ana Maria");
        u.setApellido("Lopez Gomez");
        when(usuarioRepository.sugerir(eq("ana"), any())).thenReturn(List.of(u));

        List<UsuarioSugerencia> resultado = service.sugerir("  ana ");

        assertEquals(1, resultado.size());
        assertEquals("1001", resultado.get(0).getCedula());
        assertEquals("Ana", resultado.get(0).getNombre());
        assertEquals("Lopez", resultado.get(0).getApellido());
    }

    @Test
    void sugerirConMenosDeDosCaracteresNoConsultaLaBase() {
        assertTrue(service.sugerir("a").isEmpty());
        assertTrue(service.sugerir("  ").isEmpty());
        assertTrue(service.sugerir(null).isEmpty());
        verify(usuarioRepository, never()).sugerir(any(), any());
    }

    @Test
    void sugerirQuitaLosComodinesDelLike() {
        when(usuarioRepository.sugerir(eq("an"), any())).thenReturn(List.of());

        service.sugerir("%a_n");

        verify(usuarioRepository).sugerir(eq("an"), any());
    }

    @Test
    void sugerirSoloPideUnaPaginaCorta() {
        when(usuarioRepository.sugerir(any(), any())).thenReturn(List.of());

        service.sugerir("ana");

        verify(usuarioRepository).sugerir("ana", PageRequest.of(0, 8));
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
