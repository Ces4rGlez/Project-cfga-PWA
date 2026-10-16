package com.proyecto.servicios.service.onboarding.impl;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Domicilio;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.exception.onboarding.*;
import com.proyecto.servicios.mapper.onboarding.ClienteMapper;
import com.proyecto.servicios.model.request.onboarding.ClienteActualizaDTO;
import com.proyecto.servicios.model.request.onboarding.ClienteFiltroDTO;
import com.proyecto.servicios.model.request.onboarding.ClienteRegistroDTO;
import com.proyecto.servicios.model.response.onboarding.ClienteResponseDTO;
import com.proyecto.servicios.repository.onboarding.ClienteRepository;
import com.proyecto.servicios.repository.onboarding.CuentaRepository;
import com.proyecto.servicios.repository.onboarding.UsuarioRepository;
import com.proyecto.servicios.service.onboarding.ClienteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;

/**
 * Implementación del servicio de onboarding de clientes.
 * Contiene toda la lógica de negocio: validaciones, registro transaccional,
 * consultas, actualización y baja lógica.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final CuentaRepository cuentaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final ClienteMapper clienteMapper;

    // Saldo inicial definido por el sistema
    private static final BigDecimal SALDO_INICIAL = new BigDecimal("500.00");

    /**
     * Registra un nuevo cliente con todas sus entidades asociadas en una sola transacción:
     * 1. Valida reglas de negocio (mayoría de edad, unicidad de CURP/RFC/correo)
     * 2. Crea Usuario con password cifrada (BCrypt)
     * 3. Crea Cliente con datos personales, contacto e información laboral
     * 4. Crea Domicilio asociado al cliente
     * 5. Crea Cuenta bancaria con número único y saldo inicial
     */
    @Override
    @Transactional
    public ClienteResponseDTO registrarCliente(ClienteRegistroDTO dto) {
        log.info("Iniciando registro de cliente con CURP: {}", dto.getCurp());

        // --- Validaciones de reglas de negocio ---
        validarMayoriaDeEdad(dto.getFechaNacimiento());
        validarCongruenciaCurp(dto.getCurp(), dto.getFechaNacimiento(), dto.getSexo());
        validarUnicidadCurp(dto.getCurp());
        validarUnicidadRfc(dto.getRfc());
        validarUnicidadCorreo(dto.getCorreoElectronico());

        // --- 1. Crear Usuario ---
        Usuario usuario = Usuario.builder()
                .correoElectronico(dto.getCorreoElectronico())
                .password(passwordEncoder.encode(dto.getPassword()))
                .activo(true)
                .build();

        // --- 2. Crear Cliente ---
        Cliente cliente = Cliente.builder()
                .usuario(usuario)
                .nombre(dto.getNombre())
                .segundoNombre(limpiarTextoOpcional(dto.getSegundoNombre()))
                .apellidoPaterno(dto.getApellidoPaterno())
                .apellidoMaterno(limpiarTextoOpcional(dto.getApellidoMaterno()))
                .fechaNacimiento(dto.getFechaNacimiento())
                .curp(dto.getCurp())
                .rfc(dto.getRfc())
                .sexo(dto.getSexo())
                .nacionalidad(dto.getNacionalidad())
                .estadoCivil(dto.getEstadoCivil())
                .telefonoMovil(dto.getTelefonoMovil())
                .telefonoAlternativo(limpiarTextoOpcional(dto.getTelefonoAlternativo()))
                .ocupacion(dto.getOcupacion())
                .empresa(dto.getEmpresa())
                .ingresoMensual(dto.getIngresoMensual())
                .build();

        // --- 3. Crear Domicilio ---
        Domicilio domicilio = Domicilio.builder()
                .calle(dto.getCalle())
                .numeroExterior(dto.getNumeroExterior())
                .numeroInterior(limpiarTextoOpcional(dto.getNumeroInterior()))
                .colonia(dto.getColonia())
                .municipio(dto.getMunicipio())
                .estado(dto.getEstado())
                .codigoPostal(dto.getCodigoPostal())
                .pais(dto.getPais())
                .build();
        cliente.setDomicilio(domicilio);

        // --- 4. Crear Cuenta bancaria ---
        Cuenta cuenta = Cuenta.builder()
                .numeroCuenta(generarNumeroCuentaUnico())
                .saldo(SALDO_INICIAL)
                .estatus("ACTIVA")
                .build();
        cliente.addCuenta(cuenta);

        // --- 5. Persistir todo en cascada ---
        Cliente clienteGuardado = clienteRepository.save(cliente);
        log.info("Cliente registrado exitosamente con ID: {}", clienteGuardado.getIdCliente());

        return clienteMapper.toResponseDTO(clienteGuardado);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> obtenerTodosLosClientes() {
        // Escudo de Memoria (Anti-OOM): Limita la consulta masiva a los primeros 100 registros
        // En un ataque masivo de JMeter con miles de registros, cargar todos tiraría el servidor.
        List<Cliente> clientes = clienteRepository.findAll(PageRequest.of(0, 100)).getContent();
        return clienteMapper.toResponseDTOList(clientes);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClienteResponseDTO> buscarClientes(Pageable pageable) {
        return clienteRepository.findAll(pageable).map(clienteMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClienteResponseDTO> buscarClientes(String filtro, Pageable pageable) {
        if (filtro == null || filtro.isBlank()) {
            return buscarClientes(pageable);
        }
        return clienteRepository.buscarPorFiltro(filtro.trim(), pageable).map(clienteMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClienteResponseDTO> buscarClientes(ClienteFiltroDTO filtro, Pageable pageable) {
        if (filtro == null) {
            return buscarClientes(pageable);
        }
        if (filtro.getFiltro() != null && !filtro.getFiltro().isBlank()) {
            return buscarClientes(filtro.getFiltro(), pageable);
        }
        if (filtro.getNombre() != null && !filtro.getNombre().isBlank()) {
            return clienteRepository.findByNombreContainingIgnoreCase(filtro.getNombre().trim(), pageable)
                    .map(clienteMapper::toResponseDTO);
        }
        if (filtro.getCurp() != null && !filtro.getCurp().isBlank()) {
            return clienteRepository.findByCurpContainingIgnoreCase(filtro.getCurp().trim(), pageable)
                    .map(clienteMapper::toResponseDTO);
        }
        if (filtro.getRfc() != null && !filtro.getRfc().isBlank()) {
            return clienteRepository.findByRfcContainingIgnoreCase(filtro.getRfc().trim(), pageable)
                    .map(clienteMapper::toResponseDTO);
        }
        return buscarClientes(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClienteResponseDTO> consultarClientes(ClienteFiltroDTO filtro) {
        Pageable pageable = (filtro != null) ? filtro.toPageable() : PageRequest.of(0, 10);
        var spec = com.proyecto.servicios.repository.onboarding.ClienteSpecification.conFiltros(filtro);
        return clienteRepository.findAll(spec, pageable).map(clienteMapper::toResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ClienteResponseDTO> buscarClientes(ClienteFiltroDTO filtro) {
        return consultarClientes(filtro);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO obtenerClientePorId(Long id) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el cliente con ID: " + id));
        return clienteMapper.toResponseDTO(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO obtenerClientePorCurp(String curp) {
        Cliente cliente = clienteRepository.findByCurp(curp)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el cliente con CURP: " + curp));
        return clienteMapper.toResponseDTO(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO obtenerClientePorRfc(String rfc) {
        Cliente cliente = clienteRepository.findByRfc(rfc)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el cliente con RFC: " + rfc));
        return clienteMapper.toResponseDTO(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO obtenerClientePorCorreo(String correo) {
        Cliente cliente = clienteRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el cliente con correo: " + correo));
        return clienteMapper.toResponseDTO(cliente);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO obtenerClientePorNumeroCuenta(String numeroCuenta) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró la cuenta con número: " + numeroCuenta));
        return clienteMapper.toResponseDTO(cuenta.getCliente());
    }

    /**
     * Actualiza datos personales, contacto, domicilio e información laboral.
     * No permite modificar CURP, RFC ni número de cuenta (campos inmutables).
     * Si el correo cambia, se valida que no esté duplicado y se actualiza también en el usuario.
     */
    @Override
    @Transactional
    public ClienteResponseDTO actualizarCliente(Long id, ClienteActualizaDTO dto) {
        log.info("Actualizando cliente con ID: {}", id);

        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el cliente con ID: " + id));

        // Si el correo cambió, validar que no esté duplicado
        String correoActual = cliente.getUsuario().getCorreoElectronico();
        if (!correoActual.equalsIgnoreCase(dto.getCorreoElectronico())) {
            validarUnicidadCorreo(dto.getCorreoElectronico());
            cliente.getUsuario().setCorreoElectronico(dto.getCorreoElectronico());
        }

        // Actualizar datos personales
        cliente.setNombre(dto.getNombre());
        cliente.setSegundoNombre(dto.getSegundoNombre());
        cliente.setApellidoPaterno(dto.getApellidoPaterno());
        cliente.setApellidoMaterno(dto.getApellidoMaterno());
        cliente.setSexo(dto.getSexo());
        cliente.setNacionalidad(dto.getNacionalidad());
        cliente.setEstadoCivil(dto.getEstadoCivil());

        // Actualizar datos de contacto
        cliente.setTelefonoMovil(dto.getTelefonoMovil());
        cliente.setTelefonoAlternativo(dto.getTelefonoAlternativo());

        // Actualizar domicilio
        Domicilio domicilio = cliente.getDomicilio();
        if (domicilio != null) {
            domicilio.setCalle(dto.getCalle());
            domicilio.setNumeroExterior(dto.getNumeroExterior());
            domicilio.setNumeroInterior(dto.getNumeroInterior());
            domicilio.setColonia(dto.getColonia());
            domicilio.setMunicipio(dto.getMunicipio());
            domicilio.setEstado(dto.getEstado());
            domicilio.setCodigoPostal(dto.getCodigoPostal());
            domicilio.setPais(dto.getPais());
        }

        // Actualizar información laboral
        cliente.setOcupacion(dto.getOcupacion());
        cliente.setEmpresa(dto.getEmpresa());
        cliente.setIngresoMensual(dto.getIngresoMensual());

        // Sistema de banderitas: actualizar estatus si se envía la bandera booleana activo
        if (dto.getActivo() != null) {
            boolean activo = dto.getActivo();
            cliente.getUsuario().setActivo(activo);
            if (cliente.getCuentas() != null) {
                String estatusCuenta = activo ? "ACTIVA" : "INACTIVA";
                cliente.getCuentas().forEach(c -> c.setEstatus(estatusCuenta));
            }
            log.info("Estatus del cliente ID {} actualizado con bandera activo={}", id, activo);
        }

        Cliente clienteActualizado = clienteRepository.save(cliente);
        log.info("Cliente con ID: {} actualizado exitosamente", id);

        return clienteMapper.toResponseDTO(clienteActualizado);
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDTO obtenerPorLlaveCompuesta(com.proyecto.servicios.model.request.onboarding.ClienteConsultaCompuestaDTO dto) {
        if (dto == null || dto.getNombre() == null || dto.getCorreo() == null) {
            throw new IllegalArgumentException("El nombre y el correo electrónico son obligatorios");
        }
        Cliente cliente = clienteRepository.findByNombreAndCorreo(dto.getNombre().trim(), dto.getCorreo().trim())
                .orElseThrow(() -> new UsuarioNoEncontradoException(
                        "No existe un usuario con el nombre y correo electrónico proporcionados."));
        return clienteMapper.toResponseDTO(cliente);
    }

    private Cliente localizarClientePorGestionDTO(com.proyecto.servicios.model.request.onboarding.ClienteGestionDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("El cuerpo de la solicitud no puede ser nulo");
        }
        if (dto.getIdCliente() != null) {
            return clienteRepository.findById(dto.getIdCliente())
                    .orElseThrow(() -> new RecursoNoEncontradoException(
                            "No se encontró el cliente con ID: " + dto.getIdCliente()));
        }
        if (dto.getNombre() != null && !dto.getNombre().isBlank() &&
            dto.getCorreoElectronico() != null && !dto.getCorreoElectronico().isBlank()) {
            return clienteRepository.findByNombreAndCorreo(dto.getNombre().trim(), dto.getCorreoElectronico().trim())
                    .orElseThrow(() -> new UsuarioNoEncontradoException(
                            "No existe un usuario con el nombre y correo electrónico proporcionados."));
        }
        throw new IllegalArgumentException("Debe proporcionar el idCliente o la llave compuesta (nombre y correo electrónico) para la operación");
    }

    @Override
    @Transactional
    public com.proyecto.servicios.model.response.onboarding.GestionResultadoDTO gestionarCliente(
            com.proyecto.servicios.model.request.onboarding.ClienteGestionDTO dto) {

        Cliente cliente = localizarClientePorGestionDTO(dto);

        boolean actualizar = dto.getActualizar() == null || dto.getActualizar();

        if (actualizar) {
            ClienteResponseDTO actualizado = actualizarClienteEntidad(cliente, dto);
            return com.proyecto.servicios.model.response.onboarding.GestionResultadoDTO.builder()
                    .mensaje("Cliente actualizado exitosamente mediante sistema de banderas")
                    .cliente(actualizado)
                    .build();
        } else {
            boolean permanente = Boolean.TRUE.equals(dto.getPermanente());
            eliminarCliente(cliente.getIdCliente(), permanente);
            String mensaje = permanente
                    ? "Cliente eliminado permanentemente de la base de datos mediante bandera booleana"
                    : "Cliente dado de baja exitosamente (baja lógica) mediante bandera booleana";
            return com.proyecto.servicios.model.response.onboarding.GestionResultadoDTO.builder()
                    .mensaje(mensaje)
                    .cliente(null)
                    .build();
        }
    }

    @Override
    @Transactional
    public ClienteResponseDTO actualizarCliente(com.proyecto.servicios.model.request.onboarding.ClienteGestionDTO dto) {
        Cliente cliente = localizarClientePorGestionDTO(dto);
        return actualizarClienteEntidad(cliente, dto);
    }

    private ClienteResponseDTO actualizarClienteEntidad(Cliente cliente, com.proyecto.servicios.model.request.onboarding.ClienteGestionDTO dto) {
        Long id = cliente.getIdCliente();

        // Si el correo cambió, validar que no esté duplicado
        if (dto.getCorreoElectronico() != null && !dto.getCorreoElectronico().isBlank()) {
            String correoActual = cliente.getUsuario().getCorreoElectronico();
            if (!correoActual.equalsIgnoreCase(dto.getCorreoElectronico().trim())) {
                validarUnicidadCorreo(dto.getCorreoElectronico().trim());
                cliente.getUsuario().setCorreoElectronico(dto.getCorreoElectronico().trim());
            }
        }

        // Actualizar datos personales si vienen informados
        if (dto.getNombre() != null && !dto.getNombre().isBlank()) cliente.setNombre(dto.getNombre().trim());
        if (dto.getSegundoNombre() != null) cliente.setSegundoNombre(dto.getSegundoNombre().trim());
        if (dto.getApellidoPaterno() != null && !dto.getApellidoPaterno().isBlank()) cliente.setApellidoPaterno(dto.getApellidoPaterno().trim());
        if (dto.getApellidoMaterno() != null) cliente.setApellidoMaterno(dto.getApellidoMaterno().trim());
        if (dto.getSexo() != null && !dto.getSexo().isBlank()) cliente.setSexo(dto.getSexo().trim());
        if (dto.getNacionalidad() != null && !dto.getNacionalidad().isBlank()) cliente.setNacionalidad(dto.getNacionalidad().trim());
        if (dto.getEstadoCivil() != null && !dto.getEstadoCivil().isBlank()) cliente.setEstadoCivil(dto.getEstadoCivil().trim());

        // Actualizar contacto
        if (dto.getTelefonoMovil() != null && !dto.getTelefonoMovil().isBlank()) cliente.setTelefonoMovil(dto.getTelefonoMovil().trim());
        if (dto.getTelefonoAlternativo() != null) cliente.setTelefonoAlternativo(dto.getTelefonoAlternativo().trim());

        // Actualizar domicilio
        Domicilio domicilio = cliente.getDomicilio();
        if (domicilio != null) {
            if (dto.getCalle() != null && !dto.getCalle().isBlank()) domicilio.setCalle(dto.getCalle().trim());
            if (dto.getNumeroExterior() != null && !dto.getNumeroExterior().isBlank()) domicilio.setNumeroExterior(dto.getNumeroExterior().trim());
            if (dto.getNumeroInterior() != null) domicilio.setNumeroInterior(dto.getNumeroInterior().trim());
            if (dto.getColonia() != null && !dto.getColonia().isBlank()) domicilio.setColonia(dto.getColonia().trim());
            if (dto.getMunicipio() != null && !dto.getMunicipio().isBlank()) domicilio.setMunicipio(dto.getMunicipio().trim());
            if (dto.getEstado() != null && !dto.getEstado().isBlank()) domicilio.setEstado(dto.getEstado().trim());
            if (dto.getCodigoPostal() != null && !dto.getCodigoPostal().isBlank()) domicilio.setCodigoPostal(dto.getCodigoPostal().trim());
            if (dto.getPais() != null && !dto.getPais().isBlank()) domicilio.setPais(dto.getPais().trim());
        }

        // Actualizar laboral
        if (dto.getOcupacion() != null && !dto.getOcupacion().isBlank()) cliente.setOcupacion(dto.getOcupacion().trim());
        if (dto.getEmpresa() != null && !dto.getEmpresa().isBlank()) cliente.setEmpresa(dto.getEmpresa().trim());
        if (dto.getIngresoMensual() != null) cliente.setIngresoMensual(dto.getIngresoMensual());

        // Sistema de banderas: actualizar estatus si viene bandera activo
        if (dto.getActivo() != null) {
            boolean activo = dto.getActivo();
            cliente.getUsuario().setActivo(activo);
            if (cliente.getCuentas() != null) {
                String estatusCuenta = activo ? "ACTIVA" : "INACTIVA";
                cliente.getCuentas().forEach(c -> c.setEstatus(estatusCuenta));
            }
        }

        Cliente guardado = clienteRepository.save(cliente);
        log.info("Cliente con ID: {} actualizado exitosamente vía PATCH", id);
        return clienteMapper.toResponseDTO(guardado);
    }

    @Override
    @Transactional
    public ClienteResponseDTO actualizarCliente(ClienteActualizaDTO dto) {
        if (dto == null || dto.getIdCliente() == null) {
            throw new IllegalArgumentException("Se requiere el ID del cliente para realizar la actualización");
        }
        return actualizarCliente(dto.getIdCliente(), dto);
    }

    /**
     * Sistema de banderitas booleanas para activar / desactivar:
     * - Si activo es true: Reactiva el usuario y pasa sus cuentas a "ACTIVA".
     * - Si activo es false: Desactiva el usuario y pasa sus cuentas a "INACTIVA" (baja lógica).
     */
    @Override
    @Transactional
    public ClienteResponseDTO cambiarEstatusCliente(Long id, boolean activo) {
        log.info("Cambiando estatus del cliente ID: {} con banderita activo={}", id, activo);

        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el cliente con ID: " + id));

        if (activo) {
            // Banderita TRUE: Activar cliente y sus cuentas
            cliente.getUsuario().setActivo(true);
            if (cliente.getCuentas() != null) {
                cliente.getCuentas().forEach(cuenta -> cuenta.setEstatus("ACTIVA"));
            }
            log.info("Cliente con ID {} ACTIVADO con éxito", id);
        } else {
            // Banderita FALSE: Desactivar cliente y sus cuentas (Baja lógica)
            cliente.getUsuario().setActivo(false);
            if (cliente.getCuentas() != null) {
                cliente.getCuentas().forEach(cuenta -> cuenta.setEstatus("INACTIVA"));
            }
            log.info("Cliente con ID {} DESACTIVADO (Baja lógica) con éxito", id);
        }

        Cliente guardado = clienteRepository.save(cliente);
        return clienteMapper.toResponseDTO(guardado);
    }

    /**
     * Sistema de banderitas booleanas para eliminación:
     * - Si eliminacionFisica es true: Elimina físicamente de la base de datos (DELETE).
     * - Si eliminacionFisica es false: Aplica baja lógica (desactivar).
     */
    @Override
    @Transactional
    public void eliminarCliente(Long id, boolean eliminacionFisica) {
        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el cliente con ID: " + id));

        if (eliminacionFisica) {
            clienteRepository.delete(cliente);
            log.info("Cliente con ID {} ELIMINADO FÍSICAMENTE de la base de datos", id);
        } else {
            cambiarEstatusCliente(id, false);
        }
    }

    @Override
    @Transactional
    public void darDeBajaCliente(Long id) {
        cambiarEstatusCliente(id, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> obtenerClientesActivos() {
        List<Cliente> clientes = clienteRepository.findClientesActivos();
        return clienteMapper.toResponseDTOList(clientes);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDTO> obtenerClientesPorRangoFechas(LocalDateTime desde, LocalDateTime hasta) {
        List<Cliente> clientes = clienteRepository.findByFechaRegistroBetween(desde, hasta);
        return clienteMapper.toResponseDTOList(clientes);
    }

    // ==========================================
    // Métodos privados de validación
    // ==========================================

    /**
     * Valida que el cliente tenga al menos 18 años de edad.
     */
    private void validarMayoriaDeEdad(LocalDate fechaNacimiento) {
        int edad = Period.between(fechaNacimiento, LocalDate.now()).getYears();
        if (edad < 18) {
            throw new IllegalArgumentException(
                    "El cliente debe ser mayor de edad (18 años o más). Edad calculada: " + edad);
        }
    }

    /**
     * Valida que no exista otro cliente con la misma CURP.
     */
    private void validarUnicidadCurp(String curp) {
        if (clienteRepository.existsByCurp(curp)) {
            throw new CurpDuplicadaException("Ya existe un cliente registrado con la CURP: " + curp);
        }
    }

    /**
     * Valida que no exista otro cliente con el mismo RFC.
     */
    private void validarUnicidadRfc(String rfc) {
        if (clienteRepository.existsByRfc(rfc)) {
            throw new RfcDuplicadoException("Ya existe un cliente registrado con el RFC: " + rfc);
        }
    }

    /**
     * Valida que no exista otro usuario con el mismo correo electrónico.
     */
    private void validarUnicidadCorreo(String correo) {
        if (usuarioRepository.existsByCorreoElectronico(correo)) {
            throw new CorreoDuplicadoException(
                    "Ya existe un usuario registrado con el correo: " + correo);
        }
    }

    /**
     * Genera un número de cuenta único de 10 dígitos.
     * Utiliza los dígitos del timestamp + un segmento aleatorio.
     * Verifica contra la BD que no exista previamente.
     */
    private String generarNumeroCuentaUnico() {
        String numeroCuenta;
        do {
            // Combina timestamp y UUID para generar un número de 10 dígitos
            long timestamp = System.currentTimeMillis();
            String uuidDigits = UUID.randomUUID().toString().replaceAll("[^0-9]", "");
            String combined = String.valueOf(timestamp) + uuidDigits;
            // Tomar los últimos 10 dígitos
            numeroCuenta = combined.substring(combined.length() - 10);
        } while (cuentaRepository.existsByNumeroCuenta(numeroCuenta));

        log.debug("Número de cuenta generado: {}", numeroCuenta);
        return numeroCuenta;
    }

    /**
     * Valida la congruencia lógica de la CURP comparándola contra 
     * la fecha de nacimiento y el sexo biológico proporcionados.
     */
    private void validarCongruenciaCurp(String curp, LocalDate fechaNacimiento, String sexo) {
        if (curp == null || curp.length() < 18) return; // Salvaguarda, aunque el DTO ya lo valida

        // 1. Validar Congruencia de Fecha de Nacimiento (Posiciones 4 al 9 de la CURP = YYMMDD)
        String fechaCurp = curp.substring(4, 10);
        
        String yy = String.format("%02d", fechaNacimiento.getYear() % 100);
        String mm = String.format("%02d", fechaNacimiento.getMonthValue());
        String dd = String.format("%02d", fechaNacimiento.getDayOfMonth());
        String fechaFormateada = yy + mm + dd;
        
        if (!fechaCurp.equals(fechaFormateada)) {
            throw new IllegalArgumentException(
                    "Incongruencia detectada: La fecha de nacimiento proporcionada (" + fechaNacimiento + 
                    ") no coincide con la fecha de nacimiento codificada en la CURP (" + fechaCurp + ").");
        }
        
        // 2. Validar Congruencia de Sexo (Posición 10 de la CURP)
        char sexoCurp = curp.charAt(10);
        char sexoInput = sexo.toUpperCase().charAt(0);
        
        boolean sexoValido = false;
        // En CURP: 'H' es Hombre, 'M' es Mujer. En nuestro API input: 'M' es Masculino, 'F' es Femenino.
        if (sexoInput == 'M' && sexoCurp == 'H') {
            sexoValido = true;
        } else if (sexoInput == 'F' && sexoCurp == 'M') {
            sexoValido = true;
        } else if (sexoInput == 'X') {
            // Si el cliente se identifica como 'X' (No binario), permitimos cualquier marcador biológico en la CURP
            sexoValido = true;
        }
        
        if (!sexoValido) {
            throw new IllegalArgumentException(
                    "Incongruencia detectada: El sexo proporcionado ('" + sexo + 
                    "') no corresponde con el marcador de sexo codificado en la CURP ('" + sexoCurp + "').");
        }
    }

    private String limpiarTextoOpcional(String texto) {
        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }
        return texto.trim();
    }
}
