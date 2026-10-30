package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoIntegrationClient;
import com.proyecto.servicios.exception.GestoPagoIntegrationException;
import com.proyecto.servicios.model.gestopago.EchoResponse;
import com.proyecto.servicios.model.gestopago.ProductListResponse;
import com.proyecto.servicios.model.gestopago.ValidateMeResponse;
import com.proyecto.servicios.service.GestoPagoIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;

@Slf4j
@Service
@RequiredArgsConstructor
public class GestoPagoIntegrationServiceImpl implements GestoPagoIntegrationService {

    private final GestoPagoIntegrationClient client;

    @Override
    @Retryable(value = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public ProductListResponse getProductList() {
        log.info("Iniciando peticion a GestoPago: getProductList");
        try {
            ProductListResponse response = client.getProductList();
            log.info("Peticion a GestoPago finalizada correctamente: getProductList");
            return response;
        } catch (Exception e) {
            log.error("Error al obtener la lista de productos de GestoPago: {}", e.getMessage());
            throw new GestoPagoIntegrationException("getProductList",
                    "Error al obtener el catálogo de productos desde GestoPago", e);
        }
    }

    @Override
    public EchoResponse sendEcho() {
        log.info("Iniciando peticion a GestoPago: sendEcho");
        try {
            EchoResponse response = client.sendEcho();
            log.info("Peticion a GestoPago finalizada correctamente: sendEcho");
            return response;
        } catch (Exception e) {
            log.error("Error al enviar Echo a GestoPago: {}", e.getMessage());
            throw new GestoPagoIntegrationException("sendEcho",
                    "Error al verificar conectividad con GestoPago (sendEcho)", e);
        }
    }

    @Override
    public ValidateMeResponse validateMe() {
        log.info("Iniciando peticion a GestoPago: validateMe");
        try {
            ValidateMeResponse response = client.validateMe();
            log.info("Peticion a GestoPago finalizada correctamente: validateMe");
            return response;
        } catch (Exception e) {
            log.error("Error al validar el comercio en GestoPago: {}", e.getMessage());
            throw new GestoPagoIntegrationException("validateMe",
                    "Error al validar las credenciales del comercio en GestoPago (validateMe)", e);
        }
    }

    @Override
    public com.proyecto.servicios.model.gestopago.GestoPagoSendTxResponse sendTx(String random, String signed) {
        log.info("Iniciando peticion a GestoPago: sendTx con random={}", random);
        try {
            java.util.Map<String, String> form = new java.util.HashMap<>();
            form.put("random", random);
            form.put("signed", signed);

            com.proyecto.servicios.model.gestopago.GestoPagoSendTxResponse response = client.sendTx(form);
            log.info("Peticion a GestoPago finalizada: sendTx. Codigo={}, numAutorizacion={}", 
                    response != null ? response.getCodigoFinal() : "null",
                    response != null ? response.getNumAutorizacion() : "null");
            return response;
        } catch (Exception e) {
            log.error("Error al enviar transaccion sendTx a GestoPago: {}", e.getMessage(), e);
            throw new RuntimeException("Error en integración sendTx con GestoPago: " + e.getMessage(), e);
        }
    }
}
