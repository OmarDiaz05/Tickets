package fes.proyecto.tickets.ptickets.repositories;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import fes.proyecto.tickets.ptickets.entities.Ticket;
import jakarta.persistence.criteria.Predicate;

/**
 * Convierte un TicketFiltro en una consulta JPA Criteria. Cada criterio se
 * agrega como parámetro de la consulta (sin concatenar SQL).
 */
public final class TicketSpecifications {

    private TicketSpecifications() {
    }

    public static Specification<Ticket> conFiltro(TicketFiltro filtro) {
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();

            if (filtro.usuarioId() != null) {
                condiciones.add(cb.equal(root.get("usuario").get("id"), filtro.usuarioId()));
            }
            if (filtro.administradorId() != null) {
                condiciones.add(cb.equal(root.get("administradorAsignado").get("id"), filtro.administradorId()));
            }
            if (filtro.asignadoA() != null) {
                condiciones.add(cb.equal(root.get("administradorAsignado").get("id"), filtro.asignadoA()));
            }
            if (filtro.estadoId() != null) {
                condiciones.add(cb.equal(root.get("estado").get("id"), filtro.estadoId()));
            }
            if (filtro.prioridadId() != null) {
                condiciones.add(cb.equal(root.get("prioridad").get("id"), filtro.prioridadId()));
            }
            if (filtro.categoriaId() != null) {
                condiciones.add(cb.equal(root.get("categoria").get("id"), filtro.categoriaId()));
            }
            if (filtro.sinAsignar()) {
                condiciones.add(cb.isNull(root.get("administradorAsignado")));
            }
            if (filtro.registradoDesde() != null) {
                condiciones.add(cb.greaterThanOrEqualTo(
                        root.<LocalDateTime>get("fechaRegistro"), filtro.registradoDesde()));
            }
            if (filtro.registradoAntesDe() != null) {
                condiciones.add(cb.lessThan(
                        root.<LocalDateTime>get("fechaRegistro"), filtro.registradoAntesDe()));
            }

            return cb.and(condiciones.toArray(new Predicate[0]));
        };
    }
}
