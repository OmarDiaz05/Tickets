package fes.proyecto.tickets.ptickets.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fes.proyecto.tickets.ptickets.businessLayer.TicketBusinessLayer;
import fes.proyecto.tickets.ptickets.businessLayer.TicketBusinessLayer.ResultadoRegistro;
import fes.proyecto.tickets.ptickets.dtos.ErrorDto;
import fes.proyecto.tickets.ptickets.dtos.IdDto;
import fes.proyecto.tickets.ptickets.dtos.TicketDtoIn;
import fes.proyecto.tickets.ptickets.security.UsuarioAutenticado;
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
@Tag(name = "Tickets", description = "Reporte de fallas por parte de los clientes")
public class TicketController {

    private final TicketBusinessLayer ticketBusinessLayer;

    public TicketController(TicketBusinessLayer ticketBusinessLayer) {
        this.ticketBusinessLayer = ticketBusinessLayer;
    }

    @Operation(summary = "Crear ticket",
        description = "Solo para usuarios con rol CLIENTE y sesión iniciada. El cliente se toma de la sesión; "
            + "el folio, el estado inicial (Abierto), la fecha de registro y el administrador asignado "
            + "(uno activo elegido al azar, si existe) los pone el sistema. "
            + "Si el mismo cliente repite el guid, responde 208 con el ticket existente.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Ticket creado",
            content = @Content(schema = @Schema(implementation = IdDto.class))),
        @ApiResponse(responseCode = "208", description = "Ticket registrado previamente con el mismo guid",
            content = @Content(schema = @Schema(implementation = IdDto.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos, o categoría o prioridad inexistente",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(responseCode = "401", description = "Sin sesión iniciada",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(responseCode = "403", description = "El usuario no tiene rol CLIENTE, o falta el token CSRF",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(responseCode = "409", description = "El guid ya fue usado en un ticket de otro cliente",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    })
    @PostMapping("/tickets")
    public ResponseEntity<IdDto> agregar(
            @Valid @RequestBody TicketDtoIn dtoIn,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario) {
        ResultadoRegistro resultado = ticketBusinessLayer.agregar(dtoIn, usuario.getId());
        HttpStatus status = resultado.registroPrevio() ? HttpStatus.ALREADY_REPORTED : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(resultado.idDto());
    }
}
