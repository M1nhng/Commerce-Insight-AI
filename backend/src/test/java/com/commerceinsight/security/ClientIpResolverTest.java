package com.commerceinsight.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ClientIpResolverTest — X-Forwarded-For is trusted only from a configured proxy.
 */
@DisplayName("ClientIpResolver Unit Tests")
class ClientIpResolverTest {

    private MockHttpServletRequest request(String remoteAddr, String xff) {
        MockHttpServletRequest r = new MockHttpServletRequest();
        r.setRemoteAddr(remoteAddr);
        if (xff != null) {
            r.addHeader("X-Forwarded-For", xff);
        }
        return r;
    }

    @Test
    @DisplayName("no trusted proxies configured → always the peer address")
    void noTrustedProxies_usesPeer() {
        ClientIpResolver resolver = new ClientIpResolver("");
        assertThat(resolver.resolve(request("203.0.113.9", "1.2.3.4"))).isEqualTo("203.0.113.9");
    }

    @Test
    @DisplayName("peer is NOT a trusted proxy → XFF ignored")
    void untrustedPeer_ignoresXff() {
        ClientIpResolver resolver = new ClientIpResolver("10.0.0.0/8");
        assertThat(resolver.resolve(request("203.0.113.9", "1.2.3.4"))).isEqualTo("203.0.113.9");
    }

    @Test
    @DisplayName("peer IS a trusted proxy → first XFF hop is used")
    void trustedPeer_usesFirstXffHop() {
        ClientIpResolver resolver = new ClientIpResolver("10.0.0.0/8, 192.168.0.0/16");
        assertThat(resolver.resolve(request("10.1.2.3", "198.51.100.7, 10.1.2.3")))
                .isEqualTo("198.51.100.7");
    }

    @Test
    @DisplayName("trusted peer but no XFF → falls back to X-Real-IP then peer")
    void trustedPeer_noXff_fallsBack() {
        ClientIpResolver resolver = new ClientIpResolver("127.0.0.1/32");
        MockHttpServletRequest r = request("127.0.0.1", null);
        assertThat(resolver.resolve(r)).isEqualTo("127.0.0.1");
        r.addHeader("X-Real-IP", "198.51.100.42");
        assertThat(resolver.resolve(r)).isEqualTo("198.51.100.42");
    }

    @Test
    @DisplayName("exact-IP trusted-proxy entry (no CIDR) works")
    void exactIpEntry() {
        ClientIpResolver resolver = new ClientIpResolver("172.18.0.5");
        assertThat(resolver.resolve(request("172.18.0.5", "203.0.113.1"))).isEqualTo("203.0.113.1");
        assertThat(resolver.resolve(request("172.18.0.6", "203.0.113.1"))).isEqualTo("172.18.0.6");
    }
}
