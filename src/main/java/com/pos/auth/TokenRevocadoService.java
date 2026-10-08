package com.pos.auth;

import com.pos.auth.TokenRevocado;
import com.pos.auth.TokenRevocadoRepository;
import com.pos.security.JwtUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class TokenRevocadoService {

    private final TokenRevocadoRepository repository;
    private final JwtUtil jwtUtil;

    public TokenRevocadoService(TokenRevocadoRepository repository, JwtUtil jwtUtil) {
        this.repository = repository;
        this.jwtUtil = jwtUtil;
    }

    @Transactional
    public void revocar(String token) {
        repository.deleteByExpiracionBefore(Instant.now());

        TokenRevocado revocado = new TokenRevocado();
        revocado.setJti(jwtUtil.obtenerJti(token));
        revocado.setExpiracion(jwtUtil.obtenerExpiracion(token));
        repository.save(revocado);
    }

    public boolean estaRevocado(String jti) {
        return repository.existsById(jti);
    }
}
