package com.proyecto.servicios.model.request.onboarding;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioConsultaDTO {

    @NotNull(message = "El ID del usuario es obligatorio")
    private Long idUsuario;
}
