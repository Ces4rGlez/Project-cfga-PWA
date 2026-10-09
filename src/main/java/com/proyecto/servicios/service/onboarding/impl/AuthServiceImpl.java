package com.proyecto.servicios.service.onboarding.impl;

import com.proyecto.servicios.config.JwtUtil;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.exception.onboarding.CredencialesInvalidasException;
import com.proyecto.servicios.exception.onboarding.RecursoNoEncontradoException;
import com.proyecto.servicios.exception.onboarding.UsuarioInactivoException;
import com.proyecto.servicios.model.request.onboarding.CambioPasswordDTO;
import com.proyecto.servicios.model.request.onboarding.LoginRequestDTO;
import com.proyecto.servicios.model.response.onboarding.LoginResponseDTO;
import com.proyecto.servicios.model.response.onboarding.UsuarioResponseDTO;
import com.proyecto.servicios.repository.onboarding.ClienteRepository;
import com.proyecto.servicios.repository.onboarding.UsuarioRepository;
import com.proyecto.servicios.service.onboarding.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del servicio de autenticación.
 * Maneja el login con JWT, consulta de usuarios y cambio de contraseña.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * Autentica un usuario:
     * 1. Busca el usuario por correo electrónico.
     * 2. Verifica que esté activo.
     * 3. Valida la contraseña con BCrypt.
     * 4. Genera y retorna un token JWT.
     */
    @Override
    @Transactional(readOnly = true)
    public LoginResponseDTO login(LoginRequestDTO dto) {
        log.info("Intento de login para correo: {}", dto.getCorreoElectronico());

        // 1. Buscar usuario por correo
        Usuario usuario = usuarioRepository.findByCorreoElectronico(dto.getCorreoElectronico())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró un usuario con el correo: " + dto.getCorreoElectronico()));

        // 2. Registrar si el usuario está activo (se incluye en la respuesta para modo solo lectura)
        boolean usuarioActivo = Boolean.TRUE.equals(usuario.getActivo());

        // 3. Validar contraseña
        if (!passwordEncoder.matches(dto.getPassword(), usuario.getPassword())) {
            throw new CredencialesInvalidasException("Las credenciales proporcionadas son incorrectas");
        }

        // 4. Obtener el cliente asociado (para incluir idCliente en la respuesta)
        Long idCliente = clienteRepository.findByCorreo(dto.getCorreoElectronico())
                .map(Cliente::getIdCliente)
                .orElse(null);

        // 5. Generar token JWT
        String token = jwtUtil.generarToken(dto.getCorreoElectronico());
        log.info("Login exitoso para correo: {}", dto.getCorreoElectronico());

        return LoginResponseDTO.builder()
                .token(token)
                .correoElectronico(usuario.getCorreoElectronico())
                .idUsuario(usuario.getIdUsuario())
                .idCliente(idCliente)
                .activo(usuarioActivo)
                .mensaje(usuarioActivo ? "Login exitoso" : "Su cuenta está desactivada. Solo puede consultar información, no realizar operaciones.")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponseDTO obtenerUsuarioPorId(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el usuario con ID: " + id));

        return UsuarioResponseDTO.builder()
                .idUsuario(usuario.getIdUsuario())
                .correoElectronico(usuario.getCorreoElectronico())
                .activo(usuario.getActivo())
                .fechaRegistro(usuario.getFechaRegistro())
                .build();
    }

    /**
     * Cambia la contraseña de un usuario.
     * Valida que la contraseña actual sea correcta antes de actualizar.
     */
    @Override
    @Transactional
    public void cambiarPassword(Long idUsuario, CambioPasswordDTO dto) {
        log.info("Solicitud de cambio de contraseña para usuario ID: {}", idUsuario);

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No se encontró el usuario con ID: " + idUsuario));

        // Validar contraseña actual
        if (!passwordEncoder.matches(dto.getPasswordActual(), usuario.getPassword())) {
            throw new CredencialesInvalidasException("La contraseña actual es incorrecta");
        }

        // Cifrar y guardar nueva contraseña
        usuario.setPassword(passwordEncoder.encode(dto.getPasswordNueva()));
        usuarioRepository.save(usuario);

        log.info("Contraseña actualizada exitosamente para usuario ID: {}", idUsuario);
    }
}
