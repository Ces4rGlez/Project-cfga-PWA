package com.proyecto.servicios.service.onboarding;

import com.proyecto.servicios.model.response.onboarding.CuentaResponseDTO;

import java.util.List;

/**
 * Interfaz del servicio de cuentas bancarias.
 * Define las operaciones de consulta requeridas sobre cuentas.
 */
public interface CuentaService {

    /**
     * Consulta una cuenta por su número de cuenta único.
     * @throws RecursoNoEncontradoException si no existe
     */
    CuentaResponseDTO obtenerCuentaPorNumero(String numeroCuenta);

    /**
     * Consulta todas las cuentas activas.
     */
    List<CuentaResponseDTO> obtenerCuentasActivas();

    /**
     * Consulta el saldo de una cuenta por su número.
     * @throws RecursoNoEncontradoException si no existe
     */
    CuentaResponseDTO consultarSaldo(String numeroCuenta);
}
