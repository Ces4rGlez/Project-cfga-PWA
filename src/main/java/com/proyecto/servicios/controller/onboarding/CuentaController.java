package com.proyecto.servicios.controller.onboarding;

import com.proyecto.servicios.model.response.ApiResponse;
import com.proyecto.servicios.model.response.onboarding.CuentaResponseDTO;
import com.proyecto.servicios.service.onboarding.CuentaService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para consultas de cuentas bancarias.
 *
 * Endpoints:
 *   GET /cuentas/{numeroCuenta}       → Consultar cuenta por número
 *   GET /cuentas/{numeroCuenta}/saldo → Consultar saldo de una cuenta
 */
@RestController
@RequestMapping("/cuentas")
@RequiredArgsConstructor
public class CuentaController {

    private final CuentaService cuentaService;

    // ==========================================
    // GET /cuentas/{numeroCuenta} — Consultar cuenta
    // ==========================================

    /**
     * Consulta una cuenta bancaria por su número de cuenta único.
     *
     * @param numeroCuenta Número de cuenta de 10 dígitos
     * @return CuentaResponseDTO con los datos de la cuenta (número, saldo, estatus, fecha)
     */
    @GetMapping("/{numeroCuenta}")
    public ResponseEntity<ApiResponse<CuentaResponseDTO>> obtenerCuenta(
            @PathVariable String numeroCuenta,
            HttpServletRequest request) {

        CuentaResponseDTO cuenta = cuentaService.obtenerCuentaPorNumero(numeroCuenta);

        ApiResponse<CuentaResponseDTO> response = ApiResponse.<CuentaResponseDTO>builder()
                .code(1)
                .message("Cuenta encontrada")
                .path(request.getRequestURI())
                .data(cuenta)
                .build();

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // GET /cuentas/{numeroCuenta}/saldo — Consultar saldo
    // ==========================================

    /**
     * Consulta específicamente el saldo de una cuenta bancaria.
     * Retorna el DTO completo de la cuenta que incluye el campo saldo.
     *
     * @param numeroCuenta Número de cuenta de 10 dígitos
     * @return CuentaResponseDTO con el saldo actual de la cuenta
     */
    @GetMapping("/{numeroCuenta}/saldo")
    public ResponseEntity<ApiResponse<CuentaResponseDTO>> consultarSaldo(
            @PathVariable String numeroCuenta,
            HttpServletRequest request) {

        CuentaResponseDTO cuenta = cuentaService.consultarSaldo(numeroCuenta);

        ApiResponse<CuentaResponseDTO> response = ApiResponse.<CuentaResponseDTO>builder()
                .code(1)
                .message("Consulta de saldo exitosa")
                .path(request.getRequestURI())
                .data(cuenta)
                .build();

        return ResponseEntity.ok(response);
    }
}
