package com.proyecto.servicios.model.request.gestopago;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Solicitud para generar un código OTP de doble factor (2FA).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para generar un código OTP temporal de doble factor (2FA)")
public class OtpRequestDTO {

    @NotBlank(message = "El número de cuenta es obligatorio")
    @Schema(description = "Número de cuenta bancaria del titular", example = "1000000001")
    private String numeroCuenta;

    @Schema(description = "PIN o contraseña de autorización para validar antes de emitir el OTP (opcional)", example = "Password123#")
    private String pinAutorizacion;
}
