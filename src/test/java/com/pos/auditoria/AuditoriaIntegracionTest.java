package com.pos.auditoria;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.pos.rol.RolRepository;
import com.pos.usuario.Usuario;
import com.pos.usuario.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

// Que los eventos de seguridad dejen su línea en el log, y que nunca aparezcan contraseñas ni tokens.
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuditoriaIntegracionTest {

    private static final String CLAVE = "clave-secreta-de-prueba-987";

    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private BCryptPasswordEncoder encoder;

    private ListAppender<ILoggingEvent> lineas;
    private Logger logAuditoria;

    @BeforeEach
    void captarLog() {
        logAuditoria = (Logger) LoggerFactory.getLogger("AUDITORIA");
        lineas = new ListAppender<>();
        lineas.start();
        logAuditoria.addAppender(lineas);

        Usuario u = new Usuario();
        u.setCedula("7200001");
        u.setNombre("Pedro");
        u.setApellido("Ruiz");
        u.setEmail("pedro.auditoria@pos.test");
        u.setPassword(encoder.encode(CLAVE));
        u.setActivo(true);
        u.setRol(rolRepository.findById("CAJERO").orElseThrow());
        usuarioRepository.save(u);
    }

    @AfterEach
    void soltarLog() {
        logAuditoria.detachAppender(lineas);
    }

    private List<String> mensajes() {
        return lineas.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
    }

    private String login(String cedula, String clave) throws Exception {
        return mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cedula\":\"" + cedula + "\",\"password\":\"" + clave + "\"}"))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    void unLoginFallidoRegistraLaCedulaPeroNoLaClave() throws Exception {
        login("7200001", "clave-equivocada-555");

        assertEquals(1, lineas.list.size());
        assertEquals(Level.WARN, lineas.list.get(0).getLevel());
        String linea = mensajes().get(0);
        assertTrue(linea.contains("Login fallido"));
        assertTrue(linea.contains("7200001"));
        assertFalse(linea.contains("clave-equivocada-555"));
    }

    @Test
    void unLoginExitosoNoRegistraLaClaveNiElToken() throws Exception {
        String respuesta = login("7200001", CLAVE);
        String token = respuesta.replaceAll(".*\"token\":\"([^\"]+)\".*", "$1");

        String todo = String.join("\n", mensajes());
        assertTrue(todo.contains("Login exitoso"));
        assertFalse(todo.contains(CLAVE));
        assertFalse(todo.contains(token));
    }

    @Test
    void unaCedulaConSaltosDeLineaNoPuedeFalsificarLineasDelLog() throws Exception {
        login("7200001\\n2026-01-01 INFO Login exitoso: cédula '1'", "x");

        assertEquals(1, lineas.list.size());
        assertFalse(mensajes().get(0).contains("\n"));
    }

    @Test
    void unAccesoSinTokenRegistraElRechazo401() throws Exception {
        mockMvc.perform(get("/api/productos"));

        assertTrue(mensajes().stream().anyMatch(m -> m.contains("401") && m.contains("GET /api/productos")));
    }

    @Test
    @WithMockUser(username = "7200001", roles = "CAJERO")
    void unAccesoDenegadoRegistraElUsuarioLaRutaYElMetodo() throws Exception {
        mockMvc.perform(post("/api/productos").contentType(MediaType.APPLICATION_JSON).content("{}"));

        String linea = mensajes().stream().filter(m -> m.contains("403")).findFirst().orElseThrow();
        assertTrue(linea.contains("7200001"));
        assertTrue(linea.contains("POST /api/productos"));
    }

    @Test
    @WithMockUser(username = "7200001", roles = "ADMIN")
    void unaAccionSensibleRegistraQuienLaHizoYSobreQue() throws Exception {
        mockMvc.perform(post("/api/categorias").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Auditada\",\"descripcion\":\"prueba del log\"}"));

        assertTrue(mensajes().contains("Categoría creada por 7200001: nombre Auditada"));
    }
}
