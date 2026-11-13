package fes.proyecto.tickets.ptickets.businessLayer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import fes.proyecto.tickets.ptickets.dtos.IdDto;
import fes.proyecto.tickets.ptickets.dtos.UsuarioDto;
import fes.proyecto.tickets.ptickets.dtos.UsuariooDtoIn;
import fes.proyecto.tickets.ptickets.entities.Usuario;
import fes.proyecto.tickets.ptickets.repositories.IUsuarioRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UsuarioBusinessLayer {

    @Autowired
    IUsuarioRepository repository;

    public boolean existe(UsuariooDtoIn dtoIn) {
        return repository.findByEmail(dtoIn.getEmail()).isPresent();
    }

    public IdDto agregar(UsuariooDtoIn dtoIn) {
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
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }
    public IdDto actualizar(Long id, UsuariooDtoIn dtoIn) {
        Usuario entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        entity.setNombre(dtoIn.getNombre());
        entity.setEmail(dtoIn.getEmail());
        entity.setPassword(dtoIn.getPassword()); 

        repository.save(entity);
        return new IdDto(entity.getId(), "Usuario actualizado correctamente");
    }

    public IdDto deshabilitar(Long id) {
        Usuario entity = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        entity.setActivo(false);
        repository.save(entity);
        return new IdDto(id, "Usuario deshabilitado correctamente");
    }
}
