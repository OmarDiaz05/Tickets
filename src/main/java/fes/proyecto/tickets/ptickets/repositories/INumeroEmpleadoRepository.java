package fes.proyecto.tickets.ptickets.repositories;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import fes.proyecto.tickets.ptickets.entities.NumeroEmpleado;

public interface INumeroEmpleadoRepository extends JpaRepository<NumeroEmpleado, Integer> {
    Optional<NumeroEmpleado> findByNumeroEmpleadoAndActivoTrue(String numeroEmpleado);
}