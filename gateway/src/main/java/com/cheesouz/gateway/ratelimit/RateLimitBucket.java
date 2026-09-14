package com.cheesouz.gateway.ratelimit;

public class RateLimitBucket {

    private final double capacity;
    private final double refillPerSecond;

    private double tokens;
    private long lastRefillMs;
    private volatile long lastAccessedMs;

    public RateLimitBucket(double capacity, double refillPerSecond) {
        this.capacity = capacity;
        this.refillPerSecond = refillPerSecond;
        this.tokens = capacity;
        this.lastRefillMs = now();
        this.lastAccessedMs = now();
    }

    public synchronized boolean tryConsume() {
        refill();
        lastAccessedMs = now();
        if (tokens >= 1) {
            tokens -= 1;
            return true;
        }
        return false;
    }

    public synchronized double availableTokens() {
        refill();
        lastAccessedMs = now();
        return tokens;
    }

    public synchronized long secondsUntilRefill() {
        refill();
        lastAccessedMs = now();
        if (tokens >= 1) {
            return 0;
        }
        return Math.max(1, (long) Math.ceil((1 - tokens) / refillPerSecond));
    }

    public long lastAccessedMs() {
        return lastAccessedMs;
    }

    private void refill() {
        long now = now();
        double elapsedSeconds = (now - lastRefillMs) / 1000.0;
        tokens = Math.min(capacity, tokens + elapsedSeconds * refillPerSecond);
        lastRefillMs = now;
    }

    private static long now() {
        return System.currentTimeMillis();
    }
}