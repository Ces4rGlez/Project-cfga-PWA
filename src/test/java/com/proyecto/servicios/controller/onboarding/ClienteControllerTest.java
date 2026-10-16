package com.proyecto.servicios.controller.onboarding;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.exception.GlobalExceptionHandler;
import com.proyecto.servicios.exception.onboarding.CurpDuplicadaException;
import com.proyecto.servicios.exception.onboarding.RecursoNoEncontradoException;
import com.proyecto.servicios.exception.onboarding.UsuarioNoEncontradoException;
import com.proyecto.servicios.model.request.onboarding.*;
import com.proyecto.servicios.model.response.onboarding.ClienteResponseDTO;
import com.proyecto.servicios.model.response.onboarding.GestionResultadoDTO;
import com.proyecto.servicios.service.onboarding.ClienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class ClienteControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ClienteService clienteService;

    @InjectMocks
    private ClienteController clienteController;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        org.springframework.http.converter.json.MappingJackson2HttpMessageConverter converter =
                new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter(objectMapper);

        mockMvc = MockMvcBuilders.standaloneSetup(clienteController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(converter)
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/clientes — Registro exitoso retorna 201 Created y estructura uniforme ApiResponse")
    void registrarCliente_Exito() throws Exception {
        ClienteRegistroDTO requestDto = new ClienteRegistroDTO();
        requestDto.setNombre("Carlos");
        requestDto.setApellidoPaterno("González");
        requestDto.setFechaNacimiento(LocalDate.of(1995, 5, 20));
        requestDto.setCurp("GOAC950520HMNNRL03");
        requestDto.setRfc("GOAC9505201A0");
        requestDto.setSexo("M");
        requestDto.setNacionalidad("Mexicana");
        requestDto.setEstadoCivil("Soltero");
        requestDto.setCorreoElectronico("carlos.gonzalez@test.com");
        requestDto.setPassword("Temporal@2026");
        requestDto.setTelefonoMovil("5512345678");
        requestDto.setCalle("Av Reforma");
        requestDto.setNumeroExterior("100");
        requestDto.setColonia("Juárez");
        requestDto.setMunicipio("Cuauhtémoc");
        requestDto.setEstado("CDMX");
        requestDto.setCodigoPostal("06600");
        requestDto.setPais("México");
        requestDto.setOcupacion("Ingeniero");
        requestDto.setEmpresa("Tech Corp");
        requestDto.setIngresoMensual(new BigDecimal("25000.00"));

        ClienteResponseDTO responseDto = ClienteResponseDTO.builder()
                .idCliente(1L)
                .nombre("Carlos")
                .apellidoPaterno("González")
                .curp("GOAC950520HMNNRL03")
                .rfc("GOAC9505201A0")
                .activo(true)
                .build();

        when(clienteService.registrarCliente(any(ClienteRegistroDTO.class))).thenReturn(responseDto);

        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.message").value("Cliente registrado exitosamente"))
                .andExpect(jsonPath("$.data.idCliente").value(1))
                .andExpect(jsonPath("$.data.nombre").value("Carlos"));
    }

    @Test
    @DisplayName("POST /api/v1/clientes — Error de validación Bean Validation retorna 400 Bad Request con detalle de campos")
    void registrarCliente_FalloValidacion() throws Exception {
        ClienteRegistroDTO requestDto = new ClienteRegistroDTO();
        // Campos obligatorios vacíos para provocar MethodArgumentNotValidException

        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("Error de validación en los campos de la solicitud"))
                .andExpect(jsonPath("$.data.nombre").exists())
                .andExpect(jsonPath("$.data.curp").exists())
                .andExpect(jsonPath("$.data.correoElectronico").exists());
    }

    @Test
    @DisplayName("POST /api/v1/clientes — CURP duplicada retorna 409 Conflict manejado por @RestControllerAdvice")
    void registrarCliente_CurpDuplicada() throws Exception {
        ClienteRegistroDTO requestDto = new ClienteRegistroDTO();
        requestDto.setNombre("Carlos");
        requestDto.setApellidoPaterno("González");
        requestDto.setFechaNacimiento(LocalDate.of(1995, 5, 20));
        requestDto.setCurp("GOAC950520HMNNRL03");
        requestDto.setRfc("GOAC9505201A0");
        requestDto.setSexo("M");
        requestDto.setNacionalidad("Mexicana");
        requestDto.setEstadoCivil("Soltero");
        requestDto.setCorreoElectronico("carlos.gonzalez@test.com");
        requestDto.setPassword("Temporal@2026");
        requestDto.setTelefonoMovil("5512345678");
        requestDto.setCalle("Av Reforma");
        requestDto.setNumeroExterior("100");
        requestDto.setColonia("Juárez");
        requestDto.setMunicipio("Cuauhtémoc");
        requestDto.setEstado("CDMX");
        requestDto.setCodigoPostal("06600");
        requestDto.setPais("México");
        requestDto.setOcupacion("Ingeniero");
        requestDto.setEmpresa("Tech Corp");
        requestDto.setIngresoMensual(new BigDecimal("25000.00"));

        when(clienteService.registrarCliente(any(ClienteRegistroDTO.class)))
                .thenThrow(new CurpDuplicadaException("Ya existe un cliente registrado con la CURP: GOAC950520HMNNRL03"));

        mockMvc.perform(post("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestDto)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409))
                .andExpect(jsonPath("$.message").value("Ya existe un cliente registrado con la CURP: GOAC950520HMNNRL03"));
    }

    @Test
    @DisplayName("POST /api/v1/clientes/consultar — Único endpoint de búsqueda combinando filtros retorna 200 OK")
    void consultarClientes_ConFiltrosCombinados() throws Exception {
        ClienteFiltroDTO filtro = ClienteFiltroDTO.builder()
                .nombre("Carlos")
                .activos(true)
                .page(0)
                .size(10)
                .build();

        ClienteResponseDTO cliente = ClienteResponseDTO.builder()
                .idCliente(1L)
                .nombre("Carlos")
                .curp("GOAC950520HMNNRL03")
                .activo(true)
                .build();

        when(clienteService.consultarClientes(any(ClienteFiltroDTO.class)))
                .thenReturn(new PageImpl<>(List.of(cliente), org.springframework.data.domain.PageRequest.of(0, 10), 1));

        mockMvc.perform(post("/api/v1/clientes/consultar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(filtro)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.content[0].idCliente").value(1))
                .andExpect(jsonPath("$.data.content[0].nombre").value("Carlos"));
    }

    @Test
    @DisplayName("POST /api/v1/clientes/detalle — Consulta por ID mediante Request Body sin @PathVariable")
    void obtenerDetalleCliente_Exito() throws Exception {
        ClienteIdDTO idDto = new ClienteIdDTO(1L);

        ClienteResponseDTO cliente = ClienteResponseDTO.builder()
                .idCliente(1L)
                .nombre("Carlos")
                .build();

        when(clienteService.obtenerClientePorId(1L)).thenReturn(cliente);

        mockMvc.perform(post("/api/v1/clientes/detalle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(idDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.idCliente").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/clientes/detalle — Recurso no encontrado retorna 404 Not Found")
    void obtenerDetalleCliente_NoEncontrado() throws Exception {
        ClienteIdDTO idDto = new ClienteIdDTO(999L);

        when(clienteService.obtenerClientePorId(999L))
                .thenThrow(new RecursoNoEncontradoException("No se encontró el cliente con ID: 999"));

        mockMvc.perform(post("/api/v1/clientes/detalle")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(idDto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("No se encontró el cliente con ID: 999"));
    }

    @Test
    @DisplayName("PATCH /api/v1/clientes — Actualizar cliente vía Request Body con bandera actualizar=true")
    void gestionarCliente_ActualizarExito() throws Exception {
        ClienteGestionDTO gestionDto = ClienteGestionDTO.builder()
                .idCliente(1L)
                .actualizar(true)
                .nombre("Carlos Alberto")
                .apellidoPaterno("González")
                .correoElectronico("carlos.nuevo@test.com")
                .build();

        ClienteResponseDTO actualizado = ClienteResponseDTO.builder()
                .idCliente(1L)
                .nombre("Carlos Alberto")
                .correoElectronico("carlos.nuevo@test.com")
                .build();

        when(clienteService.gestionarCliente(any(ClienteGestionDTO.class))).thenReturn(
                GestionResultadoDTO.builder()
                        .mensaje("Cliente actualizado exitosamente mediante sistema de banderas")
                        .cliente(actualizado)
                        .build());

        mockMvc.perform(patch("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gestionDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Cliente actualizado exitosamente mediante sistema de banderas"))
                .andExpect(jsonPath("$.data.nombre").value("Carlos Alberto"));
    }

    @Test
    @DisplayName("PATCH /api/v1/clientes — Baja lógica de cliente vía Request Body con bandera actualizar=false")
    void gestionarCliente_BajaLogicaExito() throws Exception {
        ClienteGestionDTO gestionDto = ClienteGestionDTO.builder()
                .idCliente(1L)
                .actualizar(false)
                .permanente(false)
                .build();

        when(clienteService.gestionarCliente(any(ClienteGestionDTO.class))).thenReturn(
                GestionResultadoDTO.builder()
                        .mensaje("Cliente dado de baja exitosamente (baja lógica) mediante bandera booleana")
                        .cliente(null)
                        .build());

        mockMvc.perform(patch("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gestionDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Cliente dado de baja exitosamente (baja lógica) mediante bandera booleana"));
    }

    @Test
    @DisplayName("PATCH /api/v1/clientes — Borrado físico permanente con banderas actualizar=false y permanente=true")
    void gestionarCliente_BorradoFisicoExito() throws Exception {
        ClienteGestionDTO gestionDto = ClienteGestionDTO.builder()
                .idCliente(1L)
                .actualizar(false)
                .permanente(true)
                .build();

        when(clienteService.gestionarCliente(any(ClienteGestionDTO.class))).thenReturn(
                GestionResultadoDTO.builder()
                        .mensaje("Cliente eliminado permanentemente de la base de datos mediante bandera booleana")
                        .cliente(null)
                        .build());

        mockMvc.perform(patch("/api/v1/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gestionDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Cliente eliminado permanentemente de la base de datos mediante bandera booleana"));
    }

    @Test
    @DisplayName("GET /api/v1/clientes — Consulta general de usuarios/clientes sin @RequestParam")
    void obtenerTodosLosClientes_Exito() throws Exception {
        ClienteResponseDTO cliente = ClienteResponseDTO.builder()
                .idCliente(1L)
                .nombre("Carlos")
                .correoElectronico("carlos@test.com")
                .activo(true)
                .build();

        when(clienteService.obtenerTodosLosClientes()).thenReturn(List.of(cliente));

        mockMvc.perform(get("/api/v1/clientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].idCliente").value(1))
                .andExpect(jsonPath("$.data[0].nombre").value("Carlos"));
    }

    @Test
    @DisplayName("POST /api/v1/clientes/consulta — Consulta por llave compuesta (nombre + correo) exitosa")
    void consultarPorLlaveCompuesta_Exito() throws Exception {
        ClienteConsultaCompuestaDTO dto = new ClienteConsultaCompuestaDTO("Carlos", "carlos@test.com");

        ClienteResponseDTO cliente = ClienteResponseDTO.builder()
                .idCliente(1L)
                .nombre("Carlos")
                .correoElectronico("carlos@test.com")
                .activo(true)
                .build();

        when(clienteService.obtenerPorLlaveCompuesta(any(ClienteConsultaCompuestaDTO.class))).thenReturn(cliente);

        mockMvc.perform(post("/api/v1/clientes/consulta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.idCliente").value(1))
                .andExpect(jsonPath("$.data.nombre").value("Carlos"));
    }

    @Test
    @DisplayName("POST /api/v1/clientes/consulta — Usuario no encontrado retorna 404 Not Found con codigo USUARIO_NO_ENCONTRADO")
    void consultarPorLlaveCompuesta_NoEncontrado() throws Exception {
        ClienteConsultaCompuestaDTO dto = new ClienteConsultaCompuestaDTO("Inexistente", "nadie@correo.com");

        when(clienteService.obtenerPorLlaveCompuesta(any(ClienteConsultaCompuestaDTO.class)))
                .thenThrow(new UsuarioNoEncontradoException("No existe un usuario con el nombre y correo electrónico proporcionados."));

        mockMvc.perform(post("/api/v1/clientes/consulta")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.codigo").value("USUARIO_NO_ENCONTRADO"))
                .andExpect(jsonPath("$.message").value("No existe un usuario con el nombre y correo electrónico proporcionados."));
    }

    @Test
    @DisplayName("PATCH /api/v1/clientes/actualizacion — Actualizar cliente localizando por llave compuesta")
    void gestionarCliente_ActualizarPorLlaveCompuesta() throws Exception {
        ClienteGestionDTO gestionDto = ClienteGestionDTO.builder()
                .nombre("Carlos")
                .correoElectronico("carlos@test.com")
                .actualizar(true)
                .telefonoMovil("5599887766")
                .build();

        ClienteResponseDTO actualizado = ClienteResponseDTO.builder()
                .idCliente(1L)
                .nombre("Carlos")
                .correoElectronico("carlos@test.com")
                .telefonoMovil("5599887766")
                .build();

        when(clienteService.gestionarCliente(any(ClienteGestionDTO.class))).thenReturn(
                GestionResultadoDTO.builder()
                        .mensaje("Cliente actualizado exitosamente mediante sistema de banderas")
                        .cliente(actualizado)
                        .build());

        mockMvc.perform(patch("/api/v1/clientes/actualizacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(gestionDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.telefonoMovil").value("5599887766"));
    }
}
