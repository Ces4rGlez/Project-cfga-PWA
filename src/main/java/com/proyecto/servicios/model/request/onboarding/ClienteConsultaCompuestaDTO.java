package com.proyecto.servicios.model.request.onboarding;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para la consulta de usuario/cliente mediante llave compuesta (nombre y correo electrónico).
 * Cumple con la restricción de no usar @PathVariable ni @RequestParam ni ID numérico.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteConsultaCompuestaDTO {

    @NotBlank(message = "El nombre es obligatorio y no puede contener solo espacios.")
    private String nombre;

    @NotBlank(message = "El correo electrónico es obligatorio.")
    @Email(message = "El correo electrónico no tiene un formato válido.")
    @JsonAlias({"correoElectronico"})
    private String correo;
}
