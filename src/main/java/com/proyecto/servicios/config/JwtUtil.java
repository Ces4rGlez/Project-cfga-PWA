package com.proyecto.servicios.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.function.Function;

/**
 * Utilidad para la generación y validación de tokens JWT.
 * Utiliza la librería jjwt con HMAC-SHA256 para firmar los tokens.
 */
@Component
public class JwtUtil {

    private final Key key;
    private final long expirationMs;

    public JwtUtil(
            @Value("${jwt.secret:clave-secreta-para-jwt-onboarding-proyecto-2026-muy-segura}") String secret,
            @Value("${jwt.expiration-ms:3600000}") long expirationMs) {
        // Se asegura de que la clave tenga al menos 256 bits para HS256
        byte[] keyBytes = secret.getBytes();
        if (keyBytes.length < 32) {
            this.key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        } else {
            this.key = Keys.hmacShaKeyFor(keyBytes);
        }
        this.expirationMs = expirationMs;
    }

    /**
     * Genera un token JWT con el correo electrónico como subject.
     */
    public String generarToken(String correoElectronico) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(correoElectronico)
                .setIssuedAt(ahora)
                .setExpiration(expiracion)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Extrae el correo electrónico (subject) del token JWT.
     */
    public String extraerCorreo(String token) {
        return extraerClaim(token, Claims::getSubject);
    }

    /**
     * Verifica si el token es válido (no expirado y el subject coincide).
     */
    public boolean validarToken(String token, String correoElectronico) {
        final String correoDelToken = extraerCorreo(token);
        return correoDelToken.equals(correoElectronico) && !estaExpirado(token);
    }

    /**
     * Verifica si el token ha expirado.
     */
    private boolean estaExpirado(String token) {
        return extraerExpiracion(token).before(new Date());
    }

    /**
     * Extrae la fecha de expiración del token.
     */
    private Date extraerExpiracion(String token) {
        return extraerClaim(token, Claims::getExpiration);
    }

    /**
     * Extrae un claim específico del token usando un resolver.
     */
    private <T> T extraerClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extraerTodosLosClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extrae todos los claims del token.
     */
    private Claims extraerTodosLosClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
