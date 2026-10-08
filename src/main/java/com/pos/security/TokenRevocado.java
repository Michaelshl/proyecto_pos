package com.pos.security;

import jakarta.persistence.*;
import lombok.Data;

import java.time.Instant;

@Data
@Entity
@Table(name = "tokens_revocados")
public class TokenRevocado {

    @Id
    private String jti;

    @Column(nullable = false)
    private Instant expiracion;
}
