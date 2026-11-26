package fes.proyecto.tickets.ptickets.controllers;

import fes.proyecto.tickets.ptickets.businessLayer.UsuarioBusinessLayer;
import fes.proyecto.tickets.ptickets.entities.NumeroEmpleado;
import fes.proyecto.tickets.ptickets.entities.Usuario;
import fes.proyecto.tickets.ptickets.exceptions.GlobalExceptionHandler;
import fes.proyecto.tickets.ptickets.repositories.INumeroEmpleadoRepository;
import fes.proyecto.tickets.ptickets.repositories.IUsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Registro de administradores vía POST /api/v1/usuarios con el controlador y la
 * capa de negocio reales (solo los repositorios están simulados), para validar
 * el formato de 9 dígitos del número de empleado de extremo a extremo.
 */
@ExtendWith(MockitoExtension.class)
class RegistroAdminNumeroEmpleadoTest {

    private static final String NUMERO_VALIDO = "100000001";

    @Mock
    private IUsuarioRepository usuarioRepository;

    @Mock
    private INumeroEmpleadoRepository numeroEmpleadoRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UsuarioBusinessLayer businessLayer = new UsuarioBusinessLayer(
                usuarioRepository, numeroEmpleadoRepository, new BCryptPasswordEncoder(4));
        mockMvc = MockMvcBuilders.standaloneSetup(new UsuarioController(businessLayer))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static String adminJson(String numeroEmpleado) {
        return """
                {"nombre":"Ing. Carlos Ruiz","email":"carlos.admin@example.com",
                 "password":"AdminPass123","rol":"ADMIN",
                 "numeroEmpleado":"%s","guid":"guid-admin-9d-001"}
                """.formatted(numeroEmpleado);
    }

    private static NumeroEmpleado numeroAutorizado(boolean usado) {
        NumeroEmpleado num = new NumeroEmpleado();
        num.setId(1);
        num.setNumeroEmpleado(NUMERO_VALIDO);
        num.setNombreReferencia(null); // nombre_referencia es opcional
        num.setUsado(usado);
        num.setActivo(true);
        num.setFechaRegistro(LocalDateTime.now());
        return num;
    }

    @ParameterizedTest
    @ValueSource(strings = {"ABCDEFGHI", "12345678", "1234567890"})
    void registrarAdmin_NumeroConFormatoInvalido_Retorna400(String numero) throws Exception {
        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminJson(numero)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detalles",
                        hasItem("numeroEmpleado: El número de empleado debe tener 9 dígitos")));

        verifyNoInteractions(usuarioRepository, numeroEmpleadoRepository);
    }

    @Test
    void registrarAdmin_NumeroValido_Retorna201YMarcaNumeroComoUsado() throws Exception {
        NumeroEmpleado num = numeroAutorizado(false);
        when(usuarioRepository.findByGuid("guid-admin-9d-001")).thenReturn(Optional.empty());
        when(usuarioRepository.existsByEmail("carlos.admin@example.com")).thenReturn(false);
        when(numeroEmpleadoRepository.findByNumeroEmpleadoAndActivoTrue(NUMERO_VALIDO)).thenReturn(Optional.of(num));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(30);
            return u;
        });

        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminJson(NUMERO_VALIDO)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(30))
                .andExpect(jsonPath("$.mensaje").value("Usuario registrado con éxito"));

        assertTrue(num.isUsado());
        verify(numeroEmpleadoRepository).save(num);
        verify(usuarioRepository).save(argThat(u ->
                "ADMIN".equals(u.getRol()) && NUMERO_VALIDO.equals(u.getNumeroEmpleado())));
    }

    @Test
    void registrarAdmin_NumeroYaUsado_Retorna409() throws Exception {
        when(usuarioRepository.findByGuid("guid-admin-9d-001")).thenReturn(Optional.empty());
        when(usuarioRepository.existsByEmail("carlos.admin@example.com")).thenReturn(false);
        when(numeroEmpleadoRepository.findByNumeroEmpleadoAndActivoTrue(NUMERO_VALIDO))
                .thenReturn(Optional.of(numeroAutorizado(true)));

        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminJson(NUMERO_VALIDO)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.mensaje").value("Este número de empleado ya fue utilizado previamente"));

        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(numeroEmpleadoRepository, never()).save(any(NumeroEmpleado.class));
    }
}
