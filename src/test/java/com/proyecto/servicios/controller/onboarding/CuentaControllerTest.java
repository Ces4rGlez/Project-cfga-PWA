package com.proyecto.servicios.controller.onboarding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.exception.GlobalExceptionHandler;
import com.proyecto.servicios.exception.onboarding.RecursoNoEncontradoException;
import com.proyecto.servicios.model.request.onboarding.CuentaConsultaDTO;
import com.proyecto.servicios.model.response.onboarding.CuentaResponseDTO;
import com.proyecto.servicios.service.onboarding.CuentaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class CuentaControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CuentaService cuentaService;

    @InjectMocks
    private CuentaController cuentaController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        org.springframework.http.converter.json.MappingJackson2HttpMessageConverter converter =
                new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(cuentaController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(converter)
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/cuentas/consultar — Consulta de cuenta vía Request Body sin @PathVariable")
    void obtenerCuenta_Exito() throws Exception {
        CuentaConsultaDTO dto = new CuentaConsultaDTO("1234567890");

        CuentaResponseDTO responseDto = CuentaResponseDTO.builder()
                .idCuenta(1L)
                .numeroCuenta("1234567890")
                .saldo(new BigDecimal("500.00"))
                .estatus("ACTIVA")
                .build();

        when(cuentaService.obtenerCuentaPorNumero("1234567890")).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/cuentas/consultar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.numeroCuenta").value("1234567890"))
                .andExpect(jsonPath("$.data.saldo").value(500.00));
    }

    @Test
    @DisplayName("POST /api/v1/cuentas/saldo — Consulta de saldo vía Request Body sin @PathVariable")
    void consultarSaldo_Exito() throws Exception {
        CuentaConsultaDTO dto = new CuentaConsultaDTO("1234567890");

        CuentaResponseDTO responseDto = CuentaResponseDTO.builder()
                .idCuenta(1L)
                .numeroCuenta("1234567890")
                .saldo(new BigDecimal("1500.75"))
                .estatus("ACTIVA")
                .build();

        when(cuentaService.consultarSaldo("1234567890")).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/cuentas/saldo")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.saldo").value(1500.75));
    }

    @Test
    @DisplayName("POST /api/v1/cuentas/consultar — Cuenta no encontrada retorna 404 Not Found")
    void obtenerCuenta_NoEncontrada() throws Exception {
        CuentaConsultaDTO dto = new CuentaConsultaDTO("9999999999");

        when(cuentaService.obtenerCuentaPorNumero("9999999999"))
                .thenThrow(new RecursoNoEncontradoException("No se encontró la cuenta con número: 9999999999"));

        mockMvc.perform(post("/api/v1/cuentas/consultar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("No se encontró la cuenta con número: 9999999999"));
    }
}
