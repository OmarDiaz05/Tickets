package fes.proyecto.tickets.ptickets.exceptions;

public class RecursoDuplicadoException extends RuntimeException {
    public RecursoDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
