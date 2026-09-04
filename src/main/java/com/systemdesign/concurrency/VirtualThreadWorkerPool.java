package com.systemdesign.concurrency;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * Worker Pool Pattern with Backpressure (Category 09 & 20).
 * <p>
 * Combines Java 21 Virtual Threads with {@link Semaphore} bounds.
 * Although Virtual Threads are lightweight, downstream dependencies (databases,
 * third-party APIs, socket pools) cannot handle unbounded concurrency.
 * This worker pool enforces strict concurrency limits without blocking carrier threads.
 */
public class VirtualThreadWorkerPool implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(VirtualThreadWorkerPool.class);

    private final ExecutorService executor;
    private final Semaphore concurrencySemaphore;
    private final int maxConcurrentTasks;

    public VirtualThreadWorkerPool(int maxConcurrentTasks) {
        if (maxConcurrentTasks <= 0) {
            throw new IllegalArgumentException("Concurrency limit must be positive");
        }
        this.maxConcurrentTasks = maxConcurrentTasks;
        this.concurrencySemaphore = new Semaphore(maxConcurrentTasks);
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    /**
     * Submits a task for execution, blocking if maximum concurrency limit is reached (Backpressure).
     *
     * @param task Callable to run
     * @param <T>  Result type
     * @return Future containing the task outcome
     */
    public <T> Future<T> submit(Callable<T> task) {
        try {
            concurrencySemaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while waiting for worker permit", e);
        }

        return executor.submit(() -> {
            try {
                return task.call();
            } finally {
                concurrencySemaphore.release();
            }
        });
    }

    public int getAvailablePermits() {
        return concurrencySemaphore.availablePermits();
    }

    public int getMaxConcurrentTasks() {
        return maxConcurrentTasks;
    }

    @Override
    public void close() {
        executor.close();
    }
}
