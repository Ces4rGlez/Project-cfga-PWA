package com.proyecto.servicios.service.Impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proyecto.servicios.entity.gestopago.ProductoEntity;
import com.proyecto.servicios.entity.gestopago.TransaccionSendTxEntity;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.enums.SendTxErrorCode;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxPayload;
import com.proyecto.servicios.model.gestopago.GestoPagoSendTxResponse;
import com.proyecto.servicios.model.request.gestopago.SendTxRequestDTO;
import com.proyecto.servicios.model.response.gestopago.SendTxResultDTO;
import com.proyecto.servicios.repository.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.gestopago.ProductoRepository;
import com.proyecto.servicios.repositorys.gestopago.TransaccionSendTxRepository;
import com.proyecto.servicios.service.GestoPagoIntegrationService;
import com.proyecto.servicios.service.GestoPagoSendTxService;
import com.proyecto.servicios.util.AesEncryptionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Implementación del servicio de transacciones sendTx.
 * Sigue la arquitectura y especificación indicada en el diagrama de pizarra:
 * 1. Validación de cuenta bancaria, estatus activa y usuario activo.
 * 2. Validación de PIN de autorización contra la contraseña cifrada del
 * usuario.
 * 3. Detección automática de tipoFront y validación de teléfono / referencia.
 * 4. Validación de saldo suficiente antes de enviar la transacción.
 * 5. Generación de UPC (10 caracteres aleatorios si no viene dado) y validación
 * de transacciones duplicadas.
 * 6. Firma y cifrado AES-256 del payload JSON antes del envío.
 * 7. Envío a sendTx de GestoPago.
 * 8. Descuento de saldo ÚNICAMENTE tras confirmación exitosa del proveedor
 * (código "01").
 * 9. Registro de evidencia en bitácora de transacciones (log obligatorio).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GestoPagoSendTxServiceImpl implements GestoPagoSendTxService {

    private final CuentaRepository cuentaRepository;
    private final ProductoRepository productoRepository;
    private final TransaccionSendTxRepository transaccionRepository;
    private final GestoPagoIntegrationService integrationService;
    private final AesEncryptionUtil aesEncryptionUtil;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final com.proyecto.servicios.service.TwoFactorOtpService twoFactorOtpService;

    @Value("${gestopago.auth.unidad:083}")
    private String defaultUnidad;

    @Value("${gestopago.auth.2fa.required:false}")
    private boolean require2fa;

    /**
     * Modo simulación: cuando es true, omite la llamada real a GestoPago sendTx.do
     * y retorna una respuesta simulada de éxito. Útil para desarrollo cuando el
     * token
     * de GestoPago está agotado o el sandbox no está disponible.
     * Desactivar en producción: gestopago.sendtx.modo-simulacion=false
     */
    @Value("${gestopago.sendtx.modo-simulacion:false}")
    private boolean modoSimulacion;

    private static final DateTimeFormatter HORA_LOCAL_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Override
    @Transactional
    public SendTxResultDTO procesarTransaccion(SendTxRequestDTO request) {
        log.info("Iniciando procesamiento de transacción sendTx para cuenta={}, idProducto={}, idServicio={}",
                request.getNumeroCuenta(), request.getIdProducto(), request.getIdServicio());

        // ==========================================
        // 1. VALIDACIÓN DE CUENTA Y USUARIO CON BLOQUEO PESIMISTA (Prevención de
        // concurrencia y doble gasto)
        // ==========================================
        Cuenta cuenta = cuentaRepository.findByNumeroCuentaWithLock(request.getNumeroCuenta())
                .orElse(null);

        if (cuenta == null) {
            log.warn("Cuenta no encontrada: {}", request.getNumeroCuenta());
            return buildErrorResult(SendTxErrorCode.ACCOUNT_INACTIVE.getCodigo(),
                    "No se encontró la cuenta bancaria con número: " + request.getNumeroCuenta(), request, null);
        }

        // Validar estatus de la cuenta
        if (!"ACTIVA".equalsIgnoreCase(cuenta.getEstatus())) {
            log.warn("Cuenta {} con estatus no activo: {}", cuenta.getNumeroCuenta(), cuenta.getEstatus());
            String errCode = "BLOQUEADA".equalsIgnoreCase(cuenta.getEstatus())
                    ? SendTxErrorCode.ACCOUNT_BLOCKED.getCodigo()
                    : SendTxErrorCode.ACCOUNT_INACTIVE.getCodigo();
            return buildErrorResult(errCode, "La cuenta bancaria se encuentra en estatus: " + cuenta.getEstatus(),
                    request, cuenta);
        }

        // Validar usuario activo
        Cliente cliente = cuenta.getCliente();
        Usuario usuario = cliente != null ? cliente.getUsuario() : null;

        if (usuario == null || !Boolean.TRUE.equals(usuario.getActivo())) {
            log.warn("Usuario inactivo o nulo para la cuenta {}", cuenta.getNumeroCuenta());
            return buildErrorResult(SendTxErrorCode.USER_INACTIVE.getCodigo(),
                    "El usuario titular de la cuenta se encuentra inactivo", request, cuenta);
        }

        // Validar que el usuario autenticado en la sesión (JWT) sea el titular de la
        // cuenta
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String correoAutenticado = auth.getName();
            if (correoAutenticado != null && usuario.getCorreoElectronico() != null
                    && !correoAutenticado.equalsIgnoreCase(usuario.getCorreoElectronico())) {
                log.warn("Intento no autorizado: usuario logueado={} intentó operar cuenta de={}",
                        correoAutenticado, usuario.getCorreoElectronico());
                return buildErrorResult(SendTxErrorCode.UNAUTHORIZED_ACCOUNT.getCodigo(),
                        "No tiene autorización para operar esta cuenta bancaria. Solo puede realizar transacciones sobre sus propias cuentas.",
                        request, cuenta);
            }
        }

        // Validar PIN de autorización (1er factor: contraseña o PIN del usuario
        // titular)
        if (request.getPinAutorizacion() == null
                || !passwordEncoder.matches(request.getPinAutorizacion(), usuario.getPassword())) {
            log.warn("PIN o contraseña de autorización inválida para usuario={}", usuario.getCorreoElectronico());
            return buildErrorResult(SendTxErrorCode.INVALID_PIN.getCodigo(),
                    "El PIN o contraseña de autorización es incorrecto", request, cuenta);
        }

        // ==========================================
        // 1.1 VALIDACIÓN DE DOBLE FACTOR DE AUTENTICACIÓN (2FA / OTP)
        // ==========================================
        boolean otpEnviado = request.getCodigoOtp() != null && !request.getCodigoOtp().trim().isEmpty();
        if (require2fa || otpEnviado) {
            if (!otpEnviado) {
                log.warn("Se requiere código OTP de 2FA para autorizar la transacción en cuenta {}",
                        cuenta.getNumeroCuenta());
                return buildErrorResult(SendTxErrorCode.OTP_REQUIRED.getCodigo(),
                        "Se requiere el código OTP de doble factor (2FA) para autorizar la transacción", request,
                        cuenta);
            }

            boolean otpValido = twoFactorOtpService.validarOtp(cuenta.getNumeroCuenta(), request.getCodigoOtp());
            if (!otpValido) {
                log.warn("Código OTP 2FA inválido o expirado para cuenta {}", cuenta.getNumeroCuenta());
                return buildErrorResult(SendTxErrorCode.INVALID_OTP.getCodigo(),
                        "El código de doble factor (OTP) es incorrecto o ha expirado. Solicite uno nuevo.", request,
                        cuenta);
            }

            // Consumir el OTP para evitar reuso
            twoFactorOtpService.consumirOtp(cuenta.getNumeroCuenta());
            log.info("Código OTP 2FA validado y consumido con éxito para cuenta {}", cuenta.getNumeroCuenta());
        }

        // ==========================================
        // 2. DETECCIÓN DE PRODUCTO Y SERVICIO tipoFront
        // ==========================================
        Optional<ProductoEntity> productoOpt = productoRepository.findById(request.getIdProducto());
        if (productoOpt.isEmpty()) {
            log.warn("Producto no encontrado en catálogo local: {}", request.getIdProducto());
            return buildErrorResult(SendTxErrorCode.PRODUCT_NOT_FOUND.getCodigo(),
                    "El producto con ID " + request.getIdProducto() + " no existe en el catálogo", request, cuenta);
        }

        ProductoEntity producto = productoOpt.get();

        // Validar que el idServicio enviado coincida con el servicio del producto en el
        // catálogo
        if (producto.getIdServicio() != null && !producto.getIdServicio().equals(request.getIdServicio())) {
            log.warn("El idServicio enviado ({}) no corresponde al idServicio ({}) del producto ({}) en el catálogo",
                    request.getIdServicio(), producto.getIdServicio(), producto.getIdProducto());
            return buildErrorResult(SendTxErrorCode.INVALID_PARAMETER.getCodigo(),
                    "El idServicio especificado (" + request.getIdServicio()
                            + ") no corresponde al producto en el catálogo (servicio oficial: "
                            + producto.getIdServicio() + ")",
                    request, cuenta);
        }

        Integer tipoFrontProducto = (producto.getTipoFront() != null) ? producto.getTipoFront() : 1;

        // Validar correspondencia estricta de tipo de operación (Recarga vs Pago de
        // Servicio)
        if (request.getTipoFront() != null && !request.getTipoFront().equals(tipoFrontProducto)) {
            String tipoEsperado = request.getTipoFront() == 1 ? "Recarga telefónica" : "Pago de servicio";
            String tipoReal = tipoFrontProducto == 1 ? "Recarga telefónica" : "Pago de servicio";
            log.warn("Discrepancia de tipoFront: se invocó como {} ({}), pero el producto {} es {} ({})",
                    tipoEsperado, request.getTipoFront(), producto.getIdProducto(), tipoReal, tipoFrontProducto);
            return buildErrorResult(SendTxErrorCode.INVALID_PARAMETER.getCodigo(),
                    "Operación inválida: El endpoint invocado es exclusivo para " + tipoEsperado +
                            ", pero el producto seleccionado (" + producto.getProducto() + ") corresponde a " + tipoReal
                            +
                            ". Por favor, utilice el endpoint correspondiente.",
                    request, cuenta);
        }

        Integer tipoFront = tipoFrontProducto;
        Integer idCatTipoServicio = producto.getIdCatTipoServicio();

        // Determinar teléfono y referencia según reglas de tipoFront e
        // idCatTipoServicio
        String telefono;
        String referencia = null;

        if (idCatTipoServicio != null && (idCatTipoServicio == 10 || idCatTipoServicio == 11)) {
            // Regla: Para tarjetas de regalo / pines redimibles (idCatTipoServicio 10 y 11)
            telefono = "#1111111111";
        } else if (tipoFront == 1) {
            // Regla tipoFront 1: Recarga telefónica - número a 10 dígitos en el campo
            // "telefono"
            String tel = request.getTelefono() != null ? request.getTelefono().trim()
                    : (request.getTelefonoOReferencia() != null ? request.getTelefonoOReferencia().trim() : null);

            if (tel == null || !tel.matches("^\\d{10}$")) {
                log.warn("Número telefónico inválido para tipoFront 1: {}", tel);
                return buildErrorResult(SendTxErrorCode.INVALID_PHONE.getCodigo(),
                        "Para recargas (tipoFront 1), se debe ingresar un número telefónico válido a 10 dígitos",
                        request, cuenta);
            }
            telefono = tel;
        } else {
            // Regla tipoFront 2: Pago de servicios - "telefono" fijo a "1111111111" y se
            // requiere "referencia"
            telefono = "1111111111";
            String ref = request.getReferencia() != null ? request.getReferencia().trim()
                    : (request.getTelefonoOReferencia() != null ? request.getTelefonoOReferencia().trim() : null);

            if (ref == null || ref.isEmpty() || ref.length() > 100) {
                log.warn("Referencia inválida para tipoFront 2: {}", ref);
                return buildErrorResult(SendTxErrorCode.INVALID_REFERENCE.getCodigo(),
                        "Para pago de servicios (tipoFront 2), se requiere una referencia alfanumérica válida (1 a 100 caracteres)",
                        request, cuenta);
            }
            referencia = ref;
        }

        // ==========================================
        // 3. DETERMINACIÓN DEL MONTO Y VALIDACIÓN CONTRA CATÁLOGO (Anti-manipulación de
        // montos)
        // ==========================================
        BigDecimal montoEfectivo = null;

        if (tipoFront == 2) {
            // Para pago de servicios el monto viene en el request
            if (request.getMonto() == null || request.getMonto().compareTo(BigDecimal.ZERO) <= 0) {
                return buildErrorResult(SendTxErrorCode.INVALID_AMOUNT.getCodigo(),
                        "El monto a pagar debe ser mayor a cero", request, cuenta);
            }
            montoEfectivo = request.getMonto();
        } else {
            // Para recargas/pines, el catálogo define los precios oficiales
            BigDecimal precioCatalogo = null;
            if (producto.getPrecio() != null && !producto.getPrecio().trim().isEmpty()) {
                try {
                    precioCatalogo = new BigDecimal(producto.getPrecio().trim());
                } catch (Exception e) {
                    log.warn("No se pudo parsear el precio del producto: {}", producto.getPrecio());
                }
            }

            // Si el producto tiene precio fijo oficial en el catálogo, debe coincidir
            // exactamente para evitar montos manipulados
            if (precioCatalogo != null && precioCatalogo.compareTo(BigDecimal.ZERO) > 0) {
                if (request.getMonto() != null && request.getMonto().compareTo(precioCatalogo) != 0) {
                    log.warn("Intento de monto manipulado: enviado={}, oficial catálogo={}", request.getMonto(),
                            precioCatalogo);
                    return buildErrorResult(SendTxErrorCode.INVALID_AMOUNT.getCodigo(),
                            "El monto solicitado ($" + request.getMonto()
                                    + ") no coincide con el valor oficial del producto en el catálogo ($"
                                    + precioCatalogo + "). No se permiten montos manipulados.",
                            request, cuenta);
                }
                montoEfectivo = precioCatalogo;
            } else if (request.getMonto() != null && request.getMonto().compareTo(BigDecimal.ZERO) > 0) {
                montoEfectivo = request.getMonto();
            }

            if (montoEfectivo == null || montoEfectivo.compareTo(BigDecimal.ZERO) <= 0) {
                return buildErrorResult(SendTxErrorCode.INVALID_AMOUNT.getCodigo(),
                        "No se pudo determinar el monto de la recarga. Especifíquelo en la petición conforme al catálogo.",
                        request, cuenta);
            }
        }

        // VALIDACIÓN DE SALDO: Detectar si el dinero es suficiente
        // (sendTx_insufficientFunds)
        if (cuenta.getSaldo().compareTo(montoEfectivo) < 0) {
            log.warn("Saldo insuficiente en la cuenta {}. Saldo actual: {}, Monto requerido: {}",
                    cuenta.getNumeroCuenta(), cuenta.getSaldo(), montoEfectivo);

            // Registrar intento fallido en evidencia
            registrarTransaccionLog(cuenta.getNumeroCuenta(), request.getIdProducto(), request.getIdServicio(),
                    tipoFront, idCatTipoServicio, telefono, referencia, montoEfectivo,
                    request.getUpc() != null ? request.getUpc() : "N/A", "N/A", null,
                    SendTxErrorCode.INSUFFICIENT_FUNDS.getCodigo(), "Saldo insuficiente en cuenta bancaria",
                    null, null, null, null, "RECHAZADA");

            return SendTxResultDTO.builder()
                    .exitoso(false)
                    .codigo(SendTxErrorCode.INSUFFICIENT_FUNDS.getCodigo())
                    .mensaje("Saldo insuficiente en la cuenta bancaria para realizar la transacción")
                    .saldoAnterior(cuenta.getSaldo())
                    .saldoNuevo(cuenta.getSaldo())
                    .montoCobrado(BigDecimal.ZERO)
                    .build();
        }

        // ==========================================
        // 4. UPC Y DETECCIÓN DE DUPLICADOS (sendTx_duplicated, "UPS -> Sacarlo 10
        // RandomString")
        // ==========================================
        String upc = (request.getUpc() != null && !request.getUpc().trim().isEmpty())
                ? request.getUpc().trim()
                : "UPC" + RandomStringUtils.randomAlphanumeric(7).toUpperCase();

        // Validar regla de duplicados según especificación PuntoRed:
        // 1. Recargas telefónicas / Pines: Duplicado si el mismo UPC se recibió en los
        // últimos 15 minutos
        // 2. Servicios: Duplicado si en las últimas 24 horas se recibió el mismo UPC o
        // mismo servicio + monto
        boolean isDuplicate = false;
        if (tipoFront == 1 || (idCatTipoServicio != null && (idCatTipoServicio == 10 || idCatTipoServicio == 11))) {
            LocalDateTime threshold15m = LocalDateTime.now().minusMinutes(15);
            if (transaccionRepository.existsByUpcAndFechaRegistroAfter(upc, threshold15m)) {
                isDuplicate = true;
            }
        } else {
            LocalDateTime threshold24h = LocalDateTime.now().minusHours(24);
            if (transaccionRepository.existsByUpcAndFechaRegistroAfter(upc, threshold24h)
                    || transaccionRepository.existsDuplicateServicePayment(request.getIdServicio(), montoEfectivo,
                            threshold24h)) {
                isDuplicate = true;
            }
        }

        if (isDuplicate) {
            log.warn("Transacción duplicada detectada para UPC={} o servicio={}/monto={}", upc, request.getIdServicio(),
                    montoEfectivo);

            registrarTransaccionLog(cuenta.getNumeroCuenta(), request.getIdProducto(), request.getIdServicio(),
                    tipoFront, idCatTipoServicio, telefono, referencia, montoEfectivo,
                    upc, "N/A", null,
                    SendTxErrorCode.DUPLICATED.getCodigo(), "Transacción duplicada detectada localmente",
                    null, null, null, null, "RECHAZADA");

            return SendTxResultDTO.builder()
                    .exitoso(false)
                    .codigo(SendTxErrorCode.DUPLICATED.getCodigo())
                    .mensaje("Transacción duplicada: Este pago o UPC ya fue procesado recientemente")
                    .saldoAnterior(cuenta.getSaldo())
                    .saldoNuevo(cuenta.getSaldo())
                    .montoCobrado(BigDecimal.ZERO)
                    .upc(upc)
                    .build();
        }

        // ==========================================
        // 5. CONSTRUCCIÓN DE PAYLOAD Y CIFRADO AES-256 (Firmar)
        // ==========================================
        String horaLocal = LocalDateTime.now().format(HORA_LOCAL_FORMATTER);
        String unidad = (request.getUnidad() != null && !request.getUnidad().trim().isEmpty())
                ? request.getUnidad().trim()
                : defaultUnidad;

        GestoPagoSendTxPayload payload = GestoPagoSendTxPayload.builder()
                .idProducto(request.getIdProducto())
                .idServicio(request.getIdServicio())
                .telefono(telefono)
                .horaLocal(horaLocal)
                .upc(upc)
                .unidad(unidad)
                .build();

        // En pagos de servicios se agregan referencia y montoPago
        if (tipoFront == 2) {
            payload.setReferencia(referencia);
            payload.setMontoPago(montoEfectivo.doubleValue());
        }

        String jsonPayload;
        try {
            jsonPayload = objectMapper.writeValueAsString(payload);
            log.info("Payload JSON generado para sendTx: {}", jsonPayload);
        } catch (Exception e) {
            log.error("Error al serializar payload JSON: {}", e.getMessage(), e);
            throw new RuntimeException("Error serializando payload para GestoPago", e);
        }

        // Cifrado AES-256-CBC ("signed")
        String signedPayload = aesEncryptionUtil.encrypt(jsonPayload);
        String randomTimestamp = String.valueOf(System.currentTimeMillis());

        // ==========================================
        // 6. LLAMADA A GESTOPAGO (POST sendTx.do) — o SIMULACIÓN LOCAL
        // ==========================================
        GestoPagoSendTxResponse response;

        if (modoSimulacion) {
            // ---------------------------------------------------------------
            // MODO SIMULACIÓN: se omite la llamada real a GestoPago.
            // Se construye una respuesta simulada de éxito (Código 01).
            // Útil cuando el token GestoPago Sandbox está agotado por el día.
            // Para desactivar: gestopago.sendtx.modo-simulacion=false
            // ---------------------------------------------------------------
            log.warn("[MODO SIMULACION] sendTx NO se envía a GestoPago. " +
                    "Respuesta simulada de éxito para cuenta={}, producto={}, monto={}",
                    cuenta.getNumeroCuenta(), request.getIdProducto(), montoEfectivo);

            String numAutorizacionSim = "SIM-" + RandomStringUtils.randomNumeric(8);
            String idTxSim = "TXS-" + RandomStringUtils.randomAlphanumeric(10).toUpperCase();

            GestoPagoSendTxResponse.MensajeDto mensajeSim = new GestoPagoSendTxResponse.MensajeDto(
                    "01", "[SIMULADO] Operacion realizada con exito");

            response = GestoPagoSendTxResponse.builder()
                    .codigo("01")
                    .idTx(idTxSim)
                    .numAutorizacion(numAutorizacionSim)
                    .fecha(LocalDateTime.now().toString())
                    .mensaje(mensajeSim)
                    .build();
        } else {
            // Llamada real a GestoPago Sandbox / Producción
            try {
                response = integrationService.sendTx(randomTimestamp, signedPayload);
            } catch (Exception e) {
                log.error("Excepción al comunicarse con sendTx de GestoPago: {}", e.getMessage(), e);

                boolean isTimeout = e instanceof java.net.SocketTimeoutException
                        || e instanceof java.util.concurrent.TimeoutException
                        || e instanceof feign.RetryableException
                        || (e.getMessage() != null && e.getMessage().toLowerCase().contains("timeout"));

                String estatusAudit = isTimeout ? "PENDIENTE_CONCILIACION" : "FALLIDA";
                String errCode = isTimeout ? SendTxErrorCode.TIMEOUT.getCodigo()
                        : SendTxErrorCode.EXTERNAL_ERROR.getCodigo();
                String errMsg = isTimeout
                        ? "Tiempo de espera agotado con el proveedor GestoPago. La transacción quedó en estado PENDIENTE_CONCILIACION para verificar su aplicación antes de cualquier reintento."
                        : "Error de comunicación con el proveedor GestoPago: " + e.getMessage();

                registrarTransaccionLog(cuenta.getNumeroCuenta(), request.getIdProducto(), request.getIdServicio(),
                        tipoFront, idCatTipoServicio, telefono, referencia, montoEfectivo,
                        upc, horaLocal, jsonPayload,
                        isTimeout ? "TIMEOUT" : "ERROR_COMM", errMsg,
                        null, null, null, null, estatusAudit);

                return SendTxResultDTO.builder()
                        .exitoso(false)
                        .codigo(errCode)
                        .mensaje(errMsg)
                        .saldoAnterior(cuenta.getSaldo())
                        .saldoNuevo(cuenta.getSaldo())
                        .montoCobrado(BigDecimal.ZERO)
                        .upc(upc)
                        .build();
            }
        }

        // ==========================================
        // 7. PROCESAMIENTO DE RESPUESTA Y ¿EN QUÉ MOMENTO DESCONTAR SALDO?
        // Regla bancaria: Descontar saldo ÚNICAMENTE tras confirmación de éxito ("01")
        // ==========================================
        String codigoRespuesta = response != null ? response.getCodigoFinal() : null;
        String textoRespuesta = response != null ? response.getTextoFinal() : "Sin respuesta del proveedor";
        String numAutorizacion = response != null ? response.getNumAutorizacion() : null;
        String idTx = response != null ? response.getIdTx() : null;
        String pin = response != null ? response.getPin() : null;
        String legend = response != null ? response.getLegend() : null;
        Double comision = response != null ? response.getComision() : null;

        BigDecimal saldoAnterior = cuenta.getSaldo();
        BigDecimal saldoNuevo = saldoAnterior;

        boolean esExitoso = response != null && response.isExitoso();

        if (esExitoso) {
            // MOMENTO DE DESCONTAR EL SALDO: Transacción confirmada por el proveedor
            // externo
            saldoNuevo = saldoAnterior.subtract(montoEfectivo);
            cuenta.setSaldo(saldoNuevo);
            cuentaRepository.save(cuenta);

            log.info(
                    "Transacción exitosa. Saldo debitado a cuenta {}. Saldo previo={}, Saldo nuevo={}, Autorización={}",
                    cuenta.getNumeroCuenta(), saldoAnterior, saldoNuevo, numAutorizacion);

            registrarTransaccionLog(cuenta.getNumeroCuenta(), request.getIdProducto(), request.getIdServicio(),
                    tipoFront, idCatTipoServicio, telefono, referencia, montoEfectivo,
                    upc, horaLocal, jsonPayload,
                    codigoRespuesta, textoRespuesta,
                    numAutorizacion, idTx, pin, legend, "EXITOSA");

            return SendTxResultDTO.builder()
                    .exitoso(true)
                    .codigo("01")
                    .mensaje(textoRespuesta)
                    .idTx(idTx)
                    .numAutorizacion(numAutorizacion)
                    .saldoAnterior(saldoAnterior)
                    .saldoNuevo(saldoNuevo)
                    .montoCobrado(montoEfectivo)
                    .comision(comision)
                    .fecha(response.getFecha() != null ? response.getFecha() : LocalDateTime.now().toString())
                    .upc(upc)
                    .pin(pin)
                    .legend(legend)
                    .referencia(referencia)
                    .telefono(telefono)
                    .build();
        } else {
            // Manejo de casos de fallo mapeados a los códigos de error requeridos
            String errorMapeado;
            String estatusAudit = "RECHAZADA";

            if ("02".equals(codigoRespuesta)) {
                // Caso especial GestoPago: Saldo insuficiente del distribuidor
                errorMapeado = SendTxErrorCode.INSUFFICIENT_FUNDS.getCodigo();
            } else if ("06".equals(codigoRespuesta)) {
                // Caso especial GestoPago: Transacción aplicada anteriormente
                errorMapeado = SendTxErrorCode.DUPLICATED.getCodigo();
            } else if ("82".equals(codigoRespuesta)) {
                // Caso timeout alcanzado en GestoPago (no asumir no procesada, requiere
                // conciliación)
                errorMapeado = SendTxErrorCode.TIMEOUT.getCodigo();
                estatusAudit = "PENDIENTE_CONCILIACION";
            } else {
                errorMapeado = SendTxErrorCode.EXTERNAL_ERROR.getCodigo();
            }

            log.warn("Transacción no exitosa reportada por GestoPago. Código={}, Mensaje={}, Estatus={}",
                    codigoRespuesta, textoRespuesta, estatusAudit);

            registrarTransaccionLog(cuenta.getNumeroCuenta(), request.getIdProducto(), request.getIdServicio(),
                    tipoFront, idCatTipoServicio, telefono, referencia, montoEfectivo,
                    upc, horaLocal, jsonPayload,
                    codigoRespuesta, textoRespuesta,
                    numAutorizacion, idTx, pin, legend, estatusAudit);

            return SendTxResultDTO.builder()
                    .exitoso(false)
                    .codigo(errorMapeado)
                    .mensaje(textoRespuesta)
                    .idTx(idTx)
                    .numAutorizacion(numAutorizacion)
                    .saldoAnterior(saldoAnterior)
                    .saldoNuevo(saldoAnterior) // No se debitó nada
                    .montoCobrado(BigDecimal.ZERO)
                    .upc(upc)
                    .referencia(referencia)
                    .telefono(telefono)
                    .build();
        }
    }

    private void registrarTransaccionLog(String numeroCuenta, Integer idProducto, Integer idServicio,
            Integer tipoFront, Integer idCatTipoServicio, String telefono,
            String referencia, BigDecimal monto, String upc, String horaLocal,
            String payloadJson, String codigoRespuesta, String mensajeRespuesta,
            String numAutorizacion, String idTx, String pin, String legend,
            String estatus) {
        try {
            TransaccionSendTxEntity tx = TransaccionSendTxEntity.builder()
                    .numeroCuenta(numeroCuenta)
                    .idProducto(idProducto)
                    .idServicio(idServicio)
                    .tipoFront(tipoFront != null ? tipoFront : 1)
                    .idCatTipoServicio(idCatTipoServicio)
                    .telefono(telefono)
                    .referencia(referencia)
                    .monto(monto != null ? monto : BigDecimal.ZERO)
                    .upc(upc)
                    .horaLocal(horaLocal != null ? horaLocal : "")
                    .unidad(defaultUnidad)
                    .payloadJson(payloadJson)
                    .codigoRespuesta(codigoRespuesta)
                    .mensajeRespuesta(mensajeRespuesta)
                    .numAutorizacion(numAutorizacion)
                    .idTxGestoPago(idTx)
                    .pin(pin)
                    .legend(legend)
                    .estatus(estatus)
                    .fechaRegistro(LocalDateTime.now())
                    .build();

            transaccionRepository.save(tx);
            log.info("Evidencia de transaccion sendTx guardada con éxito. ID_TX_LOCAL={}, Estatus={}",
                    tx.getIdTransaccion(), estatus);
        } catch (Exception e) {
            log.error("Error al registrar evidencia de transaccion en base de datos: {}", e.getMessage(), e);
        }
    }

    private SendTxResultDTO buildErrorResult(String codigo, String mensaje, SendTxRequestDTO request, Cuenta cuenta) {
        return SendTxResultDTO.builder()
                .exitoso(false)
                .codigo(codigo)
                .mensaje(mensaje)
                .saldoAnterior(cuenta != null ? cuenta.getSaldo() : null)
                .saldoNuevo(cuenta != null ? cuenta.getSaldo() : null)
                .montoCobrado(BigDecimal.ZERO)
                .upc(request.getUpc())
                .build();
    }
}
