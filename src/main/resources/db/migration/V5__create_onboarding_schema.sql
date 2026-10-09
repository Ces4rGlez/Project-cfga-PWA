-- ==========================================
-- MIGRACIÓN V5: ESQUEMA DE ONBOARDING CLIENTES
-- Última Revisión Nivel Senior: Tipos de datos, Integridad Referencial y Rendimiento
-- ==========================================

-- ==========================================
-- 0. LIMPIEZA PREVIA (Eliminar tablas creadas manualmente)
-- ==========================================
DROP TABLE IF EXISTS cuentas CASCADE;
DROP TABLE IF EXISTS domicilios CASCADE;
DROP TABLE IF EXISTS clientes CASCADE;
DROP TABLE IF EXISTS usuarios CASCADE;
DROP TYPE IF EXISTS estatus_cuenta CASCADE;
DROP TYPE IF EXISTS sexo_biologico CASCADE;

-- ==========================================
-- 1. CREACIÓN DE TIPOS ENUMERADOS (Catálogos a nivel base de datos)
-- ==========================================
CREATE TYPE estatus_cuenta AS ENUM ('ACTIVA', 'INACTIVA', 'BLOQUEADA', 'CANCELADA');
CREATE TYPE sexo_biologico AS ENUM ('M', 'F', 'X');

-- 1. TABLA USUARIOS (Autenticación y Acceso)
CREATE TABLE usuarios (
    id_usuario BIGSERIAL PRIMARY KEY,
    
    -- Correos electrónicos según RFC 5321 pueden llegar hasta 254 caracteres. 
    -- 100 puede quedarse corto. Aumentado a 255 por seguridad.
    correo_electronico VARCHAR(255) NOT NULL UNIQUE 
        CHECK (correo_electronico ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'), 
    
    password VARCHAR(255) NOT NULL, 
    face_id_token TEXT,
    
    activo BOOLEAN DEFAULT TRUE NOT NULL,
    -- TIMESTAMPTZ es obligatorio en sistemas financieros para saber la zona horaria de registro
    fecha_registro TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- 2. TABLA CLIENTES (Perfil de Negocio / KYC)
CREATE TABLE clientes (
    id_cliente BIGSERIAL PRIMARY KEY,
    id_usuario BIGINT NOT NULL UNIQUE, 
    
    -- Nombres en latinoamérica pueden ser muy largos. Aumentado de 50 a 100.
    nombre VARCHAR(100) NOT NULL,
    segundo_nombre VARCHAR(100),
    apellido_paterno VARCHAR(100) NOT NULL,
    apellido_materno VARCHAR(100),
    fecha_nacimiento DATE NOT NULL,
    
    -- CURP: Fija en 18 caracteres (Usar CHAR ahorra bytes y acelera el motor)
    curp CHAR(18) NOT NULL UNIQUE CHECK (curp ~ '^[A-Z0-9]{18}$'),
    
    -- RFC: Persona Física en México es fija en 13 caracteres.
    rfc CHAR(13) NOT NULL UNIQUE CHECK (rfc ~ '^[A-Z0-9]{13}$'),
    
    sexo sexo_biologico NOT NULL,
    
    nacionalidad VARCHAR(3) NOT NULL DEFAULT 'MEX', 
    estado_civil VARCHAR(20) NOT NULL 
        CHECK (estado_civil IN ('SOLTERO', 'CASADO', 'DIVORCIADO', 'VIUDO', 'CONCUBINATO')),
    
    -- Teléfonos fijos a 10 dígitos (CHAR(10) es mejor que VARCHAR para longitud invariable)
    telefono_movil CHAR(10) NOT NULL CHECK (telefono_movil ~ '^[0-9]{10}$'),
    telefono_alternativo CHAR(10) CHECK (telefono_alternativo ~ '^[0-9]{10}$'),
    
    ocupacion VARCHAR(100) NOT NULL,
    empresa VARCHAR(100) NOT NULL,
    ingreso_mensual NUMERIC(12, 2) NOT NULL CHECK (ingreso_mensual > 0),
    
    CONSTRAINT fk_cliente_usuario 
        FOREIGN KEY (id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE
);

-- 3. TABLA DOMICILIOS
CREATE TABLE domicilios (
    id_domicilio BIGSERIAL PRIMARY KEY,
    id_cliente BIGINT NOT NULL UNIQUE, 
    
    calle VARCHAR(100) NOT NULL,
    numero_exterior VARCHAR(20) NOT NULL,
    numero_interior VARCHAR(20),
    colonia VARCHAR(100) NOT NULL,
    municipio VARCHAR(100) NOT NULL,
    estado VARCHAR(50) NOT NULL,
    
    -- Código postal fijo en 5 dígitos
    codigo_postal CHAR(5) NOT NULL CHECK (codigo_postal ~ '^[0-9]{5}$'), 
    pais VARCHAR(3) NOT NULL DEFAULT 'MEX',
    
    CONSTRAINT fk_domicilio_cliente 
        FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente) ON DELETE CASCADE
);

-- 4. TABLA CUENTAS
CREATE TABLE cuentas (
    id_cuenta BIGSERIAL PRIMARY KEY,
    id_cliente BIGINT NOT NULL, 
    
    -- Se deja VARCHAR(20) por si a futuro manejan CLABE (18) o números internacionales
    numero_cuenta VARCHAR(20) NOT NULL UNIQUE CHECK (numero_cuenta ~ '^[0-9]+$'),
    
    -- NUMERIC(15,2) soporta hasta billones (trillions en inglés). Excelente para bancos.
    saldo NUMERIC(15, 2) DEFAULT 0.00 NOT NULL CHECK (saldo >= 0),
    
    estatus estatus_cuenta DEFAULT 'ACTIVA' NOT NULL,
    fecha_creacion TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL,
    
    -- ON DELETE RESTRICT: Regla estricta bancaria. No borrar cliente si tiene cuenta.
    CONSTRAINT fk_cuenta_cliente 
        FOREIGN KEY (id_cliente) REFERENCES clientes(id_cliente) ON DELETE RESTRICT
);
