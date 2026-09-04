package com.systemdesign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Production System Design & Backend Architecture Patterns in Modern Java 21+ and Spring Boot 3.4.
 * <p>
 * Key Capabilities Demonstrated:
 * <ul>
 *   <li><b>Java 21 Virtual Threads:</b> Enabled via {@code spring.threads.virtual.enabled=true}.</li>
 *   <li><b>Records & Sealed Types:</b> Domain integrity, state machines, and immutable DTOs.</li>
 *   <li><b>Concurrency & Resilience:</b> Singleflight, Circuit Breakers, Bulkheads, Token Bucket Rate Limiting.</li>
 *   <li><b>Persistence & Storage:</b> Transactional Outbox, Keyset Pagination, Optimistic/Pessimistic Locking.</li>
 *   <li><b>Distributed Primitives:</b> Saga Orchestrator, Distributed Lock abstractions, Idempotent Consumers.</li>
 * </ul>
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
public class SystemDesignApplication {

    public static void main(String[] args) {
        SpringApplication.run(SystemDesignApplication.class, args);
    }
}
