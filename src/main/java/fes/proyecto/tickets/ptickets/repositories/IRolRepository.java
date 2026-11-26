package fes.proyecto.tickets.ptickets.repositories;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import fes.proyecto.tickets.ptickets.entities.Rol;

public interface IRolRepository extends JpaRepository<Rol, Integer> {
    Optional<Rol> findByNombreIgnoreCase(String nombre);
}