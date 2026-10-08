package com.pos.auth;

import com.pos.auth.TokenRevocado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface TokenRevocadoRepository extends JpaRepository<TokenRevocado, String> {

    void deleteByExpiracionBefore(Instant instante);
}
