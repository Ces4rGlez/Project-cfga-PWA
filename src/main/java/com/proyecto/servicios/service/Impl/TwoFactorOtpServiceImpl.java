package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.model.request.gestopago.OtpRequestDTO;
import com.proyecto.servicios.model.response.gestopago.OtpResponseDTO;
import com.proyecto.servicios.repository.onboarding.CuentaRepository;
import com.proyecto.servicios.service.TwoFactorOtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TwoFactorOtpServiceImpl implements TwoFactorOtpService {

    private final CuentaRepository cuentaRepository;
    private final StringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;

    private static final String REDIS_OTP_PREFIX = "gestopago:2fa:otp:";
    private static final long OTP_EXPIRATION_MINUTES = 5;

    private final SecureRandom secureRandom = new SecureRandom();

    // Fallback en memoria en caso de interrupción en Redis
    private static class OtpMemoryEntry {
        final String code;
        final Instant expiresAt;

        OtpMemoryEntry(String code, Instant expiresAt) {
            this.code = code;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private final Map<String, OtpMemoryEntry> memoryFallback = new ConcurrentHashMap<>();

    @Override
    public OtpResponseDTO generarOtp(OtpRequestDTO request) {
        log.info("Solicitud de generación de OTP 2FA para cuenta={}", request.getNumeroCuenta());

        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(request.getNumeroCuenta())
                .orElse(null);

        if (cuenta == null) {
            return OtpResponseDTO.builder()
                    .exitoso(false)
                    .mensaje("No se encontró la cuenta bancaria: " + request.getNumeroCuenta())
                    .build();
        }

        if (!"ACTIVA".equalsIgnoreCase(cuenta.getEstatus())) {
            return OtpResponseDTO.builder()
                    .exitoso(false)
                    .mensaje("La cuenta bancaria no está activa (estatus: " + cuenta.getEstatus() + ")")
                    .build();
        }

        Cliente cliente = cuenta.getCliente();
        Usuario usuario = cliente != null ? cliente.getUsuario() : null;

        if (usuario == null || !Boolean.TRUE.equals(usuario.getActivo())) {
            return OtpResponseDTO.builder()
                    .exitoso(false)
                    .mensaje("El usuario asociado a la cuenta no está activo")
                    .build();
        }

        // Validar titularidad si el usuario está autenticado vía JWT
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String correoAutenticado = auth.getName();
            if (correoAutenticado != null && usuario.getCorreoElectronico() != null
                    && !correoAutenticado.equalsIgnoreCase(usuario.getCorreoElectronico())) {
                return OtpResponseDTO.builder()
                        .exitoso(false)
                        .mensaje("No tiene autorización para solicitar códigos de seguridad para esta cuenta bancaria")
                        .build();
            }
        }

        // Si se envió PIN en la solicitud de OTP, validarlo
        if (request.getPinAutorizacion() != null && !request.getPinAutorizacion().trim().isEmpty()) {
            if (!passwordEncoder.matches(request.getPinAutorizacion(), usuario.getPassword())) {
                return OtpResponseDTO.builder()
                        .exitoso(false)
                        .mensaje("El PIN o contraseña de autorización es incorrecto")
                        .build();
            }
        }

        // Generar código numérico de 6 dígitos
        int number = secureRandom.nextInt(1_000_000);
        String otp = String.format("%06d", number);

        // Guardar en Redis con TTL de 5 minutos
        String redisKey = REDIS_OTP_PREFIX + cuenta.getNumeroCuenta();
        try {
            redisTemplate.opsForValue().set(redisKey, otp, OTP_EXPIRATION_MINUTES, TimeUnit.MINUTES);
            log.info("Código OTP 2FA guardado en Redis para cuenta {}: [VALIDO POR {} MIN]",
                    cuenta.getNumeroCuenta(), OTP_EXPIRATION_MINUTES);
        } catch (Exception e) {
            log.warn("Fallo al guardar OTP en Redis. Guardando en memoria de respaldo. Error: {}", e.getMessage());
            memoryFallback.put(cuenta.getNumeroCuenta(),
                    new OtpMemoryEntry(otp, Instant.now().plusSeconds(OTP_EXPIRATION_MINUTES * 60)));
        }

        String correo = usuario.getCorreoElectronico();
        String correoEnmascarado = enmascararCorreo(correo);

        log.info(">>> [SIMULADOR NOTIFICACIÓN 2FA] Enviando código OTP [{}] al correo: {} (Móvil: {})",
                otp, correo, cliente.getTelefonoMovil());

        return OtpResponseDTO.builder()
                .exitoso(true)
                .mensaje("Código de seguridad 2FA enviado al correo " + correoEnmascarado + ". Válido por " + OTP_EXPIRATION_MINUTES + " minutos.")
                .destinatario(correoEnmascarado)
                .vigenciaSegundos(OTP_EXPIRATION_MINUTES * 60)
                .codigoOtpDemo(otp) // Para facilidad de pruebas inmediatas en Swagger / Postman
                .build();
    }

    @Override
    public boolean validarOtp(String numeroCuenta, String otp) {
        if (numeroCuenta == null || otp == null || otp.trim().isEmpty()) {
            return false;
        }

        String redisKey = REDIS_OTP_PREFIX + numeroCuenta;
        try {
            String savedOtp = redisTemplate.opsForValue().get(redisKey);
            if (savedOtp != null && savedOtp.trim().equals(otp.trim())) {
                return true;
            }
        } catch (Exception e) {
            log.warn("Error consultando Redis para validar OTP. Verificando memoria de respaldo: {}", e.getMessage());
        }

        // Verificar fallback en memoria
        OtpMemoryEntry entry = memoryFallback.get(numeroCuenta);
        if (entry != null && !entry.isExpired() && entry.code.equals(otp.trim())) {
            return true;
        }

        return false;
    }

    @Override
    public void consumirOtp(String numeroCuenta) {
        if (numeroCuenta == null) return;
        String redisKey = REDIS_OTP_PREFIX + numeroCuenta;
        try {
            redisTemplate.delete(redisKey);
        } catch (Exception e) {
            log.warn("Error eliminando OTP de Redis: {}", e.getMessage());
        }
        memoryFallback.remove(numeroCuenta);
        log.info("Código OTP consumido e invalidado para la cuenta {}", numeroCuenta);
    }

    private String enmascararCorreo(String correo) {
        if (correo == null || !correo.contains("@")) {
            return "****";
        }
        String[] partes = correo.split("@");
        String usuario = partes[0];
        String dominio = partes[1];
        if (usuario.length() <= 2) {
            return usuario.charAt(0) + "***@" + dominio;
        }
        return usuario.charAt(0) + "***" + usuario.charAt(usuario.length() - 1) + "@" + dominio;
    }
}
