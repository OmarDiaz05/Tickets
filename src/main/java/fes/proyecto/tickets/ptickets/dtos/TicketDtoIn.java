package fes.proyecto.tickets.ptickets.dtos;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos que captura el cliente al levantar un ticket.
 * No incluye nombre, correo, estado, folio ni administrador: el cliente se toma
 * de la sesión y el resto lo asigna el sistema.
 */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TicketDtoIn {

    @NotNull(message = "La categoría es obligatoria")
    private Integer categoriaId;

    @NotNull(message = "La prioridad es obligatoria")
    private Integer prioridadId;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 2000, message = "La descripción no debe exceder 2000 caracteres")
    private String descripcion;

    @NotNull(message = "La fecha desde cuándo ocurre la falla es obligatoria")
    @PastOrPresent(message = "La fecha desde cuándo ocurre la falla no puede ser futura")
    private LocalDateTime desdeCuando;

    @NotBlank(message = "El área afectada es obligatoria")
    @Size(max = 100, message = "El área afectada no debe exceder 100 caracteres")
    private String areaAfectada;

    @NotBlank(message = "El servicio impactado es obligatorio")
    @Size(max = 100, message = "El servicio impactado no debe exceder 100 caracteres")
    private String servicioImpactado;

    @Size(max = 100, message = "El medio de transmisión no debe exceder 100 caracteres")
    private String medioTransmision; // opcional

    @NotBlank(message = "El guid es obligatorio")
    @Size(max = 64, message = "El guid no debe exceder 64 caracteres")
    private String guid;
}
