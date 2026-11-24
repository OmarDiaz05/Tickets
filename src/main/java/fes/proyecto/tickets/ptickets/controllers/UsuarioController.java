package fes.proyecto.tickets.ptickets.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fes.proyecto.tickets.ptickets.businessLayer.UsuarioBusinessLayer;
import fes.proyecto.tickets.ptickets.businessLayer.UsuarioBusinessLayer.ResultadoRegistro;
import fes.proyecto.tickets.ptickets.dtos.ErrorDto;
import fes.proyecto.tickets.ptickets.dtos.IdDto;
import fes.proyecto.tickets.ptickets.dtos.UsuarioDto;
import fes.proyecto.tickets.ptickets.dtos.UsuarioDtoIn;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Usuarios", description = "Operaciones CRUD de usuarios")
public class UsuarioController {

    private final UsuarioBusinessLayer usuarioBusinessLayer;

    public UsuarioController(UsuarioBusinessLayer businessLayer) {
        this.usuarioBusinessLayer = businessLayer;
    }

    @Operation(summary = "Listar usuarios", description = "Devuelve todos los usuarios activos.")
    @ApiResponse(responseCode = "200", description = "Lista de usuarios activos")
    @GetMapping("/usuarios")
    public ResponseEntity<List<UsuarioDto>> obtenerTodos() {
        return ResponseEntity.ok(usuarioBusinessLayer.obtenerTodos());
    }

    @Operation(summary = "Obtener usuario por id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    })
    @GetMapping("/usuarios/{id}")
    public ResponseEntity<UsuarioDto> obtenerPorId(
            @Parameter(description = "Id del usuario", example = "1") @PathVariable("id") int id) {
        return ResponseEntity.ok(usuarioBusinessLayer.obtenerPorId(id));
    }

    @Operation(summary = "Crear usuario",
        description = "Registra un nuevo usuario. Si el guid ya fue usado, responde 208 con el id existente.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Usuario creado exitosamente",
            content = @Content(schema = @Schema(implementation = IdDto.class))),
        @ApiResponse(responseCode = "208", description = "Usuario registrado previamente con el mismo guid",
            content = @Content(schema = @Schema(implementation = IdDto.class))),
        @ApiResponse(responseCode = "400", description = "Datos de entrada o rol inválidos",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(responseCode = "403", description = "Número de empleado no autorizado o inactivo",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(responseCode = "409", description = "Correo ya registrado o número de empleado ya utilizado",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    })
    @PostMapping("/usuarios")
    public ResponseEntity<IdDto> agregar(@Valid @RequestBody UsuarioDtoIn dtoIn) {
        ResultadoRegistro resultado = usuarioBusinessLayer.agregar(dtoIn);
        HttpStatus status = resultado.registroPrevio() ? HttpStatus.ALREADY_REPORTED : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(resultado.idDto());
    }

    @Operation(summary = "Actualizar usuario", description = "Actualiza nombre, email y contraseña del usuario.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario actualizado exitosamente",
            content = @Content(schema = @Schema(implementation = IdDto.class))),
        @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(responseCode = "409", description = "Correo registrado por otro usuario",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    })
    @PutMapping("/usuarios/{id}")
    public ResponseEntity<IdDto> actualizar(
            @Parameter(description = "Id del usuario", example = "1") @PathVariable("id") int id,
            @Valid @RequestBody UsuarioDtoIn dtoIn) {
        return ResponseEntity.ok(usuarioBusinessLayer.actualizar(id, dtoIn));
    }

    @Operation(summary = "Deshabilitar usuario", description = "Borrado lógico: marca el usuario como inactivo.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario deshabilitado exitosamente",
            content = @Content(schema = @Schema(implementation = IdDto.class))),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    })
    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<IdDto> borrar(
            @Parameter(description = "Id del usuario", example = "1") @PathVariable("id") int id) {
        return ResponseEntity.ok(usuarioBusinessLayer.borrar(id));
    }

    @Operation(summary = "Reactivar usuario", description = "Vuelve a marcar como activo un usuario deshabilitado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario reactivado exitosamente",
            content = @Content(schema = @Schema(implementation = IdDto.class))),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    })
    @PatchMapping("/usuarios/{id}/activar")
    public ResponseEntity<IdDto> activar(
            @Parameter(description = "Id del usuario", example = "1") @PathVariable("id") int id) {
        return ResponseEntity.ok(usuarioBusinessLayer.activar(id));
    }
}
