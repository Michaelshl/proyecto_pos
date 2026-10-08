package com.pos.security;

import com.pos.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenRevocadoServiceTest {

    @Mock private TokenRevocadoRepository repository;
    @Mock private JwtUtil jwtUtil;
    @InjectMocks private TokenRevocadoService service;

    @Test
    void revocarLimpiaVencidosYGuardaElJti() {
        Instant expira = Instant.now().plusSeconds(600);
        when(jwtUtil.obtenerJti("tok")).thenReturn("jti-1");
        when(jwtUtil.obtenerExpiracion("tok")).thenReturn(expira);

        service.revocar("tok");

        InOrder orden = inOrder(repository);
        orden.verify(repository).deleteByExpiracionBefore(any(Instant.class));
        ArgumentCaptor<TokenRevocado> captor = ArgumentCaptor.forClass(TokenRevocado.class);
        orden.verify(repository).save(captor.capture());
        assertEquals("jti-1", captor.getValue().getJti());
        assertEquals(expira, captor.getValue().getExpiracion());
    }

    @Test
    void estaRevocadoConsultaPorJti() {
        when(repository.existsById("jti-1")).thenReturn(true);

        assertTrue(service.estaRevocado("jti-1"));
        assertFalse(service.estaRevocado("otro"));
    }
}
