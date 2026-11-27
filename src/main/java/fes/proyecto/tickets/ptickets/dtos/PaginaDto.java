package fes.proyecto.tickets.ptickets.dtos;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Una página de resultados. {@code pagina} empieza en 0. */
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class PaginaDto<T> {
    private List<T> contenido;
    private int pagina;
    private int tamanio;
    private long totalElementos;
    private int totalPaginas;
}
