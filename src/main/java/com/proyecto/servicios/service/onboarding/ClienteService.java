package com.proyecto.servicios.service.onboarding;

import com.proyecto.servicios.model.request.onboarding.ClienteActualizaDTO;
import com.proyecto.servicios.model.request.onboarding.ClienteFiltroDTO;
import com.proyecto.servicios.model.request.onboarding.ClienteRegistroDTO;
import com.proyecto.servicios.model.response.onboarding.ClienteResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
     * Sobrecarga: Consulta clientes con paginación.
     */
    Page<ClienteResponseDTO> buscarClientes(Pageable pageable);

    /**
     * Sobrecarga: Búsqueda flexible por filtro (RFC, CURP o Nombre con coincidencia parcial ContainingIgnoreCase) y paginada.
     */
    Page<ClienteResponseDTO> buscarClientes(String filtro, Pageable pageable);

    /**
     * Sobrecarga: Búsqueda dinámica basada en el DTO de filtros (ClienteFiltroDTO) con paginación explícita.
     */
    Page<ClienteResponseDTO> buscarClientes(ClienteFiltroDTO filtro, Pageable pageable);

    /**
     * Sobrecarga: Búsqueda basada en el payload completo de ClienteFiltroDTO (usa page, size y sort definidos en el DTO).
     */
    Page<ClienteResponseDTO> buscarClientes(ClienteFiltroDTO filtro);

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
     * Consulta dinámica mediante un único método de búsqueda que combina múltiples criterios.
     */
    Page<ClienteResponseDTO> consultarClientes(ClienteFiltroDTO filtro);

    /**
     * Consulta un usuario/cliente utilizando la llave compuesta por nombre y correo electrónico.
     * @throws UsuarioNoEncontradoException si no existe coincidencia
     */
    ClienteResponseDTO obtenerPorLlaveCompuesta(com.proyecto.servicios.model.request.onboarding.ClienteConsultaCompuestaDTO dto);

    /**
     * Gestión unificada de cliente mediante sistema de banderas (PATCH):
     * - Si actualizar es true: Actualiza datos del cliente.
     * - Si actualizar es false: Da de baja lógica o elimina permanentemente.
     * Retorna GestionResultadoDTO con el mensaje de negocio correspondiente y el cliente resultante.
     */
    com.proyecto.servicios.model.response.onboarding.GestionResultadoDTO gestionarCliente(com.proyecto.servicios.model.request.onboarding.ClienteGestionDTO dto);

    /**
     * Actualiza datos personales a partir de ClienteGestionDTO.
     */
    ClienteResponseDTO actualizarCliente(com.proyecto.servicios.model.request.onboarding.ClienteGestionDTO dto);

    /**
     * Actualiza datos personales, de contacto, domicilio e información laboral
     * recibiendo el DTO con el idCliente en el cuerpo.
     * No permite modificar CURP, RFC ni número de cuenta (campos controlados por el servidor).
     */
    ClienteResponseDTO actualizarCliente(ClienteActualizaDTO dto);

    /**
     * Actualiza datos recibiendo id explícito (sobrecarga).
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
     * Sistema de banderitas booleanas para cambiar el estatus del cliente:
     * - Si activo es true: Reactiva el usuario y pasa sus cuentas a "ACTIVA".
     * - Si activo es false: Desactiva el usuario y pasa sus cuentas a "INACTIVA" (baja lógica).
     */
    ClienteResponseDTO cambiarEstatusCliente(Long id, boolean activo);

    /**
     * Sistema de banderitas booleanas para eliminación:
     * - Si eliminacionFisica es true: Borrado permanente en la base de datos (DELETE).
     * - Si eliminacionFisica es false: Baja lógica (desactiva usuario y cuentas).
     */
    void eliminarCliente(Long id, boolean eliminacionFisica);

    /**
     * Consulta solo los clientes activos (usuario activo = true).
     */
    List<ClienteResponseDTO> obtenerClientesActivos();

    /**
     * Consulta los clientes registrados en un rango de fechas.
     */
    List<ClienteResponseDTO> obtenerClientesPorRangoFechas(LocalDateTime desde, LocalDateTime hasta);
}
