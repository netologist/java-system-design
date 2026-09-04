package com.systemdesign.resilience;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Token Bucket Rate Limiter Pattern (Category 09 & 11).
 * <p>
 * Enforces rate limits with smooth bursting capability.
 * Tokens accumulate at a fixed replenishment rate up to bucket capacity.
 */
public class TokenBucketRateLimiter {

    private final long capacity;
    private final double refillRateTokensPerSecond;
    private final AtomicLong availableTokens;
    private final AtomicLong lastRefillTimestampNanos;

    public TokenBucketRateLimiter(long capacity, double refillRateTokensPerSecond) {
        if (capacity <= 0 || refillRateTokensPerSecond <= 0) {
            throw new IllegalArgumentException("Capacity and refill rate must be positive");
        }
        this.capacity = capacity;
        this.refillRateTokensPerSecond = refillRateTokensPerSecond;
        this.availableTokens = new AtomicLong(capacity);
        this.lastRefillTimestampNanos = new AtomicLong(System.nanoTime());
    }

    /**
     * Tries to acquire 1 token. Returns true if permitted, false if rate limit exceeded.
     */
    public boolean tryAcquire() {
        return tryAcquire(1);
    }

    /**
     * Tries to acquire requested number of tokens.
     */
    public boolean tryAcquire(int tokensRequested) {
        refillTokens();

        while (true) {
            long current = availableTokens.get();
            if (current < tokensRequested) {
                return false;
            }
            if (availableTokens.compareAndSet(current, current - tokensRequested)) {
                return true;
            }
        }
    }

    private void refillTokens() {
        long now = System.nanoTime();
        long last = lastRefillTimestampNanos.get();
        long elapsedNanos = now - last;

        if (elapsedNanos <= 0) return;

        double tokensToAdd = (elapsedNanos / 1_000_000_000.0) * refillRateTokensPerSecond;
        if (tokensToAdd >= 1.0 && lastRefillTimestampNanos.compareAndSet(last, now)) {
            while (true) {
                long current = availableTokens.get();
                long updated = Math.min(capacity, current + (long) tokensToAdd);
                if (availableTokens.compareAndSet(current, updated)) {
                    break;
                }
            }
        }
    }

    public long getAvailableTokens() {
        refillTokens();
        return availableTokens.get();
    }
}
