package com.busbooking.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * IP-based rate limiting filter using Bucket4j (token-bucket algorithm).
 * <p>
 * Applies a stricter limit to authentication endpoints to prevent
 * brute-force attacks, and a general limit to all other endpoints.
 * </p>
 */
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final Map<String, Bucket> generalBuckets = new ConcurrentHashMap<>();
    private final Map<String, Bucket> authBuckets = new ConcurrentHashMap<>();

    @Value("${app.rate-limit.requests-per-minute:60}")
    private int requestsPerMinute;

    @Value("${app.rate-limit.auth-requests-per-minute:10}")
    private int authRequestsPerMinute;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        String clientIp = getClientIp(request);
        String path = request.getRequestURI();

        Bucket bucket;
        if (path.startsWith("/api/auth/")) {
            bucket = authBuckets.computeIfAbsent(clientIp, k -> createBucket(authRequestsPerMinute));
        } else {
            bucket = generalBuckets.computeIfAbsent(clientIp, k -> createBucket(requestsPerMinute));
        }

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"errorCode\":\"RATE_LIMITED\",\"message\":\"Too many requests. Please try again later.\"}");
        }
    }

    private Bucket createBucket(int capacityPerMinute) {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacityPerMinute)
                        .refillGreedy(capacityPerMinute, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : request.getRemoteAddr();
    }
}
