package fes.proyecto.tickets.ptickets.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI ticketsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API Tickets - FESA")
                        .description("API sistema de tickets. Gestión de usuarios.")
                        .version("v1"));
    }
}
