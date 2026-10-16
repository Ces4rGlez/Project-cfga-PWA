package com.proyecto.servicios.repository.onboarding;

import com.proyecto.servicios.entity.onboarding.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {
    boolean existsByCurp(String curp);
    boolean existsByRfc(String rfc);
    
    Optional<Cliente> findByCurp(String curp);
    Optional<Cliente> findByRfc(String rfc);

    @Query("SELECT c FROM Cliente c WHERE c.usuario.correoElectronico = :correo")
    Optional<Cliente> findByCorreo(@Param("correo") String correo);

    @Query("SELECT c FROM Cliente c WHERE " +
           "(LOWER(TRIM(c.nombre)) = LOWER(TRIM(:nombre)) OR " +
           "LOWER(TRIM(CONCAT(c.nombre, ' ', c.apellidoPaterno))) = LOWER(TRIM(:nombre))) AND " +
           "LOWER(TRIM(c.usuario.correoElectronico)) = LOWER(TRIM(:correo))")
    Optional<Cliente> findByNombreAndCorreo(@Param("nombre") String nombre, @Param("correo") String correo);

    @Query("SELECT c FROM Cliente c WHERE c.usuario.activo = true")
    List<Cliente> findClientesActivos();

    @Query("SELECT c FROM Cliente c WHERE c.usuario.fechaRegistro BETWEEN :startDate AND :endDate")
    List<Cliente> findByFechaRegistroBetween(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    // --- Búsquedas con coincidencias parciales (ContainingIgnoreCase) y Paginación (Page/Pageable) ---
    Page<Cliente> findByNombreContainingIgnoreCase(String nombre, Pageable pageable);
    Page<Cliente> findByCurpContainingIgnoreCase(String curp, Pageable pageable);
    Page<Cliente> findByRfcContainingIgnoreCase(String rfc, Pageable pageable);

    @Query("SELECT c FROM Cliente c WHERE " +
           "(:filtro IS NULL OR :filtro = '' OR " +
           "LOWER(c.nombre) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(c.apellidoPaterno) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(c.curp) LIKE LOWER(CONCAT('%', :filtro, '%')) OR " +
           "LOWER(c.rfc) LIKE LOWER(CONCAT('%', :filtro, '%')))")
    Page<Cliente> buscarPorFiltro(@Param("filtro") String filtro, Pageable pageable);
}
