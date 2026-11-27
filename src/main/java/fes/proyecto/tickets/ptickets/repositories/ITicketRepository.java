package fes.proyecto.tickets.ptickets.repositories;

import java.time.Year;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import fes.proyecto.tickets.ptickets.entities.Ticket;
import fes.proyecto.tickets.ptickets.helpers.FolioGenerator;

public interface ITicketRepository extends JpaRepository<Ticket, Integer>, JpaSpecificationExecutor<Ticket> {

    Optional<Ticket> findByGuid(String guid);

    boolean existsByFolio(String folio);

    /**
     * Folios que empiezan con el prefijo dado, del consecutivo mayor al menor.
     * Se ordena primero por longitud porque el consecutivo puede crecer a 4 o
     * más caracteres (ZZZ < 1000).
     */
    @Query("SELECT t.folio FROM Ticket t WHERE t.folio LIKE :patron "
            + "ORDER BY LENGTH(t.folio) DESC, t.folio DESC")
    List<String> buscarFoliosDescendentes(@Param("patron") String patron, Pageable pageable);

    /** Consecutivo máximo usado en el año indicado; 0 si aún no hay tickets. */
    default int obtenerConsecutivoMaximo(int anio) {
        List<String> folios = buscarFoliosDescendentes(
                FolioGenerator.prefijoDelAnio(anio) + "%", PageRequest.of(0, 1));
        return folios.isEmpty() ? 0 : FolioGenerator.consecutivoDe(folios.get(0));
    }

    /** Consecutivo máximo usado en el año en curso; 0 si aún no hay tickets. */
    default int obtenerConsecutivoMaximoDelAnioActual() {
        return obtenerConsecutivoMaximo(Year.now().getValue());
    }

    // ---------- Consultas (Punto 5.3) ----------

    /**
     * Búsqueda paginada con Specification. El EntityGraph trae usuario,
     * administrador, categoría, prioridad y estado en la misma consulta
     * (LEFT JOIN), para no hacer una consulta extra por ticket (N+1).
     */
    @Override
    @EntityGraph(attributePaths = {"usuario", "administradorAsignado", "categoria", "prioridad", "estado"})
    Page<Ticket> findAll(Specification<Ticket> spec, Pageable pageable);

    /** Ticket con todas sus relaciones cargadas en una sola consulta. */
    @EntityGraph(attributePaths = {"usuario", "administradorAsignado", "categoria", "prioridad", "estado"})
    @Query("SELECT t FROM Ticket t WHERE t.id = :id")
    Optional<Ticket> buscarDetallePorId(@Param("id") int id);

    /** Tickets que cumplen el filtro, con el orden y la página indicados. */
    default Page<Ticket> buscar(TicketFiltro filtro, Pageable pageable) {
        return findAll(TicketSpecifications.conFiltro(filtro), pageable);
    }
}
