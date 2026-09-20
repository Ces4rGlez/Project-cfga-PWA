package com.proyecto.servicios.client;

import com.proyecto.servicios.config.GestoPagoFeignConfig;
import com.proyecto.servicios.model.gestopago.EchoResponse;
import com.proyecto.servicios.model.gestopago.ProductListResponse;
import com.proyecto.servicios.model.gestopago.ValidateMeResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(
    name = "gestoPagoIntegration", 
    url = "${gestopago.auth.url}", 
    configuration = GestoPagoFeignConfig.class
)
public interface GestoPagoIntegrationClient {

    @GetMapping(value = "/sistema/service/getProductList.do", consumes = MediaType.APPLICATION_XML_VALUE)
    ProductListResponse getProductList();

    @GetMapping(value = "/sistema/service/sendEcho.do", consumes = MediaType.APPLICATION_XML_VALUE)
    EchoResponse sendEcho();

    @GetMapping(value = "/sistema/service/validateMe.do", consumes = MediaType.APPLICATION_XML_VALUE)
    ValidateMeResponse validateMe();
}
