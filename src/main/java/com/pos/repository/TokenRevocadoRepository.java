package com.pos.repository;

import com.pos.model.TokenRevocado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface TokenRevocadoRepository extends JpaRepository<TokenRevocado, String> {

    void deleteByExpiracionBefore(Instant instante);
}
