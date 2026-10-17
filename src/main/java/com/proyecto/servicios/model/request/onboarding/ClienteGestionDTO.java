package com.proyecto.servicios.model.request.onboarding;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para la gestión unificada de clientes mediante sistema de banderas (PATCH).
 * Unifica las operaciones de Actualización y Eliminación/Baja lógica en un solo endpoint.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteGestionDTO {

    /**
     * ID del cliente. Si no se proporciona, se debe proporcionar la llave compuesta (nombre y correo electrónico).
     */
    private Long idCliente;

    /**
     * Bandera booleana principal:
     * - true (default): Ejecuta actualización con los campos proporcionados.
     * - false: Ejecuta eliminación / desactivación (baja lógica).
     */
    @Builder.Default
    private Boolean actualizar = true;

    /**
     * Bandera booleana secundaria (solo aplica si actualizar == false):
     * - false (default): Baja lógica (desactiva usuario y cuentas bancarias).
     * - true: Eliminación física definitiva de la base de datos (DELETE).
     */
    @Builder.Default
    private Boolean permanente = false;

    // --- Datos Personales (modificables si actualizar == true) ---
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]*$", message = "El nombre solo debe contener letras y espacios")
    @Size(max = 50, message = "La longitud del nombre no debe exceder 50 caracteres")
    private String nombre;

    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]*$", message = "El segundo nombre solo debe contener letras y espacios")
    @Size(max = 50, message = "La longitud del segundo nombre no debe exceder 50 caracteres")
    private String segundoNombre;

    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]*$", message = "El apellido paterno solo debe contener letras y espacios")
    @Size(max = 50, message = "La longitud del apellido paterno no debe exceder 50 caracteres")
    private String apellidoPaterno;

    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]*$", message = "El apellido materno solo debe contener letras y espacios")
    @Size(max = 50, message = "La longitud del apellido materno no debe exceder 50 caracteres")
    private String apellidoMaterno;

    private String sexo;
    private String nacionalidad;
    private String estadoCivil;

    // --- Datos de Contacto ---
    @Email(message = "El correo electrónico no tiene un formato válido")
    @Size(max = 100, message = "El correo electrónico no puede exceder 100 caracteres")
    @com.fasterxml.jackson.annotation.JsonAlias({"correo"})
    private String correoElectronico;

    @Pattern(regexp = "^(?:\\d{10})?$", message = "El teléfono móvil debe contener exactamente 10 dígitos")
    private String telefonoMovil;

    @Pattern(regexp = "^(?:\\d{10})?$", message = "El teléfono alternativo debe contener exactamente 10 dígitos si se proporciona")
    private String telefonoAlternativo;

    // --- Domicilio ---
    private String calle;
    private String numeroExterior;
    private String numeroInterior;
    private String colonia;
    private String municipio;
    private String estado;

    @Pattern(regexp = "^(?:\\d{5})?$", message = "El código postal debe contener exactamente 5 dígitos")
    private String codigoPostal;

    private String pais;

    // --- Información Laboral ---
    private String ocupacion;
    private String empresa;

    @DecimalMin(value = "0.01", inclusive = true, message = "El ingreso mensual debe ser mayor a cero")
    private BigDecimal ingresoMensual;

    /**
     * Banderita booleana de estatus activo:
     * - true: Reactiva el cliente y sus cuentas
     * - false: Desactiva el cliente y sus cuentas
     * - null: Mantiene el estatus actual
     */
    private Boolean activo;
}
