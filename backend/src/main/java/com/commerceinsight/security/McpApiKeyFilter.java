package com.commerceinsight.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * McpApiKeyFilter — authenticates requests from the MCP server using a static API key.
 *
 * <p>The MCP server sends the header: {@code X-MCP-API-KEY: {secret}}.
 * If the key matches the configured value, the request is granted the MCP_SERVICE role,
 * which provides read-only access to analytics, products, orders, etc.
 *
 * <p>This filter runs BEFORE {@link JwtAuthenticationFilter} in the chain.
 * It only activates for requests to paths matching /api/v1/** that carry the MCP header.
 *
 * <p>Architecture Rule: MCP authentication is separate from user JWT authentication.
 * The MCP service role cannot create, update, or delete business entities.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpApiKeyFilter extends OncePerRequestFilter {

    private static final String MCP_API_KEY_HEADER = "X-MCP-API-KEY";
    private static final String MCP_SERVICE_ROLE = "ROLE_MCP_SERVICE";

    @Value("${app.mcp.api-key}")
    private String configuredApiKey;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String providedKey = request.getHeader(MCP_API_KEY_HEADER);

        if (StringUtils.hasText(providedKey) && providedKey.equals(configuredApiKey)) {
            // Valid MCP API key — grant MCP_SERVICE authentication
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            "mcp-service",
                            null,
                            List.of(new SimpleGrantedAuthority(MCP_SERVICE_ROLE))
                    );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("MCP service authenticated for: {} {}", request.getMethod(), request.getRequestURI());
        } else if (StringUtils.hasText(providedKey)) {
            // Key was provided but wrong — log a security warning
            log.warn("Invalid MCP API key received from IP: {}",
                    request.getRemoteAddr());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Skip this filter for non-API paths (e.g., actuator, swagger).
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return !path.startsWith("/api/");
    }
}
