package fes.proyecto.tickets.ptickets.controllers;

import fes.proyecto.tickets.ptickets.businessLayer.UsuarioBusinessLayer;
import fes.proyecto.tickets.ptickets.config.PasswordConfig;
import fes.proyecto.tickets.ptickets.config.SecurityConfig;
import fes.proyecto.tickets.ptickets.repositories.IUsuarioRepository;
import fes.proyecto.tickets.ptickets.security.AccesoDenegadoHandler;
import fes.proyecto.tickets.ptickets.security.NoAutenticadoEntryPoint;
import fes.proyecto.tickets.ptickets.security.RespuestaJson;
import fes.proyecto.tickets.ptickets.security.UsuarioDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * El formulario web de registro de administrador debe mostrar el error de formato
 * del número de empleado cuando se omite la validación del navegador.
 */
@WebMvcTest(AuthWebController.class)
@Import({
        SecurityConfig.class,
        PasswordConfig.class,
        UsuarioDetailsService.class,
        NoAutenticadoEntryPoint.class,
        AccesoDenegadoHandler.class,
        RespuestaJson.class
})
class RegistroAdminFormularioTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IUsuarioRepository usuarioRepository;

    @MockitoBean
    private UsuarioBusinessLayer usuarioBusinessLayer;

    @Test
    void registroAdmin_NumeroConFormatoInvalido_MuestraMensajeEnElFormulario() throws Exception {
        mockMvc.perform(post("/registro/admin")
                        .param("numeroEmpleado", "12345678")
                        .param("nombre", "Ing. Carlos Ruiz")
                        .param("email", "carlos.admin@example.com")
                        .param("password", "AdminPass123")
                        .param("rol", "ADMIN")
                        .param("guid", "guid-form-001")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("registro-admin"))
                .andExpect(model().attributeHasFieldErrors("usuario", "numeroEmpleado"))
                .andExpect(content().string(containsString("El número de empleado debe tener 9 dígitos")));

        verify(usuarioBusinessLayer, never()).agregar(any());
    }
}
