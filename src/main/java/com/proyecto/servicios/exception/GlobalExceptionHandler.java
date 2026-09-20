package com.proyecto.servicios.exception;

import com.proyecto.servicios.model.response.ApiResponse;
import feign.FeignException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Code 2: Error al comunicarse con API Externa (GestoPago, etc)
    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ApiResponse<Object>> handleFeignException(FeignException ex, HttpServletRequest request) {
        return buildResponse(2, "Error de comunicación con servicio externo: " + ex.getMessage(), 
                request.getRequestURI(), HttpStatus.valueOf(ex.status() != 0 ? ex.status() : 503));
    }

    // Code 3: Error de validación de datos (cuando falla un @Valid en un RequestBody)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errores = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return buildResponse(3, "Error de validación de datos: " + errores, 
                request.getRequestURI(), HttpStatus.BAD_REQUEST);
    }

    // Code 4: Petición malformada o JSON ilegible
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Object>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return buildResponse(4, "La petición no tiene un formato válido (JSON malformado)", 
                request.getRequestURI(), HttpStatus.BAD_REQUEST);
    }

    // Code 5: Ruta no encontrada o Método HTTP incorrecto (Ej. mandar POST a un GET)
    @ExceptionHandler({NoResourceFoundException.class, HttpRequestMethodNotSupportedException.class})
    public ResponseEntity<ApiResponse<Object>> handleNotFoundOrMethodNotSupported(Exception ex, HttpServletRequest request) {
        return buildResponse(5, "Ruta o método no soportado: " + ex.getMessage(), 
                request.getRequestURI(), HttpStatus.NOT_FOUND);
    }

    // Code 6: Errores de lógica de negocio (Argumentos inválidos proporcionados por el código)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        return buildResponse(6, "Error lógico: " + ex.getMessage(), 
                request.getRequestURI(), HttpStatus.CONFLICT);
    }

    // Code 0: Cualquier otra excepción inesperada (Catch-all)
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGeneralException(Exception ex, HttpServletRequest request) {
        return buildResponse(0, "Error interno en el servidor: " + ex.getMessage(), 
                request.getRequestURI(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // Método utilitario para construir las respuestas de error sin repetir código
    private ResponseEntity<ApiResponse<Object>> buildResponse(int code, String message, String path, HttpStatus status) {
        ApiResponse<Object> response = ApiResponse.builder()
                .code(code)
                .message(message)
                .path(path)
                .timestamp(LocalDateTime.now())
                .data(null)
                .build();
        return new ResponseEntity<>(response, status);
    }
}
