package com.proyecto.servicios.exception.onboarding;

public class CredencialesInvalidasException extends RuntimeException {
    public CredencialesInvalidasException(String message) {
        super(message);
    }
}
