package com.proyecto.servicios.controller.onboarding;

import com.proyecto.servicios.exception.onboarding.CuentaDesactivadaException;
import com.proyecto.servicios.model.request.onboarding.ClienteActualizaDTO;
import com.proyecto.servicios.model.request.onboarding.ClienteRegistroDTO;
import com.proyecto.servicios.model.response.ApiResponse;
import com.proyecto.servicios.model.response.onboarding.ClienteResponseDTO;
import com.proyecto.servicios.service.onboarding.ClienteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controlador REST para el módulo de Onboarding de Clientes Personas Físicas.
 *
 * Endpoints:
 *   POST   /clientes                          → Registrar nuevo cliente
 *   GET    /clientes                          → Listar todos los clientes (con filtros opcionales)
 *   GET    /clientes/{id}                     → Consultar cliente por ID
 *   PUT    /clientes/{id}                     → Actualizar datos del cliente
 *   DELETE /clientes/{id}                     → Baja lógica del cliente
 *
 * Query params opcionales en GET /clientes:
 *   curp, rfc, correo, numeroCuenta, activos, fechaDesde, fechaHasta
 */
@RestController
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    // ==========================================
    // POST /clientes — Registro de un nuevo cliente
    // ==========================================

    /**
     * Registra un nuevo cliente persona física con todos sus datos:
     * datos personales, contacto, domicilio, información laboral,
     * credenciales de acceso, cuenta bancaria y saldo inicial.
     *
     * @param dto Datos del cliente a registrar (validados con @Valid)
     * @return ClienteResponseDTO con el cliente creado y su cuenta asignada
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ClienteResponseDTO>> registrarCliente(
            @Valid @RequestBody ClienteRegistroDTO dto,
            HttpServletRequest request) {

        ClienteResponseDTO cliente = clienteService.registrarCliente(dto);

        ApiResponse<ClienteResponseDTO> response = ApiResponse.<ClienteResponseDTO>builder()
                .code(1)
                .message("Cliente registrado exitosamente")
                .path(request.getRequestURI())
                .data(cliente)
                .build();

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // ==========================================
    // GET /clientes — Consultas con filtros opcionales
    // ==========================================

    /**
     * Consulta clientes con filtros opcionales por query params.
     * Si no se envía ningún parámetro, retorna todos los clientes.
     *
     * Parámetros opcionales:
     *   - curp:         Buscar por CURP exacto
     *   - rfc:          Buscar por RFC exacto
     *   - correo:       Buscar por correo electrónico exacto
     *   - numeroCuenta: Buscar por número de cuenta bancaria
     *   - activos:      Si es true, retorna solo clientes activos
     *   - fechaDesde / fechaHasta: Rango de fechas de registro (ISO 8601)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ClienteResponseDTO>>> consultarClientes(
            @RequestParam(required = false) String curp,
            @RequestParam(required = false) String rfc,
            @RequestParam(required = false) String correo,
            @RequestParam(required = false) String numeroCuenta,
            @RequestParam(required = false) Boolean activos,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta,
            HttpServletRequest request) {

        List<ClienteResponseDTO> resultado;

        // Filtrar por CURP
        if (curp != null && !curp.isBlank()) {
            resultado = List.of(clienteService.obtenerClientePorCurp(curp));
        }
        // Filtrar por RFC
        else if (rfc != null && !rfc.isBlank()) {
            resultado = List.of(clienteService.obtenerClientePorRfc(rfc));
        }
        // Filtrar por correo electrónico
        else if (correo != null && !correo.isBlank()) {
            resultado = List.of(clienteService.obtenerClientePorCorreo(correo));
        }
        // Filtrar por número de cuenta
        else if (numeroCuenta != null && !numeroCuenta.isBlank()) {
            resultado = List.of(clienteService.obtenerClientePorNumeroCuenta(numeroCuenta));
        }
        // Filtrar solo clientes activos
        else if (Boolean.TRUE.equals(activos)) {
            resultado = clienteService.obtenerClientesActivos();
        }
        // Filtrar por rango de fechas de registro
        else if (fechaDesde != null && fechaHasta != null) {
            resultado = clienteService.obtenerClientesPorRangoFechas(fechaDesde, fechaHasta);
        }
        // Sin filtros → retornar todos
        else {
            resultado = clienteService.obtenerTodosLosClientes();
        }

        ApiResponse<List<ClienteResponseDTO>> response = ApiResponse.<List<ClienteResponseDTO>>builder()
                .code(1)
                .message("Consulta exitosa")
                .path(request.getRequestURI())
                .data(resultado)
                .build();

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // GET /clientes/{id} — Consultar por ID
    // ==========================================

    /**
     * Consulta un cliente específico por su ID.
     *
     * @param id ID del cliente
     * @return ClienteResponseDTO con todos los datos del cliente
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ClienteResponseDTO>> obtenerClientePorId(
            @PathVariable Long id,
            HttpServletRequest request) {

        ClienteResponseDTO cliente = clienteService.obtenerClientePorId(id);

        ApiResponse<ClienteResponseDTO> response = ApiResponse.<ClienteResponseDTO>builder()
                .code(1)
                .message("Cliente encontrado")
                .path(request.getRequestURI())
                .data(cliente)
                .build();

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // PUT /clientes/{id} — Actualizar cliente
    // ==========================================

    /**
     * Actualiza los datos de un cliente existente.
     * No permite modificar CURP, RFC ni número de cuenta (campos inmutables).
     *
     * @param id  ID del cliente a actualizar
     * @param dto Datos actualizados (validados con @Valid)
     * @return ClienteResponseDTO con los datos actualizados
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ClienteResponseDTO>> actualizarCliente(
            @PathVariable Long id,
            @Valid @RequestBody ClienteActualizaDTO dto,
            HttpServletRequest request) {

        validarUsuarioActivo();

        ClienteResponseDTO cliente = clienteService.actualizarCliente(id, dto);

        ApiResponse<ClienteResponseDTO> response = ApiResponse.<ClienteResponseDTO>builder()
                .code(1)
                .message("Cliente actualizado exitosamente")
                .path(request.getRequestURI())
                .data(cliente)
                .build();

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // DELETE /clientes/{id} — Baja lógica
    // ==========================================

    /**
     * Realiza la baja lógica de un cliente:
     * - Desactiva el usuario (activo = false)
     * - Cambia el estatus de todas sus cuentas a "INACTIVA"
     * No elimina registros de la base de datos.
     *
     * @param id ID del cliente a dar de baja
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> darDeBajaCliente(
            @PathVariable Long id,
            HttpServletRequest request) {

        validarUsuarioActivo();

        clienteService.darDeBajaCliente(id);

        ApiResponse<Void> response = ApiResponse.<Void>builder()
                .code(1)
                .message("Cliente dado de baja exitosamente (baja lógica)")
                .path(request.getRequestURI())
                .data(null)
                .build();

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // Método privado de autorización
    // ==========================================

    /**
     * Verifica que el usuario autenticado tenga una cuenta activa.
     * Si tiene el rol ROLE_READONLY (cuenta desactivada), lanza una excepción
     * que devuelve HTTP 403 Forbidden indicando modo solo lectura.
     */
    private void validarUsuarioActivo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_READONLY"))) {
            throw new CuentaDesactivadaException(
                    "Su cuenta está desactivada. Solo puede consultar información, no realizar operaciones.");
        }
    }
}
