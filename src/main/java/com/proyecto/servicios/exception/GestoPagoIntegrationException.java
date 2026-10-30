package com.proyecto.servicios.exception;

/**
 * Excepcion lanzada cuando falla la comunicacion con el proveedor externo GestoPago/PuntoRed.
 * Permite al GlobalExceptionHandler diferenciarla de otros errores internos
 * y retornar un HTTP 502/503 con un mensaje especifico, sin exponer detalles tecnicos al cliente.
 */
public class GestoPagoIntegrationException extends RuntimeException {

    private final String operacion;

    public GestoPagoIntegrationException(String operacion, String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.operacion = operacion;
    }

    public GestoPagoIntegrationException(String operacion, String mensaje) {
        super(mensaje);
        this.operacion = operacion;
    }

    public String getOperacion() {
        return operacion;
    }
}

