package com.proyecto.servicios.mapper;

import com.proyecto.servicios.entity.gestopago.ProductoEntity;
import com.proyecto.servicios.model.gestopago.ProductListResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

/**
 * Mapper que convierte los DTOs recibidos del XML de GestoPago
 * a entidades JPA persistibles en PostgreSQL.
 * MapStruct genera la implementación automáticamente en tiempo de compilación.
 */
@Mapper(componentModel = "spring")
public interface ProductoMapper {

    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    ProductoEntity toEntity(ProductListResponse.ProductDto dto);

    List<ProductoEntity> toEntityList(List<ProductListResponse.ProductDto> dtos);
}
