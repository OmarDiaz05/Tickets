package fes.proyecto.tickets.ptickets.config;

import java.util.function.Supplier;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.savedrequest.NullRequestCache;

import fes.proyecto.tickets.ptickets.dtos.ErrorDto;
import fes.proyecto.tickets.ptickets.dtos.IdDto;
import fes.proyecto.tickets.ptickets.security.AccesoDenegadoHandler;
import fes.proyecto.tickets.ptickets.security.NoAutenticadoEntryPoint;
import fes.proyecto.tickets.ptickets.security.RespuestaJson;
import fes.proyecto.tickets.ptickets.security.UsuarioAutenticado;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Autenticación por sesión de servidor (cookie JSESSIONID), sin JWT.
 *
 * Flujo para clientes como Postman o K6:
 * 1. Cualquier petición devuelve la cookie XSRF-TOKEN (si aún no se tiene).
 * 2. Las peticiones POST/PUT/PATCH/DELETE (incluidos /login y /logout) deben
 *    enviar ese valor en la cabecera X-XSRF-TOKEN.
 * 3. POST /login (username = email, password) crea la sesión y renueva la
 *    cookie XSRF-TOKEN.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            NoAutenticadoEntryPoint noAutenticadoEntryPoint,
            AccesoDenegadoHandler accesoDenegadoHandler,
            RespuestaJson respuestaJson) throws Exception {

        http
            .authorizeHttpRequests(auth -> auth
                // Documentación interactiva de Swagger y OpenAPI
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

                // Registro público de usuarios (abierto a CLIENTE y a ADMIN con validación de número de empleado)
                .requestMatchers(HttpMethod.POST, "/api/v1/usuarios").permitAll()

                // Endpoint de inicio de sesión y gestión de errores de Spring Boot
                .requestMatchers("/login").permitAll()
                .requestMatchers("/error").permitAll()

                // Apartado 4.3 - Permisos por rol:
                // Solo el rol ADMIN puede deshabilitar (borrado lógico) o reactivar usuarios
                .requestMatchers(HttpMethod.DELETE, "/api/v1/usuarios/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/usuarios/*/activar").hasRole("ADMIN")

                // Consulta y actualización disponibles para usuarios autenticados (ADMIN y CLIENTE)
                .requestMatchers(HttpMethod.GET, "/api/v1/usuarios", "/api/v1/usuarios/*").hasAnyRole("ADMIN", "CLIENTE")
                .requestMatchers(HttpMethod.PUT, "/api/v1/usuarios/*").hasAnyRole("ADMIN", "CLIENTE")

                // Fase 5 (Tickets - reglas preparadas):
                // El cliente solo ve y modifica sus propios tickets; los administradores ven todos.
                .anyRequest().authenticated())

            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenEnCookieRequestHandler()))

            .formLogin(form -> form
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler((request, response, authentication) -> {
                    UsuarioAutenticado usuario = (UsuarioAutenticado) authentication.getPrincipal();
                    respuestaJson.escribir(response, HttpStatus.OK,
                            new IdDto(usuario.getId(), "Inicio de sesión exitoso"));
                })
                // Mismo mensaje para correo inexistente, contraseña incorrecta o usuario
                // desactivado, para no revelar qué cuentas existen
                .failureHandler((request, response, exception) -> respuestaJson.escribir(response,
                        HttpStatus.UNAUTHORIZED,
                        new ErrorDto(HttpStatus.UNAUTHORIZED.value(), "Correo o contraseña incorrectos"))))

            .logout(logout -> logout
                .logoutUrl("/logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, authentication) -> {
                    int id = authentication != null && authentication.getPrincipal() instanceof UsuarioAutenticado u
                            ? u.getId() : 0;
                    respuestaJson.escribir(response, HttpStatus.OK, new IdDto(id, "Sesión cerrada correctamente"));
                }))

            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(noAutenticadoEntryPoint)
                .accessDeniedHandler(accesoDenegadoHandler))

            // API REST: no se guarda la petición original para redirigir tras el login
            // (evita crear sesiones para peticiones anónimas)
            .requestCache(cache -> cache.requestCache(new NullRequestCache()));

        return http.build();
    }

    /**
     * Acepta el token CSRF tal cual viene en la cookie XSRF-TOKEN (sin la máscara
     * XOR que Spring usa por defecto) y lo genera en cada petición en que haga falta,
     * para que la cookie siempre esté disponible. Al iniciar sesión Spring
     * Security renueva el token y esta clase escribe la cookie nueva en la
     * misma respuesta del login.
     */
    static final class CsrfTokenEnCookieRequestHandler extends CsrfTokenRequestAttributeHandler {
        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response,
                Supplier<CsrfToken> csrfToken) {
            super.handle(request, response, csrfToken);
            csrfToken.get();
        }
    }
}
