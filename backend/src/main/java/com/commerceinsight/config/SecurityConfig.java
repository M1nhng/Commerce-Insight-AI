package com.commerceinsight.config;

import com.commerceinsight.security.JwtAuthenticationFilter;
import com.commerceinsight.security.McpApiKeyFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.http.HttpStatus;

/**
 * SecurityConfig — Spring Security filter chain configuration.
 *
 * <p>Architecture:
 * <ul>
 *   <li>Stateless session (JWT-only, no cookies for auth)</li>
 *   <li>CSRF disabled (stateless API)</li>
 *   <li>MCP API key filter runs before JWT filter</li>
 *   <li>Method-level security enabled via @PreAuthorize</li>
 * </ul>
 *
 * <p>Public paths (no auth required):
 * <ul>
 *   <li>POST /api/v1/auth/login</li>
 *   <li>POST /api/v1/auth/register</li>
 *   <li>POST /api/v1/auth/refresh</li>
 *   <li>GET /actuator/health</li>
 *   <li>GET /swagger-ui/** and /v3/api-docs/**</li>
 * </ul>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final McpApiKeyFilter mcpApiKeyFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // ── Disable CSRF (stateless API, no session cookies) ──────────
            .csrf(AbstractHttpConfigurer::disable)

            // ── Session Management: Stateless ─────────────────────────────
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // ── CORS: handled by CorsConfig bean ──────────────────────────
            .cors(cors -> {}) // Uses CorsConfigurationSource bean

            // ── Request Authorization Rules ───────────────────────────────
            .authorizeHttpRequests(auth -> auth
                // Public auth endpoints
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/auth/refresh").permitAll()

                // Actuator health (monitoring)
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()

                // Swagger UI + OpenAPI
                .requestMatchers(
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs",
                    "/v3/api-docs/**",
                    "/swagger-resources/**"
                ).permitAll()

                // All other requests require authentication
                .anyRequest().authenticated()
            )

            // ── Security Headers ──────────────────────────────────────────
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(content -> {})
                .httpStrictTransportSecurity(hsts -> hsts
                    .includeSubDomains(true)
                    .maxAgeInSeconds(31536000))
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            )

            // ── Exception Handling ────────────────────────────────────────
            // Return 401 Unauthorized for unauthenticated requests (not 403)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )

            // ── Authentication Provider ───────────────────────────────────
            .authenticationProvider(authenticationProvider())

            // ── Custom Filters ────────────────────────────────────────────
            // MCP filter runs first, JWT filter runs second
            .addFilterBefore(mcpApiKeyFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * BCrypt password encoder with strength factor 12.
     * As per 06_AUTHENTICATION.md security requirements.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}
