package com.systemdesign.dataintegrity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * Payment State Machine Transition Engine (Category 23).
 * <p>
 * Validates domain state transitions using Java 21 Pattern Matching for switch.
 * Notice: NO {@code default ->} branch is used; the compiler verifies that all
 * sealed subtypes are exhaustively handled.
 */
public class PaymentStateMachine {

    private static final Logger log = LoggerFactory.getLogger(PaymentStateMachine.class);

    public PaymentState transition(PaymentState currentState, String action, String referenceOrReason) {
        return switch (currentState) {
            case PaymentState.Initiated init -> {
                if ("AUTHORIZE".equalsIgnoreCase(action)) {
                    log.info("Transitioning [{}] from Initiated to Authorized", init.paymentId());
                    yield new PaymentState.Authorized(init.paymentId(), init.amountCents(), referenceOrReason, Instant.now());
                } else if ("FAIL".equalsIgnoreCase(action)) {
                    yield new PaymentState.Failed(init.paymentId(), "AUTH_DECLINED", referenceOrReason, Instant.now());
                }
                throw new IllegalStateException("Cannot perform action '" + action + "' on Initiated payment");
            }
            case PaymentState.Authorized auth -> {
                if ("CAPTURE".equalsIgnoreCase(action)) {
                    log.info("Transitioning [{}] from Authorized to Captured", auth.paymentId());
                    yield new PaymentState.Captured(auth.paymentId(), auth.amountCents(), referenceOrReason, Instant.now());
                } else if ("CANCEL".equalsIgnoreCase(action)) {
                    yield new PaymentState.Failed(auth.paymentId(), "VOIDED", referenceOrReason, Instant.now());
                }
                throw new IllegalStateException("Cannot perform action '" + action + "' on Authorized payment");
            }
            case PaymentState.Captured cap -> {
                if ("REFUND".equalsIgnoreCase(action)) {
                    log.info("Transitioning [{}] from Captured to Refunded", cap.paymentId());
                    yield new PaymentState.Refunded(cap.paymentId(), cap.amountCents(), referenceOrReason, Instant.now());
                }
                throw new IllegalStateException("Cannot perform action '" + action + "' on Captured payment");
            }
            case PaymentState.Refunded ref ->
                throw new IllegalStateException("Terminal state: Cannot transition a Refunded payment");
            case PaymentState.Failed fail ->
                throw new IllegalStateException("Terminal state: Cannot transition a Failed payment");
        };
    }
}
