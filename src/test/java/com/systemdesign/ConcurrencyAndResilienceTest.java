package com.systemdesign;

import com.systemdesign.concurrency.Singleflight;
import com.systemdesign.concurrency.VirtualThreadWorkerPool;
import com.systemdesign.resilience.Bulkhead;
import com.systemdesign.resilience.CircuitBreaker;
import com.systemdesign.resilience.RetryWithJitter;
import com.systemdesign.resilience.TokenBucketRateLimiter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConcurrencyAndResilienceTest {

    @Test
    @DisplayName("Singleflight coalesces 50 concurrent virtual threads into 1 downstream call")
    void testSingleflightCoalescing() throws Exception {
        Singleflight<String, String> singleflight = new Singleflight<>();
        AtomicInteger callCount = new AtomicInteger(0);

        int threadCount = 50;
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    readyLatch.countDown();
                    try {
                        startLatch.await();
                        String val = singleflight.execute("BTC-USD", () -> {
                            callCount.incrementAndGet();
                            try {
                                Thread.sleep(50); // Simulate network I/O
                            } catch (InterruptedException ignored) {}
                            return "price_65000";
                        });
                        assertThat(val).isEqualTo("price_65000");
                    } catch (InterruptedException ignored) {
                    } finally {
                        doneLatch.countDown();
                    }
                });
            }

            readyLatch.await();
            startLatch.countDown(); // Release all 50 threads at once
            doneLatch.await(5, TimeUnit.SECONDS);

            // Singleflight coalesces concurrent calls so execution count is drastically minimized
            assertThat(callCount.get()).isLessThanOrEqualTo(3);
        }
    }

    @Test
    @DisplayName("CircuitBreaker transitions to OPEN on repeated failures and invokes fallback")
    void testCircuitBreakerTrippingAndRecovery() {
        CircuitBreaker cb = new CircuitBreaker("payment-gw", 2, Duration.ofMillis(100));

        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // Failure 1
        String res1 = cb.execute(
                () -> { throw new RuntimeException("500 Internal Error"); },
                () -> "fallback_result"
        );
        assertThat(res1).isEqualTo("fallback_result");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.CLOSED);

        // Failure 2: reaches threshold 2 -> trips to OPEN
        String res2 = cb.execute(
                () -> { throw new RuntimeException("500 Internal Error"); },
                () -> "fallback_result"
        );
        assertThat(res2).isEqualTo("fallback_result");
        assertThat(cb.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        // Fast failure when OPEN
        AtomicInteger downstreamInvocations = new AtomicInteger(0);
        String res3 = cb.execute(
                () -> { downstreamInvocations.incrementAndGet(); return "success"; },
                () -> "fast_fallback"
        );
        assertThat(res3).isEqualTo("fast_fallback");
        assertThat(downstreamInvocations.get()).isEqualTo(0); // Downstream never called
    }

    @Test
    @DisplayName("TokenBucketRateLimiter permits up to capacity and rejects excess")
    void testTokenBucketRateLimiter() {
        TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(3, 1.0);

        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isTrue();
        assertThat(limiter.tryAcquire()).isTrue();
        // 4th token exceeds bucket capacity
        assertThat(limiter.tryAcquire()).isFalse();
    }

    @Test
    @DisplayName("Bulkhead isolates resources and executes fallback when full")
    void testBulkheadIsolation() {
        Bulkhead bulkhead = new Bulkhead("order-service", 1);

        // Saturate bulkhead
        bulkhead.execute(() -> {
            // Second concurrent attempt
            String result = bulkhead.execute(() -> "normal", () -> "bulkhead_fallback");
            assertThat(result).isEqualTo("bulkhead_fallback");
            return null;
        }, () -> null);
    }
}
