package com.proyecto.servicios.service.onboarding;

import com.proyecto.servicios.model.request.onboarding.CambioPasswordDTO;
import com.proyecto.servicios.model.request.onboarding.LoginRequestDTO;
import com.proyecto.servicios.model.response.onboarding.LoginResponseDTO;
import com.proyecto.servicios.model.response.onboarding.UsuarioResponseDTO;

/**
 * Interfaz del servicio de autenticación y gestión de usuarios.
 * Maneja login, generación de JWT y cambio de contraseña.
 */
public interface AuthService {

    /**
     * Autentica un usuario mediante correo y contraseña.
     * Valida que el usuario exista, esté activo y las credenciales sean correctas.
     * Genera un token JWT si la autenticación es exitosa.
     *
     * @throws RecursoNoEncontradoException si el usuario no existe
     * @throws UsuarioInactivoException si el usuario está inactivo
     * @throws CredencialesInvalidasException si la contraseña es incorrecta
     */
    LoginResponseDTO login(LoginRequestDTO dto);

    /**
     * Obtiene la información de un usuario por su ID.
     * @throws RecursoNoEncontradoException si el usuario no existe
     */
    UsuarioResponseDTO obtenerUsuarioPorId(Long id);

    /**
     * Cambia la contraseña de un usuario.
     * Valida la contraseña actual antes de actualizar.
     *
     * @throws RecursoNoEncontradoException si el usuario no existe
     * @throws CredencialesInvalidasException si la contraseña actual no coincide
     */
    void cambiarPassword(Long idUsuario, CambioPasswordDTO dto);
}
