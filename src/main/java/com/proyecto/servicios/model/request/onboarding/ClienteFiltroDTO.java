package com.proyecto.servicios.model.request.onboarding;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * DTO para encapsular los filtros de búsqueda y la paginación de clientes.
 * Puede ser utilizado tanto en peticiones GET (Query Params) como en
 * peticiones POST (Request Body / Payload JSON).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteFiltroDTO {

    /**
     * Búsqueda específica por ID de cliente.
     */
    private Long idCliente;

    /**
     * Búsqueda general flexible por palabra clave con coincidencia parcial
     * (ContainingIgnoreCase) en Nombre, Apellido, CURP o RFC.
     */
    @Size(max = 100, message = "El filtro de búsqueda no puede exceder 100 caracteres")
    private String filtro;

    @Size(max = 50, message = "El nombre de búsqueda no puede exceder 50 caracteres")
    private String nombre;

    @Size(max = 50, message = "El apellido de búsqueda no puede exceder 50 caracteres")
    private String apellidoPaterno;

    @Size(max = 18, message = "El CURP no puede exceder 18 caracteres")
    private String curp;

    @Size(max = 13, message = "El RFC no puede exceder 13 caracteres")
    private String rfc;

    @Size(max = 100, message = "El correo no puede exceder 100 caracteres")
    private String correo;

    @Size(max = 20, message = "El número de cuenta no puede exceder 20 caracteres")
    private String numeroCuenta;

    private Boolean activos;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime fechaDesde;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime fechaHasta;

    // ==========================================
    // Control de Paginación y Orden dentro del Payload
    // ==========================================

    @Min(value = 0, message = "El número de página no puede ser menor a 0")
    @Builder.Default
    private Integer page = 0;

    @Min(value = 1, message = "El tamaño de página debe ser al menos 1")
    @Max(value = 100, message = "El tamaño de página no puede exceder 100")
    @Builder.Default
    private Integer size = 10;

    @Builder.Default
    private String sort = "idCliente";

    @Builder.Default
    private String direction = "DESC";

    /**
     * Convierte los parámetros del payload en un objeto Pageable de Spring Data.
     */
    public Pageable toPageable() {
        int p = (this.page != null && this.page >= 0) ? this.page : 0;
        int s = (this.size != null && this.size > 0) ? Math.min(this.size, 100) : 10;
        String sortField = (this.sort != null && !this.sort.isBlank()) ? this.sort.trim() : "idCliente";
        Sort.Direction dir = "ASC".equalsIgnoreCase(this.direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
        return PageRequest.of(p, s, Sort.by(dir, sortField));
    }
}
