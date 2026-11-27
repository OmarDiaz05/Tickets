package fes.proyecto.tickets.ptickets.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import fes.proyecto.tickets.ptickets.entities.Usuario;

public interface IUsuarioRepository extends JpaRepository<Usuario, Integer> {
    Optional<Usuario> findByGuid(String guid);
    Optional<Usuario> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, int id);
    List<Usuario> findByActivoTrue();
    List<Usuario> findByRolAndActivoTrueAndVerificadoTrue(String rol);
    List<Usuario> findByRolAndActivoTrue(String rol);
}
