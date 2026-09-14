package com.cheesouz.api_service.config;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;

@Configuration
@OpenAPIDefinition(info = @Info(title = "API Service", version = "0.0.1-SNAPSHOT",
        description = "REST API exposing user CRUD operations via db-service."))
@SecurityScheme(name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Paste a JWT issued by the auth server. Generate a test token "
                + "with the JWT secret via scripts/generate-test-token.sh.")
public class OpenApiConfig {
}