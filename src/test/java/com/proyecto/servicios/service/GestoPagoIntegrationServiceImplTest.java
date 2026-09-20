package com.proyecto.servicios.service;

import com.proyecto.servicios.client.GestoPagoIntegrationClient;
import com.proyecto.servicios.model.gestopago.EchoResponse;
import com.proyecto.servicios.model.gestopago.ProductListResponse;
import com.proyecto.servicios.model.gestopago.ValidateMeResponse;
import com.proyecto.servicios.service.Impl.GestoPagoIntegrationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para el servicio de integración con GestoPago.
 * Se utiliza Mockito para simular el comportamiento del cliente Feign
 * y verificar la lógica de negocio de forma aislada.
 */
@ExtendWith(MockitoExtension.class)
class GestoPagoIntegrationServiceImplTest {

    @Mock
    private GestoPagoIntegrationClient client;

    @InjectMocks
    private GestoPagoIntegrationServiceImpl service;

    private ProductListResponse mockProductResponse;
    private EchoResponse mockEchoResponse;
    private ValidateMeResponse mockValidateResponse;

    @BeforeEach
    void setUp() {
        // Preparar respuesta mock de productos
        mockProductResponse = new ProductListResponse();
        ProductListResponse.Mensaje mensaje = new ProductListResponse.Mensaje();
        mensaje.setCodigo(1);
        mensaje.setTexto("Operacion realizada con exito");
        mockProductResponse.setMensaje(mensaje);

        ProductListResponse.Productos productos = new ProductListResponse.Productos();
        ProductListResponse.ProductDto producto1 = new ProductListResponse.ProductDto();
        producto1.setProducto("Recarga Telcel 100");
        producto1.setServicio("Telcel");
        producto1.setIdServicio(1);
        producto1.setIdProducto(101);
        producto1.setPrecio("100.0");
        producto1.setHasDigitoVerificador(false);
        producto1.setTipoReferencia("a");

        ProductListResponse.ProductDto producto2 = new ProductListResponse.ProductDto();
        producto2.setProducto("Netflix 1 Mes");
        producto2.setServicio("Netflix");
        producto2.setIdServicio(2);
        producto2.setIdProducto(202);
        producto2.setPrecio("199.0");
        producto2.setHasDigitoVerificador(false);
        producto2.setTipoReferencia("a");

        productos.setProducto(Arrays.asList(producto1, producto2));
        mockProductResponse.setProductos(productos);

        // Preparar respuesta mock de echo
        mockEchoResponse = new EchoResponse();
        mockEchoResponse.setEcho("OK");
        mockEchoResponse.setValid(1);

        // Preparar respuesta mock de validateMe
        mockValidateResponse = new ValidateMeResponse();
        ValidateMeResponse.Mensaje msgValidate = new ValidateMeResponse.Mensaje();
        msgValidate.setCodigo(0);
        msgValidate.setTexto("Operacion realizada con exito");
        msgValidate.setNombreComercio("Comercio de Prueba");
        msgValidate.setDireccion("Calle Falsa 123");
        mockValidateResponse.setMensaje(msgValidate);

        ValidateMeResponse.IdentifyMe identifyMe = new ValidateMeResponse.IdentifyMe();
        identifyMe.setValid(1);
        mockValidateResponse.setIdentifyMe(identifyMe);
    }

    // ======================== PRUEBAS getProductList ========================

    @Test
    @DisplayName("getProductList - Debe retornar la lista de productos correctamente")
    void getProductList_debeRetornarProductosCorrectamente() {
        // Arrange
        when(client.getProductList()).thenReturn(mockProductResponse);

        // Act
        ProductListResponse response = service.getProductList();

        // Assert
        assertNotNull(response, "La respuesta no debe ser nula");
        assertNotNull(response.getMensaje(), "El mensaje no debe ser nulo");
        assertEquals(1, response.getMensaje().getCodigo(), "El código del mensaje debe ser 1");
        assertEquals("Operacion realizada con exito", response.getMensaje().getTexto());
        assertNotNull(response.getProductos(), "La lista de productos no debe ser nula");
        assertEquals(2, response.getProductos().getProducto().size(), "Deben haber 2 productos");
        verify(client, times(1)).getProductList();
    }

    @Test
    @DisplayName("getProductList - Debe contener los datos correctos del primer producto")
    void getProductList_debeContenerDatosDelProducto() {
        // Arrange
        when(client.getProductList()).thenReturn(mockProductResponse);

        // Act
        ProductListResponse response = service.getProductList();
        ProductListResponse.ProductDto primerProducto = response.getProductos().getProducto().get(0);

        // Assert
        assertEquals("Recarga Telcel 100", primerProducto.getProducto());
        assertEquals("Telcel", primerProducto.getServicio());
        assertEquals(1, primerProducto.getIdServicio());
        assertEquals(101, primerProducto.getIdProducto());
        assertEquals("100.0", primerProducto.getPrecio());
    }

    @Test
    @DisplayName("getProductList - Debe lanzar RuntimeException cuando el cliente falla")
    void getProductList_debeLanzarExcepcionCuandoClienteFalla() {
        // Arrange
        when(client.getProductList()).thenThrow(new RuntimeException("Error de conexión"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.getProductList());
        assertTrue(exception.getMessage().contains("Error en integración con GestoPago"));
        verify(client, times(1)).getProductList();
    }

    // ======================== PRUEBAS sendEcho ========================

    @Test
    @DisplayName("sendEcho - Debe retornar respuesta exitosa de echo")
    void sendEcho_debeRetornarEchoExitoso() {
        // Arrange
        when(client.sendEcho()).thenReturn(mockEchoResponse);

        // Act
        EchoResponse response = service.sendEcho();

        // Assert
        assertNotNull(response, "La respuesta de echo no debe ser nula");
        assertEquals("OK", response.getEcho(), "El echo debe ser 'OK'");
        assertEquals(1, response.getValid(), "El campo valid debe ser 1");
        verify(client, times(1)).sendEcho();
    }

    @Test
    @DisplayName("sendEcho - Debe lanzar RuntimeException cuando el cliente falla")
    void sendEcho_debeLanzarExcepcionCuandoClienteFalla() {
        // Arrange
        when(client.sendEcho()).thenThrow(new RuntimeException("Timeout"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.sendEcho());
        assertTrue(exception.getMessage().contains("Error en integración con GestoPago"));
        verify(client, times(1)).sendEcho();
    }

    // ======================== PRUEBAS validateMe ========================

    @Test
    @DisplayName("validateMe - Debe retornar validación exitosa del comercio")
    void validateMe_debeRetornarValidacionExitosa() {
        // Arrange
        when(client.validateMe()).thenReturn(mockValidateResponse);

        // Act
        ValidateMeResponse response = service.validateMe();

        // Assert
        assertNotNull(response, "La respuesta de validación no debe ser nula");
        assertNotNull(response.getMensaje(), "El mensaje no debe ser nulo");
        assertEquals(0, response.getMensaje().getCodigo());
        assertEquals("Comercio de Prueba", response.getMensaje().getNombreComercio());
        assertEquals("Calle Falsa 123", response.getMensaje().getDireccion());
        assertNotNull(response.getIdentifyMe(), "IdentifyMe no debe ser nulo");
        assertEquals(1, response.getIdentifyMe().getValid());
        verify(client, times(1)).validateMe();
    }

    @Test
    @DisplayName("validateMe - Debe lanzar RuntimeException cuando el cliente falla")
    void validateMe_debeLanzarExcepcionCuandoClienteFalla() {
        // Arrange
        when(client.validateMe()).thenThrow(new RuntimeException("403 Forbidden"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> service.validateMe());
        assertTrue(exception.getMessage().contains("Error en integración con GestoPago"));
        verify(client, times(1)).validateMe();
    }
}
