package fes.proyecto.tickets.ptickets.controllers;

import fes.proyecto.tickets.ptickets.businessLayer.TicketBusinessLayer;
import fes.proyecto.tickets.ptickets.businessLayer.TicketBusinessLayer.ResultadoRegistro;
import fes.proyecto.tickets.ptickets.config.PasswordConfig;
import fes.proyecto.tickets.ptickets.config.SecurityConfig;
import fes.proyecto.tickets.ptickets.dtos.IdDto;
import fes.proyecto.tickets.ptickets.dtos.TicketDtoIn;
import fes.proyecto.tickets.ptickets.exceptions.SolicitudInvalidaException;
import fes.proyecto.tickets.ptickets.repositories.IUsuarioRepository;
import fes.proyecto.tickets.ptickets.security.AccesoDenegadoHandler;
import fes.proyecto.tickets.ptickets.security.NoAutenticadoEntryPoint;
import fes.proyecto.tickets.ptickets.security.RespuestaJson;
import fes.proyecto.tickets.ptickets.security.UsuarioAutenticado;
import fes.proyecto.tickets.ptickets.security.UsuarioDetailsService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TicketController.class)
@Import({
        SecurityConfig.class,
        PasswordConfig.class,
        UsuarioDetailsService.class,
        NoAutenticadoEntryPoint.class,
        AccesoDenegadoHandler.class,
        RespuestaJson.class
})
class TicketControllerTest {

    private static final UsuarioAutenticado CLIENTE =
            new UsuarioAutenticado(10, "ana@example.com", "hash", "CLIENTE", true);
    private static final UsuarioAutenticado ADMIN =
            new UsuarioAutenticado(20, "carlos@example.com", "hash", "ADMIN", true);

    private static final String TICKET_JSON = """
            {"categoriaId":1,"prioridadId":2,"descripcion":"Sin internet en el edificio A",
             "desdeCuando":"2026-11-20T09:30:00","areaAfectada":"Edificio A",
             "servicioImpactado":"Internet","medioTransmision":"Fibra óptica","guid":"guid-ticket-001"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TicketBusinessLayer ticketBusinessLayer;

    @MockitoBean
    private IUsuarioRepository usuarioRepository; // requerido por UsuarioDetailsService

    @Test
    void crear_ClienteConSesion_Retorna201ConIdDelUsuarioDeLaSesion() throws Exception {
        when(ticketBusinessLayer.agregar(any(TicketDtoIn.class), anyInt()))
                .thenReturn(new ResultadoRegistro(new IdDto(50, "Ticket FESA-2026-001 creado"), false));

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TICKET_JSON)
                        .with(user(CLIENTE))
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(50))
                .andExpect(jsonPath("$.mensaje").value("Ticket FESA-2026-001 creado"));

        ArgumentCaptor<TicketDtoIn> captor = ArgumentCaptor.forClass(TicketDtoIn.class);
        verify(ticketBusinessLayer).agregar(captor.capture(), eq(10));
        assertEquals(1, captor.getValue().getCategoriaId());
        assertEquals("guid-ticket-001", captor.getValue().getGuid());
    }

    @Test
    void crear_CuerpoConUsuarioEstadoYFolio_SeIgnoranYSeUsaLaSesion() throws Exception {
        when(ticketBusinessLayer.agregar(any(TicketDtoIn.class), anyInt()))
                .thenReturn(new ResultadoRegistro(new IdDto(50, "Ticket FESA-2026-001 creado"), false));
        String conCamposDelSistema = TICKET_JSON.replace("{\"categoriaId\"",
                "{\"usuarioId\":99,\"estado\":\"Cerrado\",\"folio\":\"FESA-1999-ZZZ\","
                        + "\"administradorAsignado\":\"Otro\",\"categoriaId\"");

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(conCamposDelSistema)
                        .with(user(CLIENTE))
                        .with(csrf()))
                .andExpect(status().isCreated());

        verify(ticketBusinessLayer).agregar(any(TicketDtoIn.class), eq(10));
    }

    @Test
    void crear_GuidRepetido_Retorna208ConIdExistente() throws Exception {
        when(ticketBusinessLayer.agregar(any(TicketDtoIn.class), anyInt()))
                .thenReturn(new ResultadoRegistro(
                        new IdDto(7, "Ticket FESA-2026-005 registrado previamente (GUID duplicado)"), true));

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TICKET_JSON)
                        .with(user(CLIENTE))
                        .with(csrf()))
                .andExpect(status().isAlreadyReported())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.mensaje").value("Ticket FESA-2026-005 registrado previamente (GUID duplicado)"));
    }

    @Test
    void crear_CategoriaInexistente_Retorna400ConErrorDto() throws Exception {
        when(ticketBusinessLayer.agregar(any(TicketDtoIn.class), anyInt()))
                .thenThrow(new SolicitudInvalidaException("La categoría con id 99 no existe"));

        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TICKET_JSON.replace("\"categoriaId\":1", "\"categoriaId\":99"))
                        .with(user(CLIENTE))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensaje").value("La categoría con id 99 no existe"));
    }

    @Test
    void crear_DatosInvalidos_Retorna400SinLlamarALaCapaDeNegocio() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"descripcion\":\"\",\"desdeCuando\":\"2999-01-01T00:00:00\"}")
                        .with(user(CLIENTE))
                        .with(csrf()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Datos de solicitud inválidos"))
                .andExpect(jsonPath("$.detalles", hasItem("categoriaId: La categoría es obligatoria")))
                .andExpect(jsonPath("$.detalles",
                        hasItem("desdeCuando: La fecha desde cuándo ocurre la falla no puede ser futura")));

        verifyNoInteractions(ticketBusinessLayer);
    }

    @Test
    void crear_Admin_Retorna403ConErrorDto() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TICKET_JSON)
                        .with(user(ADMIN))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.mensaje").value("No tienes permiso para acceder a este recurso"))
                .andExpect(jsonPath("$.fechaHora").exists());

        verifyNoInteractions(ticketBusinessLayer);
    }

    @Test
    void crear_SinSesion_Retorna401ConErrorDto() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TICKET_JSON)
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").value("Se requiere iniciar sesión para acceder a este recurso"))
                .andExpect(jsonPath("$.fechaHora").exists());

        verifyNoInteractions(ticketBusinessLayer);
    }

    @Test
    void crear_SinTokenCsrf_Retorna403() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TICKET_JSON)
                        .with(user(CLIENTE)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verifyNoInteractions(ticketBusinessLayer);
    }
}
