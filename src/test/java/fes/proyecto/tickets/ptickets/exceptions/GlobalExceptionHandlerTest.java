package fes.proyecto.tickets.ptickets.exceptions;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import fes.proyecto.tickets.ptickets.dtos.ErrorDto;
import fes.proyecto.tickets.ptickets.dtos.UsuarioDtoIn;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void manejarValidacion_RetornaHttp400ConDetalles() {
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new UsuarioDtoIn(), "usuarioDtoIn");
        bindingResult.addError(new FieldError("usuarioDtoIn", "email", "El correo debe tener un formato válido"));
        MethodParameter parameter = mock(MethodParameter.class);
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

        ResponseEntity<ErrorDto> response = handler.manejarValidacion(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Datos de solicitud inválidos", response.getBody().getMensaje());
        assertNotNull(response.getBody().getFechaHora());
        assertNotNull(response.getBody().getDetalles());
        assertTrue(response.getBody().getDetalles().contains("email: El correo debe tener un formato válido"));
    }

    @Test
    void manejarSolicitudIlegible_RetornaHttp400() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);

        ResponseEntity<ErrorDto> response = handler.manejarSolicitudIlegible(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("Cuerpo de solicitud mal formado o inválido", response.getBody().getMensaje());
        assertNotNull(response.getBody().getFechaHora());
    }

    @Test
    void manejarNoAutenticado_RetornaHttp401() {
        NoAutenticadoException ex = new NoAutenticadoException("Falta autenticación en la solicitud");

        ResponseEntity<ErrorDto> response = handler.manejarNoAutenticado(ex);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(401, response.getBody().getStatus());
        assertEquals("Falta autenticación en la solicitud", response.getBody().getMensaje());
        assertNotNull(response.getBody().getFechaHora());
    }

    @Test
    void manejarAccesoDenegado_RetornaHttp403() {
        AccesoDenegadoException ex = new AccesoDenegadoException("Acceso prohibido para este recurso");

        ResponseEntity<ErrorDto> response = handler.manejarAccesoDenegado(ex);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(403, response.getBody().getStatus());
        assertEquals("Acceso prohibido para este recurso", response.getBody().getMensaje());
        assertNotNull(response.getBody().getFechaHora());
    }

    @Test
    void manejarNoEncontrado_RetornaHttp404() {
        RecursoNoEncontradoException ex = new RecursoNoEncontradoException("Usuario no encontrado con id 1");

        ResponseEntity<ErrorDto> response = handler.manejarNoEncontrado(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("Usuario no encontrado con id 1", response.getBody().getMensaje());
        assertNotNull(response.getBody().getFechaHora());
    }

    @Test
    void manejarTipoArgumentoInvalido_RetornaHttp400() {
        MethodArgumentTypeMismatchException ex = new MethodArgumentTypeMismatchException(
                "abc", Long.class, "id", mock(MethodParameter.class), new NumberFormatException("abc"));

        ResponseEntity<ErrorDto> response = handler.manejarTipoArgumentoInvalido(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getStatus());
        assertEquals("El parámetro 'id' debe ser de tipo Long", response.getBody().getMensaje());
        assertNotNull(response.getBody().getFechaHora());
    }

    @Test
    void manejarRutaNoEncontrada_RetornaHttp404() {
        NoResourceFoundException ex = new NoResourceFoundException(
                HttpMethod.GET, "/api/v1/inexistente", "api/v1/inexistente");

        ResponseEntity<ErrorDto> response = handler.manejarRutaNoEncontrada(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(404, response.getBody().getStatus());
        assertEquals("La ruta '/api/v1/inexistente' no existe", response.getBody().getMensaje());
        assertNotNull(response.getBody().getFechaHora());
    }

    @Test
    void manejarMetodoNoSoportado_RetornaHttp405ConCabeceraAllow() {
        HttpRequestMethodNotSupportedException ex =
                new HttpRequestMethodNotSupportedException("PATCH", List.of("GET", "PUT"));

        ResponseEntity<ErrorDto> response = handler.manejarMetodoNoSoportado(ex);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(405, response.getBody().getStatus());
        assertEquals("El método PATCH no está permitido para esta ruta", response.getBody().getMensaje());
        assertNotNull(response.getBody().getFechaHora());
        assertEquals(Set.of(HttpMethod.GET, HttpMethod.PUT), response.getHeaders().getAllow());
    }

    @Test
    void manejarReglaNegocio_RetornaHttp409() {
        ReglaNegocioException ex = new ReglaNegocioException("No se puede deshabilitar un usuario con tickets abiertos");

        ResponseEntity<ErrorDto> response = handler.manejarReglaNegocio(ex);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("No se puede deshabilitar un usuario con tickets abiertos", response.getBody().getMensaje());
        assertNotNull(response.getBody().getFechaHora());
    }

    @Test
    void manejarReglaNegocio_ConRecursoDuplicado_RetornaHttp409() {
        RecursoDuplicadoException ex = new RecursoDuplicadoException("El correo ya se encuentra registrado");

        ResponseEntity<ErrorDto> response = handler.manejarReglaNegocio(ex);

        assertInstanceOf(ReglaNegocioException.class, ex);
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().getStatus());
        assertEquals("El correo ya se encuentra registrado", response.getBody().getMensaje());
        assertNotNull(response.getBody().getFechaHora());
    }

    @Test
    void manejarErrorGeneral_RetornaHttp500SinInformacionSensible() {
        Exception ex = new RuntimeException("Fallo interno no controlado con datos confidenciales");

        ResponseEntity<ErrorDto> response = handler.manejarErrorGeneral(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().getStatus());
        assertEquals("Ocurrió un error interno en el servidor", response.getBody().getMensaje());
        assertNotNull(response.getBody().getFechaHora());
        assertFalse(response.getBody().getMensaje().contains("confidenciales"));
    }

    @Test
    void manejarErrorGeneral_RegistraErrorConExcepcion() {
        Logger logger = (Logger) LoggerFactory.getLogger(GlobalExceptionHandler.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            Exception ex = new IllegalStateException("Fallo de prueba");

            handler.manejarErrorGeneral(ex);

            assertEquals(1, appender.list.size());
            ILoggingEvent evento = appender.list.get(0);
            assertEquals(Level.ERROR, evento.getLevel());
            assertNotNull(evento.getThrowableProxy());
            assertEquals(IllegalStateException.class.getName(), evento.getThrowableProxy().getClassName());
            assertEquals("Fallo de prueba", evento.getThrowableProxy().getMessage());
        } finally {
            logger.detachAppender(appender);
        }
    }
}
