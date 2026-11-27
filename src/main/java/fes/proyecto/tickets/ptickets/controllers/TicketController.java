package fes.proyecto.tickets.ptickets.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fes.proyecto.tickets.ptickets.businessLayer.TicketBusinessLayer;
import fes.proyecto.tickets.ptickets.businessLayer.TicketBusinessLayer.ResultadoRegistro;
import fes.proyecto.tickets.ptickets.dtos.ErrorDto;
import fes.proyecto.tickets.ptickets.dtos.IdDto;
import fes.proyecto.tickets.ptickets.dtos.PaginaDto;
import fes.proyecto.tickets.ptickets.dtos.TicketDto;
import fes.proyecto.tickets.ptickets.dtos.TicketDtoIn;
import fes.proyecto.tickets.ptickets.dtos.TicketFiltroDto;
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

    @Operation(summary = "Listar tickets",
        description = "Para CLIENTE y ADMIN con sesión iniciada. Un CLIENTE solo ve sus propios tickets "
            + "(se ignoran administradorId y soloMios). Un ADMIN ve todos y puede combinar los filtros. "
            + "Orden: fecha de registro descendente. Un id de filtro inexistente devuelve una lista vacía. "
            + "size máximo 50: un valor mayor se limita a 50.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Página de tickets"),
        @ApiResponse(responseCode = "400",
            description = "fechaDesde posterior a fechaHasta, page negativo, size menor a 1 o parámetro con formato inválido",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(responseCode = "401", description = "Sin sesión iniciada",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    })
    @GetMapping("/tickets")
    public ResponseEntity<PaginaDto<TicketDto>> obtenerTodos(
            @ParameterObject TicketFiltroDto filtro,
            @Parameter(description = "Número de página, empieza en 0", example = "0")
            @RequestParam(name = "page", defaultValue = "0") int page,
            @Parameter(description = "Tickets por página (máximo 50)", example = "10")
            @RequestParam(name = "size", defaultValue = "10") int size,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(ticketBusinessLayer.obtenerTodos(filtro, page, size, usuario));
    }

    @Operation(summary = "Obtener ticket por id",
        description = "Un CLIENTE solo puede ver sus propios tickets; un ADMIN puede ver cualquiera.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ticket encontrado"),
        @ApiResponse(responseCode = "400", description = "Id no numérico",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(responseCode = "401", description = "Sin sesión iniciada",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(responseCode = "403", description = "El ticket pertenece a otro cliente",
            content = @Content(schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(responseCode = "404", description = "Ticket no encontrado",
            content = @Content(schema = @Schema(implementation = ErrorDto.class)))
    })
    @GetMapping("/tickets/{id}")
    public ResponseEntity<TicketDto> obtenerPorId(
            @Parameter(description = "Id del ticket", example = "1") @PathVariable("id") int id,
            @Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return ResponseEntity.ok(ticketBusinessLayer.obtenerPorId(id, usuario));
    }
}
