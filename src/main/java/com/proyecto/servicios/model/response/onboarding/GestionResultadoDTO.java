package com.proyecto.servicios.model.response.onboarding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Encapsula el resultado de una operación de gestión con banderas (PATCH).
 * Permite que la capa de servicio determine el mensaje de negocio y los datos resultantes,
 * eliminando condicionales y lógica de negocio dentro del controlador.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GestionResultadoDTO {
    private String mensaje;
    private ClienteResponseDTO cliente;
}
