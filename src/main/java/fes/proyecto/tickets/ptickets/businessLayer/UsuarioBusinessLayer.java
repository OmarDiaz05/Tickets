package fes.proyecto.tickets.ptickets.businessLayer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fes.proyecto.tickets.ptickets.dtos.IdDto;
import fes.proyecto.tickets.ptickets.dtos.UsuarioDto;
import fes.proyecto.tickets.ptickets.dtos.UsuarioDtoIn;
import fes.proyecto.tickets.ptickets.entities.Usuario;
import fes.proyecto.tickets.ptickets.exceptions.RecursoDuplicadoException;
import fes.proyecto.tickets.ptickets.exceptions.RecursoNoEncontradoException;
import fes.proyecto.tickets.ptickets.repositories.IUsuarioRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsuarioBusinessLayer {

    @Autowired
    private IUsuarioRepository repository;

    public boolean existe(UsuarioDtoIn dtoIn) {
        return repository.existsByEmail(dtoIn.getEmail());
    }

    public IdDto agregar(UsuarioDtoIn dtoIn) {
        if (repository.existsByEmail(dtoIn.getEmail())) {
            throw new RecursoDuplicadoException("El correo ya se encuentra registrado");
        }
        Usuario entity = dtoIn.toEntity();
        repository.save(entity);
        return new IdDto(entity.getId(), "Usuario registrado con éxito");
    }

    public List<UsuarioDto> obtenerTodos() {
        return repository.findByActivoTrue().stream()
                .map(Usuario::toDto)
                .collect(Collectors.toList());
    }

    public UsuarioDto obtenerPorId(Long id) {
        return repository.findById(id)
                .map(Usuario::toDto)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con id " + id));
    }

    public IdDto actualizar(Long id, UsuarioDtoIn dtoIn) {
        Usuario entity = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con id " + id));

        if (repository.existsByEmailAndIdNot(dtoIn.getEmail(), id)) {
            throw new RecursoDuplicadoException("El correo ya se encuentra registrado por otro usuario");
        }

        entity.setNombre(dtoIn.getNombre());
        entity.setEmail(dtoIn.getEmail());
        entity.setPassword(dtoIn.getPassword());

        repository.save(entity);
        return new IdDto(entity.getId(), "Usuario actualizado correctamente");
    }

    public IdDto deshabilitar(Long id) {
        Usuario entity = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Usuario no encontrado con id " + id));
        entity.setActivo(false);
        repository.save(entity);
        return new IdDto(id, "Usuario deshabilitado correctamente");
    }
}
