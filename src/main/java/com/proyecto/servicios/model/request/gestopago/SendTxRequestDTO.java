package com.proyecto.servicios.model.request.gestopago;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO de entrada para la operación de venta / pago de servicio (POST sendTx).
 * Contiene los datos requeridos por la lógica de negocio y el diagrama del profesor:
 * NumCuenta, Monto, Teléfono/Referencia, IDServicio, IdProducto, TipoFront, PinAutorización.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para realizar una transacción de venta o pago de servicio (sendTx)")
public class SendTxRequestDTO {

    @NotBlank(message = "El número de cuenta bancaria es obligatorio")
    @Schema(description = "Número de cuenta bancaria de donde se debitará el saldo", example = "1000000001")
    private String numeroCuenta;

    @Schema(description = "Monto del pago (obligatorio para pagos de servicios / tipoFront 2)", example = "150.00")
    private BigDecimal monto;

    @Schema(description = "Número telefónico a 10 dígitos (para recargas / tipoFront 1)", example = "5555555551")
    private String telefono;

    @Schema(description = "Referencia alfanumérica del recibo de servicio (para tipoFront 2)", example = "40425475190118187271")
    private String referencia;

    @Schema(description = "Campo unificado de Teléfono o Referencia según la especificación del diagrama", example = "5555555551")
    private String telefonoOReferencia;

    @NotNull(message = "El idServicio es obligatorio")
    @Schema(description = "ID del servicio obtenido del catálogo getProductList", example = "133")
    private Integer idServicio;

    @NotNull(message = "El idProducto es obligatorio")
    @Schema(description = "ID del producto obtenido del catálogo getProductList", example = "582")
    private Integer idProducto;

    @Schema(description = "Tipo de Front (1: Recarga telefónica, 2: Pago de servicios). Si se omite, se detecta del catálogo automáticamente.", example = "1")
    private Integer tipoFront;

    @NotBlank(message = "El PIN o contraseña de autorización del usuario es obligatorio")
    @Schema(description = "Contraseña o PIN del usuario titular de la cuenta para autorizar la operación", example = "Password123*")
    private String pinAutorizacion;

    @Schema(description = "Código Único de Pago (UPC). Si no se proporciona, el sistema generará automáticamente 10 caracteres alfanuméricos aleatorios.", example = "UPC_A1B2C3D4")
    private String upc;

    @Schema(description = "Unidad comercial o sucursal (por defecto se toma la configurada, e.g. 083)", example = "083")
    private String unidad;

    @Schema(description = "Código OTP de 6 dígitos del Doble Factor de Autenticación (2FA)", example = "749201")
    private String codigoOtp;
}
