package com.proyecto.servicios.model.request.gestopago;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO especializado para operaciones de Pago de Servicios (CFE, Agua, Gas, Telefonía fija, etc.).
 * Aplica validaciones estrictas: referencia alfanumérica obligatoria, monto a pagar obligatorio,
 * idProducto, idServicio y PIN.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para realizar el pago de un servicio público o privado")
public class PagoServicioRequestDTO {

    @NotBlank(message = "El número de cuenta bancaria es obligatorio")
    @Schema(description = "Número de cuenta bancaria del cliente", example = "8324279322")
    private String numeroCuenta;

    @NotNull(message = "El idProducto es obligatorio")
    @Schema(description = "ID del producto de servicio en el catálogo (e.g. 205 para CFE)", example = "205")
    private Integer idProducto;

    @NotNull(message = "El idServicio es obligatorio")
    @Schema(description = "ID del servicio en el catálogo (e.g. 76 para CFE)", example = "76")
    private Integer idServicio;

    @NotBlank(message = "La referencia del recibo o servicio es obligatoria")
    @Size(min = 1, max = 100, message = "La referencia debe contener entre 1 y 100 caracteres")
    @Schema(description = "Número de referencia, código de barras o número de contrato del recibo", example = "40425475190118187271")
    private String referencia;

    @NotNull(message = "El monto a pagar es obligatorio para pago de servicios")
    @DecimalMin(value = "0.01", message = "El monto a pagar debe ser mayor a cero")
    @Schema(description = "Monto a pagar según el recibo de servicio", example = "150.00")
    private BigDecimal monto;

    @NotBlank(message = "El PIN o contraseña de autorización es obligatorio")
    @Schema(description = "PIN o contraseña del usuario para autorizar el pago", example = "Password123#")
    private String pinAutorizacion;

    @Schema(description = "Código OTP de 6 dígitos del Doble Factor (2FA)", example = "123456")
    private String codigoOtp;

    @Schema(description = "Código Único de Pago (UPC). Si no se envía, el sistema lo generará aleatoriamente", example = "UPCCFE0001")
    private String upc;

    public SendTxRequestDTO toSendTxRequest() {
        return SendTxRequestDTO.builder()
                .numeroCuenta(numeroCuenta)
                .idProducto(idProducto)
                .idServicio(idServicio)
                .tipoFront(2)
                .referencia(referencia)
                .monto(monto)
                .pinAutorizacion(pinAutorizacion)
                .codigoOtp(codigoOtp)
                .upc(upc)
                .build();
    }
}
