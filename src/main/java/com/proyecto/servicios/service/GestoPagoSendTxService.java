package com.proyecto.servicios.service;

import com.proyecto.servicios.model.request.gestopago.SendTxRequestDTO;
import com.proyecto.servicios.model.response.gestopago.SendTxResultDTO;

public interface GestoPagoSendTxService {

    /**
     * Procesa una venta o pago de servicio a través de sendTx de GestoPago,
     * aplicando todas las reglas de negocio bancarias y de integración.
     *
     * @param request Datos de la transacción solicitada
     * @return Resultado detallado y estatus de la operación
     */
    SendTxResultDTO procesarTransaccion(SendTxRequestDTO request);
}
