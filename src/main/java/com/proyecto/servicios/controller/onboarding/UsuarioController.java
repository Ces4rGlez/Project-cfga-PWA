package com.proyecto.servicios.controller.onboarding;

import com.proyecto.servicios.exception.onboarding.CuentaDesactivadaException;
import com.proyecto.servicios.model.request.onboarding.CambioPasswordDTO;
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
 * Endpoints:
 *   GET /usuarios/{id}            → Consultar información del usuario
 *   PUT /usuarios/{id}/password   → Cambiar contraseña del usuario
 */
@RestController
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final AuthService authService;

    // ==========================================
    // GET /usuarios/{id} — Consultar usuario
    // ==========================================

    /**
     * Consulta la información de un usuario por su ID.
     * Retorna datos públicos: id, correo, estado activo y fecha de registro.
     * No incluye la contraseña por seguridad.
     *
     * @param id ID del usuario a consultar
     * @return UsuarioResponseDTO con la información del usuario
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UsuarioResponseDTO>> obtenerUsuario(
            @PathVariable Long id,
            HttpServletRequest request) {

        UsuarioResponseDTO usuario = authService.obtenerUsuarioPorId(id);

        ApiResponse<UsuarioResponseDTO> response = ApiResponse.<UsuarioResponseDTO>builder()
                .code(1)
                .message("Usuario encontrado")
                .path(request.getRequestURI())
                .data(usuario)
                .build();

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // PUT /usuarios/{id}/password — Cambiar contraseña
    // ==========================================

    /**
     * Cambia la contraseña de un usuario.
     * Requiere enviar la contraseña actual (para verificación) y la nueva contraseña.
     * La nueva contraseña debe cumplir las políticas de seguridad:
     * mínimo 8 caracteres, una mayúscula, una minúscula, un número y un carácter especial.
     *
     * Posibles errores:
     *   - 404: Usuario no encontrado
     *   - 401: Contraseña actual incorrecta
     *   - 400: La nueva contraseña no cumple las políticas
     *
     * @param id  ID del usuario
     * @param dto Contraseña actual y nueva contraseña (validadas con @Valid)
     */
    @PutMapping("/{id}/password")
    public ResponseEntity<ApiResponse<Void>> cambiarPassword(
            @PathVariable Long id,
            @Valid @RequestBody CambioPasswordDTO dto,
            HttpServletRequest request) {

        validarUsuarioActivo();

        authService.cambiarPassword(id, dto);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(1)
                .message("Contraseña actualizada exitosamente")
                .path(request.getRequestURI())
                .data(null)
                .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Verifica que el usuario autenticado tenga una cuenta activa.
     * Si tiene el rol ROLE_READONLY (cuenta desactivada), lanza una excepción.
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
