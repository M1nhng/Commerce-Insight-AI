package com.commerceinsight.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JwtAuthenticationFilter — validates the JWT Bearer token on every incoming request.
 *
 * <p>Filter logic (per-request):
 * <ol>
 *   <li>Extract "Authorization" header.</li>
 *   <li>If missing or not "Bearer ..." → skip (let Spring Security deny the request).</li>
 *   <li>Extract the token string and parse the subject (user UUID).</li>
 *   <li>Load the user from the database via UserDetailsService.</li>
 *   <li>Validate token signature and expiry against the loaded user.</li>
 *   <li>If valid, populate SecurityContext with the authentication object.</li>
 * </ol>
 *
 * <p>On any failure: clear SecurityContext and continue the filter chain.
 * The request will be rejected by Spring Security's authorization layer (401/403).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenUtil jwtTokenUtil;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        final String token = extractBearerToken(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String userId = jwtTokenUtil.extractSubject(token);

            // Only process if SecurityContext is not already populated
            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userId);

                // Reject a still-valid token whose user has since been locked or
                // deactivated — closes the ≤ access-token-TTL window where a
                // just-disabled user would otherwise keep access.
                if (!userDetails.isEnabled() || !userDetails.isAccountNonLocked()) {
                    log.debug("JWT rejected: user '{}' is disabled or locked", userId);
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                if (jwtTokenUtil.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );
                    authentication.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request)
                    );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.debug("Authenticated user '{}' for request: {} {}",
                            userId, request.getMethod(), request.getRequestURI());
                }
            }
        } catch (UsernameNotFoundException ex) {
            log.warn("JWT references non-existent user: {}", ex.getMessage());
            SecurityContextHolder.clearContext();
        } catch (Exception ex) {
            log.debug("JWT processing failed for {}: {}", request.getRequestURI(), ex.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract the Bearer token from the Authorization header.
     *
     * @param request the HTTP request
     * @return the token string, or null if not present or malformed
     */
    private String extractBearerToken(HttpServletRequest request) {
        String header = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
