package fes.proyecto.tickets.ptickets.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import fes.proyecto.tickets.ptickets.dtos.ErrorDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 401 con ErrorDto cuando se pide una ruta protegida sin sesión iniciada.
 */
@Component
public class NoAutenticadoEntryPoint implements AuthenticationEntryPoint {

    private final RespuestaJson respuestaJson;

    public NoAutenticadoEntryPoint(RespuestaJson respuestaJson) {
        this.respuestaJson = respuestaJson;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        ErrorDto error = new ErrorDto(HttpStatus.UNAUTHORIZED.value(),
                "Se requiere iniciar sesión para acceder a este recurso");
        respuestaJson.escribir(response, HttpStatus.UNAUTHORIZED, error);
    }
}
