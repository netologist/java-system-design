package com.systemdesign.resilience;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * Circuit Breaker Pattern (Category 08 & 20).
 * <p>
 * Protects downstream systems from cascading failure when a dependency degrades.
 * States:
 * <ul>
 *   <li><b>CLOSED:</b> Normal operation; requests pass through. Failures increment counter.</li>
 *   <li><b>OPEN:</b> Tripped; requests fail-fast immediately without invoking downstream.</li>
 *   <li><b>HALF_OPEN:</b> Testing recovery; allows a limited probe probe request.</li>
 * </ul>
 */
public class CircuitBreaker {

    private static final Logger log = LoggerFactory.getLogger(CircuitBreaker.class);

    public enum State { CLOSED, OPEN, HALF_OPEN }

    private final String name;
    private final int failureThreshold;
    private final Duration resetTimeout;

    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger failureCount = new AtomicInteger(0);
    private final AtomicReference<Instant> lastFailureTime = new AtomicReference<>(Instant.EPOCH);

    public CircuitBreaker(String name, int failureThreshold, Duration resetTimeout) {
        this.name = name;
        this.failureThreshold = failureThreshold;
        this.resetTimeout = resetTimeout;
    }

    public <T> T execute(Supplier<T> action, Supplier<T> fallback) {
        checkStateTransition();

        if (state.get() == State.OPEN) {
            log.warn("Circuit Breaker [{}] is OPEN. Executing fallback.", name);
            return fallback.get();
        }

        try {
            T result = action.get();
            onSuccess();
            return result;
        } catch (Exception ex) {
            onFailure(ex);
            log.warn("Circuit Breaker [{}] intercepted failure: {}. Calling fallback.", name, ex.getMessage());
            return fallback.get();
        }
    }

    private void onSuccess() {
        if (state.get() == State.HALF_OPEN) {
            log.info("Circuit Breaker [{}] recovered! Transitioning to CLOSED.", name);
            state.set(State.CLOSED);
        }
        failureCount.set(0);
    }

    private void onFailure(Exception ex) {
        lastFailureTime.set(Instant.now());
        int failures = failureCount.incrementAndGet();

        if (state.get() == State.HALF_OPEN || failures >= failureThreshold) {
            log.error("Circuit Breaker [{}] threshold reached ({} failures). Tripping to OPEN!", name, failures);
            state.set(State.OPEN);
        }
    }

    private void checkStateTransition() {
        if (state.get() == State.OPEN) {
            Instant openedAt = lastFailureTime.get();
            if (Duration.between(openedAt, Instant.now()).compareTo(resetTimeout) >= 0) {
                if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                    log.info("Circuit Breaker [{}] reset timeout elapsed. Testing with HALF_OPEN probe.", name);
                }
            }
        }
    }

    public State getState() {
        checkStateTransition();
        return state.get();
    }

    public String getName() { return name; }
}
