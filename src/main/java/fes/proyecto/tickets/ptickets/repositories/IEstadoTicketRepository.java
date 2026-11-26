package fes.proyecto.tickets.ptickets.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import fes.proyecto.tickets.ptickets.entities.EstadoTicket;

public interface IEstadoTicketRepository extends JpaRepository<EstadoTicket, Integer> {
    Optional<EstadoTicket> findByNombreIgnoreCase(String nombre);
    List<EstadoTicket> findAllByOrderByOrdenAsc();
}