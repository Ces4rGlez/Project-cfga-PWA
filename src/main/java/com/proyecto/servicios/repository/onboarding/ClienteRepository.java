package com.proyecto.servicios.repository.onboarding;

import com.proyecto.servicios.entity.onboarding.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    boolean existsByCurp(String curp);
    boolean existsByRfc(String rfc);
    
    Optional<Cliente> findByCurp(String curp);
    Optional<Cliente> findByRfc(String rfc);

    @Query("SELECT c FROM Cliente c WHERE c.usuario.correoElectronico = :correo")
    Optional<Cliente> findByCorreo(@Param("correo") String correo);

    @Query("SELECT c FROM Cliente c WHERE c.usuario.activo = true")
    List<Cliente> findClientesActivos();

    @Query("SELECT c FROM Cliente c WHERE c.usuario.fechaRegistro BETWEEN :startDate AND :endDate")
    List<Cliente> findByFechaRegistroBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
