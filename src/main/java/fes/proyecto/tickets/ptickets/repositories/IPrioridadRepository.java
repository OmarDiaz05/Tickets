package fes.proyecto.tickets.ptickets.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import fes.proyecto.tickets.ptickets.entities.Prioridad;

public interface IPrioridadRepository extends JpaRepository<Prioridad, Integer> {
    Optional<Prioridad> findByNombreIgnoreCase(String nombre);
    List<Prioridad> findAllByOrderByNivelAsc();
}