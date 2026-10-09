package com.proyecto.servicios.exception.onboarding;

/**
 * Se lanza cuando un usuario con cuenta desactivada intenta realizar
 * una operación de escritura (PUT, DELETE, cambio de contraseña, etc.).
 * Solo se le permite consultar información en modo solo lectura.
 */
public class CuentaDesactivadaException extends RuntimeException {
    public CuentaDesactivadaException(String message) {
        super(message);
    }
}
