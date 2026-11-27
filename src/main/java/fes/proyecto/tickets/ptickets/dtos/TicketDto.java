package fes.proyecto.tickets.ptickets.dtos;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TicketDto {
    private int id;
    private String folio;

    private String nombreCliente;
    private String correoCliente;
    private String administradorAsignado; // nombre del administrador; null si no hay

    private String categoria;
    private String prioridad;
    private String estado;

    private String descripcion;
    private LocalDateTime desdeCuando;
    private String areaAfectada;
    private String servicioImpactado;
    private String medioTransmision;
    private LocalDateTime fechaRegistro;

    // Fases posteriores (null mientras el ticket no se cierre)
    private LocalDateTime fechaCierre;
    private String causaRaiz;
    private String accionesCorrectivas;
    private String pruebasPosteriores;
    private String leccionesAprendidas;
}
