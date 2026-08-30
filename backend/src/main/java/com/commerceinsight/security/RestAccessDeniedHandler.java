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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * RestAccessDeniedHandler — writes the standard {@link ApiResponse} error
 * envelope for authenticated-but-forbidden requests that are rejected at the
 * filter / request-matcher layer.
 *
 * <p>Method-security denials ({@code @PreAuthorize}) throw
 * {@link AccessDeniedException} through the dispatcher and are handled by
 * {@code GlobalExceptionHandler} with the identical envelope; this handler
 * covers the filter-layer path so both are consistent.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        log.debug("Access denied for {} {}", request.getMethod(), request.getRequestURI());

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");

        ApiResponse<Void> body = ApiResponse.error(ErrorResponse.of(
                ErrorCode.ACCESS_DENIED.name(),
                "You do not have permission to perform this action."));
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
