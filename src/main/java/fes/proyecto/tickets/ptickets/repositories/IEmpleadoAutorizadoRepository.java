package fes.proyecto.tickets.ptickets.repositories;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import fes.proyecto.tickets.ptickets.entities.EmpleadoAutorizado;

public interface IEmpleadoAutorizadoRepository extends JpaRepository<EmpleadoAutorizado, String> {
    Optional<EmpleadoAutorizado> findByNumeroEmpleadoAndActivoTrue(String numeroEmpleado);
}