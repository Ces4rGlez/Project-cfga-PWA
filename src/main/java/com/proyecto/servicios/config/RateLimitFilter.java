package com.proyecto.servicios.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Filtro de Rate Limiting para proteger el servidor contra ataques de denegación de servicio (DDoS)
 * o pruebas agresivas de JMeter.
 */
@Component
@Order(1) // Se ejecuta antes que otros filtros
public class RateLimitFilter extends OncePerRequestFilter {

    // Almacena un "cubeta" de peticiones por IP
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Extraer la IP del cliente
        String ip = obtenerIpCliente(request);
        
        // Obtener o crear el limitador para esta IP
        Bucket bucket = cache.computeIfAbsent(ip, this::crearNuevoBucket);

        // Intentar consumir 1 token. Si hay tokens disponibles, procesa la petición
        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            // Si no hay tokens, el usuario mandó demasiadas peticiones. Se rechaza con 429.
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write("{\"code\": 429, \"message\": \"Demasiadas peticiones (Rate Limit). Por favor, baje la velocidad de sus pruebas en JMeter.\"}");
        }
    }

    /**
     * Define las reglas del limitador:
     * - Capacidad máxima: 20 peticiones
     * - Relleno: 20 peticiones nuevas cada 1 segundo.
     * Esto significa que una IP no puede mandar más de 20 peticiones por segundo sin ser bloqueada.
     */
    private Bucket crearNuevoBucket(String ip) {
        Bandwidth limit = Bandwidth.classic(20, Refill.greedy(20, Duration.ofSeconds(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    private String obtenerIpCliente(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0];
    }
}
