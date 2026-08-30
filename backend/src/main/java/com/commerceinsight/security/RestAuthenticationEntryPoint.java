package com.commerceinsight.security;

import com.commerceinsight.shared.dto.ApiResponse;
import com.commerceinsight.shared.dto.ErrorResponse;
import com.commerceinsight.shared.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * RestAuthenticationEntryPoint — writes the standard {@link ApiResponse} error
 * envelope for unauthenticated requests instead of an empty-body 401.
 *
 * <p>Before Sprint 12A the filter chain used {@code HttpStatusEntryPoint(401)},
 * which returned a bare status with no body — inconsistent with every other
 * error in the API. This entry point emits:
 * <pre>
 * { "success": false, "error": { "code": "AUTHENTICATION_REQUIRED",
 *   "message": "Authentication is required." }, "timestamp": "..." }
 * </pre>
 *
 * <p>No token / credential details are ever included.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        log.debug("Unauthenticated request to {} {}", request.getMethod(), request.getRequestURI());

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");

        ApiResponse<Void> body = ApiResponse.error(ErrorResponse.of(
                ErrorCode.AUTHENTICATION_REQUIRED.name(),
                "Authentication is required."));
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
