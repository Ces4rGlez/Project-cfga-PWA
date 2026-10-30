package com.proyecto.servicios.entity.gestopago;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad de persistencia para el registro de auditoría / evidencia de transacciones sendTx.
 * Cumple con el requisito mandatario de PuntoRed de guardar peticiones y respuestas para revisión y aclaraciones,
 * y permite validar transacciones duplicadas (por UPC o por servicio/monto dentro de la ventana de tiempo).
 */
@Entity
@Table(name = "transacciones_sendtx", indexes = {
        @Index(name = "idx_tx_upc", columnList = "upc"),
        @Index(name = "idx_tx_cuenta", columnList = "numero_cuenta"),
        @Index(name = "idx_tx_servicio_monto", columnList = "id_servicio, monto, fecha_registro")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransaccionSendTxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transaccion")
    private Long idTransaccion;

    @Column(name = "numero_cuenta", nullable = false, length = 20)
    private String numeroCuenta;

    @Column(name = "id_producto", nullable = false)
    private Integer idProducto;

    @Column(name = "id_servicio", nullable = false)
    private Integer idServicio;

    @Column(name = "tipo_front", nullable = false)
    private Integer tipoFront;

    @Column(name = "id_cat_tipo_servicio")
    private Integer idCatTipoServicio;

    @Column(name = "telefono", length = 30)
    private String telefono;

    @Column(name = "referencia", length = 100)
    private String referencia;

    @Column(name = "monto", precision = 15, scale = 2, nullable = false)
    private BigDecimal monto;

    @Column(name = "upc", nullable = false, length = 50)
    private String upc;

    @Column(name = "hora_local", nullable = false, length = 20)
    private String horaLocal;

    @Column(name = "unidad", length = 25)
    private String unidad;

    @Column(name = "random_str", length = 50)
    private String randomStr;

    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "codigo_respuesta", length = 30)
    private String codigoRespuesta;

    @Column(name = "mensaje_respuesta", columnDefinition = "TEXT")
    private String mensajeRespuesta;

    @Column(name = "num_autorizacion", length = 60)
    private String numAutorizacion;

    @Column(name = "id_tx_gestopago", length = 60)
    private String idTxGestoPago;

    @Column(name = "pin", length = 255)
    private String pin;

    @Column(name = "legend", columnDefinition = "TEXT")
    private String legend;

    @Column(name = "estatus", nullable = false, length = 30)
    private String estatus; // EXITOSA, RECHAZADA, FALLIDA

    @Column(name = "fecha_registro", updatable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    protected void onCreate() {
        if (this.fechaRegistro == null) {
            this.fechaRegistro = LocalDateTime.now();
        }
    }
}
