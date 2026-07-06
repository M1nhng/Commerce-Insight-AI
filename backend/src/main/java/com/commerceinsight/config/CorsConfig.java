package com.commerceinsight.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CorsConfig — Cross-Origin Resource Sharing configuration.
 *
 * <p>Allows the React frontend (running on a different port in dev)
 * to make API calls to the Spring Boot backend.
 *
 * <p>In production, only the configured allowed origins are permitted.
 * Wildcard origins are explicitly prohibited.
 */
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins}")
    private String allowedOriginsRaw;

    @Value("${app.cors.allowed-methods}")
    private String allowedMethodsRaw;

    @Value("${app.cors.max-age}")
    private long maxAge;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // Parse comma-separated origins
        List<String> origins = Arrays.asList(allowedOriginsRaw.split(","));
        configuration.setAllowedOrigins(origins);

        // HTTP methods
        List<String> methods = Arrays.asList(allowedMethodsRaw.split(","));
        configuration.setAllowedMethods(methods);

        // Allow all headers (client may send Content-Type, Authorization, etc.)
        configuration.setAllowedHeaders(List.of("*"));

        // Expose response headers the client might need
        configuration.setExposedHeaders(List.of(
                "Authorization",
                "Content-Disposition",
                "X-Total-Count"
        ));

        // Allow credentials (needed for refresh token in HttpOnly cookie, future)
        configuration.setAllowCredentials(true);

        // Cache preflight response for 1 hour
        configuration.setMaxAge(maxAge);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
