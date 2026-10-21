package com.barberhub.common;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Documentación interactiva de la API en /swagger-ui.html. */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI barberHubOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("BarberHub API")
                        .version("0.1.0")
                        .description("API multitenant de reservas para barberías. Las rutas /api/public/{slug} no requieren "
                                + "sesión; las de /api/admin usan el token JWT que devuelve /api/auth/login."))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList("bearer"));
    }
}
