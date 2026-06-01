package com.pos.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

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
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    // Crea un token JWT con el username como "subject" (identificador principal).
    // El token contiene: quién es (username), cuándo fue emitido, cuándo expira y la firma.
    public String generarToken(String username) {
        return Jwts.builder()
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getKey())
                .compact();
    }

    // Extrae el username del payload sin consultar la base de datos.
    public String obtenerUsername(String token) {
        return getClaims(token).getSubject();
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
