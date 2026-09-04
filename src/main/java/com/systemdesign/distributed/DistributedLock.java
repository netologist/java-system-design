package com.systemdesign.distributed;

import java.time.Duration;

/**
 * Distributed Lock Abstraction (Category 16).
 * Ensures mutually exclusive execution across multiple independent JVM nodes or Kubernetes pods.
 */
public interface DistributedLock {
    /**
     * Attempts to acquire the lock with a lease duration.
     *
     * @param lockKey  Unique lock identifier
     * @param leaseDuration Duration after which lock automatically expires (prevents deadlocks on pod crash)
     * @return true if lock was acquired, false otherwise
     */
    boolean tryLock(String lockKey, Duration leaseDuration);

    /**
     * Releases the lock.
     *
     * @param lockKey Unique lock identifier
     */
    void unlock(String lockKey);
}
