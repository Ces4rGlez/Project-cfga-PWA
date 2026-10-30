package com.proyecto.servicios.controller.onboarding;

import com.proyecto.servicios.model.request.onboarding.CuentaConsultaDTO;
import com.proyecto.servicios.model.response.ApiResponse;
import com.proyecto.servicios.model.response.onboarding.CuentaResponseDTO;
import com.proyecto.servicios.service.onboarding.CuentaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para consultas de cuentas bancarias.
 *
 * Cumple con los lineamientos arquitectónicos:
 * - Sin @PathVariable ni @RequestParam.
 * - Parámetros recibidos mediante Request Body con @Valid.
 * - Respuestas uniformes con ApiResponse.
 */
@RestController
@RequestMapping("/api/v1/cuentas")
@RequiredArgsConstructor
public class CuentaController {

    private final CuentaService cuentaService;

    // ==========================================
    // 1. POST /cuentas/consultar — Consultar cuenta por número
    // ==========================================
    /**
     * Consulta una cuenta bancaria a partir de su número enviado en el Request Body.
     */
    @PostMapping("/consultar")
    public ResponseEntity<ApiResponse<CuentaResponseDTO>> obtenerCuenta(
            @Valid @RequestBody CuentaConsultaDTO dto,
            HttpServletRequest request) {

        CuentaResponseDTO cuenta = cuentaService.obtenerCuentaPorNumero(dto.getNumeroCuenta());

        ApiResponse<CuentaResponseDTO> response = ApiResponse.<CuentaResponseDTO>builder()
                .code(200)
                .message("Cuenta encontrada exitosamente")
                .path(request.getRequestURI())
                .data(cuenta)
                .build();

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // 2. POST /cuentas/saldo — Consultar saldo por número de cuenta
    // ==========================================
    @PostMapping("/saldo")
    public ResponseEntity<ApiResponse<CuentaResponseDTO>> consultarSaldo(
            @Valid @RequestBody CuentaConsultaDTO dto,
            HttpServletRequest request) {

        CuentaResponseDTO cuenta = cuentaService.consultarSaldo(dto.getNumeroCuenta());

        ApiResponse<CuentaResponseDTO> response = ApiResponse.<CuentaResponseDTO>builder()
                .code(200)
                .message("Saldo consultado exitosamente")
                .path(request.getRequestURI())
                .data(cuenta)
                .build();

        return ResponseEntity.ok(response);
    }
}
