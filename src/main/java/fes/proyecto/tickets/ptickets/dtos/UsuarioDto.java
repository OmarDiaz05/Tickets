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
public class UsuarioDto {
    private int id;
    private String nombre;
    private String email;
    private String telefono;
    private String rol;
    private String numeroEmpleado;
    private boolean verificado;
    private LocalDateTime fechaRegistro;
    private boolean activo;
}