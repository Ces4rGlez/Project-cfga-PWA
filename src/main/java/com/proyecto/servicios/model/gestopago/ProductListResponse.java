package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@JacksonXmlRootElement(localName = "RESPONSE")
public class ProductListResponse {

    @JacksonXmlProperty(localName = "MENSAJE")
    private Mensaje mensaje;

    @JacksonXmlProperty(localName = "PRODUCTOS")
    private Productos productos;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Mensaje {
        @JacksonXmlProperty(localName = "CODIGO")
        private Integer codigo;

        @JacksonXmlProperty(localName = "TEXTO")
        private String texto;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Productos {
        @JacksonXmlProperty(localName = "producto")
        @JacksonXmlElementWrapper(useWrapping = false)
        private List<ProductDto> producto;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ProductDto {
        @JacksonXmlProperty(isAttribute = true, localName = "producto")
        private String producto;

        @JacksonXmlProperty(isAttribute = true, localName = "servicio")
        private String servicio;

        @JacksonXmlProperty(isAttribute = true, localName = "idServicio")
        private Integer idServicio;

        @JacksonXmlProperty(isAttribute = true, localName = "idProducto")
        private Integer idProducto;

        @JacksonXmlProperty(isAttribute = true, localName = "idCatTipoServicio")
        private Integer idCatTipoServicio;

        @JacksonXmlProperty(isAttribute = true, localName = "tipoFront")
        private Integer tipoFront;

        @JacksonXmlProperty(isAttribute = true, localName = "hasDigitoVerificador")
        private Boolean hasDigitoVerificador;

        @JacksonXmlProperty(isAttribute = true, localName = "tipoReferencia")
        private String tipoReferencia;

        @JacksonXmlProperty(isAttribute = true, localName = "precio")
        private String precio;

        @JacksonXmlProperty(isAttribute = true, localName = "showAyuda")
        private Boolean showAyuda;

        @JacksonXmlProperty(localName = "legend")
        private String legend;
    }
}
