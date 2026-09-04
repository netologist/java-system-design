package com.systemdesign.concurrency;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Fan-Out / Fan-In Pattern (Category 09).
 * <p>
 * Demonstrates parallel asynchronous querying across multiple downstream sources
 * and consolidating them into an aggregated Java Record response within a deadline.
 */
@Service
public class FanOutFanInAggregator {

    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public record UserFinancialProfile(
            String userId,
            long creditScore,
            long walletBalanceCents,
            int activeLoansCount,
            Duration totalDuration
    ) {}

    public UserFinancialProfile aggregateProfile(String userId, Duration timeout) {
        long startTime = System.nanoTime();

        // 1. Fan-Out: Launch 3 independent async requests concurrently on Virtual Threads
        CompletableFuture<Long> creditScoreFuture = CompletableFuture.supplyAsync(
                () -> fetchCreditScore(userId), virtualExecutor
        );

        CompletableFuture<Long> walletBalanceFuture = CompletableFuture.supplyAsync(
                () -> fetchWalletBalance(userId), virtualExecutor
        );

        CompletableFuture<Integer> activeLoansFuture = CompletableFuture.supplyAsync(
                () -> fetchActiveLoansCount(userId), virtualExecutor
        );

        // 2. Fan-In: Wait for all futures to complete with timeout
        CompletableFuture.allOf(creditScoreFuture, walletBalanceFuture, activeLoansFuture)
                .orTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
                .join();

        long durationNanos = System.nanoTime() - startTime;

        // 3. Construct aggregated immutable Record
        return new UserFinancialProfile(
                userId,
                creditScoreFuture.join(),
                walletBalanceFuture.join(),
                activeLoansFuture.join(),
                Duration.ofNanos(durationNanos)
        );
    }

    private Long fetchCreditScore(String userId) {
        sleepQuietly(50);
        return 780L;
    }

    private Long fetchWalletBalance(String userId) {
        sleepQuietly(60);
        return 250_000L; // 2,500.00 TL
    }

    private Integer fetchActiveLoansCount(String userId) {
        sleepQuietly(40);
        return 2;
    }

    private void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
