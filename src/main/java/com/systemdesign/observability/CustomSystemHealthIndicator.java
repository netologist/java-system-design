package com.systemdesign.observability;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom Health Indicator for Kubernetes Liveness and Readiness Probes (Category 01 & 17).
 */
@Component
public class CustomSystemHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        // Can verify database, memory pool, or circuit breaker status
        return Health.up()
                .withDetail("virtualThreadsEnabled", true)
                .withDetail("storageStatus", "HEALTHY")
                .withDetail("circuitBreakerStatus", "ALL_CLOSED")
                .build();
    }
}
