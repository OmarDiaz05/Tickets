package fes.proyecto.tickets.ptickets.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import fes.proyecto.tickets.ptickets.businessLayer.UsuarioBusinessLayer;
import fes.proyecto.tickets.ptickets.dtos.IdDto;
import fes.proyecto.tickets.ptickets.dtos.UsuarioDto;
import fes.proyecto.tickets.ptickets.dtos.UsuarioDtoIn;
import fes.proyecto.tickets.ptickets.exceptions.GlobalExceptionHandler;
import fes.proyecto.tickets.ptickets.exceptions.RecursoDuplicadoException;
import fes.proyecto.tickets.ptickets.exceptions.RecursoNoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private UsuarioBusinessLayer businessLayer;

    @InjectMocks
    private UsuarioController usuarioController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(usuarioController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void agregar_DatosValidos_RetornaHttp201() throws Exception {
        UsuarioDtoIn dtoIn = new UsuarioDtoIn();
        dtoIn.setNombre("Carlos Perez");
        dtoIn.setEmail("carlos@example.com");
        dtoIn.setPassword("secreto123");

        when(businessLayer.agregar(any(UsuarioDtoIn.class)))
                .thenReturn(new IdDto(1L, "Usuario registrado con éxito"));

        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoIn)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.mensaje").value("Usuario registrado con éxito"));
    }

    @Test
    void agregar_CorreoDuplicado_RetornaHttp409() throws Exception {
        UsuarioDtoIn dtoIn = new UsuarioDtoIn();
        dtoIn.setNombre("Carlos Perez");
        dtoIn.setEmail("carlos@example.com");
        dtoIn.setPassword("secreto123");

        when(businessLayer.agregar(any(UsuarioDtoIn.class)))
                .thenThrow(new RecursoDuplicadoException("El correo ya se encuentra registrado"));

        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoIn)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensaje").value("El correo ya se encuentra registrado"));
    }

    @Test
    void agregar_DatosInvalidos_RetornaHttp400() throws Exception {
        UsuarioDtoIn dtoIn = new UsuarioDtoIn();
        dtoIn.setNombre("");
        dtoIn.setEmail("correo-invalido");
        dtoIn.setPassword("");

        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoIn)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensaje").value("Datos de solicitud inválidos"))
                .andExpect(jsonPath("$.detalles").isArray());
    }

    @Test
    void obtenerTodos_RetornaHttp200() throws Exception {
        UsuarioDto usuario = new UsuarioDto(1L, "Carlos Perez", "carlos@example.com", LocalDateTime.now(), true);
        when(businessLayer.obtenerTodos()).thenReturn(List.of(usuario));

        mockMvc.perform(get("/api/v1/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Carlos Perez"))
                .andExpect(jsonPath("$[0].email").value("carlos@example.com"));
    }

    @Test
    void obtenerPorId_Existente_RetornaHttp200() throws Exception {
        UsuarioDto usuario = new UsuarioDto(1L, "Carlos Perez", "carlos@example.com", LocalDateTime.now(), true);
        when(businessLayer.obtenerPorId(1L)).thenReturn(usuario);

        mockMvc.perform(get("/api/v1/usuarios/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Carlos Perez"))
                .andExpect(jsonPath("$.email").value("carlos@example.com"));
    }

    @Test
    void obtenerPorId_Inexistente_RetornaHttp404() throws Exception {
        when(businessLayer.obtenerPorId(99L))
                .thenThrow(new RecursoNoEncontradoException("Usuario no encontrado con id 99"));

        mockMvc.perform(get("/api/v1/usuarios/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensaje").value("Usuario no encontrado con id 99"));
    }

    @Test
    void actualizar_DatosValidos_RetornaHttp200() throws Exception {
        UsuarioDtoIn dtoIn = new UsuarioDtoIn();
        dtoIn.setNombre("Carlos Perez Modificado");
        dtoIn.setEmail("carlos@example.com");
        dtoIn.setPassword("nuevaClave123");

        when(businessLayer.actualizar(eq(1L), any(UsuarioDtoIn.class)))
                .thenReturn(new IdDto(1L, "Usuario actualizado correctamente"));

        mockMvc.perform(put("/api/v1/usuarios/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoIn)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.mensaje").value("Usuario actualizado correctamente"));
    }

    @Test
    void actualizar_CorreoDeOtroUsuario_RetornaHttp409() throws Exception {
        UsuarioDtoIn dtoIn = new UsuarioDtoIn();
        dtoIn.setNombre("Carlos Perez");
        dtoIn.setEmail("otro@example.com");
        dtoIn.setPassword("nuevaClave123");

        when(businessLayer.actualizar(eq(1L), any(UsuarioDtoIn.class)))
                .thenThrow(new RecursoDuplicadoException("El correo ya se encuentra registrado por otro usuario"));

        mockMvc.perform(put("/api/v1/usuarios/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoIn)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensaje").value("El correo ya se encuentra registrado por otro usuario"));
    }

    @Test
    void actualizar_DatosInvalidos_RetornaHttp400() throws Exception {
        UsuarioDtoIn dtoIn = new UsuarioDtoIn();
        dtoIn.setNombre("");
        dtoIn.setEmail("formato-invalido");
        dtoIn.setPassword("");

        mockMvc.perform(put("/api/v1/usuarios/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoIn)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detalles").isArray());
    }

    @Test
    void deshabilitar_Existente_RetornaHttp200() throws Exception {
        when(businessLayer.deshabilitar(1L))
                .thenReturn(new IdDto(1L, "Usuario deshabilitado correctamente"));

        mockMvc.perform(delete("/api/v1/usuarios/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.mensaje").value("Usuario deshabilitado correctamente"));
    }
}
