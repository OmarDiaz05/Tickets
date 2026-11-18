package fes.proyecto.tickets.ptickets.controllers;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Usuarios", description = "Operaciones CRUD de usuarios")
public class UsuarioController {

    @Autowired
    UsuarioBusinessLayer businessLayer;

    @Operation(summary = "Crear usuario", description = "Registra un nuevo usuario. Si el correo ya existe responde 208.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuario creado",
            content = @Content(schema = @Schema(implementation = IdDto.class))),
        @ApiResponse(responseCode = "208", description = "Correo registrado previamente",
            content = @Content(schema = @Schema(implementation = IdDto.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content)
    })
    @PostMapping("/usuarios")
    public ResponseEntity<?> agregar(@Valid @RequestBody UsuariooDtoIn dtoIn) {
        if(businessLayer.existe(dtoIn)) {
            return ResponseEntity.status(208).body(
                new IdDto(0, "correo registrado previamente")
            );
        }
        return ResponseEntity.status(201).body(businessLayer.agregar(dtoIn));
    }

    @Operation(summary = "Listar usuarios", description = "Devuelve todos los usuarios activos.")
    @ApiResponse(responseCode = "200", description = "Lista de usuarios activos")
    @GetMapping("/usuarios")
    public ResponseEntity<List<UsuarioDto>> obtenerTodos() {
        return ResponseEntity.ok(businessLayer.obtenerTodos());
    }

    @Operation(summary = "Obtener usuario por id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
            content = @Content(schema = @Schema(implementation = IdDto.class)))
    })
    @GetMapping("/usuarios/{id}")
    public ResponseEntity<UsuarioDto> obtenerPorId(
            @Parameter(description = "Id del usuario", example = "1") @PathVariable("id") Long id) {
        return ResponseEntity.ok(businessLayer.obtenerPorId(id));
    }

    @Operation(summary = "Actualizar usuario", description = "Actualiza nombre, email y password del usuario.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario actualizado"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
            content = @Content(schema = @Schema(implementation = IdDto.class)))
    })
    @PutMapping("/usuarios/{id}")
    public ResponseEntity<IdDto> actualizar(
            @Parameter(description = "Id del usuario", example = "1") @PathVariable("id") Long id,
            @Valid @RequestBody UsuariooDtoIn dtoIn) {

        return ResponseEntity.ok(businessLayer.actualizar(id, dtoIn));
    }

    @Operation(summary = "Deshabilitar usuario", description = "Borrado lógico: marca el usuario como inactivo.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario deshabilitado"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
            content = @Content(schema = @Schema(implementation = IdDto.class)))
    })
    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<IdDto> deshabilitar(
            @Parameter(description = "Id del usuario", example = "1") @PathVariable("id") Long id) {
        return ResponseEntity.ok(businessLayer.deshabilitar(id));
    }
}
