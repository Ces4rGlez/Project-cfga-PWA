package com.proyecto.servicios.exception;

import com.proyecto.servicios.exception.onboarding.*;
import com.proyecto.servicios.exception.GestoPagoIntegrationException;
import com.proyecto.servicios.model.response.ApiResponse;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Manejador centralizado de excepciones con @RestControllerAdvice.
 * Garantiza una estructura uniforme (ApiResponse) y evita duplicar try-catch en controladores.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ==========================================
    // 1. Validaciones de DTOs (@Valid / Bean Validation)
    // ==========================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errores.put(fieldError.getField(), fieldError.getDefaultMessage());
        }

        log.warn("Error de validacion en [{}]: {}", request.getRequestURI(), errores);

        return buildResponse(400, "DATOS_INVALIDOS", "Error de validacion en los campos de la solicitud",
                request.getRequestURI(), HttpStatus.BAD_REQUEST, errores, errores);
    }

    // ==========================================
    // 2. Formato de Peticion Malformado (JSON ilegible)
    // ==========================================
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.warn("Peticion malformada en [{}]: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(400, "El cuerpo de la solicitud no tiene un formato JSON valido o contiene datos ilegibles",
                request.getRequestURI(), HttpStatus.BAD_REQUEST, null);
    }

    // ==========================================
    // 3. Reglas de Negocio / Argumentos Invalidos
    // ==========================================
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(
            IllegalArgumentException ex, HttpServletRequest request) {

        log.warn("Regla de negocio no cumplida en [{}]: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(400, ex.getMessage(),
                request.getRequestURI(), HttpStatus.BAD_REQUEST, null);
    }

    // ==========================================
    // 4. Recursos No Encontrados (404)
    // ==========================================
    @ExceptionHandler(UsuarioNoEncontradoException.class)
    public ResponseEntity<ApiResponse<Object>> handleUsuarioNoEncontradoException(
            UsuarioNoEncontradoException ex, HttpServletRequest request) {

        log.warn("Usuario no encontrado en [{}]: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(404, "USUARIO_NO_ENCONTRADO", ex.getMessage(),
                request.getRequestURI(), HttpStatus.NOT_FOUND, null, null);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiResponse<Object>> handleRecursoNoEncontradoException(
            RecursoNoEncontradoException ex, HttpServletRequest request) {

        log.warn("Recurso no encontrado en [{}]: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(404, ex.getMessage(),
                request.getRequestURI(), HttpStatus.NOT_FOUND, null);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleNoResourceFound(
            NoResourceFoundException ex, HttpServletRequest request) {

        return buildResponse(404, "La ruta solicitada no fue encontrada en el servidor",
                request.getRequestURI(), HttpStatus.NOT_FOUND, null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Object>> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {

        return buildResponse(405, "El metodo HTTP " + ex.getMethod() + " no esta soportado para este endpoint",
                request.getRequestURI(), HttpStatus.METHOD_NOT_ALLOWED, null);
    }

    // ==========================================
    // 5. Conflictos de Duplicados e Integridad (409)
    // ==========================================
    @ExceptionHandler({
            CurpDuplicadaException.class,
            RfcDuplicadoException.class,
            CorreoDuplicadoException.class
    })
    public ResponseEntity<ApiResponse<Object>> handleDuplicadosException(
            RuntimeException ex, HttpServletRequest request) {

        String codigo = (ex instanceof CorreoDuplicadoException) ? "CORREO_DUPLICADO" : "DATO_DUPLICADO";
        log.warn("Dato duplicado en [{}]: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(409, codigo, ex.getMessage(),
                request.getRequestURI(), HttpStatus.CONFLICT, null, null);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        log.error("Violacion de integridad de datos en [{}]: {}", request.getRequestURI(), ex.getMessage());

        String mensaje = "Conflicto de integridad en la base de datos";
        String causa = ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage();

        if (causa != null) {
            if (causa.contains("value too long for type")) {
                mensaje = "Uno de los campos excede la longitud maxima permitida en la base de datos";
            } else if (causa.contains("violates unique constraint") || causa.contains("llave duplicada") || causa.contains("ya existe")) {
                mensaje = "Un valor unico (como CURP, RFC o Correo) ya existe en el sistema";
            } else if (causa.contains("violates check constraint")) {
                mensaje = "Uno de los campos no cumple con el formato o valor permitido en la base de datos";
            } else if (causa.contains("violates not-null constraint")) {
                mensaje = "Un campo obligatorio requerido por la base de datos no fue proporcionado";
            } else {
                mensaje = "Error de integridad de datos en la base de datos";
            }
        }

        return buildResponse(409, mensaje,
                request.getRequestURI(), HttpStatus.CONFLICT, null);
    }

    // ==========================================
    // 6. Autenticacion y Autorizacion (401 / 403)
    // ==========================================
    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<ApiResponse<Object>> handleCredencialesInvalidas(
            CredencialesInvalidasException ex, HttpServletRequest request) {

        log.warn("Credenciales invalidas en [{}]: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(401, ex.getMessage(),
                request.getRequestURI(), HttpStatus.UNAUTHORIZED, null);
    }

    @ExceptionHandler(ContrasenaInvalidaException.class)
    public ResponseEntity<ApiResponse<Object>> handleContrasenaInvalida(
            ContrasenaInvalidaException ex, HttpServletRequest request) {

        return buildResponse(400, ex.getMessage(),
                request.getRequestURI(), HttpStatus.BAD_REQUEST, null);
    }

    @ExceptionHandler(UsuarioInactivoException.class)
    public ResponseEntity<ApiResponse<Object>> handleUsuarioInactivo(
            UsuarioInactivoException ex, HttpServletRequest request) {

        return buildResponse(403, ex.getMessage(),
                request.getRequestURI(), HttpStatus.FORBIDDEN, null);
    }

    @ExceptionHandler(CuentaDesactivadaException.class)
    public ResponseEntity<ApiResponse<Object>> handleCuentaDesactivada(
            CuentaDesactivadaException ex, HttpServletRequest request) {

        return buildResponse(403, ex.getMessage(),
                request.getRequestURI(), HttpStatus.FORBIDDEN, null);
    }

    // ==========================================
    // 7. Servicios Externos — GestoPago (excepcion de dominio propia)
    //    Retorna HTTP 502 Bad Gateway con mensaje generico, sin detalles tecnicos.
    // ==========================================
    @ExceptionHandler(GestoPagoIntegrationException.class)
    public ResponseEntity<ApiResponse<Object>> handleGestoPagoIntegrationException(
            GestoPagoIntegrationException ex, HttpServletRequest request) {

        log.error("Error de integracion con GestoPago [operacion={}] en [{}]: {}",
                ex.getOperacion(), request.getRequestURI(), ex.getMessage());
        return buildResponse(502, "GESTOPAGO_INTEGRATION_ERROR",
                "Error al comunicarse con el proveedor GestoPago. Por favor, intente de nuevo en unos momentos.",
                request.getRequestURI(), HttpStatus.BAD_GATEWAY, null, null);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiResponse<Object>> handleFeignException(
            FeignException ex, HttpServletRequest request) {

        log.error("Error en llamada Feign a servicio externo en [{}]: {}", request.getRequestURI(), ex.getMessage());
        int status = ex.status() != 0 ? ex.status() : 503;
        return buildResponse(status, "FEIGN_EXTERNAL_ERROR",
                "Error de comunicacion con un servicio externo",
                request.getRequestURI(), HttpStatus.valueOf(status), null, null);
    }

    // ==========================================
    // 8. Catch-All: Errores Inesperados (500)
    //    Sin devolver trazas tecnicas ni consultas SQL al cliente
    // ==========================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneralException(
            Exception ex, HttpServletRequest request) {

        log.error("Error interno no controlado en [{}]: ", request.getRequestURI(), ex);
        return buildResponse(500, "Ocurrio un error interno en el servidor. Por favor, comuniquese con el administrador del sistema.",
                request.getRequestURI(), HttpStatus.INTERNAL_SERVER_ERROR, null);
    }

    // ==========================================
    // Metodos Utilitarios de Respuesta Uniforme
    // ==========================================
    private ResponseEntity<ApiResponse<Object>> buildResponse(
            int code, String codigo, String message, String path, HttpStatus status, Object data, Object errores) {

        ApiResponse<Object> response = ApiResponse.builder()
                .code(code)
                .codigo(codigo)
                .message(message)
                .path(path)
                .timestamp(LocalDateTime.now())
                .data(data)
                .errores(errores)
                .build();
        return new ResponseEntity<>(response, status);
    }

    private ResponseEntity<ApiResponse<Object>> buildResponse(
            int code, String message, String path, HttpStatus status, Object data) {
        return buildResponse(code, null, message, path, status, data, null);
    }
}
