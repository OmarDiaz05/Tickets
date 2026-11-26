package fes.proyecto.tickets.ptickets.repositories;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import fes.proyecto.tickets.ptickets.entities.CategoriaFalla;

public interface ICategoriaFallaRepository extends JpaRepository<CategoriaFalla, Integer> {
    Optional<CategoriaFalla> findByNombreIgnoreCase(String nombre);
}