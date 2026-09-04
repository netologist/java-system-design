package com.systemdesign.concurrency;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Singleflight / Request Coalescing Pattern (Category 09 & 26).
 * <p>
 * Suppresses duplicate concurrent in-flight function calls for the same key.
 * If 1,000 concurrent threads request key "BTC-USD", only ONE supplier executes;
 * all other 999 threads await and share the identical result.
 * <p>
 * Eliminates <b>Cache Stampede (Thundering Herd)</b> and downstream service overload.
 *
 * @param <K> Key type
 * @param <V> Result value type
 */
public class Singleflight<K, V> {

    private final ConcurrentHashMap<K, CompletableFuture<V>> inFlight = new ConcurrentHashMap<>();

    /**
     * Executes the supplier for the given key, coalescing concurrent calls.
     *
     * @param key      Deduplication key
     * @param supplier Execution supplier for the value
     * @return The computed or shared result
     */
    public V execute(K key, Supplier<V> supplier) {
        // Atomic compute: only the first thread registers its uncompleted future
        CompletableFuture<V> future = inFlight.computeIfAbsent(key, k -> CompletableFuture.supplyAsync(supplier));

        try {
            return future.join();
        } finally {
            // Clean up once completed so future requests fetch fresh data
            inFlight.remove(key, future);
        }
    }

    /**
     * Returns the number of currently active in-flight executions.
     */
    public int inFlightCount() {
        return inFlight.size();
    }
}
