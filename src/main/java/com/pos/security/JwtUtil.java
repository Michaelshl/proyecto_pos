package com.pos.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

// Un JWT tiene tres partes: header.payload.firma
// La firma garantiza que nadie alteró el token sin conocer la clave secreta.
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    // Convierte la cadena "secret" en una clave criptográfica HMAC-SHA
    // HMAC-SHA es el algoritmo que firma el token (verifica integridad)
    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Falla al arrancar (y no en el primer login) si el secreto es demasiado corto para HS256.
    @PostConstruct
    void validarSecreto() {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("jwt.secret debe tener al menos 32 caracteres");
        }
    }

    // Crea un token JWT con la cédula como "subject" (identificador principal).
    // El token contiene: quién es (cédula), cuándo fue emitido, cuándo expira y la firma.
    public String generarToken(String cedula, String rol) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(cedula)
                .claim("rol", rol)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey())
                .compact();
    }

    // Extrae la cédula del payload sin consultar la base de datos.
    public String obtenerCedula(String token) {
        return getClaims(token).getSubject();
    }

    public String obtenerJti(String token) {
        return getClaims(token).getId();
    }

    public Instant obtenerExpiracion(String token) {
        return getClaims(token).getExpiration().toInstant();
    }

    public String obtenerRol(String token) {
        return getClaims(token).get("rol", String.class);
    }

    // Si parseSignedClaims lanza cualquier excepción (firma inválida, expirado, malformado)
    // devolvemos false en lugar de dejar que la excepción propague.
    public boolean tokenValido(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // Parsea y verifica el token, devolviendo el payload (Claims).
    // Si la firma no coincide o el token expiró, lanza JwtException automáticamente.
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
