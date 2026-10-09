package com.proyecto.servicios.model.response.onboarding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClienteResponseDTO {

    private Long idCliente;

    // Datos Personales
    private String nombre;
    private String segundoNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private LocalDate fechaNacimiento;
    private String curp;
    private String rfc;
    private String sexo;
    private String nacionalidad;
    private String estadoCivil;

    // Datos de Contacto
    private String correoElectronico;
    private String telefonoMovil;
    private String telefonoAlternativo;

    // Domicilio
    private DomicilioResponseDTO domicilio;

    // Información Laboral
    private String ocupacion;
    private String empresa;
    private BigDecimal ingresoMensual;

    // Cuentas asociadas
    private List<CuentaResponseDTO> cuentas;

    // Estado del usuario
    private Boolean activo;
    private LocalDateTime fechaRegistro;
}
