package com.proyecto.servicios.service;

import com.proyecto.servicios.model.gestopago.EchoResponse;
import com.proyecto.servicios.model.gestopago.ProductListResponse;
import com.proyecto.servicios.model.gestopago.ValidateMeResponse;

import com.proyecto.servicios.model.gestopago.GestoPagoSendTxResponse;

public interface GestoPagoIntegrationService {
    ProductListResponse getProductList();
    EchoResponse sendEcho();
    ValidateMeResponse validateMe();
    GestoPagoSendTxResponse sendTx(String random, String signed);
}
