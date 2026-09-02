package com.commerceinsight.analytics.ai;

import com.commerceinsight.analytics.ai.dto.AiInsightsRequest;
import com.commerceinsight.analytics.ai.dto.AiInsightsResponse;
import com.commerceinsight.shared.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AiAnalyticsController — the AI-assisted narrative layer over the existing
 * analytics endpoints.
 *
 * <p>Base path: {@code /api/v1/analytics/ai-insights}. Same security posture as
 * the rest of {@code /api/v1/analytics/**}: {@code @PreAuthorize("isAuthenticated()")}
 * — any authenticated role (STAFF, MANAGER, ADMIN) that may read analytics may
 * also request AI insights. No new authorization policy is introduced.
 *
 * <p>Thin HTTP adapter only. All work is in {@link AiAnalyticsService}. Envelope,
 * validation and exception handling follow the project conventions
 * ({@link ApiResponse}, {@code @Valid}, {@code GlobalExceptionHandler}).
 *
 * <p>Cost/abuse control: this route is rate-limited per authenticated principal
 * by the existing {@code RateLimitingFilter} ({@code app.rate-limit.ai-insights}).
 */
@RestController
@RequestMapping("/api/v1/analytics/ai-insights")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
@Tag(name = "Analytics", description = "AI-assisted ecommerce analytics insights")
@SecurityRequirement(name = "Bearer Authentication")
public class AiAnalyticsController {

    private final AiAnalyticsService aiAnalyticsService;

    @PostMapping
    @Operation(
            summary = "Generate AI business insights for a period",
            description = """
                    Reasons over the EXISTING analytics aggregates for the given window
                    (revenue, orders, top products, payments, customers, inventory risk)
                    and returns a short summary plus structured insights and
                    recommendations.

                    The AI never sees raw orders or customer PII — only compact
                    aggregates. It is instructed to use only the supplied data and never
                    to invent numbers.

                    If AI is disabled or the provider is unavailable, the response is
                    HTTP 200 with {@code available:false} and empty arrays — the rest of
                    the dashboard is unaffected.
                    """
    )
    public ResponseEntity<ApiResponse<AiInsightsResponse>> generate(
            @Valid @RequestBody AiInsightsRequest request) {

        AiInsightsResponse data = aiAnalyticsService.generate(request.dateFrom(), request.dateTo());
        return ResponseEntity.ok(ApiResponse.success(data,
                data.available()
                        ? "AI insights generated successfully"
                        : "AI insights are currently unavailable"));
    }
}
