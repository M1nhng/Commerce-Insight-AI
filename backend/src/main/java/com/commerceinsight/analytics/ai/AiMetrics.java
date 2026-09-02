package com.commerceinsight.analytics.ai;

import com.commerceinsight.analytics.ai.llm.LlmFailureReason;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * AiMetrics — thin wrapper over the Micrometer {@link MeterRegistry} that
 * Spring Boot Actuator already provides (no new dependency). All AI-insights
 * counters and the latency timer live here so the tag vocabulary stays
 * consistent and safe.
 *
 * <p>Tags are strictly low-cardinality and non-sensitive:
 * <ul>
 *   <li>{@code provider} — {@code openai} / {@code anthropic} / {@code none};</li>
 *   <li>{@code model}    — a configuration constant;</li>
 *   <li>{@code result}   — {@code success} / {@code unavailable} / {@code validation_error};</li>
 *   <li>{@code reason}   — a {@link LlmFailureReason} name (provider failures only).</li>
 * </ul>
 * Never tagged: user id, email, customer id, IP, prompt, request body.
 */
@Component
public class AiMetrics {

    private final MeterRegistry registry;

    public AiMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /** Every accepted generation request (before provider selection). */
    public void request() {
        registry.counter("ai.insights.requests").increment();
    }

    /** Range / bean-validation rejection (maps to HTTP 400). */
    public void validationFailure() {
        registry.counter("ai.insights.validation_failures").increment();
    }

    /** Provider returned a usable, structured response. */
    public void success(String provider, String model, Duration latency) {
        registry.counter("ai.insights.success", "provider", nz(provider), "model", nz(model)).increment();
        Timer.builder("ai.insights.latency")
                .tag("provider", nz(provider)).tag("model", nz(model)).tag("result", "success")
                .register(registry)
                .record(latency);
    }

    /** Feature off / unconfigured / context-build failure — HTTP 200 available:false. */
    public void unavailable(String provider, String model) {
        registry.counter("ai.insights.unavailable", "provider", nz(provider), "model", nz(model)).increment();
    }

    /** A provider call failed — HTTP 200 available:false, tagged by safe reason. */
    public void providerFailure(String provider, String model, LlmFailureReason reason, Duration latency) {
        String r = reason == null ? LlmFailureReason.PROVIDER_ERROR.name() : reason.name();
        registry.counter("ai.insights.provider_failures",
                "provider", nz(provider), "model", nz(model), "reason", r).increment();
        Timer.builder("ai.insights.latency")
                .tag("provider", nz(provider)).tag("model", nz(model)).tag("result", "provider_failure")
                .register(registry)
                .record(latency);
    }

    private static String nz(String s) {
        return (s == null || s.isBlank()) ? "unknown" : s;
    }
}
