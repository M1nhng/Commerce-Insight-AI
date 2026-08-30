package com.commerceinsight.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * RequestCorrelationFilter — assigns every request a correlation id, exposes it
 * to the logging framework via {@link MDC} ({@code requestId}), and echoes it
 * back on the response.
 *
 * <p>Runs first in the chain so the id is available to every downstream filter,
 * including the rate-limit and security filters. An inbound
 * {@code X-Request-Id} is reused when it looks sane; otherwise a new UUID is
 * generated. The MDC key is always cleared in a {@code finally} block.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    public static final String MDC_KEY = "requestId";

    /** Accept only short, safe inbound ids to avoid log injection. */
    private static final Pattern SAFE_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    private final String headerName;

    public RequestCorrelationFilter(
            @Value("${app.security.request-id-header:X-Request-Id}") String headerName) {
        this.headerName = headerName;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        String inbound = request.getHeader(headerName);
        String requestId = (StringUtils.hasText(inbound) && SAFE_ID.matcher(inbound).matches())
                ? inbound
                : UUID.randomUUID().toString();

        MDC.put(MDC_KEY, requestId);
        response.setHeader(headerName, requestId);
        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
