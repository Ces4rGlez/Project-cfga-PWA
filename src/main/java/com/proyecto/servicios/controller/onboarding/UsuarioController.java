package com.proyecto.servicios.controller.onboarding;

import com.proyecto.servicios.exception.onboarding.CuentaDesactivadaException;
import com.proyecto.servicios.model.request.onboarding.CambioPasswordDTO;
import com.proyecto.servicios.model.request.onboarding.UsuarioConsultaDTO;
import com.proyecto.servicios.model.response.ApiResponse;
import com.proyecto.servicios.model.response.onboarding.UsuarioResponseDTO;
import com.proyecto.servicios.service.onboarding.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para la gestión de usuarios del sistema.
 *
 * Cumple con los lineamientos arquitectónicos:
 * - Sin @PathVariable ni @RequestParam.
 * - Toda entrada se recibe vía @RequestBody y se valida con @Valid.
 * - Respuestas estandarizadas con ApiResponse.
 * - Manejo global de excepciones mediante @RestControllerAdvice.
 */
@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final AuthService authService;

    // ==========================================
    // 1. POST /usuarios/consultar — Consultar usuario
    // ==========================================
    /**
     * Consulta la información de un usuario por su ID encapsulado en el Request Body.
     * Retorna datos públicos: id, correo, estado activo y fecha de registro.
     * No incluye la contraseña por seguridad.
     */
    @PostMapping("/consultar")
    public ResponseEntity<ApiResponse<UsuarioResponseDTO>> obtenerUsuario(
            @Valid @RequestBody UsuarioConsultaDTO dto,
            HttpServletRequest request) {

        UsuarioResponseDTO usuario = authService.obtenerUsuarioPorId(dto.getIdUsuario());

        ApiResponse<UsuarioResponseDTO> response = ApiResponse.<UsuarioResponseDTO>builder()
                .code(200)
                .message("Usuario encontrado exitosamente")
                .path(request.getRequestURI())
                .data(usuario)
                .build();

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // 2. PUT /usuarios/password — Cambiar contraseña
    // ==========================================
    /**
     * Cambia la contraseña de un usuario mediante Request Body con validaciones.
     * El DTO contiene idUsuario, passwordActual y passwordNueva.
     */
    @PutMapping("/password")
    public ResponseEntity<ApiResponse<Void>> cambiarPassword(
            @Valid @RequestBody CambioPasswordDTO dto,
            HttpServletRequest request) {

        validarUsuarioActivo();

        authService.cambiarPassword(dto);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(200)
                .message("Contraseña actualizada exitosamente")
                .path(request.getRequestURI())
                .data(null)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Verifica que el usuario autenticado tenga una cuenta activa.
     */
    private void validarUsuarioActivo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_READONLY"))) {
            throw new CuentaDesactivadaException(
                    "Su cuenta está desactivada. Solo puede consultar información, no realizar operaciones.");
        }
    }
}
