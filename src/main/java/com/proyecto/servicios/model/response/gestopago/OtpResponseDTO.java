package com.proyecto.servicios.model.response.gestopago;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta de generación de código OTP de doble factor (2FA).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta de emisión de código OTP de doble factor (2FA)")
public class OtpResponseDTO {

    @Schema(description = "Indica si el OTP fue generado exitosamente", example = "true")
    private boolean exitoso;

    @Schema(description = "Mensaje informativo sobre el envío del código", example = "Código de seguridad 2FA enviado al correo a***a@gmail.com")
    private String mensaje;

    @Schema(description = "Destinatario enmascarado del código (correo o teléfono)", example = "a***a@gmail.com")
    private String destinatario;

    @Schema(description = "Vigencia del código en segundos", example = "300")
    private long vigenciaSegundos;

    @Schema(description = "Código OTP generado (disponible para pruebas en entorno de desarrollo/evaluación)", example = "749201")
    private String codigoOtpDemo;
}
