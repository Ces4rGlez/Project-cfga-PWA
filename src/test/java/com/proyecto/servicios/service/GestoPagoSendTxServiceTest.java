package com.proyecto.servicios.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.entity.gestopago.ProductoEntity;
import com.proyecto.servicios.entity.gestopago.TransaccionSendTxEntity;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.enums.SendTxErrorCode;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxResponse;
import com.proyecto.servicios.model.request.gestopago.SendTxRequestDTO;
import com.proyecto.servicios.model.response.gestopago.SendTxResultDTO;
import com.proyecto.servicios.repository.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.gestopago.ProductoRepository;
import com.proyecto.servicios.repositorys.gestopago.TransaccionSendTxRepository;
import com.proyecto.servicios.service.Impl.GestoPagoSendTxServiceImpl;
import com.proyecto.servicios.util.AesEncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestoPagoSendTxServiceTest {

    @Mock
    private CuentaRepository cuentaRepository;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private TransaccionSendTxRepository transaccionRepository;

    @Mock
    private GestoPagoIntegrationService integrationService;

    @Spy
    private AesEncryptionUtil aesEncryptionUtil = new AesEncryptionUtil(
            AesEncryptionUtil.DEFAULT_SECRET_KEY,
            AesEncryptionUtil.DEFAULT_IV
    );

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TwoFactorOtpService twoFactorOtpService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private GestoPagoSendTxServiceImpl sendTxService;

    private Cuenta cuentaActiva;
    private Usuario usuarioActivo;
    private Cliente cliente;
    private ProductoEntity productoRecarga;
    private ProductoEntity productoServicio;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sendTxService, "defaultUnidad", "083");

        usuarioActivo = Usuario.builder()
                .idUsuario(1L)
                .correoElectronico("juan.perez@test.com")
                .password("$2a$10$hashedPassword")
                .activo(true)
                .build();

        cliente = Cliente.builder()
                .idCliente(1L)
                .nombre("Juan")
                .apellidoPaterno("Perez")
                .usuario(usuarioActivo)
                .build();

        cuentaActiva = Cuenta.builder()
                .idCuenta(1L)
                .numeroCuenta("1000000001")
                .cliente(cliente)
                .saldo(new BigDecimal("500.00"))
                .estatus("ACTIVA")
                .build();

        productoRecarga = ProductoEntity.builder()
                .idProducto(582)
                .idServicio(133)
                .producto("Telcel $100")
                .servicio("Telcel")
                .tipoFront(1)
                .precio("100.00")
                .build();

        productoServicio = ProductoEntity.builder()
                .idProducto(205)
                .idServicio(76)
                .producto("CFE")
                .servicio("CFE")
                .tipoFront(2)
                .build();
    }

    @Test
    @DisplayName("Debe rechazar si la cuenta bancaria no existe")
    void testCuentaNoExiste() {
        when(cuentaRepository.findByNumeroCuentaWithLock("9999999999")).thenReturn(Optional.empty());

        SendTxRequestDTO request = SendTxRequestDTO.builder()
                .numeroCuenta("9999999999")
                .idProducto(582)
                .idServicio(133)
                .pinAutorizacion("Secret123")
                .build();

        SendTxResultDTO result = sendTxService.procesarTransaccion(request);

        assertFalse(result.isExitoso());
        assertEquals(SendTxErrorCode.ACCOUNT_INACTIVE.getCodigo(), result.getCodigo());
    }

    @Test
    @DisplayName("Debe rechazar si la cuenta está BLOQUEADA o INACTIVA")
    void testCuentaBloqueada() {
        cuentaActiva.setEstatus("BLOQUEADA");
        when(cuentaRepository.findByNumeroCuentaWithLock(cuentaActiva.getNumeroCuenta())).thenReturn(Optional.of(cuentaActiva));

        SendTxRequestDTO request = SendTxRequestDTO.builder()
                .numeroCuenta(cuentaActiva.getNumeroCuenta())
                .idProducto(582)
                .idServicio(133)
                .pinAutorizacion("Secret123")
                .build();

        SendTxResultDTO result = sendTxService.procesarTransaccion(request);

        assertFalse(result.isExitoso());
        assertEquals(SendTxErrorCode.ACCOUNT_BLOCKED.getCodigo(), result.getCodigo());
    }

    @Test
    @DisplayName("Debe rechazar si el PIN / contraseña de autorización es incorrecto")
    void testPinIncorrecto() {
        when(cuentaRepository.findByNumeroCuentaWithLock(cuentaActiva.getNumeroCuenta())).thenReturn(Optional.of(cuentaActiva));
        when(passwordEncoder.matches("WrongPin", usuarioActivo.getPassword())).thenReturn(false);

        SendTxRequestDTO request = SendTxRequestDTO.builder()
                .numeroCuenta(cuentaActiva.getNumeroCuenta())
                .idProducto(582)
                .idServicio(133)
                .pinAutorizacion("WrongPin")
                .build();

        SendTxResultDTO result = sendTxService.procesarTransaccion(request);

        assertFalse(result.isExitoso());
        assertEquals(SendTxErrorCode.INVALID_PIN.getCodigo(), result.getCodigo());
    }

    @Test
    @DisplayName("Debe rechazar si el idServicio enviado no coincide con el producto en el catálogo")
    void testServicioNoCorrespondeAProducto() {
        when(cuentaRepository.findByNumeroCuentaWithLock(cuentaActiva.getNumeroCuenta())).thenReturn(Optional.of(cuentaActiva));
        when(passwordEncoder.matches("CorrectPin", usuarioActivo.getPassword())).thenReturn(true);
        when(productoRepository.findById(582)).thenReturn(Optional.of(productoRecarga)); // Servicio oficial: 133

        SendTxRequestDTO request = SendTxRequestDTO.builder()
                .numeroCuenta(cuentaActiva.getNumeroCuenta())
                .idProducto(582)
                .idServicio(999) // Servicio discrepante
                .pinAutorizacion("CorrectPin")
                .build();

        SendTxResultDTO result = sendTxService.procesarTransaccion(request);

        assertFalse(result.isExitoso());
        assertEquals(SendTxErrorCode.INVALID_PARAMETER.getCodigo(), result.getCodigo());
    }

    @Test
    @DisplayName("Debe rechazar si se intenta enviar un producto de servicio (tipoFront 2) en endpoint de recargas (tipoFront 1)")
    void testDiscrepanciaTipoFrontServicioEnRecarga() {
        when(cuentaRepository.findByNumeroCuentaWithLock(cuentaActiva.getNumeroCuenta())).thenReturn(Optional.of(cuentaActiva));
        when(passwordEncoder.matches("CorrectPin", usuarioActivo.getPassword())).thenReturn(true);
        when(productoRepository.findById(205)).thenReturn(Optional.of(productoServicio)); // CFE es tipoFront=2

        SendTxRequestDTO request = SendTxRequestDTO.builder()
                .numeroCuenta(cuentaActiva.getNumeroCuenta())
                .idProducto(205)
                .idServicio(76)
                .tipoFront(1) // Invocado desde /recargas
                .telefono("5511223344")
                .pinAutorizacion("CorrectPin")
                .build();

        SendTxResultDTO result = sendTxService.procesarTransaccion(request);

        assertFalse(result.isExitoso());
        assertEquals(SendTxErrorCode.INVALID_PARAMETER.getCodigo(), result.getCodigo());
        assertTrue(result.getMensaje().contains("exclusivo para Recarga telefónica"));
    }

    @Test
    @DisplayName("Debe rechazar si se intenta manipular el monto en recargas con precio fijo en catálogo")
    void testMontoManipuladoEnRecarga() {
        when(cuentaRepository.findByNumeroCuentaWithLock(cuentaActiva.getNumeroCuenta())).thenReturn(Optional.of(cuentaActiva));
        when(passwordEncoder.matches("CorrectPin", usuarioActivo.getPassword())).thenReturn(true);
        when(productoRepository.findById(582)).thenReturn(Optional.of(productoRecarga)); // Precio catálogo: 100.00

        SendTxRequestDTO request = SendTxRequestDTO.builder()
                .numeroCuenta(cuentaActiva.getNumeroCuenta())
                .idProducto(582)
                .idServicio(133)
                .telefono("5511223344")
                .monto(new BigDecimal("10.00")) // Intento de pagar 10 pesos por un paquete de 100
                .pinAutorizacion("CorrectPin")
                .build();

        SendTxResultDTO result = sendTxService.procesarTransaccion(request);

        assertFalse(result.isExitoso());
        assertEquals(SendTxErrorCode.INVALID_AMOUNT.getCodigo(), result.getCodigo());
    }

    @Test
    @DisplayName("Debe rechazar si el saldo en la cuenta del usuario es insuficiente (sendTx_insufficientFunds)")
    void testSaldoInsuficienteEnCuenta() {
        when(cuentaRepository.findByNumeroCuentaWithLock(cuentaActiva.getNumeroCuenta())).thenReturn(Optional.of(cuentaActiva));
        when(passwordEncoder.matches("CorrectPin", usuarioActivo.getPassword())).thenReturn(true);
        when(productoRepository.findById(205)).thenReturn(Optional.of(productoServicio));

        SendTxRequestDTO request = SendTxRequestDTO.builder()
                .numeroCuenta(cuentaActiva.getNumeroCuenta())
                .idProducto(205)
                .idServicio(76)
                .monto(new BigDecimal("1000.00")) // Saldo en cuenta es 500.00
                .referencia("REF-123456789")
                .pinAutorizacion("CorrectPin")
                .build();

        SendTxResultDTO result = sendTxService.procesarTransaccion(request);

        assertFalse(result.isExitoso());
        assertEquals(SendTxErrorCode.INSUFFICIENT_FUNDS.getCodigo(), result.getCodigo());
        verify(transaccionRepository, times(1)).save(any(TransaccionSendTxEntity.class));
        verify(cuentaRepository, never()).save(any(Cuenta.class)); // Saldo NO debitado
    }

    @Test
    @DisplayName("Debe rechazar si la transacción es detectada como duplicada (sendTx_duplicated)")
    void testTransaccionDuplicada() {
        when(cuentaRepository.findByNumeroCuentaWithLock(cuentaActiva.getNumeroCuenta())).thenReturn(Optional.of(cuentaActiva));
        when(passwordEncoder.matches("CorrectPin", usuarioActivo.getPassword())).thenReturn(true);
        when(productoRepository.findById(582)).thenReturn(Optional.of(productoRecarga));
        when(transaccionRepository.existsByUpcAndFechaRegistroAfter(eq("UPC_EXISTENTE"), any(LocalDateTime.class))).thenReturn(true);

        SendTxRequestDTO request = SendTxRequestDTO.builder()
                .numeroCuenta(cuentaActiva.getNumeroCuenta())
                .idProducto(582)
                .idServicio(133)
                .telefono("5555555551")
                .pinAutorizacion("CorrectPin")
                .upc("UPC_EXISTENTE")
                .build();

        SendTxResultDTO result = sendTxService.procesarTransaccion(request);

        assertFalse(result.isExitoso());
        assertEquals(SendTxErrorCode.DUPLICATED.getCodigo(), result.getCodigo());
        verify(integrationService, never()).sendTx(anyString(), anyString()); // No se envió a GestoPago
    }

    @Test
    @DisplayName("Debe procesar exitosamente: GestoPago retorna 01, debita saldo y guarda evidencia")
    void testTransaccionExitosa() {
        when(cuentaRepository.findByNumeroCuentaWithLock(cuentaActiva.getNumeroCuenta())).thenReturn(Optional.of(cuentaActiva));
        when(passwordEncoder.matches("CorrectPin", usuarioActivo.getPassword())).thenReturn(true);
        when(productoRepository.findById(582)).thenReturn(Optional.of(productoRecarga));
        when(transaccionRepository.existsByUpcAndFechaRegistroAfter(anyString(), any(LocalDateTime.class))).thenReturn(false);

        GestoPagoSendTxResponse mockResponse = GestoPagoSendTxResponse.builder()
                .codigo("01")
                .idTx("TX-998877")
                .numAutorizacion("AUTH-123456")
                .fecha("23/Oct/2026 12:30:00")
                .comision(5.0)
                .mensaje(new GestoPagoSendTxResponse.MensajeDto("01", "Operacion realizada con exito"))
                .build();

        when(integrationService.sendTx(anyString(), anyString())).thenReturn(mockResponse);

        SendTxRequestDTO request = SendTxRequestDTO.builder()
                .numeroCuenta(cuentaActiva.getNumeroCuenta())
                .idProducto(582)
                .idServicio(133)
                .telefono("5555555551")
                .pinAutorizacion("CorrectPin")
                .build();

        SendTxResultDTO result = sendTxService.procesarTransaccion(request);

        assertTrue(result.isExitoso());
        assertEquals("01", result.getCodigo());
        assertEquals("AUTH-123456", result.getNumAutorizacion());
        assertEquals(new BigDecimal("500.00"), result.getSaldoAnterior());
        assertEquals(new BigDecimal("400.00"), result.getSaldoNuevo()); // 500 - 100 de recarga
        assertEquals(new BigDecimal("100.00"), result.getMontoCobrado());

        verify(cuentaRepository, times(1)).save(argThat(c -> c.getSaldo().compareTo(new BigDecimal("400.00")) == 0));
        verify(transaccionRepository, times(1)).save(argThat(tx -> "EXITOSA".equals(tx.getEstatus())));
    }

    @Test
    @DisplayName("Debe manejar Timeout externo marcando PENDIENTE_CONCILIACION sin debitar saldo")
    void testTimeoutMarcadoComoPendienteConciliacion() {
        when(cuentaRepository.findByNumeroCuentaWithLock(cuentaActiva.getNumeroCuenta())).thenReturn(Optional.of(cuentaActiva));
        when(passwordEncoder.matches("CorrectPin", usuarioActivo.getPassword())).thenReturn(true);
        when(productoRepository.findById(582)).thenReturn(Optional.of(productoRecarga));
        when(transaccionRepository.existsByUpcAndFechaRegistroAfter(anyString(), any(LocalDateTime.class))).thenReturn(false);

        when(integrationService.sendTx(anyString(), anyString()))
                .thenThrow(new RuntimeException("SocketTimeoutException: Read timed out", new SocketTimeoutException("Read timed out")));

        SendTxRequestDTO request = SendTxRequestDTO.builder()
                .numeroCuenta(cuentaActiva.getNumeroCuenta())
                .idProducto(582)
                .idServicio(133)
                .telefono("5555555551")
                .pinAutorizacion("CorrectPin")
                .build();

        SendTxResultDTO result = sendTxService.procesarTransaccion(request);

        assertFalse(result.isExitoso());
        assertEquals(SendTxErrorCode.TIMEOUT.getCodigo(), result.getCodigo());
        assertEquals(new BigDecimal("500.00"), result.getSaldoNuevo()); // Saldo intacto
        verify(cuentaRepository, never()).save(any(Cuenta.class));
        verify(transaccionRepository, times(1)).save(argThat(tx -> "PENDIENTE_CONCILIACION".equals(tx.getEstatus())));
    }

    @Test
    @DisplayName("Debe mapear rechazo de GestoPago (código 02 Saldo Insuficiente en proveedor) a sendTx_insufficientFunds sin descontar saldo")
    void testRechazoGestoPagoSaldoInsuficiente() {
        when(cuentaRepository.findByNumeroCuentaWithLock(cuentaActiva.getNumeroCuenta())).thenReturn(Optional.of(cuentaActiva));
        when(passwordEncoder.matches("CorrectPin", usuarioActivo.getPassword())).thenReturn(true);
        when(productoRepository.findById(582)).thenReturn(Optional.of(productoRecarga));
        when(transaccionRepository.existsByUpcAndFechaRegistroAfter(anyString(), any(LocalDateTime.class))).thenReturn(false);

        GestoPagoSendTxResponse mockResponse = GestoPagoSendTxResponse.builder()
                .codigo("02")
                .mensaje(new GestoPagoSendTxResponse.MensajeDto("02", "Saldo Insuficiente"))
                .build();

        when(integrationService.sendTx(anyString(), anyString())).thenReturn(mockResponse);

        SendTxRequestDTO request = SendTxRequestDTO.builder()
                .numeroCuenta(cuentaActiva.getNumeroCuenta())
                .idProducto(582)
                .idServicio(133)
                .telefono("5555555551")
                .pinAutorizacion("CorrectPin")
                .build();

        SendTxResultDTO result = sendTxService.procesarTransaccion(request);

        assertFalse(result.isExitoso());
        assertEquals(SendTxErrorCode.INSUFFICIENT_FUNDS.getCodigo(), result.getCodigo());
        assertEquals(new BigDecimal("500.00"), result.getSaldoNuevo()); // Saldo intacto
        verify(cuentaRepository, never()).save(any(Cuenta.class));
        verify(transaccionRepository, times(1)).save(argThat(tx -> "RECHAZADA".equals(tx.getEstatus())));
    }

    @Test
    @DisplayName("Debe mapear rechazo de GestoPago (código 06 Duplicada) a sendTx_duplicated sin descontar saldo")
    void testRechazoGestoPagoDuplicada() {
        when(cuentaRepository.findByNumeroCuentaWithLock(cuentaActiva.getNumeroCuenta())).thenReturn(Optional.of(cuentaActiva));
        when(passwordEncoder.matches("CorrectPin", usuarioActivo.getPassword())).thenReturn(true);
        when(productoRepository.findById(582)).thenReturn(Optional.of(productoRecarga));
        when(transaccionRepository.existsByUpcAndFechaRegistroAfter(anyString(), any(LocalDateTime.class))).thenReturn(false);

        GestoPagoSendTxResponse mockResponse = GestoPagoSendTxResponse.builder()
                .codigo("06")
                .mensaje(new GestoPagoSendTxResponse.MensajeDto("06", "Transaccion aplicada anteriormente"))
                .build();

        when(integrationService.sendTx(anyString(), anyString())).thenReturn(mockResponse);

        SendTxRequestDTO request = SendTxRequestDTO.builder()
                .numeroCuenta(cuentaActiva.getNumeroCuenta())
                .idProducto(582)
                .idServicio(133)
                .telefono("5555555551")
                .pinAutorizacion("CorrectPin")
                .build();

        SendTxResultDTO result = sendTxService.procesarTransaccion(request);

        assertFalse(result.isExitoso());
        assertEquals(SendTxErrorCode.DUPLICATED.getCodigo(), result.getCodigo());
        verify(cuentaRepository, never()).save(any(Cuenta.class));
        verify(transaccionRepository, times(1)).save(argThat(tx -> "RECHAZADA".equals(tx.getEstatus())));
    }

    @Test
    @DisplayName("Debe validar OTP exitosamente, consumirlo y proceder con la transacción sendTx")
    void testValidacionOtpCorrecto() {
        when(cuentaRepository.findByNumeroCuentaWithLock(cuentaActiva.getNumeroCuenta())).thenReturn(Optional.of(cuentaActiva));
        when(passwordEncoder.matches("CorrectPin", usuarioActivo.getPassword())).thenReturn(true);
        when(twoFactorOtpService.validarOtp(cuentaActiva.getNumeroCuenta(), "123456")).thenReturn(true);
        when(productoRepository.findById(582)).thenReturn(Optional.of(productoRecarga));
        when(transaccionRepository.existsByUpcAndFechaRegistroAfter(anyString(), any(LocalDateTime.class))).thenReturn(false);

        GestoPagoSendTxResponse mockResponse = GestoPagoSendTxResponse.builder()
                .codigo("01")
                .numAutorizacion("AUTH-OTP-123")
                .mensaje(new GestoPagoSendTxResponse.MensajeDto("01", "Operacion realizada con exito"))
                .build();
        when(integrationService.sendTx(anyString(), anyString())).thenReturn(mockResponse);

        SendTxRequestDTO request = SendTxRequestDTO.builder()
                .numeroCuenta(cuentaActiva.getNumeroCuenta())
                .idProducto(582)
                .idServicio(133)
                .telefono("5555555551")
                .pinAutorizacion("CorrectPin")
                .codigoOtp("123456")
                .build();

        SendTxResultDTO result = sendTxService.procesarTransaccion(request);

        assertTrue(result.isExitoso());
        verify(twoFactorOtpService, times(1)).consumirOtp(cuentaActiva.getNumeroCuenta());
        verify(cuentaRepository, times(1)).save(any(Cuenta.class));
    }

    @Test
    @DisplayName("Debe rechazar con sendTx_invalidOtp si el código OTP es incorrecto o ha expirado")
    void testValidacionOtpInvalido() {
        when(cuentaRepository.findByNumeroCuentaWithLock(cuentaActiva.getNumeroCuenta())).thenReturn(Optional.of(cuentaActiva));
        when(passwordEncoder.matches("CorrectPin", usuarioActivo.getPassword())).thenReturn(true);
        when(twoFactorOtpService.validarOtp(cuentaActiva.getNumeroCuenta(), "999999")).thenReturn(false);

        SendTxRequestDTO request = SendTxRequestDTO.builder()
                .numeroCuenta(cuentaActiva.getNumeroCuenta())
                .idProducto(582)
                .idServicio(133)
                .telefono("5555555551")
                .pinAutorizacion("CorrectPin")
                .codigoOtp("999999")
                .build();

        SendTxResultDTO result = sendTxService.procesarTransaccion(request);

        assertFalse(result.isExitoso());
        assertEquals(SendTxErrorCode.INVALID_OTP.getCodigo(), result.getCodigo());
        verify(twoFactorOtpService, never()).consumirOtp(anyString());
        verify(integrationService, never()).sendTx(anyString(), anyString());
        verify(cuentaRepository, never()).save(any(Cuenta.class));
    }
}
