package com.commerceinsight.config;

import lombok.extern.slf4j.Slf4j;
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
 * <p>Allows the configured frontend origins to call the API. Wildcard origins
 * are explicitly rejected. All values come from {@code app.cors.*}.
 *
 * <p>Sprint 12A: {@code allowed-headers} and {@code allow-credentials} are now
 * read from configuration (previously hard-coded and dead); origins are trimmed;
 * a literal {@code *} origin aborts startup.
 */
@Slf4j
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins}")
    private String allowedOriginsRaw;

    @Value("${app.cors.allowed-methods}")
    private String allowedMethodsRaw;

    @Value("${app.cors.allowed-headers:Authorization,Content-Type,Accept,Origin,X-Requested-With,X-Request-Id}")
    private String allowedHeadersRaw;

    @Value("${app.cors.allow-credentials:false}")
    private boolean allowCredentials;

    @Value("${app.cors.max-age:3600}")
    private long maxAge;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> origins = splitTrim(allowedOriginsRaw);
        if (origins.contains("*")) {
            throw new IllegalStateException(
                    "app.cors.allowed-origins must not contain '*'. List explicit origins.");
        }
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(splitTrim(allowedMethodsRaw));
        configuration.setAllowedHeaders(splitTrim(allowedHeadersRaw));
        configuration.setExposedHeaders(List.of(
                "Authorization",
                "Content-Disposition",
                "X-Total-Count",
                "X-Request-Id",
                "Retry-After"
        ));
        configuration.setAllowCredentials(allowCredentials);
        configuration.setMaxAge(maxAge);

        log.info("CORS configured: origins={}, credentials={}", origins, allowCredentials);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    private static List<String> splitTrim(String raw) {
        return Arrays.stream(raw.split(","))
                .map(s -> s.trim())
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
