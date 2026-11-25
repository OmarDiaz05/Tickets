package fes.proyecto.tickets.ptickets.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

import fes.proyecto.tickets.ptickets.dtos.ErrorDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 403 con ErrorDto cuando el usuario no tiene permiso o cuando falta/no
 * coincide el token CSRF en una petición POST, PUT, PATCH o DELETE.
 */
@Component
public class AccesoDenegadoHandler implements AccessDeniedHandler {

    private final RespuestaJson respuestaJson;

    public AccesoDenegadoHandler(RespuestaJson respuestaJson) {
        this.respuestaJson = respuestaJson;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        String mensaje = accessDeniedException instanceof CsrfException
                ? "Token CSRF ausente o inválido. Envía el valor de la cookie XSRF-TOKEN en la cabecera X-XSRF-TOKEN"
                : "No tienes permiso para acceder a este recurso";
        ErrorDto error = new ErrorDto(HttpStatus.FORBIDDEN.value(), mensaje);
        respuestaJson.escribir(response, HttpStatus.FORBIDDEN, error);
    }
}
