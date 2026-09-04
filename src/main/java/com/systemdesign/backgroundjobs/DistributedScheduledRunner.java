package com.systemdesign.backgroundjobs;

import com.systemdesign.distributed.DistributedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Scheduled Runner with Distributed Lock Pattern (Category 24 & 16).
 * <p>
 * Ensures that in a clustered Kubernetes environment with multiple pods,
 * scheduled cron jobs execute on ONLY ONE pod per period, preventing duplicate runs.
 */
@Component
public class DistributedScheduledRunner {

    private static final Logger log = LoggerFactory.getLogger(DistributedScheduledRunner.class);
    private final DistributedLock distributedLock;

    public DistributedScheduledRunner(DistributedLock distributedLock) {
        this.distributedLock = distributedLock;
    }

    @Scheduled(fixedDelay = 5000)
    public void runPeriodicMaintenance() {
        String lockKey = "cron_maintenance_task";
        Duration lease = Duration.ofSeconds(4);

        if (distributedLock.tryLock(lockKey, lease)) {
            try {
                log.info("Acquired distributed lock for [{}]. Running cluster-wide maintenance.", lockKey);
                // Perform single-node maintenance work
            } finally {
                distributedLock.unlock(lockKey);
            }
        } else {
            log.debug("Lock [{}] already held by another pod. Skipping execution.", lockKey);
        }
    }
}
