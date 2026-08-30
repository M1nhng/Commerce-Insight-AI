package com.commerceinsight.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * ClientIpResolver — resolves the originating client IP for a request.
 *
 * <p>{@code X-Forwarded-For} / {@code X-Real-IP} are honoured <b>only</b> when
 * the immediate peer ({@link HttpServletRequest#getRemoteAddr()}) is a
 * configured trusted proxy ({@code app.security.trusted-proxies}). Otherwise the
 * peer address is used directly. This prevents a client from spoofing its IP to
 * evade rate limiting or poison audit logs.
 *
 * <p>{@code app.security.trusted-proxies} is a comma-separated list of exact IPs
 * or CIDR prefixes (e.g. {@code 10.0.0.0/8,172.18.0.0/16}). Empty (the default)
 * means "trust nothing" — always use the peer address.
 */
@Slf4j
@Component
public class ClientIpResolver {

    private static final String XFF_HEADER = "X-Forwarded-For";
    private static final String XRI_HEADER = "X-Real-IP";

    private final List<CidrRange> trustedProxies = new ArrayList<>();

    public ClientIpResolver(@Value("${app.security.trusted-proxies:}") String trustedProxiesRaw) {
        if (StringUtils.hasText(trustedProxiesRaw)) {
            for (String entry : trustedProxiesRaw.split(",")) {
                String trimmed = entry.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }
                try {
                    trustedProxies.add(CidrRange.parse(trimmed));
                } catch (RuntimeException ex) {
                    log.warn("Ignoring invalid app.security.trusted-proxies entry: {}", trimmed);
                }
            }
        }
        log.info("ClientIpResolver initialised with {} trusted proxy range(s)", trustedProxies.size());
    }

    /**
     * @return the best-effort client IP, never {@code null} (falls back to the peer address).
     */
    public String resolve(HttpServletRequest request) {
        String peer = request.getRemoteAddr();
        if (!isTrustedProxy(peer)) {
            return peer;
        }

        String forwardedFor = request.getHeader(XFF_HEADER);
        if (StringUtils.hasText(forwardedFor)) {
            // First entry is the original client; the rest are proxies.
            String first = forwardedFor.split(",")[0].trim();
            if (StringUtils.hasText(first)) {
                return first;
            }
        }

        String realIp = request.getHeader(XRI_HEADER);
        if (StringUtils.hasText(realIp)) {
            return realIp.trim();
        }
        return peer;
    }

    private boolean isTrustedProxy(String ip) {
        if (ip == null) {
            return false;
        }
        for (CidrRange range : trustedProxies) {
            if (range.contains(ip)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Minimal IPv4/IPv6 CIDR matcher. An entry without a {@code /} is treated as
     * a /32 (IPv4) or /128 (IPv6) exact match.
     */
    static final class CidrRange {
        private final byte[] network;
        private final int prefixBits;

        private CidrRange(byte[] network, int prefixBits) {
            this.network = network;
            this.prefixBits = prefixBits;
        }

        static CidrRange parse(String cidr) {
            String[] parts = cidr.split("/");
            byte[] addr = toBytes(parts[0].trim());
            int bits = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : addr.length * 8;
            if (bits < 0 || bits > addr.length * 8) {
                throw new IllegalArgumentException("Invalid prefix length: " + cidr);
            }
            return new CidrRange(addr, bits);
        }

        boolean contains(String ip) {
            byte[] addr;
            try {
                addr = toBytes(ip);
            } catch (RuntimeException ex) {
                return false;
            }
            if (addr.length != network.length) {
                return false; // don't cross IPv4/IPv6 families
            }
            int fullBytes = prefixBits / 8;
            for (int i = 0; i < fullBytes; i++) {
                if (addr[i] != network[i]) {
                    return false;
                }
            }
            int remainingBits = prefixBits % 8;
            if (remainingBits == 0) {
                return true;
            }
            int mask = (0xFF00 >> remainingBits) & 0xFF;
            return (addr[fullBytes] & mask) == (network[fullBytes] & mask);
        }

        private static byte[] toBytes(String ip) {
            try {
                return java.net.InetAddress.getByName(ip).getAddress();
            } catch (java.net.UnknownHostException ex) {
                throw new IllegalArgumentException("Not an IP literal: " + ip, ex);
            }
        }
    }
}
