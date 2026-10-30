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
    private final com.proyecto.servicios.service.GestoPagoSendTxService sendTxService;
    private final com.proyecto.servicios.service.TwoFactorOtpService otpService;

    @io.swagger.v3.oas.annotations.Operation(
        summary = "Solicitar código OTP de Doble Factor (2FA)",
        description = "Genera un código OTP temporal de 6 dígitos con vigencia de 5 minutos almacenado en Redis/Memoria " +
                "y lo remite al correo electrónico/móvil registrado del usuario para autorizar la transacción sendTx."
    )
    @org.springframework.web.bind.annotation.PostMapping("/otp/solicitar")
    public ResponseEntity<ApiResponse<com.proyecto.servicios.model.response.gestopago.OtpResponseDTO>> solicitarOtp(
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.proyecto.servicios.model.request.gestopago.OtpRequestDTO otpRequest,
            HttpServletRequest request) {

        com.proyecto.servicios.model.response.gestopago.OtpResponseDTO result = otpService.generarOtp(otpRequest);
        int code = result.isExitoso() ? 1 : 0;
        return ResponseEntity.ok(ApiResponse.<com.proyecto.servicios.model.response.gestopago.OtpResponseDTO>builder()
                .code(code)
                .message(result.getMensaje())
                .path(request.getRequestURI())
                .data(result)
                .build());
    }

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

    @io.swagger.v3.oas.annotations.Operation(
        summary = "Realizar recarga telefónica o compra de paquete de datos",
        description = "Ejecuta una recarga telefónica en GestoPago/PuntoRed. Requiere número telefónico a 10 dígitos obligatorio, " +
                "cuenta activa, saldo suficiente, PIN de autorización del usuario y OTP 2FA. " +
                "Si se omite el monto, se tomará automáticamente el precio oficial del producto en el catálogo."
    )
    @org.springframework.web.bind.annotation.PostMapping("/recargas")
    public ResponseEntity<ApiResponse<com.proyecto.servicios.model.response.gestopago.SendTxResultDTO>> recarga(
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.proyecto.servicios.model.request.gestopago.RecargaRequestDTO recargaRequest,
            HttpServletRequest request) {

        com.proyecto.servicios.model.response.gestopago.SendTxResultDTO result =
                sendTxService.procesarTransaccion(recargaRequest.toSendTxRequest());

        int code = result.isExitoso() ? 1 : 0;
        return ResponseEntity.ok(ApiResponse.<com.proyecto.servicios.model.response.gestopago.SendTxResultDTO>builder()
                .code(code)
                .codigo(result.getCodigo())
                .message(result.getMensaje())
                .path(request.getRequestURI())
                .data(result)
                .build());
    }

    @io.swagger.v3.oas.annotations.Operation(
        summary = "Realizar pago de recibo de servicio (Luz, Agua, Gas, Telefonía fija, etc.)",
        description = "Ejecuta el pago de un servicio público o privado en GestoPago/PuntoRed. " +
                "Requiere referencia alfanumérica del recibo obligatoria, monto a pagar obligatorio, " +
                "cuenta activa, saldo suficiente, PIN de autorización del usuario y OTP 2FA."
    )
    @org.springframework.web.bind.annotation.PostMapping("/servicios")
    public ResponseEntity<ApiResponse<com.proyecto.servicios.model.response.gestopago.SendTxResultDTO>> pagarServicio(
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.proyecto.servicios.model.request.gestopago.PagoServicioRequestDTO servicioRequest,
            HttpServletRequest request) {

        com.proyecto.servicios.model.response.gestopago.SendTxResultDTO result =
                sendTxService.procesarTransaccion(servicioRequest.toSendTxRequest());

        int code = result.isExitoso() ? 1 : 0;
        return ResponseEntity.ok(ApiResponse.<com.proyecto.servicios.model.response.gestopago.SendTxResultDTO>builder()
                .code(code)
                .codigo(result.getCodigo())
                .message(result.getMensaje())
                .path(request.getRequestURI())
                .data(result)
                .build());
    }

    @io.swagger.v3.oas.annotations.Operation(
        summary = "Realizar venta o pago de servicio (POST sendTx)",
        description = "Endpoint unificado para venta de recargas o pago de servicios en GestoPago/PuntoRed. " +
                "Aplica reglas de negocio: validación de cuenta activa, usuario activo, saldo suficiente en cuenta, " +
                "validación de PIN de autorización, detección de tipoFront (recarga vs servicio), " +
                "detección de transacciones duplicadas, firma AES-256 del payload y débito de saldo tras confirmación exitosa."
    )
    @org.springframework.web.bind.annotation.PostMapping("/sendTx")
    public ResponseEntity<ApiResponse<com.proyecto.servicios.model.response.gestopago.SendTxResultDTO>> sendTx(
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody com.proyecto.servicios.model.request.gestopago.SendTxRequestDTO sendTxRequest,
            HttpServletRequest request) {

        com.proyecto.servicios.model.response.gestopago.SendTxResultDTO result = sendTxService.procesarTransaccion(sendTxRequest);

        int code = result.isExitoso() ? 1 : 0;
        return ResponseEntity.ok(ApiResponse.<com.proyecto.servicios.model.response.gestopago.SendTxResultDTO>builder()
                .code(code)
                .codigo(result.getCodigo())
                .message(result.getMensaje())
                .path(request.getRequestURI())
                .data(result)
                .build());
    }
}
