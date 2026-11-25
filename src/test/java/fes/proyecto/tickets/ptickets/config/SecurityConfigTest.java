package fes.proyecto.tickets.ptickets.config;

import fes.proyecto.tickets.ptickets.businessLayer.UsuarioBusinessLayer;
import fes.proyecto.tickets.ptickets.businessLayer.UsuarioBusinessLayer.ResultadoRegistro;
import fes.proyecto.tickets.ptickets.controllers.UsuarioController;
import fes.proyecto.tickets.ptickets.dtos.IdDto;
import fes.proyecto.tickets.ptickets.dtos.UsuarioDtoIn;
import fes.proyecto.tickets.ptickets.entities.Usuario;
import fes.proyecto.tickets.ptickets.repositories.IUsuarioRepository;
import fes.proyecto.tickets.ptickets.security.AccesoDenegadoHandler;
import fes.proyecto.tickets.ptickets.security.NoAutenticadoEntryPoint;
import fes.proyecto.tickets.ptickets.security.RespuestaJson;
import fes.proyecto.tickets.ptickets.security.UsuarioDetailsService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UsuarioController.class)
@Import({
        SecurityConfig.class,
        PasswordConfig.class,
        UsuarioDetailsService.class,
        NoAutenticadoEntryPoint.class,
        AccesoDenegadoHandler.class,
        RespuestaJson.class
})
class SecurityConfigTest {

    private static final String EMAIL = "ana@example.com";
    private static final String PASSWORD = "secreto123";

    private static final String REGISTRO_JSON = """
            {"nombre":"Ana Lopez","email":"ana@example.com","password":"secreto123",
             "rol":"CLIENTE","guid":"guid-ana-001"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private IUsuarioRepository usuarioRepository;

    @MockitoBean
    private UsuarioBusinessLayer usuarioBusinessLayer;

    private Usuario usuario(boolean activo) {
        Usuario u = new Usuario();
        u.setId(10);
        u.setNombre("Ana Lopez");
        u.setEmail(EMAIL);
        u.setPassword(passwordEncoder.encode(PASSWORD));
        u.setRol("CLIENTE");
        u.setVerificado(true);
        u.setActivo(activo);
        u.setFechaRegistro(LocalDateTime.now());
        u.setGuid("guid-ana-001");
        return u;
    }

    private MvcResult iniciarSesion() throws Exception {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario(true)));
        return mockMvc.perform(post("/login")
                        .param("username", EMAIL)
                        .param("password", PASSWORD)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andReturn();
    }

    // ---------- login ----------

    @Test
    void login_CredencialesCorrectas_Retorna200ConIdYRol() throws Exception {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario(true)));

        MvcResult resultado = mockMvc.perform(post("/login")
                        .param("username", "  Ana@Example.com ")
                        .param("password", PASSWORD)
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.mensaje").value("Inicio de sesión exitoso"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(authenticated()
                        .withUsername(EMAIL)
                        .withAuthentication(auth -> assertTrue(auth.getAuthorities().stream()
                                .anyMatch(a -> "ROLE_CLIENTE".equals(a.getAuthority())))))
                .andReturn();

        assertFalse(resultado.getResponse().getContentAsString().contains("$2"),
                "La respuesta no debe incluir el hash de la contraseña");
    }

    @Test
    void login_SesionPermiteAccederARutaProtegida() throws Exception {
        MockHttpSession sesion = (MockHttpSession) iniciarSesion().getRequest().getSession(false);
        assertNotNull(sesion);

        mockMvc.perform(get("/api/v1/usuarios").session(sesion))
                .andExpect(status().isOk());
    }

    @Test
    void login_PasswordIncorrecta_Retorna401ConErrorDto() throws Exception {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario(true)));

        mockMvc.perform(post("/login")
                        .param("username", EMAIL)
                        .param("password", "incorrecta")
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos"))
                .andExpect(jsonPath("$.fechaHora").exists())
                .andExpect(unauthenticated());
    }

    @Test
    void login_CorreoInexistente_Retorna401() throws Exception {
        when(usuarioRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        mockMvc.perform(post("/login")
                        .param("username", "nadie@example.com")
                        .param("password", PASSWORD)
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Correo o contraseña incorrectos"))
                .andExpect(unauthenticated());
    }

    @Test
    void login_UsuarioDesactivado_Retorna401AunqueLaPasswordSeaCorrecta() throws Exception {
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario(false)));

        mockMvc.perform(post("/login")
                        .param("username", EMAIL)
                        .param("password", PASSWORD)
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(unauthenticated());
    }

    @Test
    void login_SinTokenCsrf_Retorna403ConErrorDto() throws Exception {
        mockMvc.perform(post("/login")
                        .param("username", EMAIL)
                        .param("password", PASSWORD))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.mensaje").exists());

        verify(usuarioRepository, never()).findByEmail(any());
    }

    // csrf() de spring-security-test reemplaza el repositorio del CsrfFilter compartido por
    // uno basado en HttpSession; se recrea el contexto para probar el CookieCsrfTokenRepository real
    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void login_ConCookieYCabeceraCsrf_ComoPostmanOK6() throws Exception {
        // 1. Una petición cualquiera entrega la cookie XSRF-TOKEN
        Cookie cookieCsrf = mockMvc.perform(get("/api/v1/usuarios"))
                .andExpect(status().isUnauthorized())
                .andExpect(cookie().exists("XSRF-TOKEN"))
                .andExpect(cookie().httpOnly("XSRF-TOKEN", false))
                .andReturn().getResponse().getCookie("XSRF-TOKEN");
        assertNotNull(cookieCsrf);

        // 2. Login reenviando la cookie y su valor en la cabecera X-XSRF-TOKEN
        when(usuarioRepository.findByEmail(EMAIL)).thenReturn(Optional.of(usuario(true)));
        MvcResult login = mockMvc.perform(post("/login")
                        .cookie(cookieCsrf)
                        .header("X-XSRF-TOKEN", cookieCsrf.getValue())
                        .param("username", EMAIL)
                        .param("password", PASSWORD))
                .andExpect(status().isOk())
                .andReturn();

        // 3. Tras el login el token se renueva en la misma respuesta
        boolean tokenRenovado = Arrays.stream(login.getResponse().getCookies())
                .anyMatch(c -> "XSRF-TOKEN".equals(c.getName())
                        && c.getValue() != null && !c.getValue().isEmpty()
                        && !c.getValue().equals(cookieCsrf.getValue()));
        assertTrue(tokenRenovado, "El login debe entregar una cookie XSRF-TOKEN nueva");
    }

    // ---------- rutas protegidas / públicas ----------

    @Test
    void rutaProtegida_SinSesion_Retorna401ConErrorDto() throws Exception {
        mockMvc.perform(get("/api/v1/usuarios"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.mensaje").value("Se requiere iniciar sesión para acceder a este recurso"))
                .andExpect(jsonPath("$.fechaHora").exists());

        verify(usuarioBusinessLayer, never()).obtenerTodos();
    }

    @Test
    void registro_SinSesionConCsrf_EsPublico() throws Exception {
        when(usuarioBusinessLayer.agregar(any(UsuarioDtoIn.class)))
                .thenReturn(new ResultadoRegistro(new IdDto(10, "Usuario registrado con éxito"), false));

        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_JSON)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void registro_SinTokenCsrf_Retorna403() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REGISTRO_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        verify(usuarioBusinessLayer, never()).agregar(any());
    }

    // ---------- logout ----------

    @Test
    void logout_InvalidaSesionYBorraCookieJsessionid() throws Exception {
        MockHttpSession sesion = (MockHttpSession) iniciarSesion().getRequest().getSession(false);
        assertNotNull(sesion);

        mockMvc.perform(post("/logout").session(sesion).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.mensaje").value("Sesión cerrada correctamente"))
                .andExpect(cookie().maxAge("JSESSIONID", 0));

        assertTrue(sesion.isInvalid());
        mockMvc.perform(get("/api/v1/usuarios").session(new MockHttpSession()))
                .andExpect(status().isUnauthorized());
    }
}
