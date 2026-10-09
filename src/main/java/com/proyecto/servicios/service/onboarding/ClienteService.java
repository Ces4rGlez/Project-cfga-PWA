package com.proyecto.servicios.service.onboarding;

import com.proyecto.servicios.model.request.onboarding.ClienteActualizaDTO;
import com.proyecto.servicios.model.request.onboarding.ClienteRegistroDTO;
import com.proyecto.servicios.model.response.onboarding.ClienteResponseDTO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Interfaz del servicio de onboarding de clientes.
 * Define todas las operaciones de negocio requeridas para el
 * registro, consulta, actualización y baja lógica de clientes personas físicas.
 */
public interface ClienteService {

    /**
     * Registra un nuevo cliente con toda su información:
     * crea Usuario (con password cifrada), Cliente, Domicilio y Cuenta bancaria.
     * Todas las operaciones se ejecutan en una sola transacción.
     */
    ClienteResponseDTO registrarCliente(ClienteRegistroDTO dto);

    /**
     * Consulta todos los clientes registrados.
     */
    List<ClienteResponseDTO> obtenerTodosLosClientes();

    /**
     * Consulta un cliente por su ID.
     * @throws RecursoNoEncontradoException si no existe
     */
    ClienteResponseDTO obtenerClientePorId(Long id);

    /**
     * Consulta un cliente por su CURP.
     * @throws RecursoNoEncontradoException si no existe
     */
    ClienteResponseDTO obtenerClientePorCurp(String curp);

    /**
     * Consulta un cliente por su RFC.
     * @throws RecursoNoEncontradoException si no existe
     */
    ClienteResponseDTO obtenerClientePorRfc(String rfc);

    /**
     * Consulta un cliente por su correo electrónico.
     * @throws RecursoNoEncontradoException si no existe
     */
    ClienteResponseDTO obtenerClientePorCorreo(String correo);

    /**
     * Consulta un cliente por el número de su cuenta bancaria.
     * @throws RecursoNoEncontradoException si no existe
     */
    ClienteResponseDTO obtenerClientePorNumeroCuenta(String numeroCuenta);

    /**
     * Actualiza datos personales, de contacto, domicilio e información laboral.
     * No permite modificar CURP, RFC ni número de cuenta.
     * @throws RecursoNoEncontradoException si el cliente no existe
     */
    ClienteResponseDTO actualizarCliente(Long id, ClienteActualizaDTO dto);

    /**
     * Realiza la baja lógica de un cliente: desactiva el usuario
     * y cambia el estatus de sus cuentas a INACTIVA.
     * No elimina datos de la base de datos.
     * @throws RecursoNoEncontradoException si el cliente no existe
     */
    void darDeBajaCliente(Long id);

    /**
     * Consulta solo los clientes activos (usuario activo = true).
     */
    List<ClienteResponseDTO> obtenerClientesActivos();

    /**
     * Consulta los clientes registrados en un rango de fechas.
     */
    List<ClienteResponseDTO> obtenerClientesPorRangoFechas(LocalDateTime desde, LocalDateTime hasta);
}
