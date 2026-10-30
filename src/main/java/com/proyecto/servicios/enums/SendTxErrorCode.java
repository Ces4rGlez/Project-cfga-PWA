package com.proyecto.servicios.enums;

import lombok.Getter;

/**
 * Códigos de error y resultado para el flujo sendTx.
 * Cumple con los requerimientos específicos:
 * - sendTx_insufficientFunds (Saldo insuficiente)
 * - sendTx_duplicated (Transacción duplicada)
 * - Y demás códigos de negocio y proveedor.
 */
@Getter
public enum SendTxErrorCode {

    EXITO("01", "Operación realizada con éxito"),
    INSUFFICIENT_FUNDS("sendTx_insufficientFunds", "Saldo insuficiente para realizar la transacción"),
    DUPLICATED("sendTx_duplicated", "Transacción duplicada detectada (UPC o servicio/monto procesado recientemente)"),
    TIMEOUT("sendTx_timeout", "Tiempo de espera alcanzado en GestoPago (código 82)"),
    ACCOUNT_INACTIVE("sendTx_accountInactive", "La cuenta se encuentra inactiva o deshabilitada"),
    ACCOUNT_BLOCKED("sendTx_accountBlocked", "La cuenta se encuentra bloqueada"),
    USER_INACTIVE("sendTx_userInactive", "El usuario asociado a la cuenta bancaria está inactivo"),
    INVALID_PIN("sendTx_invalidPin", "PIN o contraseña de autorización incorrecta"),
    INVALID_PHONE("sendTx_invalidPhone", "El número de teléfono debe contener 10 dígitos válidos"),
    INVALID_REFERENCE("sendTx_invalidReference", "La referencia del servicio es requerida o tiene un formato no válido"),
    PRODUCT_NOT_FOUND("sendTx_productNotFound", "El producto o servicio especificado no existe en el catálogo"),
    OTP_REQUIRED("sendTx_otpRequired", "Se requiere el código de doble factor (OTP) para autorizar la transacción"),
    INVALID_OTP("sendTx_invalidOtp", "El código de doble factor (OTP) es incorrecto o ha expirado"),
    UNAUTHORIZED_ACCOUNT("sendTx_unauthorizedAccount", "No tiene autorización para operar esta cuenta bancaria"),
    INVALID_AMOUNT("sendTx_invalidAmount", "El monto especificado no corresponde con los valores o restricciones permitidas en el catálogo"),
    INVALID_PARAMETER("sendTx_invalidParameter", "Los parámetros del servicio y producto no corresponden entre sí o son inválidos"),
    EXTERNAL_ERROR("sendTx_externalError", "Error reportado por el proveedor de servicios GestoPago");

    private final String codigo;
    private final String descripcion;

    SendTxErrorCode(String codigo, String descripcion) {
        this.codigo = codigo;
        this.descripcion = descripcion;
    }
}
