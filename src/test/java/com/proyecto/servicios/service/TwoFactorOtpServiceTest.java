package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.model.request.gestopago.OtpRequestDTO;
import com.proyecto.servicios.model.response.gestopago.OtpResponseDTO;
import com.proyecto.servicios.repository.onboarding.CuentaRepository;
import com.proyecto.servicios.service.Impl.TwoFactorOtpServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TwoFactorOtpServiceTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TwoFactorOtpServiceImpl otpService;

    private Cuenta cuenta;
    private Usuario usuario;
    private Cliente cliente;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .idUsuario(1L)
                .correoElectronico("ana.mendoza.castro@gmail.com")
                .password("$2a$10$hashedPassword")
                .activo(true)
                .build();

        cliente = Cliente.builder()
                .idCliente(1L)
                .nombre("Ana")
                .apellidoPaterno("Mendoza")
                .telefonoMovil("5511223344")
                .usuario(usuario)
                .build();

        cuenta = Cuenta.builder()
                .idCuenta(1L)
                .numeroCuenta("1000000001")
                .cliente(cliente)
                .estatus("ACTIVA")
                .build();
    }

    @Test
    @DisplayName("Debe generar OTP de 6 dígitos exitosamente y guardarlo en Redis")
    void testGenerarOtpExitoso() {
        when(cuentaRepository.findByNumeroCuenta("1000000001")).thenReturn(Optional.of(cuenta));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        OtpRequestDTO request = OtpRequestDTO.builder()
                .numeroCuenta("1000000001")
                .build();

        OtpResponseDTO response = otpService.generarOtp(request);

        assertTrue(response.isExitoso());
        assertNotNull(response.getCodigoOtpDemo());
        assertEquals(6, response.getCodigoOtpDemo().length());
        assertTrue(response.getDestinatario().contains("***"));
        assertEquals(300, response.getVigenciaSegundos());

        verify(valueOperations, times(1)).set(
                eq("gestopago:2fa:otp:1000000001"),
                eq(response.getCodigoOtpDemo()),
                eq(5L),
                eq(TimeUnit.MINUTES)
        );
    }

    @Test
    @DisplayName("Debe validar OTP correctamente contra Redis")
    void testValidarOtp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("gestopago:2fa:otp:1000000001")).thenReturn("482910");

        boolean valido = otpService.validarOtp("1000000001", "482910");
        assertTrue(valido);

        boolean invalido = otpService.validarOtp("1000000001", "000000");
        assertFalse(invalido);
    }

    @Test
    @DisplayName("Debe consumir y eliminar el OTP de Redis")
    void testConsumirOtp() {
        otpService.consumirOtp("1000000001");
        verify(redisTemplate, times(1)).delete("gestopago:2fa:otp:1000000001");
    }
}
