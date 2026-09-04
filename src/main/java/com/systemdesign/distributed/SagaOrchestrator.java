package com.systemdesign.distributed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Saga Orchestrator Pattern (Category 16).
 * <p>
 * Manages distributed transactions across multiple microservices or boundaries.
 * If a forward step fails, previously completed steps are rolled back
 * by executing their compensating transactions in reverse order.
 */
public class SagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SagaOrchestrator.class);

    public record SagaStep(
            String name,
            Runnable forwardAction,
            Runnable compensationAction
    ) {}

    public static class SagaExecutionException extends RuntimeException {
        public SagaExecutionException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static void executeSaga(String sagaId, List<SagaStep> steps) {
        log.info("Starting Saga [{}] with {} steps", sagaId, steps.size());
        List<SagaStep> completedSteps = new ArrayList<>();

        for (SagaStep step : steps) {
            try {
                log.info("Saga [{}]: Executing forward step [{}]", sagaId, step.name());
                step.forwardAction().run();
                completedSteps.add(step);
            } catch (Exception ex) {
                log.error("Saga [{}]: Step [{}] failed: {}. Initiating rollback compensations!", 
                        sagaId, step.name(), ex.getMessage());
                rollbackCompensations(sagaId, completedSteps);
                throw new SagaExecutionException("Saga [" + sagaId + "] failed at step [" + step.name() + "]", ex);
            }
        }
        log.info("Saga [{}] completed successfully!", sagaId);
    }

    private static void rollbackCompensations(String sagaId, List<SagaStep> completedSteps) {
        // Compensate in reverse order
        for (int i = completedSteps.size() - 1; i >= 0; i--) {
            SagaStep step = completedSteps.get(i);
            try {
                log.warn("Saga [{}]: Executing compensation for step [{}]", sagaId, step.name());
                step.compensationAction().run();
            } catch (Exception compEx) {
                log.error("Saga [{}]: CRITICAL! Compensation for step [{}] failed: {}", 
                        sagaId, step.name(), compEx.getMessage());
                // In production: send to DLQ / alert on-call
            }
        }
    }
}
