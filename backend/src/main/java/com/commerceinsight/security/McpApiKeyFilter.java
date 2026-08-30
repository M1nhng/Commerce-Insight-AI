package com.commerceinsight.security;

import com.commerceinsight.shared.dto.ApiResponse;
import com.commerceinsight.shared.dto.ErrorResponse;
import com.commerceinsight.shared.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

/**
 * McpApiKeyFilter — authenticates requests from the MCP server using a static API key.
 *
 * <p>The MCP server sends the header {@code X-MCP-API-KEY: {secret}}. A matching
 * key grants the synthetic {@code ROLE_MCP_SERVICE} authority (read-only reach —
 * it can never satisfy a {@code hasRole}/{@code hasAnyRole} business check).
 *
 * <p>Runs BEFORE {@link JwtAuthenticationFilter}. Only active for {@code /api/**}.
 *
 * <p>Sprint 12A hardening:
 * <ul>
 *   <li>Constant-time key comparison ({@link MessageDigest#isEqual}).</li>
 *   <li><b>Fail closed</b> — a present-but-wrong key returns 401 (enveloped) and
 *       stops the chain, instead of silently continuing unauthenticated.</li>
 *   <li>A <b>missing</b> key is still a no-op pass-through so normal JWT traffic
 *       is unaffected.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class McpApiKeyFilter extends OncePerRequestFilter {

    private static final String MCP_API_KEY_HEADER = "X-MCP-API-KEY";
    private static final String MCP_SERVICE_ROLE = "ROLE_MCP_SERVICE";

    private final ObjectMapper objectMapper;

    @Value("${app.mcp.api-key}")
    private String configuredApiKey;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        String providedKey = request.getHeader(MCP_API_KEY_HEADER);

        if (!StringUtils.hasText(providedKey)) {
            // No MCP key — leave authentication to the JWT filter / entry point.
            filterChain.doFilter(request, response);
            return;
        }

        if (constantTimeEquals(providedKey, configuredApiKey)) {
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            "mcp-service",
                            null,
                            List.of(new SimpleGrantedAuthority(MCP_SERVICE_ROLE))
                    );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("MCP service authenticated for: {} {}", request.getMethod(), request.getRequestURI());
            filterChain.doFilter(request, response);
            return;
        }

        // Key present but invalid — reject immediately (fail closed).
        log.warn("Invalid MCP API key received from IP: {} ({} {})",
                request.getRemoteAddr(), request.getMethod(), request.getRequestURI());
        SecurityContextHolder.clearContext();
        writeUnauthorized(response);
    }

    private static boolean constantTimeEquals(String provided, String expected) {
        if (expected == null) {
            return false;
        }
        byte[] a = provided.getBytes(StandardCharsets.UTF_8);
        byte[] b = expected.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(a, b);
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        ApiResponse<Void> body = ApiResponse.error(ErrorResponse.of(
                ErrorCode.MCP_INVALID_API_KEY.name(),
                "Invalid MCP API key."));
        objectMapper.writeValue(response.getOutputStream(), body);
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
