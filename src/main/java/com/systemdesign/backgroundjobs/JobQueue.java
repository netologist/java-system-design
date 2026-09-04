package com.systemdesign.backgroundjobs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * In-Memory Async Job Queue Pattern (Category 24).
 * Backed by Java 21 Virtual Threads and a bounded BlockingQueue.
 */
public class JobQueue implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(JobQueue.class);

    public record Job(String id, Runnable action) {}

    private final BlockingQueue<Job> queue;
    private final ExecutorService consumerExecutor;
    private volatile boolean running = true;

    public JobQueue(int capacity) {
        this.queue = new ArrayBlockingQueue<>(capacity);
        this.consumerExecutor = Executors.newVirtualThreadPerTaskExecutor();

        // Start background worker consumer on Virtual Thread
        consumerExecutor.submit(this::processQueue);
    }

    public boolean enqueue(String jobId, Runnable action) {
        return queue.offer(new Job(jobId, action));
    }

    private void processQueue() {
        while (running && !Thread.currentThread().isInterrupted()) {
            try {
                Job job = queue.poll(500, TimeUnit.MILLISECONDS);
                if (job != null) {
                    log.info("Processing background job [{}] on virtual thread", job.id());
                    job.action().run();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                log.error("Background job failed: {}", ex.getMessage());
            }
        }
    }

    public int queueSize() {
        return queue.size();
    }

    @Override
    public void close() {
        running = false;
        consumerExecutor.close();
    }
}
