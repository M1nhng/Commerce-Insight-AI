package com.commerceinsight.analytics.ai.llm;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A tiny in-process HTTP server (JDK {@code com.sun.net.httpserver}) used to
 * exercise the real HTTP path of the LLM providers without any external API.
 * Captures the last request (method, path, headers, body) and replies with a
 * scripted status / body / delay.
 */
final class MockLlmHttpServer implements AutoCloseable {

    private final HttpServer server;
    final AtomicReference<String> lastPath = new AtomicReference<>();
    final AtomicReference<String> lastBody = new AtomicReference<>();
    final Map<String, String> lastHeaders = new HashMap<>();

    private volatile int status = 200;
    private volatile String responseBody = "{}";
    private volatile long delayMs = 0;

    private MockLlmHttpServer(HttpServer server) {
        this.server = server;
    }

    static MockLlmHttpServer start() throws IOException {
        HttpServer s = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        MockLlmHttpServer mock = new MockLlmHttpServer(s);
        s.createContext("/", mock::handle);
        s.setExecutor(null);
        s.start();
        return mock;
    }

    int port() {
        return server.getAddress().getPort();
    }

    String baseUrl() {
        return "http://127.0.0.1:" + port();
    }

    MockLlmHttpServer respondWith(int status, String body) {
        this.status = status;
        this.responseBody = body;
        return this;
    }

    MockLlmHttpServer withDelay(long ms) {
        this.delayMs = ms;
        return this;
    }

    private void handle(HttpExchange ex) throws IOException {
        lastPath.set(ex.getRequestURI().getPath());
        ex.getRequestHeaders().forEach((k, v) -> lastHeaders.put(k.toLowerCase(), String.join(",", v)));
        lastBody.set(new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        byte[] out = responseBody.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, out.length);
        ex.getResponseBody().write(out);
        ex.close();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
