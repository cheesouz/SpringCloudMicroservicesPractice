package com.cheesouz.gateway.ratelimit;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Component
public class RateLimitFilter implements Filter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final String X_FORWARDED_FOR_HEADER = "X-Forwarded-For";
    private static final String RATE_LIMIT_LIMIT_HEADER = "X-RateLimit-Limit";
    private static final String RATE_LIMIT_REMAINING_HEADER = "X-RateLimit-Remaining";
    private static final String RETRY_AFTER_HEADER = "Retry-After";
    private static final int STATUS_TOO_MANY_REQUESTS = 429;

    private final double capacity;
    private final double refillRate;
    private final long idleTimeoutMs;

    private final ConcurrentHashMap<String, RateLimitBucket> buckets = new ConcurrentHashMap<>();

    public RateLimitFilter(
            @Value("${gateway.rate-limit.capacity:20}") double capacity,
            @Value("${gateway.rate-limit.refill-rate:10}") double refillRate,
            @Value("${gateway.rate-limit.idle-timeout-ms:600000}") long idleTimeoutMs) {
        this.capacity = capacity;
        this.refillRate = refillRate;
        this.idleTimeoutMs = idleTimeoutMs;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        if (isExemptPath(request.getRequestURI())) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        RateLimitBucket bucket = buckets.compute(clientIp, (ip, existing) ->
                existing != null ? existing : new RateLimitBucket(capacity, refillRate));

        if (bucket.tryConsume()) {
            response.setHeader(RATE_LIMIT_LIMIT_HEADER, String.valueOf((long) capacity));
            response.setHeader(RATE_LIMIT_REMAINING_HEADER, String.valueOf((long) Math.floor(bucket.availableTokens())));
            chain.doFilter(request, response);
            return;
        }

        long retryAfter = bucket.secondsUntilRefill();
        log.warn("[RATE-LIMIT] client {} rejected - too many requests, retry in {}s", clientIp, retryAfter);

        response.setStatus(STATUS_TOO_MANY_REQUESTS);
        response.setContentType("application/json");
        response.setHeader(RATE_LIMIT_LIMIT_HEADER, String.valueOf((long) capacity));
        response.setHeader(RATE_LIMIT_REMAINING_HEADER, String.valueOf((long) Math.floor(bucket.availableTokens())));
        response.setHeader(RETRY_AFTER_HEADER, String.valueOf(retryAfter));
        response.getWriter().write("{\"message\":\"Too many requests\",\"retryAfterSeconds\":" + retryAfter + "}");
    }

    @Scheduled(fixedDelayString = "${gateway.rate-limit.eviction-interval-ms:300000}")
    public void evictIdleBuckets() {
        long now = System.currentTimeMillis();
        int sizeBefore = buckets.size();
        buckets.entrySet().removeIf(entry -> now - entry.getValue().lastAccessedMs() > idleTimeoutMs);
        int removed = sizeBefore - buckets.size();
        if (removed > 0) {
            log.info("[RATE-LIMIT] evicted {} idle buckets", removed);
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader(X_FORWARDED_FOR_HEADER);
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static boolean isExemptPath(String uri) {
    return uri.equals("/actuator/health")
        || uri.equals("/actuator/info")
        || uri.startsWith("/swagger-ui")
        || uri.startsWith("/v3/api-docs")
        || uri.startsWith("/webjars");
}

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}