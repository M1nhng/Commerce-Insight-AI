package com.commerceinsight.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenApiConfig — SpringDoc OpenAPI (Swagger) configuration.
 *
 * <p>Configures:
 * <ul>
 *   <li>API metadata (title, version, contact)</li>
 *   <li>JWT Bearer auth in Swagger UI</li>
 *   <li>MCP API Key auth scheme</li>
 *   <li>Server URLs</li>
 * </ul>
 *
 * <p>Swagger UI is available at: http://localhost:8080/swagger-ui.html
 * <p>OpenAPI JSON is available at: http://localhost:8080/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Value("${app.openapi.title}")
    private String title;

    @Value("${app.openapi.description}")
    private String description;

    @Value("${app.openapi.version}")
    private String version;

    @Value("${app.openapi.contact-name}")
    private String contactName;

    @Value("${app.openapi.contact-email}")
    private String contactEmail;

    private static final String JWT_SECURITY_SCHEME = "Bearer Authentication";
    private static final String MCP_SECURITY_SCHEME = "MCP API Key";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("Local Development"),
                        new Server().url("https://api.commerceinsight.ai").description("Production")
                ))
                .addSecurityItem(new SecurityRequirement().addList(JWT_SECURITY_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(JWT_SECURITY_SCHEME, jwtSecurityScheme())
                        .addSecuritySchemes(MCP_SECURITY_SCHEME, mcpApiKeyScheme())
                );
    }

    private Info apiInfo() {
        return new Info()
                .title(title)
                .description(description)
                .version(version)
                .contact(new Contact()
                        .name(contactName)
                        .email(contactEmail));
    }

    private SecurityScheme jwtSecurityScheme() {
        return new SecurityScheme()
                .name(JWT_SECURITY_SCHEME)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Provide your JWT access token. Obtain it from POST /api/v1/auth/login");
    }

    private SecurityScheme mcpApiKeyScheme() {
        return new SecurityScheme()
                .name("X-MCP-API-KEY")
                .type(SecurityScheme.Type.APIKEY)
                .in(SecurityScheme.In.HEADER)
                .description("API key for MCP server access");
    }
}
