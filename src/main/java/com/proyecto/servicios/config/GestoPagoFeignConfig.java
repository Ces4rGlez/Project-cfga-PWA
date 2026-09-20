package com.proyecto.servicios.config;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import feign.codec.Decoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.http.HttpMessageConverters;
import org.springframework.cloud.openfeign.support.ResponseEntityDecoder;
import org.springframework.cloud.openfeign.support.SpringDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;

import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class GestoPagoFeignConfig {

    private final GestoPagoTokenService tokenService;

    @Value("${gestopago.auth.id-distribuidor}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo}")
    private String codigoDispositivo;

    @Bean
    public RequestInterceptor bearerTokenRequestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // Intentamos buscar el token en base de datos
                Optional<GestoPagoToken> tokenOpt = tokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo);
                
                if (tokenOpt.isPresent() && tokenOpt.get().getToken() != null) {
                    template.header("Authorization", "Bearer " + tokenOpt.get().getToken());
                } else {
                    log.warn("No se encontró un token activo para GestoPago. La petición podría fallar.");
                }
            }
        };
    }

    /**
     * Decoder XML para que Feign pueda decodificar las respuestas en formato XML
     * que devuelve la API de GestoPago (PuntoRed).
     */
    @Bean
    public Decoder feignXmlDecoder() {
        XmlMapper xmlMapper = new XmlMapper();
        MappingJackson2XmlHttpMessageConverter xmlConverter = new MappingJackson2XmlHttpMessageConverter(xmlMapper);
        ObjectFactory<HttpMessageConverters> objectFactory = () -> new HttpMessageConverters(xmlConverter);
        return new ResponseEntityDecoder(new SpringDecoder(objectFactory));
    }
}

