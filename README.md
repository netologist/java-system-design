# 🏛️ Production System Design & Backend Architecture Patterns in Modern Java & Spring Boot

A comprehensive, production-grade reference implementation of **core backend architecture patterns, distributed systems primitives, and concurrency models** built with **Modern Java (Java 21 / 25+)** and **Spring Boot 3.4.x**.

This project translates and reimagines the patterns from the Go [`system-design/`](../system-design/) repository into idiomatic, enterprise-grade Java, leveraging **Java Records**, **Virtual Threads (Project Loom)**, **Sealed Interfaces**, **Pattern Matching for Switch**, and **Spring Boot 3 Web/JPA/Actuator**.

---

## 📑 Table of Contents

1. [Architectural Overview & Highlights](#architectural-overview--highlights)
2. [Go vs Modern Java 21+ Architectural Comparison](#go-vs-modern-java-21-architectural-comparison)
3. [Implemented Pattern Categories & Packages](#implemented-pattern-categories--packages)
4. [Deep Dive: Core Architectural Patterns](#deep-dive-core-architectural-patterns)
   - [Concurrency: Singleflight & Virtual Thread Worker Pool](#1-concurrency-singleflight--virtual-thread-worker-pool)
   - [Resilience: Circuit Breaker, Bulkhead & Full Jitter Retry](#2-resilience-circuit-breaker-bulkhead--full-jitter-retry)
   - [Persistence: Optimistic Locking, Keyset Pagination & Soft Delete](#3-persistence-optimistic-locking-keyset-pagination--soft-delete)
   - [Messaging: Transactional Outbox & Idempotent Consumer](#4-messaging-transactional-outbox--idempotent-consumer)
   - [Distributed Systems: Distributed Lock & Saga Orchestrator](#5-distributed-systems-distributed-lock--saga-orchestrator)
   - [Data Integrity: Sealed State Machine (Compiler Exhaustiveness)](#6-data-integrity-sealed-state-machine-compiler-exhaustiveness)
   - [Security & API Design: Constant-Time HMAC & Idempotency Key Filter](#7-security--api-design-constant-time-hmac--idempotency-key-filter)
5. [Running the Application & Tests](#running-the-application--tests)

---

## Architectural Overview & Highlights

Modern Java (Java 21+) fundamentally transforms how high-throughput backend services are architected:

- **Virtual Threads (Project Loom):** Enabled via `spring.threads.virtual.enabled=true`. Eliminates the need for reactive callback hell (WebFlux/Mono) by allowing standard blocking I/O (JDBC, HTTP clients) to run on lightweight virtual threads without pinning carrier OS threads.
- **Java Records:** Immutable by default, concise domain entities and DTOs with zero boilerplate.
- **Sealed Hierarchies & Pattern Matching:** Compilers strictly enforce complete handling of domain states (exhaustiveness checking without brittle fallback branches).
- **Spring Boot 3.4 Ecosystem:** Actuator health/readiness probes, declarative transactions (`@Transactional`), and unified configuration.

---

## Go vs Modern Java 21+ Architectural Comparison

| Architectural Concern | Go Implementation (`system-design/`) | Modern Java 21+ & Spring Boot 3 (`java-system-design/`) | Why Modern Java Approach |
|:---|:---|:---|:---|
| **Concurrency Scaling** | Goroutines (`go func()`) + channels | **Virtual Threads** (`Thread.ofVirtual()`) | JVM manages millions of lightweight threads on a ForkJoinPool; synchronous code with asynchronous scalability. |
| **Request Coalescing** | `golang.org/x/sync/singleflight` | `Singleflight<K, V>` (ConcurrentHashMap + CompletableFuture) | Eliminates Cache Stampede (Thundering Herd) when hundreds of concurrent requests query the same key. |
| **Domain State Machine** | Constants / enums + mutex | **Java 21 Sealed Interfaces** + **Record Patterns** | Compiler guarantees exhaustiveness at build time. No unhandled states or silent failures. |
| **Data Integrity** | Manual SQL rollback with defer | **Spring `@Transactional`** + **`@Version` Optimistic Locking** | Automated 2-phase transaction demarcation with automatic rollback on runtime exceptions. |
| **Persistence Scaling** | Manual SQL `OFFSET` / limit | **Keyset Pagination** (`createdAt > :last AND id > :id`) | $O(1)$ index seek performance regardless of page depth; prevents table scans. |
| **Distributed Resilience** | Custom ticker loops + atomic compare | **CircuitBreaker**, **Bulkhead**, **TokenBucketRateLimiter** | Full Jitter exponential backoff prevents thundering herd against downstream dependencies. |
| **Transactional Outbox** | Manual DB transaction + polling loop | **JPA Outbox Entity** + **Scheduled Virtual Thread Publisher** | Prevents dual-write inconsistencies; guarantees at-least-once message delivery. |
| **Timing Attack Defense** | `crypto/subtle.ConstantTimeCompare` | `MessageDigest.isEqual(...)` | Constant-time byte comparison prevents side-channel brute force of webhook HMAC signatures. |
| **Distributed Transactions** | Custom goroutine compensations | **Saga Orchestrator** | Coordinates multi-service distributed transactions with automated reverse compensation rollback. |

---

## Implemented Pattern Categories & Packages

```
java-system-design/
├── src/main/java/com/systemdesign/
│   ├── SystemDesignApplication.java                 # Spring Boot Main (Virtual Threads Enabled)
│   ├── apidesign/
│   │   ├── ApiResponse.java                         # Category 10: Standard Response Envelope
│   │   └── IdempotencyKeyFilter.java                # Category 10: Idempotency Key Replay Filter
│   ├── backgroundjobs/
│   │   ├── JobQueue.java                            # Category 24: Bounded Job Queue on Virtual Threads
│   │   └── DistributedScheduledRunner.java          # Category 24: Cron with Distributed Lock
│   ├── caching/
│   │   └── CacheAsideWithJitter.java                # Category 14 & 26: Cache-Aside with Jitter & Negative Caching
│   ├── concurrency/
│   │   ├── Singleflight.java                        # Category 09 & 26: Request Coalescing
│   │   ├── VirtualThreadWorkerPool.java             # Category 09 & 20: Worker Pool with Semaphore Backpressure
│   │   └── FanOutFanInAggregator.java               # Category 09: Parallel Fan-Out/Fan-In Aggregator
│   ├── dataintegrity/
│   │   ├── PaymentState.java                        # Category 23: Sealed State Machine Hierarchy
│   │   └── PaymentStateMachine.java                 # Category 23: Pattern Matching State Transitions
│   ├── distributed/
│   │   ├── DistributedLock.java                     # Category 16: Distributed Lock Abstraction
│   │   ├── DatabaseDistributedLock.java             # Category 16: DB-backed Distributed Lock
│   │   └── SagaOrchestrator.java                    # Category 16: Distributed Saga with Compensations
│   ├── messaging/
│   │   ├── OutboxEvent.java                         # Category 15 & 26: Transactional Outbox Entity
│   │   ├── OutboxEventRepository.java               # Category 15: Outbox JPA Repository
│   │   ├── OutboxPublisher.java                     # Category 15: Reliable Outbox Publisher
│   │   ├── ProcessedMessage.java                    # Category 15: Inbox Pattern Entity
│   │   ├── ProcessedMessageRepository.java          # Category 15: Inbox JPA Repository
│   │   └── IdempotentConsumer.java                  # Category 15: Exactly-Once Consumer Wrapper
│   ├── observability/
│   │   ├── CorrelationIdFilter.java                 # Category 07 & 17: Request Correlation ID Filter (MDC)
│   │   └── CustomSystemHealthIndicator.java         # Category 01 & 17: Kubernetes Health Probes
│   ├── persistence/
│   │   ├── Account.java                             # Category 04: Optimistic Lock (@Version) & Soft Delete
│   │   ├── AccountRepository.java                   # Category 04: Keyset Pagination & Pessimistic Lock
│   │   └── AccountService.java                      # Category 04: Optimistic Concurrency Retry Loop
│   ├── resilience/
│   │   ├── CircuitBreaker.java                      # Category 08 & 20: Closed / Open / Half-Open Breaker
│   │   ├── Bulkhead.java                            # Category 08 & 20: Resource Isolation
│   │   ├── RetryWithJitter.java                     # Category 08 & 26: Exponential Backoff with Full Jitter
│   │   └── TokenBucketRateLimiter.java              # Category 09 & 11: Token Bucket Rate Limiter
│   ├── security/
│   │   ├── HmacSignatureValidator.java              # Category 19: Constant-Time HMAC Signature Check
│   │   └── SsrfValidator.java                       # Category 19: Outbound URL SSRF Defense
│   └── api/
│       └── AccountController.java                   # REST Controller exercising the patterns
```

---

## Deep Dive: Core Architectural Patterns

### 1. Concurrency: Singleflight & Virtual Thread Worker Pool

```java
// Singleflight suppresses duplicate in-flight requests for the same key
Singleflight<String, String> singleflight = new Singleflight<>();
String result = singleflight.execute("BTC-USD", () -> fetchPriceFromBinance());
```

- When 1,000 threads simultaneously request `"BTC-USD"`, only **one** network call executes; the remaining 999 threads await and share the computed result.
- Backpressure in `VirtualThreadWorkerPool` prevents downstream database and connection pool exhaustion via bounded `Semaphore` permits.

---

### 2. Resilience: Circuit Breaker, Bulkhead & Full Jitter Retry

```java
CircuitBreaker cb = new CircuitBreaker("payment-gw", 3, Duration.ofSeconds(10));
String response = cb.execute(
    () -> remotePaymentService.call(),
    () -> "fallback_degraded_response"
);
```

- **Full Jitter Algorithm:** Rather than static retries ($2s, 4s, 8s$) that hit downstream servers in synchronized waves, sleep time is randomized:
  $$\text{sleep} = \text{random}(0, \min(\text{cap}, \text{base} \times 2^{\text{attempt}}))$$

---

### 3. Persistence: Optimistic Locking, Keyset Pagination & Soft Delete

```java
@Query("""
    SELECT a FROM Account a
    WHERE (a.createdAt > :lastCreatedAt)
       OR (a.createdAt = :lastCreatedAt AND a.id > :lastId)
    ORDER BY a.createdAt ASC, a.id ASC
""")
List<Account> findNextKeysetPage(Instant lastCreatedAt, Long lastId, Pageable pageable);
```

- **Keyset (Cursor) Pagination:** Completely bypasses SQL `OFFSET 100000`, scanning directly from indexed keys with constant $O(1)$ response time.
- **Optimistic Locking:** JPA `@Version` rejects concurrent conflicting balance updates, retried automatically via `AccountService.updateBalanceWithOptimisticRetry`.

---

### 4. Messaging: Transactional Outbox & Idempotent Consumer

```
[Local Business Transaction]
        |
        +---> [UPDATE accounts SET balance_cents = ...]
        +---> [INSERT INTO outbox_events (aggregate_id, payload, PENDING)]
[COMMIT]
        |
[Async OutboxPublisher (Virtual Thread)]
        |
        +---> Reads PENDING events
        +---> Publishes to Message Broker (Kafka/RabbitMQ)
        +---> Updates status to PUBLISHED
```

- Eliminates the **Dual-Write Problem** where a database write succeeds but message publishing fails (or vice versa).

---

### 5. Distributed Systems: Distributed Lock & Saga Orchestrator

```java
SagaOrchestrator.executeSaga("order-tx-123", List.of(
    new SagaStep("ReserveInventory", () -> inventory.reserve(), () -> inventory.release()),
    new SagaStep("DebitCustomer",    () -> wallet.debit(),       () -> wallet.refund()),
    new SagaStep("DispatchDelivery", () -> shipping.schedule(),  () -> shipping.cancel())
));
```

- If `DispatchDelivery` throws an exception, the Saga Orchestrator immediately halts forward execution and runs `wallet.refund()` followed by `inventory.release()` in reverse order.

---

### 6. Data Integrity: Sealed State Machine (Compiler Exhaustiveness)

```java
public sealed interface PaymentState permits Initiated, Authorized, Captured, Refunded, Failed {}

public PaymentState transition(PaymentState state, String action) {
    return switch (state) {
        case Initiated init -> authorize(init);
        case Authorized auth -> capture(auth);
        case Captured cap -> refund(cap);
        case Refunded ref -> throw new IllegalStateException("Terminal");
        case Failed fail -> throw new IllegalStateException("Terminal");
        // NO DEFAULT ->: Compiler forces all subtypes to be explicitly handled!
    };
}
```

---

### 7. Security & API Design: Constant-Time HMAC & Idempotency Key Filter

- **Constant-Time Verification:** `MessageDigest.isEqual(calculated, received)` takes uniform execution time regardless of where bytes differ, stopping **Timing Attack** side-channel exploits.
- **SSRF Defense:** `SsrfValidator` verifies that outbound webhook targets do not resolve to private subnets (`10.0.0.0/8`, `192.168.0.0/16`), loopbacks (`127.0.0.1`), or cloud metadata endpoints (`169.254.169.254`).

---

## Running the Application & Tests

### Prerequisites
- **JDK 21+** (JDK 21 or 25)
- **Apache Maven 3.9+**

### Running the Test Suite
Executes unit tests, Spring Boot `@SpringBootTest` integration tests, MockMvc API validation, and Virtual Thread concurrency stress tests:

```bash
cd java-system-design
mvn test -o
```

Expected output:
```
[INFO] ----------------< com.systemdesign:java-system-design >-----------------
[INFO] Building java-system-design 1.0.0
[INFO] Results:
[INFO] Tests run: 11, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Running the Spring Boot Application
```bash
cd java-system-design
mvn spring-boot:run -o
```

Endpoints available:
- **API Base:** `http://localhost:8080/api/v1/accounts`
- **Kubernetes Liveness Probe:** `http://localhost:8080/actuator/health/liveness`
- **Kubernetes Readiness Probe:** `http://localhost:8080/actuator/health/readiness`
- **Prometheus Metrics:** `http://localhost:8080/actuator/metrics`
