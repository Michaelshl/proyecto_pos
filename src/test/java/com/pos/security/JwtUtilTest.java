package com.pos.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private static final String SECRET = "clave-de-prueba-de-al-menos-32-caracteres-xxxxxxxx";

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", SECRET);
        ReflectionTestUtils.setField(jwtUtil, "expiration", 60_000L);
    }

    @Test
    void tokenGeneradoEsValidoYConservaDatos() {
        String token = jwtUtil.generarToken("1001", "ADMIN");

        assertTrue(jwtUtil.tokenValido(token));
        assertEquals("1001", jwtUtil.obtenerCedula(token));
        assertEquals("ADMIN", jwtUtil.obtenerRol(token));
        assertNotNull(jwtUtil.obtenerJti(token));
    }

    @Test
    void cadaTokenTieneJtiDistinto() {
        assertNotEquals(
                jwtUtil.obtenerJti(jwtUtil.generarToken("1001", "ADMIN")),
                jwtUtil.obtenerJti(jwtUtil.generarToken("1001", "ADMIN")));
    }

    @Test
    void tokenExpiradoNoEsValido() {
        ReflectionTestUtils.setField(jwtUtil, "expiration", -1000L);

        assertFalse(jwtUtil.tokenValido(jwtUtil.generarToken("1001", "ADMIN")));
    }

    @Test
    void tokenConFirmaAlteradaNoEsValido() {
        String token = jwtUtil.generarToken("1001", "ADMIN");
        String alterado = token.substring(0, token.length() - 2) + "xx";

        assertFalse(jwtUtil.tokenValido(alterado));
    }

    @Test
    void tokenFirmadoConOtraClaveNoEsValido() {
        JwtUtil otro = new JwtUtil();
        ReflectionTestUtils.setField(otro, "secret", "otra-clave-distinta-de-al-menos-32-caracteres-yy");
        ReflectionTestUtils.setField(otro, "expiration", 60_000L);

        assertFalse(jwtUtil.tokenValido(otro.generarToken("1001", "ADMIN")));
    }

    @Test
    void tokenMalformadoNoEsValido() {
        assertFalse(jwtUtil.tokenValido("esto-no-es-un-jwt"));
    }

    @Test
    void secretoCortoHaceFallarElArranque() {
        ReflectionTestUtils.setField(jwtUtil, "secret", "corto");

        assertThrows(IllegalStateException.class, jwtUtil::validarSecreto);
    }
}
