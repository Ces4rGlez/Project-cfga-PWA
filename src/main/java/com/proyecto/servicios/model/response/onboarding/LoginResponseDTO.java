package com.proyecto.servicios.model.response.onboarding;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDTO {
    private String token;
    private String correoElectronico;
    private Long idUsuario;
    private Long idCliente;
    private Boolean activo;
    private String mensaje;
}
