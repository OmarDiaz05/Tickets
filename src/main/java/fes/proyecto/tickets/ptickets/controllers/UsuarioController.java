package fes.proyecto.tickets.ptickets.controllers;

import java.util.List;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import fes.proyecto.tickets.ptickets.businessLayer.UsuarioBusinessLayer;
import fes.proyecto.tickets.ptickets.dtos.IdDto;
import fes.proyecto.tickets.ptickets.dtos.UsuarioDto;
import fes.proyecto.tickets.ptickets.dtos.UsuarioDtoIn;
import jakarta.validation.Valid;

@RestController
public class UsuarioController {

    private final UsuarioBusinessLayer usuarioBusinessLayer;

    public UsuarioController(UsuarioBusinessLayer businessLayer) {
        this.usuarioBusinessLayer = businessLayer;
    }

    @GetMapping("/api/v1/usuarios")
    public ResponseEntity<List<UsuarioDto>> obtenerTodos() {
        return ResponseEntity.ok(usuarioBusinessLayer.obtenerTodos()); // 200
    }

    @GetMapping("/api/v1/usuarios/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable("id") int id) {
        UsuarioDto dto = usuarioBusinessLayer.obtenerPorId(id);
        if (dto == null) {
            return ResponseEntity.status(404).body(new IdDto(0, "Usuario no encontrado"));
        }
        return ResponseEntity.ok(dto); // 200
    }

    @PostMapping("/api/v1/usuarios")
    public ResponseEntity<IdDto> guardarDto(@Valid @RequestBody UsuarioDtoIn dtoIn) {
        //Idempotencia por GUID
        UsuarioDto porGuid = usuarioBusinessLayer.obtenerPorGuid(dtoIn.getGuid());
        if (porGuid != null) {
            return ResponseEntity.status(208)
                    .body(new IdDto(porGuid.getId(), "Usuario registrado previamente (GUID duplicado)"));
        }

        // Correo duplicado
        UsuarioDto porEmail = usuarioBusinessLayer.obtenerPorEmail(dtoIn.getEmail());
        if (porEmail != null) {
            return ResponseEntity.status(208)
                    .body(new IdDto(porEmail.getId(), "Correo registrado previamente"));
        }

        IdDto idDto = usuarioBusinessLayer.agregar(dtoIn);
        if (idDto.getId() == 0) {
            return ResponseEntity.badRequest().body(idDto); 
        }

        return ResponseEntity.status(201).body(idDto); 
    }

    @PutMapping("/api/v1/usuarios/{id}")
    public ResponseEntity<IdDto> actualizar(
            @PathVariable("id") int id,
            @Valid @RequestBody UsuarioDtoIn dtoIn) {
        IdDto idDto = usuarioBusinessLayer.actualizar(id, dtoIn);
        if (idDto == null) {
            return ResponseEntity.status(404).body(new IdDto(0, "Usuario no encontrado"));
        }
        return ResponseEntity.ok(idDto); 
    }

    @DeleteMapping("/api/v1/usuarios/{id}")
    public ResponseEntity<IdDto> borrar(@PathVariable("id") int id) {
        IdDto idDto = usuarioBusinessLayer.borrar(id);
        if (idDto == null) {
            return ResponseEntity.status(404).body(new IdDto(0, "Usuario no encontrado"));
        }
        return ResponseEntity.ok(idDto); 
    }

@PatchMapping("/api/v1/usuarios/{id}/activar")
public ResponseEntity<IdDto> activar(@PathVariable("id") int id) {
    IdDto idDto = usuarioBusinessLayer.activar(id);
    if (idDto == null) {
        return ResponseEntity.status(404).body(new IdDto(0, "Usuario no encontrado"));
    }
    return ResponseEntity.ok(idDto); 
}
}