package com.proyecto.servicios.repositorys.gestopago;

import com.proyecto.servicios.entity.gestopago.TransaccionSendTxEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransaccionSendTxRepository extends JpaRepository<TransaccionSendTxEntity, Long> {

    Optional<TransaccionSendTxEntity> findTopByUpcOrderByFechaRegistroDesc(String upc);

    boolean existsByUpcAndFechaRegistroAfter(String upc, LocalDateTime threshold);

    @Query("SELECT COUNT(t) > 0 FROM TransaccionSendTxEntity t " +
            "WHERE t.idServicio = :idServicio " +
            "AND t.monto = :monto " +
            "AND t.fechaRegistro >= :threshold " +
            "AND t.estatus = 'EXITOSA'")
    boolean existsDuplicateServicePayment(
            @Param("idServicio") Integer idServicio,
            @Param("monto") BigDecimal monto,
            @Param("threshold") LocalDateTime threshold);

    List<TransaccionSendTxEntity> findByNumeroCuentaOrderByFechaRegistroDesc(String numeroCuenta);
}
