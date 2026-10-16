package com.proyecto.servicios.exception.onboarding;

/**
 * Excepción de negocio lanzada cuando no existe un usuario que coincida
 * con los criterios de búsqueda (por ejemplo, la llave compuesta de nombre y correo).
 * Devuelve código HTTP 404 Not Found con el código "USUARIO_NO_ENCONTRADO".
 */
public class UsuarioNoEncontradoException extends RuntimeException {
    public UsuarioNoEncontradoException(String message) {
        super(message);
    }
}
