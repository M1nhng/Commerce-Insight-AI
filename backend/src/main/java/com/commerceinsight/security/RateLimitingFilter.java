package com.commerceinsight.security;

import com.commerceinsight.config.RateLimitProperties;
import com.commerceinsight.config.RateLimitProperties.Rule;
import com.commerceinsight.shared.dto.ApiResponse;
import com.commerceinsight.shared.dto.ErrorResponse;
import com.commerceinsight.shared.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.local.LocalBucketBuilder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RateLimitingFilter — in-memory (bucket4j) throttling for sensitive endpoints.
 * No Redis; single-instance scope.
 *
 * <p>Keyed by resolved client IP for the auth routes, and by authenticated
 * principal (falling back to IP) for import/export. Limits come from
 * {@link RateLimitProperties} ({@code app.rate-limit.*}). Exceeded → HTTP 429
 * with the standard error envelope ({@code RATE_LIMIT_EXCEEDED}) and a
 * {@code Retry-After} header. Disabled when {@code app.rate-limit.enabled=false}
 * (the {@code test} profile).
 *
 * <p>This filter is IP-scoped. Per-account brute-force is separately bounded by
 * the existing 5-attempt account lockout in
 * {@link com.commerceinsight.auth.service.AuthService}.
 *
 * <p>Runs before {@link McpApiKeyFilter}, so an unauthenticated flood on
 * {@code /auth/**} is rejected before any auth work.
 */
@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private enum Group { LOGIN, REGISTER, REFRESH, IMPORT, EXPORT }

    private final RateLimitProperties props;
    private final ObjectMapper objectMapper;
    private final ClientIpResolver clientIpResolver;

    /** key = "<GROUP>|<identity>" */
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public RateLimitingFilter(RateLimitProperties props,
                              ObjectMapper objectMapper,
                              ClientIpResolver clientIpResolver) {
        this.props = props;
        this.objectMapper = objectMapper;
        this.clientIpResolver = clientIpResolver;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !props.isEnabled() || groupFor(request) == null;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        Group group = groupFor(request);
        if (group == null) {
            filterChain.doFilter(request, response);
            return;
        }

        String identity = identityFor(group, request);
        ConsumptionProbe probe = bucket(group, identity).tryConsumeAndReturnRemaining(1);

        if (!probe.isConsumed()) {
            long retryAfter = Math.max(1, probe.getNanosToWaitForRefill() / 1_000_000_000L);
            log.warn("Rate limit hit: group={} identity={} path={} retryAfter={}s",
                    group, identity, request.getRequestURI(), retryAfter);
            writeTooManyRequests(response, retryAfter);
            return;
        }
        filterChain.doFilter(request, response);
    }

    // ── Bucket management ──────────────────────────────────────────────────

    private Bucket bucket(Group group, String identity) {
        return buckets.computeIfAbsent(group + "|" + identity, k -> {
            Rule rule = ruleFor(group);
            LocalBucketBuilder b = Bucket.builder()
                    .addLimit(bandwidth(rule.getCapacity(), rule.getWindowSeconds()));
            if (rule.hasSecondary()) {
                b.addLimit(bandwidth(rule.getSecondaryCapacity(), rule.getSecondaryWindowSeconds()));
            }
            return b.build();
        });
    }

    private static Bandwidth bandwidth(int capacity, int windowSeconds) {
        return Bandwidth.builder()
                .capacity(capacity)
                .refillGreedy(capacity, Duration.ofSeconds(windowSeconds))
                .build();
    }

    private Rule ruleFor(Group group) {
        return switch (group) {
            case LOGIN -> props.getLogin();
            case REGISTER -> props.getRegister();
            case REFRESH -> props.getRefresh();
            case IMPORT -> props.getImportUpload();
            case EXPORT -> props.getExport();
        };
    }

    // ── Route → group / identity ──────────────────────────────────────────

    private Group groupFor(HttpServletRequest request) {
        String path = requestPath(request);
        String method = request.getMethod();
        if (HttpMethod.POST.matches(method)) {
            if (path.equals("/api/v1/auth/login")) return Group.LOGIN;
            if (path.equals("/api/v1/auth/register")) return Group.REGISTER;
            if (path.equals("/api/v1/auth/refresh")) return Group.REFRESH;
            if (path.startsWith("/api/v1/import/")) return Group.IMPORT;
        }
        if (HttpMethod.GET.matches(method) && path.startsWith("/api/v1/export/")) {
            return Group.EXPORT;
        }
        return null;
    }

    /**
     * The request path relative to the context. Uses {@code getRequestURI()}
     * (always populated) rather than {@code getServletPath()}, which is empty
     * under MockMvc and can be empty for a root ("/") servlet mapping — that
     * emptiness silently disabled this filter.
     */
    private static String requestPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isEmpty() && uri.startsWith(ctx)) {
            uri = uri.substring(ctx.length());
        }
        return uri;
    }

    private String identityFor(Group group, HttpServletRequest request) {
        if (group == Group.IMPORT || group == Group.EXPORT) {
            String principal = currentPrincipal();
            if (principal != null) {
                return "user:" + principal;
            }
        }
        return "ip:" + clientIpResolver.resolve(request);
    }

    private static String currentPrincipal() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        String name = auth.getName();
        return "anonymousUser".equals(name) ? null : name;
    }

    private void writeTooManyRequests(HttpServletResponse response, long retryAfterSeconds) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
        response.setHeader("Cache-Control", "no-store");
        ApiResponse<Void> body = ApiResponse.error(ErrorResponse.of(
                ErrorCode.RATE_LIMIT_EXCEEDED.name(),
                "Too many requests. Please retry after " + retryAfterSeconds + " second(s)."));
        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
