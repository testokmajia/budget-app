package com.techmanage.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.scheduling.annotation.Scheduled;

/**
 * Simple in-memory rate limiter for the login endpoint.
 * For production, replace with Redis-backed rate limiting.
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final int maxAttempts;
    private final long windowMinutes;
    private final Set<String> trustedProxies;

    private record AttemptEntry(int count, Instant windowStart) {}

    private final Map<String, AttemptEntry> attempts = new ConcurrentHashMap<>();

    public RateLimitFilter(
            @Value("${app.rate-limit.login-max-attempts:10}") int maxAttempts,
            @Value("${app.rate-limit.login-window-minutes:15}") long windowMinutes,
            @Value("${app.rate-limit.trusted-proxies:127.0.0.1,0:0:0:0:0:0:0:1}") String trustedProxyList) {
        this.maxAttempts = maxAttempts;
        this.windowMinutes = windowMinutes;
        this.trustedProxies = new HashSet<>(Arrays.asList(trustedProxyList.split(",")));
        // Remove empty entries
        this.trustedProxies.removeIf(String::isBlank);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        if (!"/api/auth/login".equals(request.getRequestURI())
                || !"POST".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        Instant now = Instant.now();
        AttemptEntry entry = attempts.compute(clientIp, (key, existing) -> {
            if (existing == null || existing.windowStart().plusSeconds(windowMinutes * 60).isBefore(now)) {
                return new AttemptEntry(1, now);
            }
            return new AttemptEntry(existing.count() + 1, existing.windowStart());
        });

        // Clean up stale entries periodically
        if (entry.count() == 1) {
            attempts.entrySet().removeIf(e ->
                e.getValue().windowStart().plusSeconds(windowMinutes * 60 * 2).isBefore(now));
        }

        if (entry.count() > maxAttempts) {
            log.warn("Rate limit exceeded for IP: {} ({} attempts in {} min)",
                    clientIp, entry.count(), windowMinutes);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(
                "{\"success\":false,\"error\":\"登录尝试过于频繁，请" + windowMinutes + "分钟后再试\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Resolve the client IP for rate-limiting purposes.
     *
     * X-Forwarded-For and X-Real-IP are client-controlled headers and can be
     * spoofed by an attacker. We only trust them when the immediate TCP peer
     * (getRemoteAddr()) is a known reverse proxy, configured via
     * {@code app.rate-limit.trusted-proxies}.
     *
     * When NOT behind a reverse proxy, getRemoteAddr() returns the real client
     * IP and is the only safe source.
     */
    private String getClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        // Only trust proxy headers when the immediate peer is a known proxy
        if (trustedProxies.contains(remoteAddr)) {
            // X-Forwarded-For: client, proxy1, proxy2, ...
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                // The leftmost IP is the original client (per convention)
                String clientIp = xForwardedFor.split(",")[0].trim();
                if (!clientIp.isEmpty()) {
                    return clientIp;
                }
            }
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isBlank()) {
                return xRealIp.trim();
            }
        }

        return remoteAddr;
    }

    /**
     * Periodically evict expired entries to prevent unbounded map growth.
     * Runs every 10 minutes.
     */
    @Scheduled(fixedDelay = 600_000)
    public void evictExpiredEntries() {
        Instant now = Instant.now();
        attempts.entrySet().removeIf(e ->
            e.getValue().windowStart().plusSeconds(windowMinutes * 60 * 2).isBefore(now));
    }
}
