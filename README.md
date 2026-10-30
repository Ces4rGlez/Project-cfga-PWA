# Aplicativo CFGA - GestoPago Integration

![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.6-6db33f?style=flat-square&logo=spring)
![Java](https://img.shields.io/badge/Java-17-007396?style=flat-square&logo=java)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-336791?style=flat-square&logo=postgresql)
![Redis](https://img.shields.io/badge/Redis-Cache-dc382d?style=flat-square&logo=redis)
![Docker](https://img.shields.io/badge/Docker-Compose-2496ed?style=flat-square&logo=docker)

## Descripcion del Proyecto

Este proyecto es una integracion robusta del catalogo de productos de GestoPago (PuntoRed) utilizando una arquitectura empresarial en Java Spring Boot. Su objetivo principal es mantener sincronizado el catalogo de productos de forma automatica y ofrecer consultas de ultra-baja latencia.

## Arquitectura y Tecnologias

El sistema implementa una arquitectura con manejo avanzado de excepciones, programacion de tareas y una estrategia de fallback de tres niveles para garantizar la maxima disponibilidad:

1. **Redis Cache (Nivel 1):** Almacenamiento en memoria ultra rapido para respuestas en milisegundos.
2. **PostgreSQL (Nivel 2):** Persistencia en disco relacional mediante Spring Data JPA y Flyway.
3. **GestoPago API Externa (Nivel 3):** Consumo directo de la API externa mediante Feign Clients si los niveles locales fallan, aplicando procesos de Upsert para auto-reparar la base de datos local y la cache.

## Prerrequisitos

Para ejecutar este proyecto en un entorno local, se requiere:

* **Java Development Kit (JDK) 17+**
* **Docker y Docker Compose** (Para instanciar Postgres y Redis)
* **Gradle**

## Instrucciones de Instalacion

1. Clonar el repositorio.
2. Configurar el archivo de propiedades:
   Copiar `src/main/resources/application.properties.example` a `src/main/resources/application.properties` y agregar las credenciales requeridas.
3. Levantar los contenedores de base de datos y cache mediante Docker:
   ```bash
   docker-compose up -d
   ```
4. Ejecutar el aplicativo mediante Gradle:
   ```bash
   .\gradlew.bat bootRun
   ```

## Tareas Programadas (Cron Jobs)

La aplicacion incluye un **Task Scheduler** configurado para ejecutarse diariamente a las 03:00 AM. Este proceso:
- Invoca la API de GestoPago.
- Mapea el XML de respuesta a entidades DTO y JPA.
- Inserta o actualiza el catalogo completo en la base de datos PostgreSQL.
- Refresca de forma integra la cache en Redis.

En caso de ser necesario sincronizar manualmente, se encuentra disponible el endpoint oculto: `GET /api/gestopago/sync`.

## Manejo de Excepciones Estandarizado

Se ha implementado un `GlobalExceptionHandler` que intercepta fallos internos, errores de validacion o caidas en la API de terceros y devuelve respuestas JSON normalizadas para facilitar el consumo desde clientes HTTP.

---

#  Nuevo Módulo: Onboarding de Clientes Personas Físicas

Se ha implementado con éxito el nuevo requerimiento de **Onboarding de Clientes**. Este módulo permite el registro integral de clientes físicos, validaciones estrictas de datos (como congruencia lógica de CURP/RFC), apertura automática de cuenta bancaria con saldo inicial, y creación de credenciales de acceso seguro.

###  Características Principales
*   **Gestión de Clientes (CRUD):** Registro con validaciones avanzadas (Regex para RFC, CURP cruzada con fecha de nacimiento/sexo, mayoría de edad).
*   **Creación Automática de Cuentas:** Al registrarse, se asigna automáticamente una cuenta única con estatus activo y un saldo predefinido ($500.00).
*   **Seguridad con JWT:** Cifrado de contraseñas con **BCrypt** y autenticación basada en tokens `Bearer`.
*   **Modo Solo Lectura (Baja Lógica):** Eliminación lógica de clientes. Si un usuario inactivo inicia sesión, se le asigna el rol `ROLE_READONLY`, bloqueándolo con HTTP 403 ante cualquier intento de alteración de datos.
*   **Alta Resiliencia (JMeter Proof):** Integración de `PageRequest` para paginación dura anti-OOM (Out Of Memory) en búsquedas, pool de conexiones HikariCP y límite de peticiones anti-DDoS con **Bucket4j** (20 req/s).

##  Entregables y Documentación (Requerimiento)

A continuación, se adjuntan los enlaces directos a las evidencias, manuales técnicos y diagramas del requerimiento, almacenados en la carpeta `/docs` de este repositorio:

|  Tipo de Documento |  Archivo |  Enlace Directo |
| :--- | :--- | :--- |
| **Manual Técnico** | Documentación de Arquitectura y Solución | [Ver PDF](./docs/Documentacion%20Teorica_Gonzalez%20Avalos.pdf) |
| **Casos de Prueba** | Evidencia de Pruebas de APIs (Endpoints) | [Ver Excel](./docs/Pruebas_Requerimiento_Gonzalez%20Avalos.xlsx) |
| **Diseño de Base de Datos** | Diagrama Entidad-Relación | [Ver Imagen](./docs/Diagrama_EntidadRelacion.png) |
| **Diseño de Base de Datos** | Diagrama Relacional | [Ver Imagen](./docs/Diagrama_Relacional.jpg) |

> *Para revisar la documentación heredada de la integración original de GestoPago, favor de consultar este [Documento Técnico (Google Drive)](https://drive.google.com/file/d/1qGDQGRoE0jk-HPLVawuHzzuZ4ZM80YiG/view?usp=sharing).*


---

#  Módulo: Integración de Pagos de Servicios (sendTx — GestoPago)

## Descripción del requerimiento

Integración segura con el endpoint `POST https://gestopago.portalventas.net/sistema/service/sendTx.do` de GestoPago/PuntoRed para procesar recargas telefónicas y pagos de servicios (agua, luz, gas, telefonía, gobierno, etc.) desde las cuentas de los clientes registrados en el sistema.

---

## Endpoints expuestos

| Método | Endpoint | Descripción | Autenticación |
|--------|----------|-------------|--------------|
| `POST` | `/api/gestopago/recargas` | Recarga telefónica o paquete de datos | JWT Bearer obligatorio |
| `POST` | `/api/gestopago/servicios` | Pago de recibo de servicio (agua, luz, etc.) | JWT Bearer obligatorio |
| `POST` | `/api/gestopago/sendTx` | Endpoint unificado (uso avanzado) | JWT Bearer obligatorio |
| `POST` | `/api/gestopago/otp/solicitar` | Solicitar código 2FA para autorizar transacción | JWT Bearer obligatorio |
| `GET`  | `/api/gestopago/products` | Catálogo de productos disponibles | Público |

---

## Flujo completo de una transacción

```
Cliente                  API (sendTx)                GestoPago
  │                          │                            │
  │── POST /auth/login ──►   │                            │
  │◄── JWT Token ────────── │                            │
  │                          │                            │
  │── POST /otp/solicitar ──►│                            │
  │◄── {codigoOtp: "XXXX"} ─│ (almacenado en Redis 5min) │
  │                          │                            │
  │── POST /recargas ───────►│                            │
  │  {numeroCuenta, idProducto, telefono, pin, otp}       │
  │                          │                            │
  │              [1] Bloqueo pesimista cuenta (PESSIMISTIC_WRITE)
  │              [2] Verifica cuenta ACTIVA               │
  │              [3] Verifica usuario ACTIVO              │
  │              [4] Verifica titularidad (JWT == titular)│
  │              [5] Valida PIN (BCrypt)                  │
  │              [6] Valida OTP 2FA (Redis) → consume OTP │
  │              [7] Verifica producto en catálogo        │
  │              [8] Valida idServicio == catálogo        │
  │              [9] Valida tipoFront endpoint vs producto│
  │              [10] Valida/toma monto del catálogo      │
  │              [11] Verifica saldo suficiente           │
  │              [12] Anti-duplicado (UPC / 15min/24h)    │
  │              [13] Cifra payload AES-256-CBC           │
  │                          │── POST sendTx.do ─────────►│
  │                          │◄── {codigo, numAutorizacion}│
  │                          │                            │
  │              [Si código=="01"] Descuenta saldo y persiste
  │              [Si timeout]      → PENDIENTE_CONCILIACION (NO descuenta)
  │              [Si error]        → Registra RECHAZADA (NO descuenta)
  │                          │                            │
  │◄── {exitoso, saldo, auth}│                            │
```

---

## Decisiones de diseño y justificación

### 1. ¿Cuándo se descuenta el saldo?

**Estrategia elegida: Post-Confirmación del proveedor**

El saldo se descuenta **únicamente** cuando GestoPago retorna código `"01"` (éxito confirmado). Antes de la llamada al proveedor, el saldo nunca se modifica.

> **Justificación:** GestoPago/PuntoRed no documenta un mecanismo de idempotencia propio para `sendTx`. Descontar el saldo antes de la llamada y luego recibir un error no recuperable podría generar un cargo sin servicio entregado. La estrategia Post-Confirmación es la más conservadora y protege al usuario.

### 2. Manejo de Timeout — No asumir fracaso

Si GestoPago no responde antes del tiempo límite, la transacción se registra como `PENDIENTE_CONCILIACION`. **No se descuenta el saldo** y **no se debe reintentar automáticamente** sin verificar primero si el proveedor procesó la solicitud original.

> **Justificación:** Un timeout no significa que la transacción no se procesó. Reintentar sin verificar puede generar un doble cargo. El UPC único y la bitácora con `horaLocal` permiten conciliar con el equipo de GestoPago/PuntoRed.

### 3. Control de concurrencia — Bloqueo pesimista

Se usa `@Lock(PESSIMISTIC_WRITE)` en `CuentaRepository.findByNumeroCuentaWithLock()` para evitar condiciones de carrera (double spending) ante solicitudes simultáneas sobre la misma cuenta.

### 4. Anti-duplicados por ventana temporal

- **Recargas:** Ventana de 15 minutos por UPC.
- **Servicios:** Ventana de 24 horas por UPC y/o combinación (idServicio + monto).
- GestoPago responde con código `"06"` si detecta duplicado en su lado; este se mapea a `sendTx_duplicated`.

### 5. Bearer Token de GestoPago — 1 token por día

La regla crítica de PuntoRed es **"One token must be taken per day"**. Solicitar múltiples tokens en el mismo día invalida todos y sendTx responde con código `55` en todas las transacciones. Por ello, el `GestoPagoTokenServiceImpl` verifica si el token tiene menos de 23 horas antes de renovarlo.

El token se almacena en base de datos (`gestopago_token`), nunca en código fuente ni en logs.

### 6. Validación cruzada de tipoFront

Los endpoints `/recargas` y `/servicios` inyectan el `tipoFront` correcto (1 y 2 respectivamente) al construir el `SendTxRequestDTO`. El servicio verifica que el producto en el catálogo corresponda al mismo `tipoFront`; si no coincide, la transacción se rechaza con `sendTx_invalidParameter` antes de llegar al proveedor.

---

## Configuración de variables de entorno

Para ejecutar la aplicación, definir las siguientes variables de entorno (ver `.env.example`):

| Variable | Descripción | Obligatoria en prod |
|----------|-------------|---------------------|
| `GESTOPAGO_URL` | URL base de GestoPago | SI |
| `GESTOPAGO_ID_DISTRIBUIDOR` | ID numérico del distribuidor | SI |
| `GESTOPAGO_CODIGO_DISPOSITIVO` | Código TPV asignado por PuntoRed | SI |
| `GESTOPAGO_PASSWORD` | Contraseña de autenticación GestoPago | SI |
| `GESTOPAGO_AES_KEY` | Clave AES-256 en Base64 | SI |
| `GESTOPAGO_AES_IV` | IV AES-256 en Base64 | SI |
| `GESTOPAGO_SIMULACION` | `true`=sin llamadas reales, `false`=producción | SI |
| `JWT_SECRET` | Secreto de firma de tokens JWT (mín. 32 chars) | SI |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USER`, `DB_PASS` | Conexión PostgreSQL | SI |
| `REDIS_HOST`, `REDIS_PORT` | Conexión Redis (OTP 2FA) | SI |

**En desarrollo local:**
```bash
# 1. Copiar la plantilla
cp .env.example .env
# 2. Editar .env con los valores reales (nunca subir al repositorio)
# 3. Exportar o usar un plugin de dotenv
```

---

## Cómo probar en Swagger

URL Swagger: `http://localhost:8081/swagger-ui/index.html`

**Paso 1 — Login:**
```
POST /api/v1/auth/login
{ "correoElectronico": "ana.mendoza.castro@gmail.com", "password": "Password123#" }
```
Copiar el token → clic en **Authorize ** → pegar `Bearer <token>`.

**Paso 2 — Solicitar OTP:**
```
POST /api/gestopago/otp/solicitar
{ "numeroCuenta": "8324279322" }
```
El OTP aparece en el campo `codigoOtpDemo` de la respuesta (solo en modo desarrollo).

**Paso 3 — Ejecutar recarga:**
```
POST /api/gestopago/recargas
{
  "numeroCuenta": "8324279322",
  "idProducto": 582,
  "idServicio": 133,
  "telefono": "5511223344",
  "monto": 10.00,
  "pinAutorizacion": "Password123#",
  "codigoOtp": "<OTP del paso 2>"
}
```

**Paso 3 alternativo — Pago de servicio:**
```
POST /api/gestopago/servicios
{
  "numeroCuenta": "8324279322",
  "idProducto": 183,
  "idServicio": 54,
  "referencia": "40425475190118187271",
  "monto": 150.00,
  "pinAutorizacion": "Password123#",
  "codigoOtp": "<OTP del paso 2>"
}
```

---

## Riesgos identificados y mitigaciones

| Riesgo | Mitigación implementada |
|--------|------------------------|
| Doble gasto ante peticiones simultáneas | `PESSIMISTIC_WRITE` + `@Transactional` |
| Cargo sin servicio entregado (error post-llamada) | Descuento solo tras código `"01"` |
| Timeout sin saber si se procesó | Estado `PENDIENTE_CONCILIACION` en bitácora, UPC para conciliar |
| Monto manipulado por el cliente | Precio oficial siempre del catálogo local; `sendTx_invalidAmount` si no coincide |
| Transacción duplicada | Ventana temporal por UPC (15min/24h) + detección código `"06"` |
| Uso de cuenta ajena | Verificación de titularidad JWT vs. titular de cuenta |
| Exposición de credenciales GestoPago | Token en BD; credenciales en variables de entorno; nunca en logs |
| Token GestoPago invalidado por múltiples solicitudes | Lógica de cooldown 23h en `GestoPagoTokenServiceImpl` |
| Fuerza bruta en PIN / OTP | Rate limiting Bucket4j (20 req/s global) + OTP de un solo uso |

---

## Estructura de clases del módulo sendTx

```
controller/
  GestoPagoController.java          → Endpoints HTTP (/recargas, /servicios, /sendTx, /otp)

service/
  GestoPagoSendTxService.java       → Interfaz del servicio transaccional
  Impl/
    GestoPagoSendTxServiceImpl.java → Logica de negocio completa (validaciones + envio)
  GestoPagoIntegrationService.java  → Interfaz del cliente externo
  Impl/
    GestoPagoIntegrationServiceImpl → Llama a GestoPago via Feign
  GestoPagoTokenService.java        → Gestion del Bearer Token de GestoPago
  Impl/
    GestoPagoTokenServiceImpl       → Renovacion automatica diaria del token
  TwoFactorOtpService.java          → Interfaz del servicio OTP 2FA
  Impl/
    TwoFactorOtpServiceImpl         → Genera/valida/consume OTP en Redis + fallback memoria

client/
  GestoPagoIntegrationClient.java   → Feign Client (sendTx.do, getProductList, sendEcho)
  GestoPagoAuthClient.java          → Feign Client (autenticacion GestoPago)

config/
  GestoPagoFeignConfig.java         → Interceptor Bearer Token + Decoder XML + Encoder Form

entity/gestopago/
  TransaccionSendTxEntity.java      → Bitacora de transacciones (evidencia)
  ProductoEntity.java               → Catalogo de productos
  GestoPagoToken.java               → Token Bearer almacenado en BD

repository/
  CuentaRepository.java             → findByNumeroCuentaWithLock (PESSIMISTIC_WRITE)
  TransaccionSendTxRepository.java  → Anti-duplicados (existsByUpc..., existsDuplicateServicePayment)
  ProductoRepository.java           → Catalogo local

util/
  AesEncryptionUtil.java            → Cifrado AES-256-CBC del payload sendTx

enums/
  SendTxErrorCode.java              → Codigos de error normalizados

exception/
  GlobalExceptionHandler.java       → @RestControllerAdvice centralizado
  GestoPagoIntegrationException.java→ Excepcion de dominio para errores del proveedor
```
