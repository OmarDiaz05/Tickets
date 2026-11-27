package fes.proyecto.tickets.ptickets.dtos;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class TicketDtoInTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void crearValidador() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        factory.close();
    }

    private static TicketDtoIn dtoValido() {
        return new TicketDtoIn(1, 2, "Sin acceso a internet en el edificio A",
                LocalDateTime.now().minusHours(2), "Edificio A", "Internet", null, "guid-ticket-001");
    }

    private static Set<String> mensajes(TicketDtoIn dto) {
        return validator.validate(dto).stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }

    @Test
    void dtoValido_SinMedioTransmision_NoTieneErrores() {
        assertTrue(validator.validate(dtoValido()).isEmpty());
    }

    @Test
    void camposObligatoriosVacios_RetornaMensajesEnEspanol() {
        TicketDtoIn dto = new TicketDtoIn();
        dto.setDescripcion("  ");
        dto.setAreaAfectada("");

        Set<String> mensajes = mensajes(dto);

        assertTrue(mensajes.contains("La categoría es obligatoria"));
        assertTrue(mensajes.contains("La prioridad es obligatoria"));
        assertTrue(mensajes.contains("La descripción es obligatoria"));
        assertTrue(mensajes.contains("La fecha desde cuándo ocurre la falla es obligatoria"));
        assertTrue(mensajes.contains("El área afectada es obligatoria"));
        assertTrue(mensajes.contains("El servicio impactado es obligatorio"));
        assertTrue(mensajes.contains("El guid es obligatorio"));
    }

    @Test
    void desdeCuandoFutura_RetornaError() {
        TicketDtoIn dto = dtoValido();
        dto.setDesdeCuando(LocalDateTime.now().plusDays(1));

        assertEquals(Set.of("La fecha desde cuándo ocurre la falla no puede ser futura"), mensajes(dto));
    }

    @Test
    void textosMasLargosQueLaColumna_RetornaErrores() {
        TicketDtoIn dto = dtoValido();
        dto.setDescripcion("x".repeat(2001));
        dto.setAreaAfectada("x".repeat(101));
        dto.setServicioImpactado("x".repeat(101));
        dto.setMedioTransmision("x".repeat(101));
        dto.setGuid("x".repeat(65));

        assertEquals(Set.of(
                "La descripción no debe exceder 2000 caracteres",
                "El área afectada no debe exceder 100 caracteres",
                "El servicio impactado no debe exceder 100 caracteres",
                "El medio de transmisión no debe exceder 100 caracteres",
                "El guid no debe exceder 64 caracteres"), mensajes(dto));
    }

    @Test
    void textosEnElLimite_SonValidos() {
        TicketDtoIn dto = dtoValido();
        dto.setDescripcion("x".repeat(2000));
        dto.setAreaAfectada("x".repeat(100));
        dto.setServicioImpactado("x".repeat(100));
        dto.setMedioTransmision("x".repeat(100));
        dto.setGuid("x".repeat(64));

        assertTrue(validator.validate(dto).isEmpty());
    }

    @Test
    void noAceptaCamposQueAsignaElSistema() {
        Set<String> campos = Arrays.stream(TicketDtoIn.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());

        for (String prohibido : Set.of("nombre", "nombreCliente", "email", "correo", "correoCliente",
                "estado", "estadoId", "folio", "administrador", "administradorAsignado", "administradorId")) {
            assertFalse(campos.contains(prohibido), "TicketDtoIn no debe tener el campo " + prohibido);
        }
    }
}
