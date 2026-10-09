package com.proyecto.servicios.config;

import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.repository.onboarding.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Filtro que intercepta cada request HTTP para validar el token JWT.
 * Si el token es válido y el usuario existe y está activo,
 * establece la autenticación en el SecurityContext de Spring.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");

        // Si no hay header Authorization o no empieza con "Bearer ", continuar sin autenticar
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String token = authHeader.substring(7);
            final String correo = jwtUtil.extraerCorreo(token);

            // Solo procesar si tenemos un correo y no hay autenticación previa en el contexto
            if (correo != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                Optional<Usuario> usuarioOpt = usuarioRepository.findByCorreoElectronico(correo);

                if (usuarioOpt.isPresent()) {
                    Usuario usuario = usuarioOpt.get();

                    // Validar token (permitimos tanto activos como inactivos)
                    if (jwtUtil.validarToken(token, correo)) {
                        // Si está activo: acceso completo. Si está inactivo: solo lectura.
                        var authorities = Boolean.TRUE.equals(usuario.getActivo())
                                ? List.of(new SimpleGrantedAuthority("ROLE_ACTIVO"))
                                : List.of(new SimpleGrantedAuthority("ROLE_READONLY"));

                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        correo,
                                        null,
                                        authorities
                                );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error al procesar token JWT: {}", e.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
