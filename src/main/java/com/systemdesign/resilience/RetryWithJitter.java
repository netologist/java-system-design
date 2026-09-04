package com.systemdesign.resilience;

import java.time.Duration;
import java.util.Random;
import java.util.function.Supplier;

/**
 * Exponential Backoff with Full Jitter Pattern (Category 08 & 26).
 * <p>
 * Standard AWS / Google architecture pattern:
 * When dozens of clients retry at the exact same exponential interval (2s, 4s, 8s),
 * they create periodic synchronization waves (Thundering Herd) that keep crashing the downstream server.
 * Full Jitter randomizes the sleep interval: {@code sleep = random(0, min(cap, base * 2^attempt))}.
 */
public class RetryWithJitter {

    private final int maxAttempts;
    private final Duration baseDelay;
    private final Duration maxDelay;
    private final Random random = new Random();

    public RetryWithJitter(int maxAttempts, Duration baseDelay, Duration maxDelay) {
        this.maxAttempts = maxAttempts;
        this.baseDelay = baseDelay;
        this.maxDelay = maxDelay;
    }

    public <T> T execute(Supplier<T> action) {
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxAttempts) {
            try {
                attempt++;
                return action.get();
            } catch (Exception ex) {
                lastException = ex;
                if (attempt >= maxAttempts) {
                    break;
                }

                // Calculate exponential backoff: base * 2^(attempt - 1)
                long expMillis = (long) (baseDelay.toMillis() * Math.pow(2, attempt - 1));
                long cappedMillis = Math.min(maxDelay.toMillis(), expMillis);

                // Full Jitter: pick uniform random between 0 and cappedMillis
                long sleepMillis = random.nextLong(1, Math.max(2, cappedMillis + 1));

                try {
                    Thread.sleep(sleepMillis);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Retry interrupted", ie);
                }
            }
        }

        throw new IllegalStateException("Exhausted all " + maxAttempts + " retry attempts", lastException);
    }
}
