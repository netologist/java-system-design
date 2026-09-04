package com.systemdesign.distributed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Distributed Lock backed by atomic state and lease expiration (Category 16).
 * Simulates a Redis (Redlock) or SQL-backed distributed lock with TTL safety.
 */
@Component
public class DatabaseDistributedLock implements DistributedLock {

    private static final Logger log = LoggerFactory.getLogger(DatabaseDistributedLock.class);

    private record LockRecord(String owner, Instant expiresAt) {}

    private final Map<String, LockRecord> lockStore = new ConcurrentHashMap<>();

    @Override
    public boolean tryLock(String lockKey, Duration leaseDuration) {
        Instant now = Instant.now();
        Instant expiry = now.plus(leaseDuration);
        String currentOwner = Thread.currentThread().getName();

        return lockStore.compute(lockKey, (k, existing) -> {
            if (existing == null || now.isAfter(existing.expiresAt())) {
                log.debug("Distributed lock [{}] acquired by [{}] until [{}]", lockKey, currentOwner, expiry);
                return new LockRecord(currentOwner, expiry);
            }
            log.debug("Distributed lock [{}] already held by [{}] until [{}]", lockKey, existing.owner(), existing.expiresAt());
            return existing; // Lock busy
        }).owner().equals(currentOwner);
    }

    @Override
    public void unlock(String lockKey) {
        lockStore.remove(lockKey);
        log.debug("Distributed lock [{}] released", lockKey);
    }
}
