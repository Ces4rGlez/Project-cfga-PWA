package com.proyecto.servicios.controller.onboarding;

import com.proyecto.servicios.model.request.onboarding.LoginRequestDTO;
import com.proyecto.servicios.model.response.ApiResponse;
import com.proyecto.servicios.model.response.onboarding.LoginResponseDTO;
import com.proyecto.servicios.service.onboarding.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para autenticación de usuarios.
 *
 * Endpoints:
 *   POST /auth/login → Iniciar sesión y obtener token JWT
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ==========================================
    // POST /auth/login — Iniciar sesión
    // ==========================================

    /**
     * Autentica un usuario mediante correo electrónico y contraseña.
     * Si las credenciales son válidas, retorna un token JWT junto con
     * información básica del usuario y su cliente asociado.
     *
     * Posibles errores:
     *   - 404: Usuario no encontrado
     *   - 401: Usuario inactivo o credenciales incorrectas
     *   - 400: Datos de validación inválidos
     *
     * @param dto Credenciales de acceso (correo y contraseña, validados con @Valid)
     * @return LoginResponseDTO con token JWT, correo, idUsuario e idCliente
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(
            @Valid @RequestBody LoginRequestDTO dto,
            HttpServletRequest request) {

        LoginResponseDTO loginResponse = authService.login(dto);

        ApiResponse<LoginResponseDTO> response = ApiResponse.<LoginResponseDTO>builder()
                .code(1)
                .message("Login exitoso")
                .path(request.getRequestURI())
                .data(loginResponse)
                .build();

        return ResponseEntity.ok(response);
    }
}
