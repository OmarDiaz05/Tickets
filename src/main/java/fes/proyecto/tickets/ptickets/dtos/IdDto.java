package fes.proyecto.tickets.ptickets.dtos;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class IdDto {
    private long id;
    private String mensaje;
}