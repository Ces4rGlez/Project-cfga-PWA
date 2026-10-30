-- ==========================================
-- MIGRACIÓN V6: REGISTRO DE EVIDENCIA Y AUDITORÍA DE TRANSACCIONES SENDTX
-- Cumple con el requerimiento de PuntoRed / GestoPago de guardar peticiones y respuestas
-- y detección de transacciones duplicadas (UPC, idServicio, monto y ventana de tiempo).
-- ==========================================

CREATE TABLE IF NOT EXISTS transacciones_sendtx (
    id_transaccion BIGSERIAL PRIMARY KEY,
    numero_cuenta VARCHAR(20) NOT NULL,
    id_producto INT NOT NULL,
    id_servicio INT NOT NULL,
    tipo_front INT NOT NULL,
    id_cat_tipo_servicio INT,
    telefono VARCHAR(30),
    referencia VARCHAR(100),
    monto NUMERIC(15, 2) NOT NULL,
    upc VARCHAR(50) NOT NULL,
    hora_local VARCHAR(20) NOT NULL,
    unidad VARCHAR(25),
    random_str VARCHAR(50),
    payload_json TEXT,
    codigo_respuesta VARCHAR(30),
    mensaje_respuesta TEXT,
    num_autorizacion VARCHAR(60),
    id_tx_gestopago VARCHAR(60),
    pin VARCHAR(255),
    legend TEXT,
    estatus VARCHAR(30) NOT NULL,
    fecha_registro TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_tx_upc ON transacciones_sendtx(upc);
CREATE INDEX IF NOT EXISTS idx_tx_cuenta ON transacciones_sendtx(numero_cuenta);
CREATE INDEX IF NOT EXISTS idx_tx_servicio_monto ON transacciones_sendtx(id_servicio, monto, fecha_registro);
