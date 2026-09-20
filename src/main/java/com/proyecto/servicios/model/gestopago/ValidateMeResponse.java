package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "RESPONSE")
public class ValidateMeResponse {

    @JacksonXmlProperty(localName = "MENSAJE")
    private Mensaje mensaje;

    @JacksonXmlProperty(localName = "IDENTIFYME")
    private IdentifyMe identifyMe;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Mensaje {
        @JacksonXmlProperty(localName = "CODIGO")
        private Integer codigo;

        @JacksonXmlProperty(localName = "TEXTO")
        private String texto;

        @JacksonXmlProperty(localName = "NOMBRECOMERCIO")
        private String nombreComercio;

        @JacksonXmlProperty(localName = "DIRECCION")
        private String direccion;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class IdentifyMe {
        @JacksonXmlProperty(localName = "VALID")
        private Integer valid;
    }
}
