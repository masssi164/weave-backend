package com.massimotter.weave.backend.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = "bearer-jwt",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class OpenApiConfig {

    @Bean
    OpenAPI weaveOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Weave Backend API")
                        .version("v1")
                        .description("JWT-protected product API for workspace capabilities and orchestration."));
    }
}
