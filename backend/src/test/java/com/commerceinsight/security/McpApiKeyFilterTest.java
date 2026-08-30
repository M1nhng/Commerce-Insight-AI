package com.commerceinsight.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * McpApiKeyFilterTest — constant-time compare, fail-closed on a wrong key,
 * pass-through when the header is absent.
 */
@DisplayName("McpApiKeyFilter Unit Tests")
class McpApiKeyFilterTest {

    private static final String KEY = "the-real-mcp-key-value";

    private McpApiKeyFilter filter;

    @BeforeEach
    void setUp() {
        filter = new McpApiKeyFilter(new ObjectMapper().findAndRegisterModules());
        ReflectionTestUtils.setField(filter, "configuredApiKey", KEY);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private MockHttpServletRequest apiRequest() {
        MockHttpServletRequest r = new MockHttpServletRequest("GET", "/api/v1/products");
        r.setServletPath("/api/v1/products");
        return r;
    }

    @Test
    @DisplayName("valid key → ROLE_MCP_SERVICE authentication, chain continues")
    void validKey_authenticates() throws Exception {
        MockHttpServletRequest req = apiRequest();
        req.addHeader("X-MCP-API-KEY", KEY);
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString).containsExactly("ROLE_MCP_SERVICE");
        assertThat(chain.getRequest()).isNotNull(); // chain proceeded
        assertThat(res.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("wrong key → 401 envelope, chain stopped, context cleared")
    void wrongKey_failsClosed() throws Exception {
        MockHttpServletRequest req = apiRequest();
        req.addHeader("X-MCP-API-KEY", "not-the-key");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        verify(chain, never()).doFilter(req, res);
        assertThat(res.getStatus()).isEqualTo(401);
        assertThat(res.getContentAsString()).contains("MCP_INVALID_API_KEY");
        assertThat(res.getContentAsString()).doesNotContain(KEY);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    @DisplayName("no key header → pass-through, no authentication set")
    void noKey_passesThrough() throws Exception {
        MockHttpServletRequest req = apiRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(req, res, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(chain.getRequest()).isNotNull();
    }

    @Test
    @DisplayName("filter is skipped for non-/api paths")
    void nonApiPath_skipped() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/actuator/health");
        req.setServletPath("/actuator/health");
        assertThat(filter.shouldNotFilter(req)).isTrue();
    }
}
