package fes.proyecto.tickets.ptickets.businessLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fes.proyecto.tickets.ptickets.dtos.IdDto;
import fes.proyecto.tickets.ptickets.dtos.UsuarioDto;
import fes.proyecto.tickets.ptickets.dtos.UsuarioDtoIn;
import fes.proyecto.tickets.ptickets.entities.NumeroEmpleado;
import fes.proyecto.tickets.ptickets.entities.Usuario;
import fes.proyecto.tickets.ptickets.exceptions.AccesoDenegadoException;
import fes.proyecto.tickets.ptickets.exceptions.RecursoDuplicadoException;
import fes.proyecto.tickets.ptickets.exceptions.RecursoNoEncontradoException;
import fes.proyecto.tickets.ptickets.exceptions.ReglaNegocioException;
import fes.proyecto.tickets.ptickets.exceptions.SolicitudInvalidaException;
import fes.proyecto.tickets.ptickets.helpers.Mappers;
import fes.proyecto.tickets.ptickets.repositories.INumeroEmpleadoRepository;
import fes.proyecto.tickets.ptickets.repositories.IUsuarioRepository;

@Service
public class UsuarioBusinessLayer {

    /**
     * Resultado de un registro. {@code registroPrevio} es true cuando ya existía
     * un usuario con el mismo guid (idempotencia): no se crea nada nuevo y se
     * devuelve el id existente.
     */
    public record ResultadoRegistro(IdDto idDto, boolean registroPrevio) {
    }

    private final IUsuarioRepository repository;
    private final INumeroEmpleadoRepository numeroEmpleadoRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioBusinessLayer(
            IUsuarioRepository repository,
            INumeroEmpleadoRepository numeroEmpleadoRepository,
            PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.numeroEmpleadoRepository = numeroEmpleadoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public List<UsuarioDto> obtenerTodos() {
        List<UsuarioDto> usuarios = new ArrayList<>();
        List<Usuario> entities = repository.findByActivoTrue();
        for (Usuario entity : entities) {
            usuarios.add(Mappers.toDto(entity));
        }
        return usuarios;
    }

    @Transactional
    public ResultadoRegistro agregar(UsuarioDtoIn dtoIn) {
        // Idempotencia por GUID: misma solicitud repetida devuelve el usuario existente
        Optional<Usuario> porGuid = repository.findByGuid(dtoIn.getGuid().trim());
        if (porGuid.isPresent()) {
            return new ResultadoRegistro(
                    new IdDto(porGuid.get().getId(), "Usuario registrado previamente (GUID duplicado)"),
                    true);
        }

        String email = dtoIn.getEmail().trim().toLowerCase();
        if (repository.existsByEmail(email)) {
            throw new RecursoDuplicadoException("El correo ya se encuentra registrado");
        }

        String rol = dtoIn.getRol().trim().toUpperCase();
        if (!rol.equals("CLIENTE") && !rol.equals("ADMIN")) {
            throw new SolicitudInvalidaException("Rol inválido. Debe ser CLIENTE o ADMIN");
        }

        NumeroEmpleado numEmpleadoAutorizado = null;

        if (rol.equals("ADMIN")) {
            if (dtoIn.getNumeroEmpleado() == null || dtoIn.getNumeroEmpleado().isBlank()) {
                throw new SolicitudInvalidaException("El número de empleado es obligatorio para administradores");
            }

            String clave = dtoIn.getNumeroEmpleado().trim();
            numEmpleadoAutorizado = numeroEmpleadoRepository.findByNumeroEmpleadoAndActivoTrue(clave)
                    .orElseThrow(() -> new AccesoDenegadoException("Número de empleado no autorizado o inactivo"));

            if (numEmpleadoAutorizado.isUsado()) {
                throw new ReglaNegocioException("Este número de empleado ya fue utilizado previamente");
            }
        }

        Usuario entity = Mappers.toEntity(dtoIn, codificarPassword(dtoIn.getPassword()));
        Usuario guardado = repository.save(entity);

        // Marcar el número de empleado como usado (un solo uso)
        if (numEmpleadoAutorizado != null) {
            numEmpleadoAutorizado.setUsado(true);
            numeroEmpleadoRepository.save(numEmpleadoAutorizado);
        }

        return new ResultadoRegistro(new IdDto(guardado.getId(), "Usuario registrado con éxito"), false);
    }

    public UsuarioDto obtenerPorId(int id) {
        return Mappers.toDto(buscarPorId(id));
    }

    public IdDto actualizar(int id, UsuarioDtoIn dtoIn) {
        // Restricción de seguridad: un CLIENTE solo puede modificar su propia información; un ADMIN puede modificar a cualquiera
        org.springframework.security.core.Authentication auth =
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof fes.proyecto.tickets.ptickets.security.UsuarioAutenticado usuarioLogueado) {
            boolean esAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
            if (!esAdmin && usuarioLogueado.getId() != id) {
                throw new AccesoDenegadoException("No tienes permiso para modificar la información de otro usuario");
            }
        }

        Usuario entity = buscarPorId(id);

        String email = dtoIn.getEmail().trim().toLowerCase();
        if (repository.existsByEmailAndIdNot(email, id)) {
            throw new RecursoDuplicadoException("El correo ya se encuentra registrado por otro usuario");
        }

        entity.setNombre(dtoIn.getNombre().trim());
        entity.setEmail(email);
        entity.setPassword(codificarPassword(dtoIn.getPassword()));
        repository.save(entity);

        return new IdDto(entity.getId(), "Usuario actualizado correctamente");
    }

    public IdDto borrar(int id) {
        Usuario entity = buscarPorId(id);
        entity.setActivo(false);
        repository.save(entity);

        return new IdDto(entity.getId(), "Usuario deshabilitado correctamente");
    }

    public IdDto activar(int id) {
        Usuario entity = buscarPorId(id);
        entity.setActivo(true);
        repository.save(entity);

        return new IdDto(entity.getId(), "Usuario reactivado correctamente");
    }

    /**
     * Devuelve el hash BCrypt de la contraseña. BCrypt solo admite hasta 72 bytes
     * y en ese caso el encoder lanza IllegalArgumentException; se responde 400.
     */
    private String codificarPassword(String password) {
        try {
            return passwordEncoder.encode(password);
        } catch (IllegalArgumentException ex) {
            throw new SolicitudInvalidaException("La contraseña no puede exceder 72 bytes");
        }
    }

    private Usuario buscarPorId(int id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con id " + id));
    }
}
