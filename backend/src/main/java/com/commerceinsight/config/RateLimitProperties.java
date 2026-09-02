package com.commerceinsight.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * RateLimitProperties — binds {@code app.rate-limit.*}.
 *
 * <p>All limits are per rolling window and applied in-memory (bucket4j), keyed
 * by resolved client IP (auth routes) or authenticated principal
 * (import/export). Disabled entirely in the {@code test} profile.
 */
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
@Getter
@Setter
public class RateLimitProperties {

    /** Master switch. */
    private boolean enabled = true;

    private final Rule login = new Rule(5, 60, 20, 3600);
    private final Rule register = new Rule(5, 3600, 0, 0);
    private final Rule refresh = new Rule(30, 60, 0, 0);
    private final Rule importUpload = new Rule(10, 60, 0, 0);
    private final Rule export = new Rule(10, 60, 0, 0);
    /** AI insights generation — expensive LLM call, kept deliberately low. */
    private final Rule aiInsights = new Rule(10, 3600, 0, 0);

    /**
     * A rate rule: {@code capacity} tokens per {@code windowSeconds}, with an
     * optional secondary (burst-ceiling) limit. A rule with capacity &lt;= 0 is
     * treated as "no secondary limit".
     */
    @Getter
    @Setter
    public static class Rule {
        private int capacity;
        private int windowSeconds;
        private int secondaryCapacity;
        private int secondaryWindowSeconds;

        public Rule() {
        }

        public Rule(int capacity, int windowSeconds, int secondaryCapacity, int secondaryWindowSeconds) {
            this.capacity = capacity;
            this.windowSeconds = windowSeconds;
            this.secondaryCapacity = secondaryCapacity;
            this.secondaryWindowSeconds = secondaryWindowSeconds;
        }

        public boolean hasSecondary() {
            return secondaryCapacity > 0 && secondaryWindowSeconds > 0;
        }
    }
}
