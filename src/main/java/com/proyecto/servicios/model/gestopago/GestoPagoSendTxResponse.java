package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mapeo de la respuesta XML de GestoPago (PuntoRed) al invocar sendTx.do.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "RESPONSE")
public class GestoPagoSendTxResponse {

    @JacksonXmlProperty(localName = "CODIGO")
    private String codigo;

    @JacksonXmlProperty(localName = "ID_TX")
    private String idTx;

    @JacksonXmlProperty(localName = "NUM_AUTORIZACION")
    private String numAutorizacion;

    @JacksonXmlProperty(localName = "SALDO")
    private String saldo;

    @JacksonXmlProperty(localName = "SALDO_F")
    private String saldoF;

    @JacksonXmlProperty(localName = "COMISION")
    private Double comision;

    @JacksonXmlProperty(localName = "COMISION_F")
    private Double comisionF;

    @JacksonXmlProperty(localName = "FECHA")
    private String fecha;

    @JacksonXmlProperty(localName = "MONTO")
    private Double monto;

    @JacksonXmlProperty(localName = "REFERENCIA")
    private String referencia;

    @JacksonXmlProperty(localName = "PIN")
    private String pin;

    @JacksonXmlProperty(localName = "legend")
    private String legend;

    @JacksonXmlProperty(localName = "MENSAJE")
    private MensajeDto mensaje;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class MensajeDto {
        @JacksonXmlProperty(localName = "CODIGO")
        private String codigo;

        @JacksonXmlProperty(localName = "TEXTO")
        private String texto;
    }

    /**
     * Obtiene el código de respuesta efectivo (sea a nivel raíz o dentro del nodo MENSAJE).
     */
    public String getCodigoFinal() {
        if (codigo != null && !codigo.trim().isEmpty()) {
            return codigo.trim();
        }
        if (mensaje != null && mensaje.getCodigo() != null) {
            return mensaje.getCodigo().trim();
        }
        return null;
    }

    /**
     * Obtiene el texto descriptivo del resultado.
     */
    public String getTextoFinal() {
        if (mensaje != null && mensaje.getTexto() != null && !mensaje.getTexto().trim().isEmpty()) {
            return mensaje.getTexto().trim();
        }
        if (legend != null && !legend.trim().isEmpty()) {
            return legend.trim();
        }
        return "Sin mensaje descriptivo del proveedor";
    }

    /**
     * Indica si la transacción fue exitosa ("01" o "1").
     */
    public boolean isExitoso() {
        String code = getCodigoFinal();
        return "01".equals(code) || "1".equals(code);
    }
}
