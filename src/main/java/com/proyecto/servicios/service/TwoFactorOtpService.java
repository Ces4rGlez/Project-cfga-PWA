package com.proyecto.servicios.service;

import com.proyecto.servicios.model.request.gestopago.OtpRequestDTO;
import com.proyecto.servicios.model.response.gestopago.OtpResponseDTO;

/**
 * Servicio para la gestión del Doble Factor de Autenticación (2FA / OTP temporal).
 */
public interface TwoFactorOtpService {

    /**
     * Genera un código OTP de 6 dígitos con vigencia temporal (ej. 5 minutos),
     * lo almacena en Redis/Memoria y lo envía al usuario.
     */
    OtpResponseDTO generarOtp(OtpRequestDTO request);

    /**
     * Valida si el código OTP proporcionado coincide con el emitido y sigue vigente.
     */
    boolean validarOtp(String numeroCuenta, String otp);

    /**
     * Invalida el código OTP una vez utilizado con éxito (One-Time Password).
     */
    void consumirOtp(String numeroCuenta);
}
