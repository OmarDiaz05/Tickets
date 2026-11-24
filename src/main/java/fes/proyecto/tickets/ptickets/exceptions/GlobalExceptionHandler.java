package fes.proyecto.tickets.ptickets.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import fes.proyecto.tickets.ptickets.dtos.ErrorDto;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDto> manejarValidacion(MethodArgumentNotValidException ex) {
        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.toList());

        ErrorDto error = new ErrorDto(HttpStatus.BAD_REQUEST.value(), "Datos de solicitud inválidos", detalles);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDto> manejarSolicitudIlegible(HttpMessageNotReadableException ex) {
        ErrorDto error = new ErrorDto(HttpStatus.BAD_REQUEST.value(), "Cuerpo de solicitud mal formado o inválido");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorDto> manejarTipoArgumentoInvalido(MethodArgumentTypeMismatchException ex) {
        String tipo = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "válido";
        String mensaje = "El parámetro '" + ex.getName() + "' debe ser de tipo " + tipo;
        ErrorDto error = new ErrorDto(HttpStatus.BAD_REQUEST.value(), mensaje);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(SolicitudInvalidaException.class)
    public ResponseEntity<ErrorDto> manejarSolicitudInvalida(SolicitudInvalidaException ex) {
        ErrorDto error = new ErrorDto(HttpStatus.BAD_REQUEST.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorDto> manejarNoEncontrado(RecursoNoEncontradoException ex) {
        ErrorDto error = new ErrorDto(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorDto> manejarRutaNoEncontrada(NoResourceFoundException ex) {
        ErrorDto error = new ErrorDto(HttpStatus.NOT_FOUND.value(), "La ruta '/" + ex.getResourcePath() + "' no existe");
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorDto> manejarMetodoNoSoportado(HttpRequestMethodNotSupportedException ex) {
        ErrorDto error = new ErrorDto(HttpStatus.METHOD_NOT_ALLOWED.value(),
                "El método " + ex.getMethod() + " no está permitido para esta ruta");
        Set<HttpMethod> permitidos = ex.getSupportedHttpMethods();
        ResponseEntity.BodyBuilder respuesta = ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED);
        if (permitidos != null) {
            respuesta.allow(permitidos.toArray(new HttpMethod[0]));
        }
        return respuesta.body(error);
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorDto> manejarReglaNegocio(ReglaNegocioException ex) {
        ErrorDto error = new ErrorDto(HttpStatus.CONFLICT.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(NoAutenticadoException.class)
    public ResponseEntity<ErrorDto> manejarNoAutenticado(NoAutenticadoException ex) {
        ErrorDto error = new ErrorDto(HttpStatus.UNAUTHORIZED.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<ErrorDto> manejarAccesoDenegado(AccesoDenegadoException ex) {
        ErrorDto error = new ErrorDto(HttpStatus.FORBIDDEN.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> manejarErrorGeneral(Exception ex) {
        log.error("Error no controlado: {}", ex.getMessage(), ex);
        ErrorDto error = new ErrorDto(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Ocurrió un error interno en el servidor");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
