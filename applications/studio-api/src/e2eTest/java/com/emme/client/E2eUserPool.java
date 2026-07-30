package com.emme.client;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Multi-user pool for E2E REST tests.
 * Logs in users on-demand — only when {@link #acquire()} or
 * {@link #acquire(Role, String)} needs a role/tenant combination
 * that isn't already in the pool.
 * Each TestUser carries its own token, role, and tenant context.
 */
public final class E2eUserPool {

    public static final E2eUserPool INSTANCE = new E2eUserPool();

    private static final long POOL_TIMEOUT_MS = Long.getLong("e2e.pool.timeout.ms", 10_000);
    private static final long TOKEN_REFRESH_BEFORE_MS = 60_000;

    private final Queue<TestUser> available = new ConcurrentLinkedQueue<>();
    private final Set<String> inUse = Collections.synchronizedSet(new HashSet<>());
    private final HttpClient http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10)).build();

    private E2eUserPool() {
        // No pre-init — users are logged in on-demand
    }

    // ── On-Demand Login ──

    private TestUser loginOnDemand(Role role, String tenantId) {
        for (var pu : PoolUser.values()) {
            if (pu.role == role && (tenantId.isEmpty() || pu.tenantId.toString().equals(tenantId))) {
                var user = login(pu);
                if (user != null) {
                    inUse.add(user.userId());
                    return user;
                }
            }
        }
        return null;
    }

    // ── Acquire (no args) ──

    public synchronized TestUser acquire() {
        if (!available.isEmpty()) {
            var user = available.poll();
            if (user.expiresAt() - System.currentTimeMillis() < TOKEN_REFRESH_BEFORE_MS) {
                var pu = PoolUser.valueOf(user.userId());
                var fresh = login(pu);
                if (fresh != null) user = fresh;
            }
            inUse.add(user.userId());
            return user;
        }
        // Login admin on demand as default
        var user = loginOnDemand(Role.PLATFORM_ADMIN, "");
        if (user != null) return user;
        // Fallback: login any pool user
        for (var pu : PoolUser.values()) {
            var u = login(pu);
            if (u != null) { inUse.add(u.userId()); return u; }
        }
        throw new RuntimeException("No users could be logged in. Is the backend running?");
    }

    // ── Acquire (with role/tenant) ──

    /** Acquire a user matching the given role (any tenant). */
    public synchronized TestUser acquire(Role role) {
        return acquire(role, "");
    }

    /** Acquire a user matching the given role and tenant. Empty tenantId = any. */
    public synchronized TestUser acquire(Role role, String tenantId) {
        // First, check if user is already in the pool
        for (var user : new java.util.ArrayList<>(available)) {
            if (user.role() == role && (tenantId.isEmpty() || user.tenantId().equals(tenantId))) {
                if (!available.remove(user)) continue;
                if (user.expiresAt() - System.currentTimeMillis() < TOKEN_REFRESH_BEFORE_MS) {
                    var pu = PoolUser.valueOf(user.userId());
                    var fresh = login(pu);
                    if (fresh != null) user = fresh;
                }
                inUse.add(user.userId());
                return user;
            }
        }
        // Not in pool — login on demand
        var user = loginOnDemand(role, tenantId);
        if (user != null) return user;
        // Wait for release if user is currently in-use
        long startedAt = System.currentTimeMillis();
        while (System.currentTimeMillis() - startedAt < POOL_TIMEOUT_MS) {
            for (var u : new java.util.ArrayList<>(available)) {
                if (u.role() == role && (tenantId.isEmpty() || u.tenantId().equals(tenantId))) {
                    if (!available.remove(u)) continue;
                    inUse.add(u.userId());
                    return u;
                }
            }
            try { Thread.sleep(100); } catch (InterruptedException e) { Thread.currentThread().interrupt(); break; }
        }
        throw new RuntimeException("No user matching " + role + "@" +
            (tenantId.isEmpty() ? "*" : tenantId) + " available after " + POOL_TIMEOUT_MS + "ms");
    }

    // ── Release ──

    public synchronized void release(String userId) {
        inUse.remove(userId);
        for (var pu : PoolUser.values()) {
            if (pu.name().equals(userId)) {
                loginAndEnqueue(pu);
                return;
            }
        }
    }

    // ── Internal helpers ──

    private void loginAndEnqueue(PoolUser pu) {
        var testUser = login(pu);
        if (testUser != null) {
            available.add(testUser);
            System.out.printf("[E2eUserPool] ✓ %s logged in as %s%n", pu.email, pu.role);
        }
    }

    private TestUser login(PoolUser pu) {
        try {
            var body = String.format("{\"email\":\"%s\",\"password\":\"%s\"}", pu.email, pu.password);
            var request = HttpRequest.newBuilder()
                .uri(URI.create(E2eTest.baseUrl() + "/api/auth/login"))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            var response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                System.err.printf("[E2eUserPool] Login failed for %s: HTTP %d%n",
                    pu.email, response.statusCode());
                return null;
            }
            var json = response.body();
            var token = extractJson(json, "accessToken");
            if (token == null) {
                System.err.printf("[E2eUserPool] No accessToken for %s%n", pu.email);
                return null;
            }
            return new TestUser(
                pu.name(), pu.tenantId.toString(), pu.name(),
                pu.email, token, pu.role,
                System.currentTimeMillis() + 3_600_000);
        } catch (IOException | InterruptedException e) {
            System.err.printf("[E2eUserPool] Login error for %s: %s%n", pu.email, e.getMessage());
            return null;
        }
    }

    private static String extractJson(String json, String key) {
        var search = "\"" + key + "\":\"";
        var start = json.indexOf(search);
        if (start < 0) return null;
        start += search.length();
        var end = json.indexOf("\"", start);
        return end > start ? json.substring(start, end) : null;
    }

    // ── Public types ──

    public record TestUser(
        String userId, String tenantId, String name, String email,
        String token, Role role, long expiresAt) {}

    public List<String> inUseIds() { return new ArrayList<>(inUse); }
}
