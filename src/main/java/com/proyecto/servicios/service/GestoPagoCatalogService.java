package com.proyecto.servicios.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.entity.gestopago.ProductoEntity;
import com.proyecto.servicios.model.gestopago.ProductListResponse;
import com.proyecto.servicios.repositorys.gestopago.ProductoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GestoPagoCatalogService {

    @lombok.Data
    @lombok.AllArgsConstructor
    public static class CatalogResult {
        private List<ProductoEntity> catalog;
        private int code;
        private String message;
    }

    private final GestoPagoIntegrationService integrationService;
    private final ProductoRepository productoRepository;
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String REDIS_CATALOG_KEY = "gestopago:catalog";
    private static final long REDIS_TTL_HOURS = 24;

    /**
     * Fallback strategy: Redis -> PostgreSQL -> GestoPago API
     */
    public CatalogResult getProductCatalog() {
        // 1. Try Redis
        try {
            String cachedCatalog = redisTemplate.opsForValue().get(REDIS_CATALOG_KEY);
            if (cachedCatalog != null) {
                log.info("Catálogo obtenido desde Redis Cache");
                List<ProductoEntity> data = objectMapper.readValue(cachedCatalog,
                        new TypeReference<List<ProductoEntity>>() {
                        });
                return new CatalogResult(data, 10, "Éxito: Catálogo obtenido desde Redis Cache");
            }
        } catch (Exception e) {
            log.warn("Fallo al obtener el catálogo de Redis. Fallback a PostgreSQL. Error: {}", e.getMessage());
        }

        // 2. Try PostgreSQL
        List<ProductoEntity> dbCatalog = productoRepository.findAll();
        if (dbCatalog != null && !dbCatalog.isEmpty()) {
            log.info("Catálogo obtenido desde PostgreSQL. Actualizando Redis...");
            saveToRedis(dbCatalog);
            return new CatalogResult(dbCatalog, 11,
                    "Atención: Caché vacía/caída. Catálogo obtenido desde PostgreSQL y restaurado en Redis.");
        }

        // 3. Fallback to GestoPago API (Usually on first run if DB is empty)
        log.warn("Catálogo vacío en PostgreSQL. Llamando a API GestoPago de emergencia...");
        try {
            List<ProductoEntity> apiCatalog = syncCatalogFromGestoPago();
            return new CatalogResult(apiCatalog, 12,
                    "Advertencia Crítica: BD y Caché vacías. Catálogo forzado a sincronizarse desde GestoPago API.");
        } catch (Exception e) {
            throw new RuntimeException(
                    "Error fatal: No se pudo obtener el catálogo desde Redis, PostgreSQL ni GestoPago API.");
        }
    }

    /**
     * Tarea programada que corre a las 3:00 AM todos los días.
     * Descarga el catálogo, actualiza la base de datos (upsert) y refresca Redis.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    @Transactional
    public void scheduledCatalogSync() {
        log.info("Iniciando tarea programada: Sincronización del catálogo de GestoPago (3:00 AM)");
        syncCatalogFromGestoPago();
    }

    @Transactional
    public List<ProductoEntity> syncCatalogFromGestoPago() {
        ProductListResponse response = integrationService.getProductList();

        if (response == null || response.getProductos() == null || response.getProductos().getProducto() == null) {
            log.error("No se pudo obtener el catálogo de productos desde la API");
            throw new RuntimeException("Error al consultar GestoPago");
        }

        List<ProductListResponse.ProductDto> dtos = response.getProductos().getProducto();

        // Mapear DTOs a Entidades
        List<ProductoEntity> entities = dtos.stream().map(dto -> ProductoEntity.builder()
                .idProducto(dto.getIdProducto())
                .producto(dto.getProducto())
                .servicio(dto.getServicio())
                .idServicio(dto.getIdServicio())
                .idCatTipoServicio(dto.getIdCatTipoServicio())
                .tipoFront(dto.getTipoFront())
                .hasDigitoVerificador(dto.getHasDigitoVerificador())
                .tipoReferencia(dto.getTipoReferencia())
                .precio(dto.getPrecio())
                .showAyuda(dto.getShowAyuda())
                .legend(dto.getLegend())
                .build()).collect(Collectors.toList());

        // Upsert a PostgreSQL (Guarda nuevos y actualiza existentes por ID)
        log.info("Guardando/Actualizando {} productos en PostgreSQL...", entities.size());
        List<ProductoEntity> savedEntities = productoRepository.saveAll(entities);

        // Guardar en Redis
        saveToRedis(savedEntities);

        return savedEntities;
    }

    private void saveToRedis(List<ProductoEntity> catalog) {
        try {
            String jsonCatalog = objectMapper.writeValueAsString(catalog);
            redisTemplate.opsForValue().set(REDIS_CATALOG_KEY, jsonCatalog, REDIS_TTL_HOURS, TimeUnit.HOURS);
            log.info("Catálogo guardado exitosamente en Redis");
        } catch (JsonProcessingException e) {
            log.error("Error serializando catálogo para Redis: {}", e.getMessage());
        } catch (Exception e) {
            log.error("Error guardando en Redis: {}", e.getMessage());
        }
    }
}
