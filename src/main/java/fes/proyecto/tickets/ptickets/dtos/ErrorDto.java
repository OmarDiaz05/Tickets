package fes.proyecto.tickets.ptickets.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorDto {
    private int status;
    private String mensaje;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime fechaHora;

    private List<String> detalles;

    public ErrorDto(int status, String mensaje) {
        this.status = status;
        this.mensaje = mensaje;
        this.fechaHora = LocalDateTime.now();
    }

    public ErrorDto(int status, String mensaje, List<String> detalles) {
        this.status = status;
        this.mensaje = mensaje;
        this.fechaHora = LocalDateTime.now();
        this.detalles = detalles;
    }
}
