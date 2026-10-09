package com.proyecto.servicios.model.request.onboarding;

import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ClienteActualizaDTO {

    // --- Datos Personales (modificables) ---
    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El nombre solo debe contener letras y espacios")
    @Size(min = 2, max = 50, message = "La longitud del nombre debe estar entre 2 y 50 caracteres")
    private String nombre;

    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]*$", message = "El segundo nombre solo debe contener letras y espacios")
    @Size(max = 50, message = "La longitud del segundo nombre no debe exceder 50 caracteres")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El apellido paterno solo debe contener letras y espacios")
    @Size(min = 2, max = 50, message = "La longitud del apellido paterno debe estar entre 2 y 50 caracteres")
    private String apellidoPaterno;

    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]*$", message = "El apellido materno solo debe contener letras y espacios")
    @Size(max = 50, message = "La longitud del apellido materno no debe exceder 50 caracteres")
    private String apellidoMaterno;

    @NotBlank(message = "El sexo es obligatorio")
    private String sexo;

    @NotBlank(message = "La nacionalidad es obligatoria")
    private String nacionalidad;

    @NotBlank(message = "El estado civil es obligatorio")
    private String estadoCivil;

    // --- Datos de Contacto ---
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico no tiene un formato válido")
    @Size(max = 100, message = "El correo electrónico no puede exceder 100 caracteres")
    private String correoElectronico;

    @NotBlank(message = "El teléfono móvil es obligatorio")
    @Pattern(regexp = "^\\d{10}$", message = "El teléfono móvil debe contener exactamente 10 dígitos")
    private String telefonoMovil;

    @Pattern(regexp = "^(?:\\d{10})?$", message = "El teléfono alternativo debe contener exactamente 10 dígitos si se proporciona")
    private String telefonoAlternativo;

    // --- Domicilio ---
    @NotBlank(message = "La calle es obligatoria")
    private String calle;

    @NotBlank(message = "El número exterior es obligatorio")
    private String numeroExterior;

    private String numeroInterior;

    @NotBlank(message = "La colonia es obligatoria")
    private String colonia;

    @NotBlank(message = "El municipio es obligatorio")
    private String municipio;

    @NotBlank(message = "El estado es obligatorio")
    private String estado;

    @NotBlank(message = "El código postal es obligatorio")
    @Pattern(regexp = "^\\d{5}$", message = "El código postal debe contener exactamente 5 dígitos")
    private String codigoPostal;

    @NotBlank(message = "El país es obligatorio")
    private String pais;

    // --- Información Laboral ---
    @NotBlank(message = "La ocupación es obligatoria")
    private String ocupacion;

    @NotBlank(message = "La empresa es obligatoria")
    private String empresa;

    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.01", inclusive = true, message = "El ingreso mensual debe ser mayor a cero")
    private BigDecimal ingresoMensual;
}
