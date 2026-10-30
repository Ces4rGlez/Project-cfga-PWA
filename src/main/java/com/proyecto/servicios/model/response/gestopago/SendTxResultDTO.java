package com.proyecto.servicios.model.response.gestopago;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Resultado uniforme de la transacción sendTx para el cliente.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Respuesta final de la transacción de pago o compra de producto")
public class SendTxResultDTO {

    @Schema(description = "Indica si la transacción fue exitosa", example = "true")
    private boolean exitoso;

    @Schema(description = "Código de resultado normalizado (01, sendTx_insufficientFunds, sendTx_duplicated, etc.)", example = "01")
    private String codigo;

    @Schema(description = "Mensaje descriptivo del resultado de la transacción", example = "Operación realizada con éxito")
    private String mensaje;

    @Schema(description = "Número único de transacción retornado por GestoPago", example = "73224244")
    private String idTx;

    @Schema(description = "Número de autorización retornado por el proveedor", example = "AUTH987654")
    private String numAutorizacion;

    @Schema(description = "Saldo de la cuenta antes de la transacción", example = "1000.00")
    private BigDecimal saldoAnterior;

    @Schema(description = "Saldo resultante en la cuenta bancaria del usuario tras la transacción", example = "850.00")
    private BigDecimal saldoNuevo;

    @Schema(description = "Monto debitado por el servicio o producto", example = "150.00")
    private BigDecimal montoCobrado;

    @Schema(description = "Comisión cobrada por la transacción", example = "10.00")
    private Double comision;

    @Schema(description = "Fecha y hora del procesamiento de la transacción", example = "23/Oct/2026 12:30:26")
    private String fecha;

    @Schema(description = "Código Único de Pago (UPC) utilizado para la transacción", example = "UPC_ABC12345")
    private String upc;

    @Schema(description = "PIN o URL de descarga (si el producto es redimible como tarjeta de regalo)", example = "NETFLIX-99238-CODE")
    private String pin;

    @Schema(description = "Instrucciones de canje para el cliente final", example = "1. Visita netflix.com/codigo 2. Ingresa el PIN")
    private String legend;

    @Schema(description = "Referencia aplicada al pago", example = "40425475190118187271")
    private String referencia;

    @Schema(description = "Teléfono de recarga aplicado", example = "5555555551")
    private String telefono;
}
