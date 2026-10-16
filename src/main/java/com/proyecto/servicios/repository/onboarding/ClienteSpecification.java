package com.proyecto.servicios.repository.onboarding;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Usuario;
import com.proyecto.servicios.model.request.onboarding.ClienteFiltroDTO;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Especificación dinámica de Spring Data JPA para combinar múltiples filtros de búsqueda en un solo endpoint.
 */
public class ClienteSpecification {

    public static Specification<Cliente> conFiltros(ClienteFiltroDTO filtro) {
        return (root, query, cb) -> {
            if (filtro == null) {
                return cb.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();

            // 1. Filtro por ID de cliente
            if (filtro.getIdCliente() != null) {
                predicates.add(cb.equal(root.get("idCliente"), filtro.getIdCliente()));
            }

            // 2. Filtro por CURP (búsqueda parcial o exacta case-insensitive)
            if (filtro.getCurp() != null && !filtro.getCurp().isBlank()) {
                predicates.add(cb.like(cb.upper(root.get("curp")), "%" + filtro.getCurp().trim().toUpperCase() + "%"));
            }

            // 3. Filtro por RFC (búsqueda parcial o exacta case-insensitive)
            if (filtro.getRfc() != null && !filtro.getRfc().isBlank()) {
                predicates.add(cb.like(cb.upper(root.get("rfc")), "%" + filtro.getRfc().trim().toUpperCase() + "%"));
            }

            // 4. Filtro por Nombre
            if (filtro.getNombre() != null && !filtro.getNombre().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("nombre")), "%" + filtro.getNombre().trim().toLowerCase() + "%"));
            }

            // 5. Filtro por Apellido Paterno
            if (filtro.getApellidoPaterno() != null && !filtro.getApellidoPaterno().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("apellidoPaterno")), "%" + filtro.getApellidoPaterno().trim().toLowerCase() + "%"));
            }

            // 6. Filtro por Correo Electrónico (join usuario)
            if (filtro.getCorreo() != null && !filtro.getCorreo().isBlank()) {
                Join<Cliente, Usuario> usuarioJoin = root.join("usuario", JoinType.INNER);
                predicates.add(cb.like(cb.lower(usuarioJoin.get("correoElectronico")), "%" + filtro.getCorreo().trim().toLowerCase() + "%"));
            }

            // 7. Filtro por Número de Cuenta (join cuentas)
            if (filtro.getNumeroCuenta() != null && !filtro.getNumeroCuenta().isBlank()) {
                Join<Cliente, Cuenta> cuentaJoin = root.join("cuentas", JoinType.LEFT);
                predicates.add(cb.equal(cuentaJoin.get("numeroCuenta"), filtro.getNumeroCuenta().trim()));
            }

            // 8. Filtro por Estatus Activo / Inactivo
            if (filtro.getActivos() != null) {
                Join<Cliente, Usuario> usuarioJoin = root.join("usuario", JoinType.INNER);
                predicates.add(cb.equal(usuarioJoin.get("activo"), filtro.getActivos()));
            }

            // 9. Filtro por Rango de Fechas de Registro
            if (filtro.getFechaDesde() != null && filtro.getFechaHasta() != null) {
                Join<Cliente, Usuario> usuarioJoin = root.join("usuario", JoinType.INNER);
                predicates.add(cb.between(usuarioJoin.get("fechaRegistro"), filtro.getFechaDesde(), filtro.getFechaHasta()));
            } else if (filtro.getFechaDesde() != null) {
                Join<Cliente, Usuario> usuarioJoin = root.join("usuario", JoinType.INNER);
                predicates.add(cb.greaterThanOrEqualTo(usuarioJoin.get("fechaRegistro"), filtro.getFechaDesde()));
            } else if (filtro.getFechaHasta() != null) {
                Join<Cliente, Usuario> usuarioJoin = root.join("usuario", JoinType.INNER);
                predicates.add(cb.lessThanOrEqualTo(usuarioJoin.get("fechaRegistro"), filtro.getFechaHasta()));
            }

            // 10. Filtro General Flexible (busca en nombre, segundo nombre, apellido paterno, apellido materno, CURP o RFC)
            if (filtro.getFiltro() != null && !filtro.getFiltro().isBlank()) {
                String pattern = "%" + filtro.getFiltro().trim().toLowerCase() + "%";
                Predicate orNombre = cb.like(cb.lower(root.get("nombre")), pattern);
                Predicate orSegundoNombre = cb.like(cb.lower(root.get("segundoNombre")), pattern);
                Predicate orApellidoP = cb.like(cb.lower(root.get("apellidoPaterno")), pattern);
                Predicate orApellidoM = cb.like(cb.lower(root.get("apellidoMaterno")), pattern);
                Predicate orCurp = cb.like(cb.lower(root.get("curp")), pattern);
                Predicate orRfc = cb.like(cb.lower(root.get("rfc")), pattern);
                predicates.add(cb.or(orNombre, orSegundoNombre, orApellidoP, orApellidoM, orCurp, orRfc));
            }

            if (query != null) {
                query.distinct(true);
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
