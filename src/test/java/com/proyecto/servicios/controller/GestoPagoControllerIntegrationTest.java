package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.proyecto.servicios.enums.SendTxErrorCode;
import com.proyecto.servicios.exception.GlobalExceptionHandler;
import com.proyecto.servicios.model.request.gestopago.OtpRequestDTO;
import com.proyecto.servicios.model.request.gestopago.PagoServicioRequestDTO;
import com.proyecto.servicios.model.request.gestopago.RecargaRequestDTO;
import com.proyecto.servicios.model.response.gestopago.OtpResponseDTO;
import com.proyecto.servicios.model.response.gestopago.SendTxResultDTO;
import com.proyecto.servicios.service.GestoPagoCatalogService;
import com.proyecto.servicios.service.GestoPagoIntegrationService;
import com.proyecto.servicios.service.GestoPagoSendTxService;
import com.proyecto.servicios.service.TwoFactorOtpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de integración HTTP para los endpoints de GestoPago (sendTx, recargas, servicios, OTP).
 * Verifica la capa web completa:
 *  - Mapeo de rutas y serialización JSON
 *  - Validaciones estrictas de Bean Validation (@Valid, @NotNull, @Pattern, @Size)
 *  - Respuestas HTTP estructuradas mediante ApiResponse y GlobalExceptionHandler (@RestControllerAdvice)
 *  - Códigos de error de negocio (saldo insuficiente, duplicado, cuenta inactiva)
 */
@ExtendWith(MockitoExtension.class)
class GestoPagoControllerIntegrationTest {

    private MockMvc mockMvc;

    @Mock
    private GestoPagoSendTxService sendTxService;

    @Mock
    private TwoFactorOtpService otpService;

    @Mock
    private GestoPagoCatalogService catalogService;

    @Mock
    private GestoPagoIntegrationService integrationService;

    @InjectMocks
    private GestoPagoController controller;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        MappingJackson2HttpMessageConverter converter =
                new MappingJackson2HttpMessageConverter(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(converter)
                .build();
    }

    // ===================================================================
    // FLUJOS EXITOSOS
    // ===================================================================

    @Test
    @DisplayName("POST /api/gestopago/recargas - Recarga exitosa debe retornar HTTP 200 y code=1")
    void recargas_exitosa_retorna200() throws Exception {
        SendTxResultDTO mockResult = SendTxResultDTO.builder()
                .exitoso(true)
                .codigo("01")
                .mensaje("Operación realizada con éxito")
                .numAutorizacion("AUTH-123456")
                .saldoAnterior(new BigDecimal("500.00"))
                .saldoNuevo(new BigDecimal("490.00"))
                .montoCobrado(new BigDecimal("10.00"))
                .build();

        when(sendTxService.procesarTransaccion(any())).thenReturn(mockResult);

        RecargaRequestDTO body = RecargaRequestDTO.builder()
                .numeroCuenta("8324279322")
                .idProducto(582)
                .idServicio(133)
                .telefono("5511223344")
                .monto(new BigDecimal("10.00"))
                .pinAutorizacion("Password123#")
                .codigoOtp("123456")
                .build();

        mockMvc.perform(post("/api/gestopago/recargas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.exitoso").value(true))
                .andExpect(jsonPath("$.data.codigo").value("01"))
                .andExpect(jsonPath("$.data.numAutorizacion").value("AUTH-123456"));
    }

    @Test
    @DisplayName("POST /api/gestopago/servicios - Pago de servicio exitoso debe retornar HTTP 200 y code=1")
    void servicios_exitoso_retorna200() throws Exception {
        SendTxResultDTO mockResult = SendTxResultDTO.builder()
                .exitoso(true)
                .codigo("01")
                .mensaje("Operación realizada con éxito")
                .numAutorizacion("AUTH-SERV-999")
                .saldoAnterior(new BigDecimal("500.00"))
                .saldoNuevo(new BigDecimal("350.00"))
                .montoCobrado(new BigDecimal("150.00"))
                .build();

        when(sendTxService.procesarTransaccion(any())).thenReturn(mockResult);

        PagoServicioRequestDTO body = PagoServicioRequestDTO.builder()
                .numeroCuenta("8324279322")
                .idProducto(183)
                .idServicio(54)
                .referencia("40425475190118187271")
                .monto(new BigDecimal("150.00"))
                .pinAutorizacion("Password123#")
                .codigoOtp("123456")
                .build();

        mockMvc.perform(post("/api/gestopago/servicios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.exitoso").value(true))
                .andExpect(jsonPath("$.data.numAutorizacion").value("AUTH-SERV-999"));
    }

    @Test
    @DisplayName("POST /api/gestopago/otp/solicitar - Solicitud de OTP válida debe retornar HTTP 200")
    void solicitarOtp_exitoso_retorna200() throws Exception {
        OtpResponseDTO mockResponse = OtpResponseDTO.builder()
                .exitoso(true)
                .mensaje("Código enviado al correo titular")
                .vigenciaSegundos(300)
                .codigoOtpDemo("654321")
                .build();

        when(otpService.generarOtp(any(OtpRequestDTO.class))).thenReturn(mockResponse);

        String body = "{\"numeroCuenta\": \"8324279322\"}";

        mockMvc.perform(post("/api/gestopago/otp/solicitar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.data.exitoso").value(true));
    }

    // ===================================================================
    // BEAN VALIDATION (@Valid) — Respuestas HTTP 400 Centralizadas
    // ===================================================================

    @Test
    @DisplayName("POST /api/gestopago/recargas - Teléfono con longitud distinta a 10 debe retornar 400")
    void recargas_telefonoLongitudInvalida_retorna400() throws Exception {
        RecargaRequestDTO body = RecargaRequestDTO.builder()
                .numeroCuenta("8324279322")
                .idProducto(582)
                .idServicio(133)
                .telefono("55112") // 5 dígitos en vez de 10
                .monto(new BigDecimal("10.00"))
                .pinAutorizacion("Password123#")
                .build();

        mockMvc.perform(post("/api/gestopago/recargas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
                .andExpect(jsonPath("$.errores.telefono").exists());

        verify(sendTxService, never()).procesarTransaccion(any());
    }

    @Test
    @DisplayName("POST /api/gestopago/recargas - Teléfono con caracteres alfabéticos debe retornar 400")
    void recargas_telefonoConLetras_retorna400() throws Exception {
        RecargaRequestDTO body = RecargaRequestDTO.builder()
                .numeroCuenta("8324279322")
                .idProducto(582)
                .idServicio(133)
                .telefono("551122ABCD") // Letras no permitidas
                .monto(new BigDecimal("10.00"))
                .pinAutorizacion("Password123#")
                .build();

        mockMvc.perform(post("/api/gestopago/recargas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
                .andExpect(jsonPath("$.errores.telefono").exists());

        verify(sendTxService, never()).procesarTransaccion(any());
    }

    @Test
    @DisplayName("POST /api/gestopago/recargas - Sin idProducto debe retornar 400")
    void recargas_sinIdProducto_retorna400() throws Exception {
        RecargaRequestDTO body = RecargaRequestDTO.builder()
                .numeroCuenta("8324279322")
                .idServicio(133)
                .telefono("5511223344")
                .pinAutorizacion("Password123#")
                .build();

        mockMvc.perform(post("/api/gestopago/recargas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
                .andExpect(jsonPath("$.errores.idProducto").exists());

        verify(sendTxService, never()).procesarTransaccion(any());
    }

    @Test
    @DisplayName("POST /api/gestopago/servicios - Referencia vacía debe retornar 400")
    void servicios_referenciaVacia_retorna400() throws Exception {
        PagoServicioRequestDTO body = PagoServicioRequestDTO.builder()
                .numeroCuenta("8324279322")
                .idProducto(183)
                .idServicio(54)
                .referencia("") // Vacía
                .monto(new BigDecimal("150.00"))
                .pinAutorizacion("Password123#")
                .build();

        mockMvc.perform(post("/api/gestopago/servicios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
                .andExpect(jsonPath("$.errores.referencia").exists());

        verify(sendTxService, never()).procesarTransaccion(any());
    }

    @Test
    @DisplayName("POST /api/gestopago/servicios - Monto cero debe retornar 400")
    void servicios_montoCero_retorna400() throws Exception {
        PagoServicioRequestDTO body = PagoServicioRequestDTO.builder()
                .numeroCuenta("8324279322")
                .idProducto(183)
                .idServicio(54)
                .referencia("40425475190118187271")
                .monto(BigDecimal.ZERO) // Debe ser > 0
                .pinAutorizacion("Password123#")
                .build();

        mockMvc.perform(post("/api/gestopago/servicios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
                .andExpect(jsonPath("$.errores.monto").exists());

        verify(sendTxService, never()).procesarTransaccion(any());
    }

    @Test
    @DisplayName("POST /api/gestopago/servicios - Sin PIN de autorización debe retornar 400")
    void servicios_sinPin_retorna400() throws Exception {
        PagoServicioRequestDTO body = PagoServicioRequestDTO.builder()
                .numeroCuenta("8324279322")
                .idProducto(183)
                .idServicio(54)
                .referencia("40425475190118187271")
                .monto(new BigDecimal("150.00"))
                .build(); // pinAutorizacion es nulo

        mockMvc.perform(post("/api/gestopago/servicios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"))
                .andExpect(jsonPath("$.errores.pinAutorizacion").exists());

        verify(sendTxService, never()).procesarTransaccion(any());
    }

    @Test
    @DisplayName("Petición con cuerpo JSON malformado debe retornar 400")
    void jsonMalformado_retorna400() throws Exception {
        mockMvc.perform(post("/api/gestopago/recargas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{telefono: inalido,"))
                .andExpect(status().isBadRequest());
    }

    // ===================================================================
    // RESPUESTAS DE NEGOCIO CONTROLADAS (exitoso=false con código específico)
    // ===================================================================

    @Test
    @DisplayName("Rechazo por saldo insuficiente debe retornar código sendTx_insufficientFunds")
    void rechazo_saldoInsuficiente() throws Exception {
        SendTxResultDTO mockResult = SendTxResultDTO.builder()
                .exitoso(false)
                .codigo(SendTxErrorCode.INSUFFICIENT_FUNDS.getCodigo())
                .mensaje("Saldo insuficiente para realizar la transacción")
                .saldoAnterior(new BigDecimal("10.00"))
                .saldoNuevo(new BigDecimal("10.00"))
                .montoCobrado(BigDecimal.ZERO)
                .build();

        when(sendTxService.procesarTransaccion(any())).thenReturn(mockResult);

        RecargaRequestDTO body = RecargaRequestDTO.builder()
                .numeroCuenta("8324279322")
                .idProducto(582)
                .idServicio(133)
                .telefono("5511223344")
                .monto(new BigDecimal("100.00"))
                .pinAutorizacion("Password123#")
                .build();

        mockMvc.perform(post("/api/gestopago/recargas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.exitoso").value(false))
                .andExpect(jsonPath("$.data.codigo").value("sendTx_insufficientFunds"));
    }

    @Test
    @DisplayName("Rechazo por duplicado debe retornar código sendTx_duplicated")
    void rechazo_transaccionDuplicada() throws Exception {
        SendTxResultDTO mockResult = SendTxResultDTO.builder()
                .exitoso(false)
                .codigo(SendTxErrorCode.DUPLICATED.getCodigo())
                .mensaje("Transacción duplicada detectada")
                .saldoAnterior(new BigDecimal("500.00"))
                .saldoNuevo(new BigDecimal("500.00"))
                .montoCobrado(BigDecimal.ZERO)
                .build();

        when(sendTxService.procesarTransaccion(any())).thenReturn(mockResult);

        RecargaRequestDTO body = RecargaRequestDTO.builder()
                .numeroCuenta("8324279322")
                .idProducto(582)
                .idServicio(133)
                .telefono("5511223344")
                .monto(new BigDecimal("100.00"))
                .pinAutorizacion("Password123#")
                .upc("UPC_YA_REGISTRADO")
                .build();

        mockMvc.perform(post("/api/gestopago/recargas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.exitoso").value(false))
                .andExpect(jsonPath("$.data.codigo").value("sendTx_duplicated"));
    }
}
