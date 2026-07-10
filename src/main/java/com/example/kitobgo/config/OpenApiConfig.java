package com.example.kitobgo.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger / OpenAPI hujjatlari sozlamasi.
 *
 * <p>Swagger UI:   <a href="http://localhost:8080/swagger-ui.html">/swagger-ui.html</a>
 * <p>OpenAPI JSON: <a href="http://localhost:8080/v3/api-docs">/v3/api-docs</a>
 *
 * <p>JWT bilan himoyalangan endpointlarni sinash uchun Swagger UI dagi
 * "Authorize" tugmasini bosib, {@code /api/auth/login} dan olingan tokenni
 * kiriting (faqat token, "Bearer" so'zisiz).
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Kitobgo API",
                version = "v1",
                description = "Kitobgo buyurtma boshqaruv tizimi uchun REST API hujjatlari.",
                contact = @Contact(name = "Kitobgo Team")
        ),
        servers = @Server(url = "/", description = "Joriy server"),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "JWT tokenni kiriting (login'dan olingan access token)."
)
public class OpenApiConfig {
}
