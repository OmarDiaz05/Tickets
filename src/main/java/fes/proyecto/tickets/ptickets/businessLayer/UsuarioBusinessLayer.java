package fes.proyecto.tickets.ptickets.businessLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.stereotype.Service;

import fes.proyecto.tickets.ptickets.dtos.IdDto;
import fes.proyecto.tickets.ptickets.dtos.UsuarioDto;
import fes.proyecto.tickets.ptickets.dtos.UsuarioDtoIn;
import fes.proyecto.tickets.ptickets.entities.Usuario;
import fes.proyecto.tickets.ptickets.helpers.Mappers;
import fes.proyecto.tickets.ptickets.repositories.IUsuarioRepository;

@Service
public class UsuarioBusinessLayer {

    private final IUsuarioRepository repository;

    public UsuarioBusinessLayer(IUsuarioRepository repository) {
        this.repository = repository;
    }

    public List<UsuarioDto> obtenerTodos() {
        List<UsuarioDto> usuarios = new ArrayList<>();
        List<Usuario> entities = repository.findByActivoTrue();
        for (Usuario entity : entities) {
            usuarios.add(Mappers.toDto(entity));
        }
        return usuarios;
    }

    public IdDto agregar(UsuarioDtoIn dtoIn) {
        String rol = dtoIn.getRol().trim().toUpperCase();
        if (!rol.equals("CLIENTE") && !rol.equals("ADMIN")) {
            return new IdDto(0, "Rol inválido. Debe ser CLIENTE o ADMIN");
        }

        if (rol.equals("ADMIN") && (dtoIn.getNumeroEmpleado() == null || dtoIn.getNumeroEmpleado().isBlank())) {
            return new IdDto(0, "El número de empleado es obligatorio para administradores");
        }

        Usuario entity = Mappers.toEntity(dtoIn);
        Usuario guardado = repository.save(entity);

        return new IdDto(guardado.getId(), "Usuario registrado con éxito");
    }

    public UsuarioDto obtenerPorId(int id) {
        Usuario entity = repository.findById(id).orElse(null);
        return Mappers.toDto(entity);
    }

    public UsuarioDto obtenerPorGuid(String guid) {
        Optional<Usuario> usuarioOpt = repository.findByGuid(guid);
        if (usuarioOpt.isPresent()) {
            return Mappers.toDto(usuarioOpt.get());
        }
        return null;
    }

    public UsuarioDto obtenerPorEmail(String email) {
        Optional<Usuario> usuarioOpt = repository.findByEmail(email.trim().toLowerCase());
        if (usuarioOpt.isPresent()) {
            return Mappers.toDto(usuarioOpt.get());
        }
        return null;
    }

    public IdDto actualizar(int id, UsuarioDtoIn dtoIn) {
        Optional<Usuario> usuarioOpt = repository.findById(id);
        if (usuarioOpt.isEmpty()) {
            return null;
        }
        Usuario entity = usuarioOpt.get();
        entity.setNombre(dtoIn.getNombre().trim());
        entity.setEmail(dtoIn.getEmail().trim().toLowerCase());
        entity.setPassword(dtoIn.getPassword());
        repository.save(entity);

        return new IdDto(entity.getId(), "Usuario actualizado correctamente");
    }

    public IdDto borrar(int id) {
        Optional<Usuario> usuarioOpt = repository.findById(id);
        if (usuarioOpt.isEmpty()) {
            return null;
        }
        Usuario entity = usuarioOpt.get();
        entity.setActivo(false);
        repository.save(entity);

        return new IdDto(entity.getId(), "Usuario deshabilitado correctamente");
    }

public IdDto activar(int id) {
    Optional<Usuario> usuarioOpt = repository.findById(id);
    if (usuarioOpt.isEmpty()) {
        return null;
    }
    Usuario entity = usuarioOpt.get();
    entity.setActivo(true);
    repository.save(entity);

    return new IdDto(entity.getId(), "Usuario reactivado correctamente");
}
}