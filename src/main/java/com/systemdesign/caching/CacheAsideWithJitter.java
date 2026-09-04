package com.systemdesign.caching;

import com.systemdesign.concurrency.Singleflight;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Advanced Caching Strategies (Category 14 & 26).
 * <p>
 * Combines 4 production resilience caching patterns:
 * <ol>
 *   <li><b>Cache-Aside:</b> Read cache first, on miss fetch from source and populate.</li>
 *   <li><b>TTL with Jitter:</b> Randomizes TTL window by ±20% to avoid simultaneous expiry waves.</li>
 *   <li><b>Singleflight Stampede Prevention:</b> Coalesces concurrent misses so only 1 DB query executes.</li>
 *   <li><b>Negative Caching:</b> Caches missing/null items with a short TTL to prevent repeated DB hammering.</li>
 * </ol>
 */
@Service
public class CacheAsideWithJitter {

    private static final Logger log = LoggerFactory.getLogger(CacheAsideWithJitter.class);

    private record CacheEntry<V>(V value, Instant expiresAt, boolean isNegative) {}

    private final Map<String, CacheEntry<Object>> cache = new ConcurrentHashMap<>();
    private final Singleflight<String, Object> singleflight = new Singleflight<>();
    private final Random random = new Random();

    @SuppressWarnings("unchecked")
    public <T> Optional<T> getOrLoad(
            String key,
            Duration baseTtl,
            Supplier<Optional<T>> loader
    ) {
        Instant now = Instant.now();
        CacheEntry<Object> entry = cache.get(key);

        if (entry != null && now.isBefore(entry.expiresAt())) {
            if (entry.isNegative()) {
                return Optional.empty(); // Negative cache hit
            }
            return Optional.of((T) entry.value());
        }

        // Cache miss or expired: use Singleflight to protect downstream database
        Object result = singleflight.execute(key, () -> {
            // Double-check cache inside singleflight boundary
            CacheEntry<Object> recheck = cache.get(key);
            if (recheck != null && Instant.now().isBefore(recheck.expiresAt())) {
                return recheck.isNegative() ? Optional.empty() : Optional.of(recheck.value());
            }

            log.info("Cache miss for key [{}]. Loading from database...", key);
            Optional<T> loaded = loader.get();

            if (loaded.isPresent()) {
                // Apply TTL with Jitter (±20% randomness)
                Duration jitteredTtl = applyJitter(baseTtl);
                Instant expiry = Instant.now().plus(jitteredTtl);
                cache.put(key, new CacheEntry<>(loaded.get(), expiry, false));
                return loaded;
            } else {
                // Negative Caching with short TTL (e.g. 5 seconds) to prevent brute-force DB overload
                Instant shortExpiry = Instant.now().plusSeconds(5);
                cache.put(key, new CacheEntry<>(null, shortExpiry, true));
                return Optional.empty();
            }
        });

        if (result instanceof Optional<?> opt) {
            return (Optional<T>) opt;
        }
        return Optional.ofNullable((T) result);
    }

    private Duration applyJitter(Duration baseTtl) {
        double jitterMultiplier = 0.8 + (random.nextDouble() * 0.4); // 0.8x to 1.2x
        long millis = (long) (baseTtl.toMillis() * jitterMultiplier);
        return Duration.ofMillis(Math.max(100, millis));
    }

    public void invalidate(String key) {
        cache.remove(key);
    }

    public int cacheSize() {
        return cache.size();
    }
}
