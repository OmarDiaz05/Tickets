package fes.proyecto.tickets.ptickets.dtos;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Filtros opcionales (query params) de GET /api/v1/tickets.
 * No incluye el usuario: la identidad sale siempre de la sesión.
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TicketFiltroDto {

    @Schema(description = "Id del estado del ticket", example = "1")
    private Integer estadoId;

    @Schema(description = "Id de la prioridad", example = "2")
    private Integer prioridadId;

    @Schema(description = "Id de la categoría de falla", example = "1")
    private Integer categoriaId;

    @Schema(description = "Solo ADMIN: tickets asignados a este administrador. Se ignora para CLIENTE.")
    private Integer administradorId;

    @Schema(description = "true: solo tickets sin administrador asignado")
    private Boolean sinAsignar;

    @Schema(description = "Solo ADMIN: true para ver solo los tickets asignados a él. Se ignora para CLIENTE.")
    private Boolean soloMios;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "Fecha de registro mínima, inclusiva (yyyy-MM-dd)", example = "2026-11-01")
    private LocalDate fechaDesde;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Schema(description = "Fecha de registro máxima, inclusiva (yyyy-MM-dd)", example = "2026-11-30")
    private LocalDate fechaHasta;
}
