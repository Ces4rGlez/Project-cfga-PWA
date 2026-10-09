package com.proyecto.servicios.service.onboarding.impl;

import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.exception.onboarding.RecursoNoEncontradoException;
import com.proyecto.servicios.mapper.onboarding.ClienteMapper;
import com.proyecto.servicios.model.response.onboarding.CuentaResponseDTO;
import com.proyecto.servicios.repository.onboarding.CuentaRepository;
import com.proyecto.servicios.service.onboarding.CuentaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de cuentas bancarias.
 * Proporciona las operaciones de consulta sobre cuentas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CuentaServiceImpl implements CuentaService {

    private final CuentaRepository cuentaRepository;
    private final ClienteMapper clienteMapper;

    @Override
    @Transactional(readOnly = true)
    public CuentaResponseDTO obtenerCuentaPorNumero(String numeroCuenta) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la cuenta con número: " + numeroCuenta));
        return clienteMapper.toCuentaDTO(cuenta);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaResponseDTO> obtenerCuentasActivas() {
        List<Cuenta> cuentas = cuentaRepository.findByEstatus("ACTIVA");
        return cuentas.stream()
                .map(clienteMapper::toCuentaDTO)
                .collect(Collectors.toList());
    }

    /**
     * Consulta el saldo de una cuenta.
     * Retorna el DTO completo de la cuenta que incluye el saldo.
     */
    @Override
    @Transactional(readOnly = true)
    public CuentaResponseDTO consultarSaldo(String numeroCuenta) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la cuenta con número: " + numeroCuenta));
        return clienteMapper.toCuentaDTO(cuenta);
    }
}
