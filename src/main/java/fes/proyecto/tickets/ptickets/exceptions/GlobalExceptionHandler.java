package fes.proyecto.tickets.ptickets.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import fes.proyecto.tickets.ptickets.dtos.ErrorDto;

import java.util.List;
import java.util.stream.Collectors;

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

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorDto> manejarNoEncontrado(RecursoNoEncontradoException ex) {
        ErrorDto error = new ErrorDto(HttpStatus.NOT_FOUND.value(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<ErrorDto> manejarDuplicado(RecursoDuplicadoException ex) {
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
        ErrorDto error = new ErrorDto(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Ocurrió un error interno en el servidor");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
