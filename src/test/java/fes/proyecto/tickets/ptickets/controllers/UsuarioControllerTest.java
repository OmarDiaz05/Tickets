package fes.proyecto.tickets.ptickets.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import fes.proyecto.tickets.ptickets.businessLayer.UsuarioBusinessLayer;
import fes.proyecto.tickets.ptickets.businessLayer.UsuarioBusinessLayer.ResultadoRegistro;
import fes.proyecto.tickets.ptickets.dtos.IdDto;
import fes.proyecto.tickets.ptickets.dtos.UsuarioDto;
import fes.proyecto.tickets.ptickets.dtos.UsuarioDtoIn;
import fes.proyecto.tickets.ptickets.exceptions.AccesoDenegadoException;
import fes.proyecto.tickets.ptickets.exceptions.GlobalExceptionHandler;
import fes.proyecto.tickets.ptickets.exceptions.RecursoDuplicadoException;
import fes.proyecto.tickets.ptickets.exceptions.RecursoNoEncontradoException;
import fes.proyecto.tickets.ptickets.exceptions.ReglaNegocioException;
import fes.proyecto.tickets.ptickets.exceptions.SolicitudInvalidaException;
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

    private UsuarioDtoIn dtoValido() {
        UsuarioDtoIn dtoIn = new UsuarioDtoIn();
        dtoIn.setNombre("Carlos Perez");
        dtoIn.setEmail("carlos@example.com");
        dtoIn.setPassword("secreto123");
        dtoIn.setRol("CLIENTE");
        dtoIn.setGuid("guid-001");
        return dtoIn;
    }

    private UsuarioDto usuarioDto() {
        return new UsuarioDto(1, "Carlos Perez", "carlos@example.com", "CLIENTE", null,
                false, LocalDateTime.now(), true);
    }

    // ---------- POST ----------

    @Test
    void agregar_DatosValidos_RetornaHttp201() throws Exception {
        when(businessLayer.agregar(any(UsuarioDtoIn.class)))
                .thenReturn(new ResultadoRegistro(new IdDto(1, "Usuario registrado con éxito"), false));

        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoValido())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.mensaje").value("Usuario registrado con éxito"));
    }

    @Test
    void agregar_GuidRepetido_RetornaHttp208ConIdExistente() throws Exception {
        when(businessLayer.agregar(any(UsuarioDtoIn.class)))
                .thenReturn(new ResultadoRegistro(
                        new IdDto(5, "Usuario registrado previamente (GUID duplicado)"), true));

        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoValido())))
                .andExpect(status().isAlreadyReported())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.mensaje").value("Usuario registrado previamente (GUID duplicado)"));
    }

    @Test
    void agregar_CorreoRepetidoConOtroGuid_RetornaHttp409() throws Exception {
        when(businessLayer.agregar(any(UsuarioDtoIn.class)))
                .thenThrow(new RecursoDuplicadoException("El correo ya se encuentra registrado"));

        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoValido())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensaje").value("El correo ya se encuentra registrado"));
    }

    @Test
    void agregar_NumeroEmpleadoYaUsado_RetornaHttp409() throws Exception {
        when(businessLayer.agregar(any(UsuarioDtoIn.class)))
                .thenThrow(new ReglaNegocioException("Este número de empleado ya fue utilizado previamente"));

        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoValido())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensaje").value("Este número de empleado ya fue utilizado previamente"));
    }

    @Test
    void agregar_NumeroEmpleadoNoAutorizado_RetornaHttp403() throws Exception {
        when(businessLayer.agregar(any(UsuarioDtoIn.class)))
                .thenThrow(new AccesoDenegadoException("Número de empleado no autorizado o inactivo"));

        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoValido())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.mensaje").value("Número de empleado no autorizado o inactivo"));
    }

    @Test
    void agregar_RolInvalido_RetornaHttp400() throws Exception {
        when(businessLayer.agregar(any(UsuarioDtoIn.class)))
                .thenThrow(new SolicitudInvalidaException("Rol inválido. Debe ser CLIENTE o ADMIN"));

        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoValido())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.mensaje").value("Rol inválido. Debe ser CLIENTE o ADMIN"));
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

    // ---------- GET ----------

    @Test
    void obtenerTodos_RetornaHttp200() throws Exception {
        when(businessLayer.obtenerTodos()).thenReturn(List.of(usuarioDto()));

        mockMvc.perform(get("/api/v1/usuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Carlos Perez"))
                .andExpect(jsonPath("$[0].email").value("carlos@example.com"));
    }

    @Test
    void obtenerPorId_Existente_RetornaHttp200() throws Exception {
        when(businessLayer.obtenerPorId(1)).thenReturn(usuarioDto());

        mockMvc.perform(get("/api/v1/usuarios/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nombre").value("Carlos Perez"))
                .andExpect(jsonPath("$.email").value("carlos@example.com"));
    }

    @Test
    void obtenerPorId_Inexistente_RetornaHttp404() throws Exception {
        when(businessLayer.obtenerPorId(99))
                .thenThrow(new RecursoNoEncontradoException("Usuario no encontrado con id 99"));

        mockMvc.perform(get("/api/v1/usuarios/{id}", 99))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.mensaje").value("Usuario no encontrado con id 99"));
    }

    // ---------- PUT ----------

    @Test
    void actualizar_DatosValidos_RetornaHttp200() throws Exception {
        UsuarioDtoIn dtoIn = dtoValido();
        dtoIn.setNombre("Carlos Perez Modificado");

        when(businessLayer.actualizar(eq(1), any(UsuarioDtoIn.class)))
                .thenReturn(new IdDto(1, "Usuario actualizado correctamente"));

        mockMvc.perform(put("/api/v1/usuarios/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoIn)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.mensaje").value("Usuario actualizado correctamente"));
    }

    @Test
    void actualizar_CorreoDeOtroUsuario_RetornaHttp409() throws Exception {
        UsuarioDtoIn dtoIn = dtoValido();
        dtoIn.setEmail("otro@example.com");

        when(businessLayer.actualizar(eq(1), any(UsuarioDtoIn.class)))
                .thenThrow(new RecursoDuplicadoException("El correo ya se encuentra registrado por otro usuario"));

        mockMvc.perform(put("/api/v1/usuarios/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoIn)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensaje").value("El correo ya se encuentra registrado por otro usuario"));
    }

    @Test
    void actualizar_Inexistente_RetornaHttp404() throws Exception {
        when(businessLayer.actualizar(eq(99), any(UsuarioDtoIn.class)))
                .thenThrow(new RecursoNoEncontradoException("Usuario no encontrado con id 99"));

        mockMvc.perform(put("/api/v1/usuarios/{id}", 99)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoValido())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void actualizar_DatosInvalidos_RetornaHttp400() throws Exception {
        UsuarioDtoIn dtoIn = new UsuarioDtoIn();
        dtoIn.setNombre("");
        dtoIn.setEmail("formato-invalido");
        dtoIn.setPassword("");

        mockMvc.perform(put("/api/v1/usuarios/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dtoIn)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detalles").isArray());
    }

    // ---------- DELETE / PATCH ----------

    @Test
    void borrar_Existente_RetornaHttp200() throws Exception {
        when(businessLayer.borrar(1))
                .thenReturn(new IdDto(1, "Usuario deshabilitado correctamente"));

        mockMvc.perform(delete("/api/v1/usuarios/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.mensaje").value("Usuario deshabilitado correctamente"));
    }

    @Test
    void borrar_Inexistente_RetornaHttp404() throws Exception {
        when(businessLayer.borrar(99))
                .thenThrow(new RecursoNoEncontradoException("Usuario no encontrado con id 99"));

        mockMvc.perform(delete("/api/v1/usuarios/{id}", 99))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void activar_Existente_RetornaHttp200() throws Exception {
        when(businessLayer.activar(1))
                .thenReturn(new IdDto(1, "Usuario reactivado correctamente"));

        mockMvc.perform(patch("/api/v1/usuarios/{id}/activar", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.mensaje").value("Usuario reactivado correctamente"));
    }

    @Test
    void obtenerPorId_IdNoNumerico_RetornaHttp400() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
