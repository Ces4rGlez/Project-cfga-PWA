package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Payload JSON que se cifra con AES256 antes de enviarse en el parámetro 'signed' a sendTx.do.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GestoPagoSendTxPayload {

    private Integer idProducto;
    private Integer idServicio;
    private String telefono;
    private String horaLocal;
    private String referencia;
    private Double montoPago;
    private String upc;
    private String unidad;
}
