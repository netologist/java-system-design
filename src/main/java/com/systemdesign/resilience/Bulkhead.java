package com.systemdesign.resilience;

import java.util.concurrent.Semaphore;
import java.util.function.Supplier;

/**
 * Bulkhead Pattern (Category 08 & 20).
 * <p>
 * Partitioning strategy that isolates execution resources.
 * Prevents a surge in requests or high latency in one downstream service
 * from consuming all server resources and starving other critical paths.
 */
public class Bulkhead {

    private final String name;
    private final Semaphore semaphore;
    private final int maxConcurrentCalls;

    public Bulkhead(String name, int maxConcurrentCalls) {
        if (maxConcurrentCalls <= 0) {
            throw new IllegalArgumentException("Max concurrent calls must be positive");
        }
        this.name = name;
        this.maxConcurrentCalls = maxConcurrentCalls;
        this.semaphore = new Semaphore(maxConcurrentCalls);
    }

    public <T> T execute(Supplier<T> action, Supplier<T> fallback) {
        if (!semaphore.tryAcquire()) {
            // Bulkhead saturated: fail-fast or execute fallback without queuing
            return fallback.get();
        }

        try {
            return action.get();
        } finally {
            semaphore.release();
        }
    }

    public int getAvailablePermits() {
        return semaphore.availablePermits();
    }

    public String getName() {
        return name;
    }
}
