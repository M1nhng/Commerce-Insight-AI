package com.commerceinsight.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Set;

/**
 * SecretsValidator — fails fast when the application is started in the
 * {@code prod} profile with a committed development secret or a JWT signing key
 * that is too short for HS256.
 *
 * <p>Rationale: {@code application-prod.yml} intentionally carries no
 * {@code app.*} block, so an operator who forgets to set {@code JWT_SECRET} /
 * {@code MCP_API_KEY} would otherwise silently run production on the values
 * committed to the repo. Outside {@code prod} this only logs a warning.
 *
 * <p>Runs as an {@link ApplicationRunner} so a thrown exception aborts startup
 * with a clear message (no secret value is ever logged).
 */
@Slf4j
@Component
public class SecretsValidator implements ApplicationRunner {

    /** Values committed to application.yml / application-test.yml as dev defaults. */
    private static final Set<String> KNOWN_DEV_SECRETS = Set.of(
            "CommerceInsightAIDevSecretKeyMustBe256BitsLongForHMACSHA256OK",
            "test-secret-key-at-least-32-characters-for-testing-purposes-only",
            "mcp-dev-secret-key-change-in-production",
            "test-mcp-api-key",
            "change-in-production"
    );

    private static final int MIN_JWT_SECRET_BYTES = 32; // 256 bits for HS256

    private final Environment environment;
    private final String jwtSecret;
    private final String mcpApiKey;

    public SecretsValidator(Environment environment,
                            @org.springframework.beans.factory.annotation.Value("${app.jwt.secret}") String jwtSecret,
                            @org.springframework.beans.factory.annotation.Value("${app.mcp.api-key}") String mcpApiKey) {
        this.environment = environment;
        this.jwtSecret = jwtSecret;
        this.mcpApiKey = mcpApiKey;
    }

    @Override
    public void run(ApplicationArguments args) {
        boolean prod = Arrays.asList(environment.getActiveProfiles()).contains("prod");

        boolean jwtIsDevDefault = KNOWN_DEV_SECRETS.contains(jwtSecret);
        boolean mcpIsDevDefault = KNOWN_DEV_SECRETS.contains(mcpApiKey);
        boolean jwtTooShort = decodedKeyLength(jwtSecret) < MIN_JWT_SECRET_BYTES;

        if (prod) {
            StringBuilder problems = new StringBuilder();
            if (jwtIsDevDefault) {
                problems.append("\n  - app.jwt.secret is a committed development value (set JWT_SECRET)");
            }
            if (jwtTooShort) {
                problems.append("\n  - app.jwt.secret is shorter than 256 bits (HS256 requires >= 32 bytes)");
            }
            if (mcpIsDevDefault) {
                problems.append("\n  - app.mcp.api-key is a committed development value (set MCP_API_KEY)");
            }
            if (!problems.isEmpty()) {
                throw new IllegalStateException(
                        "Refusing to start the 'prod' profile with insecure secrets:" + problems
                                + "\nProvide real values via environment variables and restart.");
            }
            log.info("SecretsValidator: production secrets OK.");
            return;
        }

        if (jwtIsDevDefault || mcpIsDevDefault) {
            log.warn("SecretsValidator: running with committed development secret(s). "
                    + "This is expected for local/dev but MUST NOT be used in production "
                    + "(jwtDevDefault={}, mcpDevDefault={}).", jwtIsDevDefault, mcpIsDevDefault);
        }
        if (jwtTooShort) {
            log.warn("SecretsValidator: app.jwt.secret decodes to < 256 bits; HS256 signing will fail.");
        }
    }

    /**
     * Mirrors {@code JwtTokenUtil.getSigningKey()}: try Base64, else raw UTF-8 bytes.
     */
    private static int decodedKeyLength(String secret) {
        if (secret == null) {
            return 0;
        }
        try {
            return io.jsonwebtoken.io.Decoders.BASE64.decode(secret).length;
        } catch (RuntimeException ex) {
            return secret.getBytes(StandardCharsets.UTF_8).length;
        }
    }
}
