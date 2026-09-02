package com.commerceinsight.analytics.ai.llm;

import com.commerceinsight.analytics.ai.config.AiProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * LlmClient — the single seam the AI analytics layer depends on. It is the
 * "registry": it owns the list of {@link LlmProvider} beans and resolves the
 * one named by {@code app.ai.provider}. Callers never learn which concrete
 * provider ran.
 *
 * <p>{@link #isAvailable()} lets the feature degrade gracefully: when it returns
 * {@code false} the service short-circuits to an "unavailable" response and no
 * network call is attempted.
 */
@Slf4j
@Component
public class LlmClient {

    private final AiProperties props;
    private final List<LlmProvider> providers;

    public LlmClient(AiProperties props, List<LlmProvider> providers) {
        this.props = props;
        this.providers = providers;
    }

    /** True when the feature is enabled and a matching, configured provider exists. */
    public boolean isAvailable() {
        if (!props.isEnabled()) {
            return false;
        }
        return resolve().map(LlmProvider::isConfigured).orElse(false);
    }

    /**
     * Run one completion through the configured provider.
     *
     * @throws LlmException when no provider is available or the provider call fails
     */
    public LlmCompletion complete(LlmRequest request) {
        LlmProvider provider = resolve()
                .orElseThrow(() -> new LlmException(LlmFailureReason.NOT_CONFIGURED,
                        "No AI provider matches '" + props.getProvider() + "'"));
        if (!provider.isConfigured()) {
            throw new LlmException(LlmFailureReason.NOT_CONFIGURED,
                    "The configured AI provider is not usable");
        }
        return provider.complete(request);
    }

    /** The id of the provider that would serve a request, or {@code "none"}. */
    public String activeProviderId() {
        return resolve().map(LlmProvider::id).orElse("none");
    }

    private java.util.Optional<LlmProvider> resolve() {
        String wanted = props.getProvider() == null ? "" : props.getProvider().trim();
        // Match by supports() (handles aliases like ollama → OpenAI-compatible).
        // Fall back to the sole provider only when exactly one bean exists.
        return providers.stream()
                .filter(p -> p.supports(wanted))
                .findFirst()
                .or(() -> providers.size() == 1 ? java.util.Optional.of(providers.get(0)) : java.util.Optional.empty());
    }
}
