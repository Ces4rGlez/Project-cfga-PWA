package com.proyecto.servicios.controller.onboarding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.exception.GlobalExceptionHandler;
import com.proyecto.servicios.exception.onboarding.CredencialesInvalidasException;
import com.proyecto.servicios.model.request.onboarding.CambioPasswordDTO;
import com.proyecto.servicios.model.request.onboarding.UsuarioConsultaDTO;
import com.proyecto.servicios.model.response.onboarding.UsuarioResponseDTO;
import com.proyecto.servicios.service.onboarding.AuthService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private UsuarioController usuarioController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        org.springframework.http.converter.json.MappingJackson2HttpMessageConverter converter =
                new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(usuarioController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(converter)
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/usuarios/consultar — Consulta de usuario vía Request Body sin @PathVariable")
    void obtenerUsuario_Exito() throws Exception {
        UsuarioConsultaDTO requestDto = new UsuarioConsultaDTO(1L);

        UsuarioResponseDTO responseDto = UsuarioResponseDTO.builder()
                .idUsuario(1L)
                .correoElectronico("usuario@test.com")
                .activo(true)
                .build();

        when(authService.obtenerUsuarioPorId(1L)).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/usuarios/consultar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.idUsuario").value(1))
                .andExpect(jsonPath("$.data.correoElectronico").value("usuario@test.com"));
    }

    @Test
    @DisplayName("PUT /api/v1/usuarios/password — Cambiar contraseña vía Request Body sin @PathVariable")
    void cambiarPassword_Exito() throws Exception {
        CambioPasswordDTO dto = new CambioPasswordDTO();
        dto.setIdUsuario(1L);
        dto.setPasswordActual("Temporal@2026");
        dto.setPasswordNueva("NuevaSegura@2026");

        doNothing().when(authService).cambiarPassword(any(CambioPasswordDTO.class));

        mockMvc.perform(put("/api/v1/usuarios/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Contraseña actualizada exitosamente"));
    }

    @Test
    @DisplayName("PUT /api/v1/usuarios/password — Contraseña actual incorrecta retorna 401 Unauthorized")
    void cambiarPassword_ActualIncorrecta() throws Exception {
        CambioPasswordDTO dto = new CambioPasswordDTO();
        dto.setIdUsuario(1L);
        dto.setPasswordActual("PasswordEquivocado@1");
        dto.setPasswordNueva("NuevaSegura@2026");

        doThrow(new CredencialesInvalidasException("La contraseña actual es incorrecta"))
                .when(authService).cambiarPassword(any(CambioPasswordDTO.class));

        mockMvc.perform(put("/api/v1/usuarios/password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401))
                .andExpect(jsonPath("$.message").value("La contraseña actual es incorrecta"));
    }
}
