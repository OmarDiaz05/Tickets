package fes.proyecto.tickets.ptickets.repositories;

import java.time.LocalDateTime;

/**
 * Criterios de búsqueda de tickets ya resueltos por la capa de negocio
 * (incluida la restricción por usuario de la sesión). Un valor null significa
 * "sin filtro"; todos los criterios se combinan con AND.
 *
 * @param usuarioId          cliente dueño del ticket (lo fuerza el businessLayer para CLIENTE)
 * @param administradorId    administrador asignado pedido en el filtro
 * @param asignadoA          administrador de la sesión cuando pide "soloMios"
 * @param sinAsignar         true: solo tickets sin administrador
 * @param registradoDesde    fechaRegistro mayor o igual
 * @param registradoAntesDe  fechaRegistro estrictamente menor (día siguiente a fechaHasta)
 */
public record TicketFiltro(
        Integer usuarioId,
        Integer administradorId,
        Integer asignadoA,
        Integer estadoId,
        Integer prioridadId,
        Integer categoriaId,
        boolean sinAsignar,
        LocalDateTime registradoDesde,
        LocalDateTime registradoAntesDe) {
}
