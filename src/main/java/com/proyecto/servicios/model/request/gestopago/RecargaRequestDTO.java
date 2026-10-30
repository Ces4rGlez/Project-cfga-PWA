package com.proyecto.servicios.model.request.gestopago;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO especializado para operaciones de Recargas Telefónicas y Paquetes de Datos.
 * Aplica validaciones estrictas: teléfono a 10 dígitos obligatorio, idProducto, idServicio y PIN.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud para realizar una recarga telefónica o compra de paquete")
public class RecargaRequestDTO {

    @NotBlank(message = "El número de cuenta bancaria es obligatorio")
    @Schema(description = "Número de cuenta bancaria del cliente", example = "8324279322")
    private String numeroCuenta;

    @NotNull(message = "El idProducto es obligatorio")
    @Schema(description = "ID del producto de recarga obtenido del catálogo (e.g. 582 para Telcel $100)", example = "582")
    private Integer idProducto;

    @NotNull(message = "El idServicio es obligatorio")
    @Schema(description = "ID del servicio telefónico (e.g. 133 para Telcel)", example = "133")
    private Integer idServicio;

    @NotBlank(message = "El número de teléfono es obligatorio para recargas")
    @Pattern(regexp = "^\\d{10}$", message = "El número de teléfono debe contener exactamente 10 dígitos numéricos")
    @Schema(description = "Número telefónico celular a 10 dígitos", example = "5511223344")
    private String telefono;

    @Schema(description = "Monto de la recarga. Si se omite, se tomará automáticamente el precio oficial del catálogo", example = "100.00")
    private BigDecimal monto;

    @NotBlank(message = "El PIN o contraseña de autorización es obligatorio")
    @Schema(description = "PIN o contraseña del usuario para autorizar la recarga", example = "Password123#")
    private String pinAutorizacion;

    @Schema(description = "Código OTP de 6 dígitos del Doble Factor (2FA)", example = "123456")
    private String codigoOtp;

    @Schema(description = "Código Único de Pago (UPC). Si no se envía, el sistema lo generará aleatoriamente", example = "UPCTELCEL1")
    private String upc;

    public SendTxRequestDTO toSendTxRequest() {
        return SendTxRequestDTO.builder()
                .numeroCuenta(numeroCuenta)
                .idProducto(idProducto)
                .idServicio(idServicio)
                .tipoFront(1)
                .telefono(telefono)
                .monto(monto)
                .pinAutorizacion(pinAutorizacion)
                .codigoOtp(codigoOtp)
                .upc(upc)
                .build();
    }
}
