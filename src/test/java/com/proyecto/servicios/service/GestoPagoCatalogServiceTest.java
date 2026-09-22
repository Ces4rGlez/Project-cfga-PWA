package com.proyecto.servicios.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.entity.gestopago.ProductoEntity;
import com.proyecto.servicios.model.gestopago.ProductListResponse;
import com.proyecto.servicios.model.gestopago.Producto;
import com.proyecto.servicios.repositorys.gestopago.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GestoPagoCatalogServiceTest {

    @Mock
    private GestoPagoIntegrationService integrationService;

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private GestoPagoCatalogService catalogService;

    private List<ProductoEntity> mockDbCatalog;

    @BeforeEach
    void setUp() {
        mockDbCatalog = new ArrayList<>();
        ProductoEntity p = new ProductoEntity();
        p.setIdProducto(1);
        p.setProducto("Recarga Telcel 50");
        mockDbCatalog.add(p);
    }

    @Test
    void getProductCatalog_ShouldReturnFromRedis_WhenCacheExists() throws Exception {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("gestopago:catalog")).thenReturn("[{\"idProducto\":1}]");
        when(objectMapper.readValue(anyString(), any(TypeReference.class))).thenReturn(mockDbCatalog);

        // Act
        GestoPagoCatalogService.CatalogResult result = catalogService.getProductCatalog();

        // Assert
        assertEquals(10, result.getCode());
        assertEquals(1, result.getCatalog().size());
        verify(productoRepository, never()).findAll(); // Nunca debe tocar BD
    }

    @Test
    void getProductCatalog_ShouldReturnFromPostgres_WhenRedisIsEmpty() throws Exception {
        // Arrange
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("gestopago:catalog")).thenReturn(null);
        when(productoRepository.findAll()).thenReturn(mockDbCatalog);
        when(objectMapper.writeValueAsString(any())).thenReturn("[]");

        // Act
        GestoPagoCatalogService.CatalogResult result = catalogService.getProductCatalog();

        // Assert
        assertEquals(11, result.getCode());
        assertEquals(1, result.getCatalog().size());
        verify(productoRepository, times(1)).findAll();
        verify(valueOperations, times(1)).set(eq("gestopago:catalog"), anyString(), anyLong(), any()); // Verifica que se reparó Redis
    }

    @Test
    void syncCatalogFromGestoPago_ShouldCallApiAndSaveToDb() {
        // Arrange
        ProductListResponse apiResponse = new ProductListResponse();
        ProductListResponse.Productos wrapper = new ProductListResponse.Productos();
        List<Producto> productosApi = new ArrayList<>();
        Producto pApi = new Producto();
        pApi.setIdProducto(2);
        pApi.setProducto("Paquete Movistar");
        productosApi.add(pApi);
        wrapper.setProducto(productosApi);
        apiResponse.setProductos(wrapper);

        when(integrationService.getProductList()).thenReturn(apiResponse);

        // Act
        List<ProductoEntity> result = catalogService.syncCatalogFromGestoPago();

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Paquete Movistar", result.get(0).getProducto());
        verify(productoRepository, times(1)).saveAll(any()); // Verifica que se guardó en BD
    }
}
