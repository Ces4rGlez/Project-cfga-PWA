package com.proyecto.servicios.controller;

import com.proyecto.servicios.entity.gestopago.ProductoEntity;
import com.proyecto.servicios.model.gestopago.EchoResponse;
import com.proyecto.servicios.model.gestopago.ValidateMeResponse;
import com.proyecto.servicios.model.response.ApiResponse;
import com.proyecto.servicios.service.GestoPagoCatalogService;
import com.proyecto.servicios.service.GestoPagoIntegrationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/gestopago")
@RequiredArgsConstructor
public class GestoPagoController {

    private final GestoPagoIntegrationService integrationService;
    private final GestoPagoCatalogService catalogService;

    @GetMapping("/products")
    public ResponseEntity<ApiResponse<List<ProductoEntity>>> getProductList(HttpServletRequest request) {
        GestoPagoCatalogService.CatalogResult result = catalogService.getProductCatalog();
        return ResponseEntity.ok(ApiResponse.<List<ProductoEntity>>builder()
                .code(result.getCode())
                .message(result.getMessage())
                .path(request.getRequestURI())
                .data(result.getCatalog())
                .build());
    }

    @GetMapping("/sync")
    public ResponseEntity<ApiResponse<List<ProductoEntity>>> forceSync(HttpServletRequest request) {
        List<ProductoEntity> catalog = catalogService.syncCatalogFromGestoPago();
        return ResponseEntity.ok(ApiResponse.<List<ProductoEntity>>builder()
                .code(1)
                .message("Catálogo sincronizado exitosamente con GestoPago")
                .path(request.getRequestURI())
                .data(catalog)
                .build());
    }

    @GetMapping("/echo")
    public ResponseEntity<ApiResponse<EchoResponse>> sendEcho(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.<EchoResponse>builder()
                .code(1)
                .message("Operación exitosa")
                .path(request.getRequestURI())
                .data(integrationService.sendEcho())
                .build());
    }

    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<ValidateMeResponse>> validateMe(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.<ValidateMeResponse>builder()
                .code(1)
                .message("Operación exitosa")
                .path(request.getRequestURI())
                .data(integrationService.validateMe())
                .build());
    }
}
