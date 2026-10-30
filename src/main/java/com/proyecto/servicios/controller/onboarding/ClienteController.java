package com.proyecto.servicios.controller.onboarding;

import com.proyecto.servicios.exception.onboarding.CuentaDesactivadaException;
import com.proyecto.servicios.model.request.onboarding.*;
import com.proyecto.servicios.model.response.ApiResponse;
import com.proyecto.servicios.model.response.onboarding.ClienteResponseDTO;
import com.proyecto.servicios.service.onboarding.ClienteService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para la administración de clientes y usuarios.
 *
 * Cumple con los lineamientos arquitectónicos y restricciones:
 * - Sin @PathVariable ni @RequestParam.
 * - Toda entrada de datos se recibe mediante Request DTO con @RequestBody y @Valid.
 * - Respuestas normalizadas con ApiResponse y Response DTOs (nunca entidades JPA).
 * - Único endpoint unificado de consulta dinámica que combina múltiples filtros.
 * - Tratamiento de errores centralizado en @RestControllerAdvice (sin try-catch en controladores).
 */
@RestController
@RequestMapping("/api/v1/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    // ==========================================
    // 1. POST /clientes — Registro de nuevo cliente
    // ==========================================
    /**
     * Registra un nuevo cliente con todas sus dependencias (usuario, domicilio y cuenta).
     * Los campos controlados por el servidor (saldo, número de cuenta, fecha de registro)
     * son asignados internamente y no pueden ser alterados por el cliente.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ClienteResponseDTO>> registrarCliente(
            @Valid @RequestBody ClienteRegistroDTO dto,
            HttpServletRequest request) {

        ClienteResponseDTO cliente = clienteService.registrarCliente(dto);

        ApiResponse<ClienteResponseDTO> response = ApiResponse.<ClienteResponseDTO>builder()
                .code(201)
                .message("Cliente registrado exitosamente")
                .path(request.getRequestURI())
                .data(cliente)
                .build();

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    // ==========================================
    // 2. GET /clientes — Consulta general de usuarios/clientes sin @RequestParam
    // ==========================================
    @GetMapping
    public ResponseEntity<ApiResponse<java.util.List<ClienteResponseDTO>>> obtenerTodos(HttpServletRequest request) {
        java.util.List<ClienteResponseDTO> clientes = clienteService.obtenerTodosLosClientes();
        ApiResponse<java.util.List<ClienteResponseDTO>> response = ApiResponse.<java.util.List<ClienteResponseDTO>>builder()
                .code(200)
                .message("Listado de clientes obtenido exitosamente")
                .path(request.getRequestURI())
                .data(clientes)
                .build();
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // 3. POST /clientes/consulta — Consulta por llave compuesta (nombre + correo)
    // ==========================================
    /**
     * Localiza un usuario/cliente mediante la llave compuesta por nombre y correo electrónico.
     * No utiliza ID ni @PathVariable ni @RequestParam.
     */
    @PostMapping("/consulta")
    public ResponseEntity<ApiResponse<ClienteResponseDTO>> consultarPorLlaveCompuesta(
            @Valid @RequestBody ClienteConsultaCompuestaDTO dto,
            HttpServletRequest request) {

        ClienteResponseDTO cliente = clienteService.obtenerPorLlaveCompuesta(dto);
        ApiResponse<ClienteResponseDTO> response = ApiResponse.<ClienteResponseDTO>builder()
                .code(200)
                .message("Usuario localizado exitosamente")
                .path(request.getRequestURI())
                .data(cliente)
                .build();

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // 4. POST /clientes/consultar — Consulta dinámica combinable (Specification)
    // ==========================================
    /**
     * Endpoint unificado de consulta dinámica de clientes.
     * Permite combinar cualquier conjunto de filtros (ID, CURP, RFC, correo, cuenta, nombre,
     * apellido, estatus activo/inactivo, rango de fechas) y paginación dentro del Request Body.
     */
    @PostMapping("/consultar")
    public ResponseEntity<ApiResponse<Page<ClienteResponseDTO>>> consultarClientes(
            @Valid @RequestBody(required = false) ClienteFiltroDTO filtro,
            HttpServletRequest request) {

        ClienteFiltroDTO filtroEfectivo = (filtro != null) ? filtro : new ClienteFiltroDTO();
        Page<ClienteResponseDTO> resultado = clienteService.consultarClientes(filtroEfectivo);

        ApiResponse<Page<ClienteResponseDTO>> response = ApiResponse.<Page<ClienteResponseDTO>>builder()
                .code(200)
                .message("Consulta de clientes realizada exitosamente")
                .path(request.getRequestURI())
                .data(resultado)
                .build();

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // 5. POST /clientes/detalle — Consultar detalle de cliente por ID
    // ==========================================
    /**
     * Consulta el detalle completo de un cliente a partir de su ID encapsulado en el Request Body.
     */
    @PostMapping("/detalle")
    public ResponseEntity<ApiResponse<ClienteResponseDTO>> obtenerDetalleCliente(
            @Valid @RequestBody ClienteIdDTO dto,
            HttpServletRequest request) {

        ClienteResponseDTO cliente = clienteService.obtenerClientePorId(dto.getIdCliente());

        ApiResponse<ClienteResponseDTO> response = ApiResponse.<ClienteResponseDTO>builder()
                .code(200)
                .message("Cliente encontrado exitosamente")
                .path(request.getRequestURI())
                .data(cliente)
                .build();

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // 6. PATCH /clientes — Sistema de banderas unificado (Actualizar o Eliminar)
    // ==========================================
    /**
     * Endpoint unificado mediante sistema de banderas booleanas (PATCH):
     * - Si actualizar es true (default): Ejecuta actualización con los campos enviados en el DTO.
     * - Si actualizar es false: Da de baja lógica al cliente (o borrado físico si permanente es true).
     *
     * Permite identificar al cliente por idCliente o por llave compuesta (nombre + correo electrónico).
     */
    @PatchMapping({"", "/actualizacion"})
    public ResponseEntity<ApiResponse<ClienteResponseDTO>> gestionarCliente(
            @Valid @RequestBody ClienteGestionDTO dto,
            HttpServletRequest request) {

        validarUsuarioActivo();

        com.proyecto.servicios.model.response.onboarding.GestionResultadoDTO resultado = clienteService.gestionarCliente(dto);

        ApiResponse<ClienteResponseDTO> response = ApiResponse.<ClienteResponseDTO>builder()
                .code(200)
                .message(resultado.getMensaje())
                .path(request.getRequestURI())
                .data(resultado.getCliente())
                .build();

        return ResponseEntity.ok(response);
    }

    // ==========================================
    // Validación de Autorización
    // ==========================================
    private void validarUsuarioActivo() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_READONLY"))) {
            throw new CuentaDesactivadaException(
                    "Su cuenta está desactivada. Solo puede consultar información, no realizar operaciones.");
        }
    }
}
