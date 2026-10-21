package com.pos.auditoria;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuditoriaTest {

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sinSesionElActorEsAnonimo() {
        assertEquals("anónimo", Auditoria.actorActual());
    }

    @Test
    void conSesionElActorEsLaCedula() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("1000000002", null, List.of()));
        assertEquals("1000000002", Auditoria.actorActual());
    }

    @Test
    void limpiarQuitaSaltosDeLineaYCaracteresDeControl() {
        String limpio = Auditoria.limpiar("123\n2026-01-01 INFO falso\r\tfin");
        assertFalse(limpio.contains("\n"));
        assertFalse(limpio.contains("\r"));
        assertFalse(limpio.contains("\t"));
    }

    @Test
    void limpiarRecortaLosValoresLargos() {
        String limpio = Auditoria.limpiar("9".repeat(500));
        assertTrue(limpio.length() <= 61);
        assertTrue(limpio.endsWith("…"));
    }

    @Test
    void limpiarAceptaNulo() {
        assertEquals("", Auditoria.limpiar(null));
    }
}
