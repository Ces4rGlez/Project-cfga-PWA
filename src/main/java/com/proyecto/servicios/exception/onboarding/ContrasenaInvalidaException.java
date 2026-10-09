package com.proyecto.servicios.exception.onboarding;

public class ContrasenaInvalidaException extends RuntimeException {
    public ContrasenaInvalidaException(String message) {
        super(message);
    }
}
