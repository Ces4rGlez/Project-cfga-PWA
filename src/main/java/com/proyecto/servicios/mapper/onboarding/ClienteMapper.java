package com.proyecto.servicios.mapper.onboarding;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Domicilio;
import com.proyecto.servicios.model.response.onboarding.ClienteResponseDTO;
import com.proyecto.servicios.model.response.onboarding.CuentaResponseDTO;
import com.proyecto.servicios.model.response.onboarding.DomicilioResponseDTO;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mapper manual para convertir entidades del onboarding a DTOs de respuesta.
 * Se usa un approach manual en lugar de MapStruct para mantener
 * el control explícito sobre las relaciones bidireccionales y evitar
 * problemas de lazy loading.
 */
@Component
public class ClienteMapper {

    /**
     * Convierte una entidad Cliente completa a su DTO de respuesta,
     * incluyendo domicilio, cuentas y datos del usuario asociado.
     */
    public ClienteResponseDTO toResponseDTO(Cliente cliente) {
        if (cliente == null) return null;

        return ClienteResponseDTO.builder()
                .idCliente(cliente.getIdCliente())
                // Datos Personales
                .nombre(cliente.getNombre())
                .segundoNombre(cliente.getSegundoNombre())
                .apellidoPaterno(cliente.getApellidoPaterno())
                .apellidoMaterno(cliente.getApellidoMaterno())
                .fechaNacimiento(cliente.getFechaNacimiento())
                .curp(cliente.getCurp())
                .rfc(cliente.getRfc())
                .sexo(cliente.getSexo())
                .nacionalidad(cliente.getNacionalidad())
                .estadoCivil(cliente.getEstadoCivil())
                // Datos de Contacto
                .correoElectronico(cliente.getUsuario() != null ? cliente.getUsuario().getCorreoElectronico() : null)
                .telefonoMovil(cliente.getTelefonoMovil())
                .telefonoAlternativo(cliente.getTelefonoAlternativo())
                // Domicilio
                .domicilio(toDomicilioDTO(cliente.getDomicilio()))
                // Información Laboral
                .ocupacion(cliente.getOcupacion())
                .empresa(cliente.getEmpresa())
                .ingresoMensual(cliente.getIngresoMensual())
                // Cuentas
                .cuentas(toCuentaDTOList(cliente.getCuentas()))
                // Estado y Fechas
                .activo(cliente.getUsuario() != null ? cliente.getUsuario().getActivo() : null)
                .fechaRegistro(cliente.getUsuario() != null ? cliente.getUsuario().getFechaRegistro() : null)
                .build();
    }

    /**
     * Convierte una lista de entidades Cliente a una lista de DTOs.
     */
    public List<ClienteResponseDTO> toResponseDTOList(List<Cliente> clientes) {
        if (clientes == null) return Collections.emptyList();
        return clientes.stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convierte la entidad Domicilio a su DTO de respuesta.
     */
    public DomicilioResponseDTO toDomicilioDTO(Domicilio domicilio) {
        if (domicilio == null) return null;

        return DomicilioResponseDTO.builder()
                .idDomicilio(domicilio.getIdDomicilio())
                .calle(domicilio.getCalle())
                .numeroExterior(domicilio.getNumeroExterior())
                .numeroInterior(domicilio.getNumeroInterior())
                .colonia(domicilio.getColonia())
                .municipio(domicilio.getMunicipio())
                .estado(domicilio.getEstado())
                .codigoPostal(domicilio.getCodigoPostal())
                .pais(domicilio.getPais())
                .build();
    }

    /**
     * Convierte una lista de entidades Cuenta a una lista de CuentaResponseDTO.
     */
    public List<CuentaResponseDTO> toCuentaDTOList(List<Cuenta> cuentas) {
        if (cuentas == null) return Collections.emptyList();
        return cuentas.stream()
                .map(this::toCuentaDTO)
                .collect(Collectors.toList());
    }

    /**
     * Convierte una entidad Cuenta individual a su DTO de respuesta.
     */
    public CuentaResponseDTO toCuentaDTO(Cuenta cuenta) {
        if (cuenta == null) return null;

        return CuentaResponseDTO.builder()
                .idCuenta(cuenta.getIdCuenta())
                .numeroCuenta(cuenta.getNumeroCuenta())
                .saldo(cuenta.getSaldo())
                .estatus(cuenta.getEstatus())
                .fechaCreacion(cuenta.getFechaCreacion())
                .build();
    }
}
