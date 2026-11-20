package fes.proyecto.tickets.ptickets.exceptions;

public class NoAutenticadoException extends RuntimeException {
    public NoAutenticadoException(String mensaje) {
        super(mensaje);
    }
}
