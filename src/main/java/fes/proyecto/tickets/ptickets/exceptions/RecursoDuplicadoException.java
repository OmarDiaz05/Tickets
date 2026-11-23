package fes.proyecto.tickets.ptickets.exceptions;

public class RecursoDuplicadoException extends ReglaNegocioException {
    public RecursoDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
