package com.proyecto.servicios.service.onboarding.impl;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Domicilio;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.exception.onboarding.*;
import com.proyecto.servicios.mapper.onboarding.ClienteMapper;
import com.proyecto.servicios.model.request.onboarding.ClienteActualizaDTO;
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
                .segundoNombre(dto.getSegundoNombre())
                .apellidoPaterno(dto.getApellidoPaterno())
                .apellidoMaterno(dto.getApellidoMaterno())
                .fechaNacimiento(dto.getFechaNacimiento())
                .curp(dto.getCurp())
                .rfc(dto.getRfc())
                .sexo(dto.getSexo())
                .nacionalidad(dto.getNacionalidad())
                .estadoCivil(dto.getEstadoCivil())
                .telefonoMovil(dto.getTelefonoMovil())
                .telefonoAlternativo(dto.getTelefonoAlternativo())
                .ocupacion(dto.getOcupacion())
                .empresa(dto.getEmpresa())
                .ingresoMensual(dto.getIngresoMensual())
                .build();

        // --- 3. Crear Domicilio ---
        Domicilio domicilio = Domicilio.builder()
                .calle(dto.getCalle())
                .numeroExterior(dto.getNumeroExterior())
                .numeroInterior(dto.getNumeroInterior())
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

        Cliente clienteActualizado = clienteRepository.save(cliente);
        log.info("Cliente con ID: {} actualizado exitosamente", id);

        return clienteMapper.toResponseDTO(clienteActualizado);
    }

    /**
     * Baja lógica: desactiva el usuario y cambia todas sus cuentas a INACTIVA.
     * No elimina ningún registro de la base de datos.
     */
    @Override
    @Transactional
    public void darDeBajaCliente(Long id) {
        log.info("Realizando baja lógica del cliente con ID: {}", id);

        Cliente cliente = clienteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el cliente con ID: " + id));

        // Desactivar usuario
        cliente.getUsuario().setActivo(false);

        // Desactivar todas las cuentas del cliente
        if (cliente.getCuentas() != null) {
            cliente.getCuentas().forEach(cuenta -> cuenta.setEstatus("INACTIVA"));
        }

        clienteRepository.save(cliente);
        log.info("Baja lógica completada para cliente con ID: {}", id);
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
}
