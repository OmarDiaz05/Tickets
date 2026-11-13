package fes.proyecto.tickets.ptickets.controllers;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import fes.proyecto.tickets.ptickets.businessLayer.UsuarioBusinessLayer;
import fes.proyecto.tickets.ptickets.dtos.IdDto;
import fes.proyecto.tickets.ptickets.dtos.UsuarioDto;
import fes.proyecto.tickets.ptickets.dtos.UsuariooDtoIn;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class UsuarioController {

    @Autowired
    UsuarioBusinessLayer businessLayer;

    @PostMapping("/usuarios")
    public ResponseEntity<?> agregar(@Valid @RequestBody UsuariooDtoIn dtoIn) {
        if(businessLayer.existe(dtoIn)) {
            return ResponseEntity.status(208).body(
                new IdDto(0, "correo registrado previamente")
            );
        }
        return ResponseEntity.status(201).body(businessLayer.agregar(dtoIn));
    }

    @GetMapping("/usuarios")
    public ResponseEntity<List<UsuarioDto>> obtenerTodos() {
        return ResponseEntity.ok(businessLayer.obtenerTodos());
    }

    @GetMapping("/usuarios/{id}")
    public ResponseEntity<UsuarioDto> obtenerPorId(@PathVariable("id") Long id) {
        return ResponseEntity.ok(businessLayer.obtenerPorId(id));
    }
    @PutMapping("/usuarios/{id}")
    public ResponseEntity<IdDto> actualizar(
            @PathVariable("id") Long id, 
            @Valid @RequestBody UsuariooDtoIn dtoIn) {
        
        return ResponseEntity.ok(businessLayer.actualizar(id, dtoIn));
    }

    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<IdDto> deshabilitar(@PathVariable("id") Long id) {
        return ResponseEntity.ok(businessLayer.deshabilitar(id));
    }
}