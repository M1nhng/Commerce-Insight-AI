package com.commerceinsight.security;

import com.commerceinsight.config.RateLimitProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * RateLimitingFilterTest — window enforcement, per-identity isolation, and the
 * master disable switch.
 */
@DisplayName("RateLimitingFilter Unit Tests")
class RateLimitingFilterTest {

    private RateLimitProperties props;
    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        props = new RateLimitProperties();
        props.setEnabled(true);
        // 2 requests / 60s for login, no secondary
        props.getLogin().setCapacity(2);
        props.getLogin().setWindowSeconds(60);
        props.getLogin().setSecondaryCapacity(0);
        props.getLogin().setSecondaryWindowSeconds(0);
        filter = new RateLimitingFilter(props, new ObjectMapper().findAndRegisterModules(),
                new ClientIpResolver(""));
    }

    private MockHttpServletRequest login(String ip) {
        MockHttpServletRequest r = new MockHttpServletRequest("POST", "/api/v1/auth/login");
        r.setServletPath("/api/v1/auth/login");
        r.setRemoteAddr(ip);
        return r;
    }

    @Test
    @DisplayName("blocks the (capacity + 1)th request in the window with 429 + Retry-After")
    void blocksOverCapacity() throws Exception {
        for (int i = 0; i < 2; i++) {
            MockHttpServletResponse ok = new MockHttpServletResponse();
            filter.doFilter(login("203.0.113.10"), ok, new MockFilterChain());
            assertThat(ok.getStatus()).isEqualTo(200);
        }
        MockHttpServletResponse blocked = new MockHttpServletResponse();
        filter.doFilter(login("203.0.113.10"), blocked, new MockFilterChain());

        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getHeader("Retry-After")).isNotNull();
        assertThat(blocked.getContentAsString()).contains("RATE_LIMIT_EXCEEDED");
    }

    @Test
    @DisplayName("different client IPs get independent buckets")
    void perIpIsolation() throws Exception {
        for (int i = 0; i < 2; i++) {
            filter.doFilter(login("198.51.100.1"), new MockHttpServletResponse(), new MockFilterChain());
        }
        MockHttpServletResponse other = new MockHttpServletResponse();
        filter.doFilter(login("198.51.100.2"), other, new MockFilterChain());
        assertThat(other.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("disabled → shouldNotFilter true, nothing is throttled")
    void disabledSwitch() throws Exception {
        props.setEnabled(false);
        MockHttpServletRequest req = login("203.0.113.99");
        assertThat(filter.shouldNotFilter(req)).isTrue();
        for (int i = 0; i < 10; i++) {
            MockHttpServletResponse res = new MockHttpServletResponse();
            filter.doFilter(login("203.0.113.99"), res, new MockFilterChain());
            assertThat(res.getStatus()).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("non-matching route is not throttled")
    void unmatchedRoute() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/v1/products");
        req.setServletPath("/api/v1/products");
        assertThat(filter.shouldNotFilter(req)).isTrue();
    }
}
