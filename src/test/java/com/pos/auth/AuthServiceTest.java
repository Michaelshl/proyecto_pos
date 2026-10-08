package com.pos.auth;

import com.pos.auth.dto.LoginRequest;
import com.pos.rol.Rol;
import com.pos.usuario.Usuario;
import com.pos.usuario.UsuarioRepository;
import com.pos.security.JwtUtil;
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
class AuthServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private TokenRevocadoService tokenRevocadoService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private AuthService authService;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        authService = new AuthService(usuarioRepository, encoder, jwtUtil, tokenRevocadoService);

        Rol rol = new Rol();
        rol.setNombre("ADMIN");
        usuario = new Usuario();
        usuario.setUsername("ana");
        usuario.setPassword(encoder.encode("clave-correcta"));
        usuario.setActivo(true);
        usuario.setRol(rol);
    }

    private LoginRequest request(String username, String password) {
        LoginRequest r = new LoginRequest();
        r.setUsername(username);
        r.setPassword(password);
        return r;
    }

    private HttpStatus estadoDe(ResponseStatusException e) {
        return HttpStatus.valueOf(e.getStatusCode().value());
    }

    @Test
    void loginCorrectoDevuelveToken() {
        when(usuarioRepository.findByUsername("ana")).thenReturn(Optional.of(usuario));
        when(jwtUtil.generarToken("ana", "ADMIN")).thenReturn("el-token");

        assertEquals("el-token", authService.login(request("ana", "clave-correcta")).getToken());
    }

    @Test
    void usuarioInexistenteDa401() {
        when(usuarioRepository.findByUsername("nadie")).thenReturn(Optional.empty());

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> authService.login(request("nadie", "x")));
        assertEquals(HttpStatus.UNAUTHORIZED, estadoDe(e));
        verify(jwtUtil, never()).generarToken(any(), any());
    }

    @Test
    void claveIncorrectaDa401() {
        when(usuarioRepository.findByUsername("ana")).thenReturn(Optional.of(usuario));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> authService.login(request("ana", "mala")));
        assertEquals(HttpStatus.UNAUTHORIZED, estadoDe(e));
    }

    @Test
    void usuarioInactivoConClaveIncorrectaDa401NoRevelaEstado() {
        usuario.setActivo(false);
        when(usuarioRepository.findByUsername("ana")).thenReturn(Optional.of(usuario));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> authService.login(request("ana", "mala")));
        assertEquals(HttpStatus.UNAUTHORIZED, estadoDe(e));
    }

    @Test
    void usuarioInactivoConClaveCorrectaDa403() {
        usuario.setActivo(false);
        when(usuarioRepository.findByUsername("ana")).thenReturn(Optional.of(usuario));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> authService.login(request("ana", "clave-correcta")));
        assertEquals(HttpStatus.FORBIDDEN, estadoDe(e));
    }

    @Test
    void usuarioInactivoVeElMotivoAlIniciarSesion() {
        usuario.setActivo(false);
        usuario.setMotivoInactivacion("Renuncia");
        when(usuarioRepository.findByUsername("ana")).thenReturn(Optional.of(usuario));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> authService.login(request("ana", "clave-correcta")));
        assertEquals(HttpStatus.FORBIDDEN, estadoDe(e));
        assertEquals("Usuario inactivo. Motivo: Renuncia", e.getReason());
    }

    @Test
    void estadoActivoNuloSeTrataComoInactivo() {
        usuario.setActivo(null);
        when(usuarioRepository.findByUsername("ana")).thenReturn(Optional.of(usuario));

        ResponseStatusException e = assertThrows(ResponseStatusException.class,
                () -> authService.login(request("ana", "clave-correcta")));
        assertEquals(HttpStatus.FORBIDDEN, estadoDe(e));
    }

    @Test
    void logoutRevocaElToken() {
        authService.logout("tok");

        verify(tokenRevocadoService).revocar("tok");
    }
}
