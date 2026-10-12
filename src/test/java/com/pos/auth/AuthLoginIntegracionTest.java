package com.pos.auth;

import com.pos.rol.RolRepository;
import com.pos.usuario.Usuario;
import com.pos.usuario.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// El login completo (JSON de entrada y de salida) contra una base H2 en memoria.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthLoginIntegracionTest {

    private static final String CLAVE = "clave-de-prueba-123";

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private BCryptPasswordEncoder encoder;

    @BeforeEach
    void usuarioDePrueba() {
        Usuario u = new Usuario();
        u.setCedula("7100001");
        u.setNombre("Laura");
        u.setApellido("Gómez");
        u.setEmail("laura.integracion@pos.test");
        u.setPassword(encoder.encode(CLAVE));
        u.setActivo(true);
        u.setRol(rolRepository.findById("CAJERO").orElseThrow());
        usuarioRepository.save(u);
    }

    @Test
    void elLoginDevuelveElTokenYElNombreParaMostrar() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cedula\":\"7100001\",\"password\":\"" + CLAVE + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(not(emptyString())))
                .andExpect(jsonPath("$.nombre").value("Laura Gómez"));
    }

    @Test
    void conClaveIncorrectaNoHayNombreNiToken() throws Exception {
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cedula\":\"7100001\",\"password\":\"otra-clave\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.nombre").doesNotExist());
    }
}
