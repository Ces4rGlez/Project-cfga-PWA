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
