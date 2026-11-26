package fes.proyecto.tickets.ptickets.config;

import java.util.function.Supplier;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.web.util.matcher.RequestMatcher;

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
 * Soporta tanto vistas Thymeleaf (navegador) como API REST (Postman/Swagger).
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

        RequestMatcher esPeticionHtml = request -> {
            String accept = request.getHeader("Accept");
            return accept != null && accept.contains("text/html") && !request.getRequestURI().startsWith("/api/");
        };

        http
            .authorizeHttpRequests(auth -> auth
                // Documentación interactiva de Swagger y OpenAPI
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**").permitAll()

                // Pantallas públicas de login, registro y recursos estáticos (Puntos 4.8 y 4.9)
                .requestMatchers("/", "/login", "/registro/**", "/css/**", "/js/**", "/images/**", "/error").permitAll()

                // Registro público de usuarios por API
                .requestMatchers(HttpMethod.POST, "/api/v1/usuarios").permitAll()

                // Vistas protegidas por rol (Punto 4.10)
                .requestMatchers("/cliente/**").hasRole("CLIENTE")
                .requestMatchers("/admin/**").hasRole("ADMIN")

                // Apartado 4.3 - Permisos por rol en API:
                .requestMatchers(HttpMethod.DELETE, "/api/v1/usuarios/*").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/v1/usuarios/*/activar").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/v1/usuarios", "/api/v1/usuarios/*").hasAnyRole("ADMIN", "CLIENTE")
                .requestMatchers(HttpMethod.PUT, "/api/v1/usuarios/*").hasAnyRole("ADMIN", "CLIENTE")

                // Punto 5.2 - Solo los clientes levantan tickets
                .requestMatchers(HttpMethod.POST, "/api/v1/tickets").hasRole("CLIENTE")

                .anyRequest().authenticated())

            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenEnCookieRequestHandler()))

            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler((request, response, authentication) -> {
                    UsuarioAutenticado usuario = (UsuarioAutenticado) authentication.getPrincipal();
                    if (esPeticionHtml.matches(request)) {
                        boolean esAdmin = authentication.getAuthorities().stream()
                                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
                        response.sendRedirect(esAdmin ? "/admin/panel" : "/cliente/tickets");
                    } else {
                        respuestaJson.escribir(response, HttpStatus.OK,
                                new IdDto(usuario.getId(), "Inicio de sesión exitoso"));
                    }
                })
                .failureHandler((request, response, exception) -> {
                    if (esPeticionHtml.matches(request)) {
                        response.sendRedirect("/login?error=true");
                    } else {
                        respuestaJson.escribir(response, HttpStatus.UNAUTHORIZED,
                                new ErrorDto(HttpStatus.UNAUTHORIZED.value(), "Correo o contraseña incorrectos"));
                    }
                }))

            .logout(logout -> logout
                .logoutUrl("/logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, authentication) -> {
                    if (esPeticionHtml.matches(request)) {
                        response.sendRedirect("/login?logout=true");
                    } else {
                        int id = authentication != null && authentication.getPrincipal() instanceof UsuarioAutenticado u
                                ? u.getId() : 0;
                        respuestaJson.escribir(response, HttpStatus.OK, new IdDto(id, "Sesión cerrada correctamente"));
                    }
                }))

            .exceptionHandling(ex -> ex
                .defaultAuthenticationEntryPointFor(
                        new LoginUrlAuthenticationEntryPoint("/login"), esPeticionHtml)
                .defaultAuthenticationEntryPointFor(
                        noAutenticadoEntryPoint, request -> true)
                .accessDeniedHandler(accesoDenegadoHandler))

            .requestCache(cache -> cache.requestCache(new NullRequestCache()));

        return http.build();
    }

    static final class CsrfTokenEnCookieRequestHandler extends CsrfTokenRequestAttributeHandler {
        @Override
        public void handle(HttpServletRequest request, HttpServletResponse response,
                Supplier<CsrfToken> csrfToken) {
            super.handle(request, response, csrfToken);
            csrfToken.get();
        }
    }
}