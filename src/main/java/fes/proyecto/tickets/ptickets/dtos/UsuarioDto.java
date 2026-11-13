package fes.proyecto.tickets.ptickets.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UsuarioDto {
    private Long id;
    private String nombre;
    private String email;
    private LocalDateTime fechaRegistro;
    private Boolean activo;
}