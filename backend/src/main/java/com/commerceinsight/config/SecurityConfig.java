package com.commerceinsight.config;

import com.commerceinsight.security.JwtAuthenticationFilter;
import com.commerceinsight.security.McpApiKeyFilter;
import com.commerceinsight.security.RateLimitingFilter;
import com.commerceinsight.security.RestAccessDeniedHandler;
import com.commerceinsight.security.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
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
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.header.writers.ContentSecurityPolicyHeaderWriter;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.AnyRequestMatcher;

/**
 * SecurityConfig — Spring Security filter chain configuration.
 *
 * <p>Architecture:
 * <ul>
 *   <li>Stateless session (JWT-only, no cookies for auth)</li>
 *   <li>CSRF disabled (stateless API)</li>
 *   <li>Filter order: RateLimiting → McpApiKey → Jwt → app</li>
 *   <li>Method-level security enabled via {@code @PreAuthorize}, with a
 *       {@code RoleHierarchy} (ADMIN &gt; MANAGER &gt; STAFF) as defense-in-depth</li>
 *   <li>401 / 403 emit the standard {@code ApiResponse} error envelope</li>
 * </ul>
 *
 * <p>Public paths: {@code POST /api/v1/auth/{login,register,refresh}},
 * {@code /actuator/health|info}, Swagger UI (disabled in the {@code prod} profile).
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final McpApiKeyFilter mcpApiKeyFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final UserDetailsService userDetailsService;
    private final RestAuthenticationEntryPoint authenticationEntryPoint;
    private final RestAccessDeniedHandler accessDeniedHandler;

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

                // Actuator: health/info are public for orchestrator probes;
                // everything else exposed (e.g. metrics) is ADMIN-only and never public.
                .requestMatchers("/actuator/health", "/actuator/health/**", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")

                // Swagger UI + OpenAPI (springdoc disables these entirely in prod)
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
                    .preload(true)
                    .maxAgeInSeconds(31536000)
                    // Emit on every response, not just request.isSecure(). TLS is
                    // terminated at nginx, so the backend always sees plain HTTP
                    // and would otherwise never send HSTS. A browser ignores the
                    // header on a non-HTTPS hop (RFC 6797 §8.1) and honours it
                    // once nginx forwards it over the real HTTPS connection.
                    .requestMatcher(AnyRequestMatcher.INSTANCE))
                .referrerPolicy(referrer -> referrer
                    .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
                .permissionsPolicyHeader(pp -> pp.policy(
                    "geolocation=(), camera=(), microphone=(), payment=(), usb=(), interest-cohort=()"))
                // Strict CSP for the JSON API only — Swagger UI (dev) keeps its own.
                .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                    PathPatternRequestMatcher.withDefaults().matcher("/api/**"),
                    new ContentSecurityPolicyHeaderWriter(
                        "default-src 'none'; frame-ancestors 'none'; base-uri 'none'; form-action 'none'")))
            )

            // ── Exception Handling: enveloped 401 / 403 ───────────────────
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )

            // ── Authentication Provider ───────────────────────────────────
            .authenticationProvider(authenticationProvider())

            // ── Custom Filters ────────────────────────────────────────────
            // Rate limiting first, then MCP key, then JWT.
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
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

    /**
     * Role hierarchy — defense-in-depth only. Current {@code @PreAuthorize}
     * annotations already enumerate every accepted role, so effective access is
     * unchanged; this guards against a future dropped-role mistake and lets a
     * {@code hasRole('STAFF')} check also admit MANAGER/ADMIN.
     * {@code ROLE_MCP_SERVICE} is intentionally outside the hierarchy.
     */
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.withDefaultRolePrefix()
                .role("ADMIN").implies("MANAGER")
                .role("MANAGER").implies("STAFF")
                .build();
    }
}
