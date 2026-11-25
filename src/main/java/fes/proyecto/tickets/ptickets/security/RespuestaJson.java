package fes.proyecto.tickets.ptickets.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

/**
 * Escribe respuestas JSON desde los filtros de seguridad (que se ejecutan antes
 * de los controladores y del GlobalExceptionHandler). Usa el mismo JsonMapper
 * que Spring MVC para que el formato sea idéntico.
 */
@Component
public class RespuestaJson {

    private final JsonMapper jsonMapper;

    public RespuestaJson(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    public void escribir(HttpServletResponse response, HttpStatus status, Object cuerpo) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        jsonMapper.writeValue(response.getOutputStream(), cuerpo);
    }
}
