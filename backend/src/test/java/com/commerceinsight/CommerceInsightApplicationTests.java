package com.commerceinsight;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Application context smoke test.
 * Verifies the Spring context loads without errors.
 */
@SpringBootTest
@ActiveProfiles("test")
class CommerceInsightApplicationTests {

    @Test
    void contextLoads() {
        // Smoke test — verifies Spring context loads successfully.
        // Domain-specific tests will be added per module.
    }
}
