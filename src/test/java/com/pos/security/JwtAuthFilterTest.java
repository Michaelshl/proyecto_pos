package com.pos.security;

import com.pos.rol.Rol;
import com.pos.usuario.Usuario;
import com.pos.usuario.UsuarioRepository;
import com.pos.auth.TokenRevocadoService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock private TokenRevocadoService tokenRevocadoService;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private FilterChain chain;

    private JwtUtil jwtUtil;
    private SecurityConfig.JwtAuthFilter filter;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "clave-de-prueba-de-al-menos-32-caracteres-xxxxxxxx");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 60_000L);
        filter = new SecurityConfig.JwtAuthFilter(jwtUtil, tokenRevocadoService, usuarioRepository);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private Usuario usuario(boolean activo, String rolNombre) {
        Rol rol = new Rol();
        rol.setNombre(rolNombre);
        Usuario u = new Usuario();
        u.setCedula("1001");
        u.setActivo(activo);
        u.setRol(rol);
        return u;
    }

    private void ejecutar(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (token != null) {
            request.addHeader("Authorization", "Bearer " + token);
        }
        filter.doFilter(request, new MockHttpServletResponse(), chain);
        verify(chain).doFilter(any(), any());
    }

    @Test
    void sinTokenNoAutentica() throws Exception {
        ejecutar(null);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void tokenValidoAutenticaConElRolDeLaBd() throws Exception {
        String token = jwtUtil.generarToken("1001", "CAJERO");
        when(tokenRevocadoService.estaRevocado(any())).thenReturn(false);
        // El rol en la BD cambió a ADMIN después de emitir el token.
        when(usuarioRepository.findById("1001")).thenReturn(Optional.of(usuario(true, "ADMIN")));

        ejecutar(token);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals("1001", auth.getName());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")));
        assertEquals(1, auth.getAuthorities().size());
    }

    @Test
    void usuarioInactivadoPierdeAccesoConTokenVigente() throws Exception {
        String token = jwtUtil.generarToken("1001", "ADMIN");
        when(tokenRevocadoService.estaRevocado(any())).thenReturn(false);
        when(usuarioRepository.findById("1001")).thenReturn(Optional.of(usuario(false, "ADMIN")));

        ejecutar(token);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void usuarioEliminadoNoAutentica() throws Exception {
        String token = jwtUtil.generarToken("1001", "ADMIN");
        when(tokenRevocadoService.estaRevocado(any())).thenReturn(false);
        when(usuarioRepository.findById("1001")).thenReturn(Optional.empty());

        ejecutar(token);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void tokenRevocadoNoAutentica() throws Exception {
        String token = jwtUtil.generarToken("1001", "ADMIN");
        when(tokenRevocadoService.estaRevocado(any())).thenReturn(true);

        ejecutar(token);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void tokenInvalidoNoAutentica() throws Exception {
        ejecutar("basura");

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verifyNoInteractions(usuarioRepository);
    }
}
