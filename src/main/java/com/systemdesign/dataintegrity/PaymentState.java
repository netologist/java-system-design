package com.systemdesign.dataintegrity;

import java.time.Instant;

/**
 * State Machine Pattern utilizing Java 21 Sealed Interfaces (Category 23).
 * <p>
 * Forms a strictly closed type hierarchy representing payment lifecycle states.
 * Guarantees compiler-enforced exhaustiveness in pattern matching switch expressions.
 */
public sealed interface PaymentState permits
        PaymentState.Initiated,
        PaymentState.Authorized,
        PaymentState.Captured,
        PaymentState.Refunded,
        PaymentState.Failed {

    String paymentId();
    Instant timestamp();

    record Initiated(String paymentId, long amountCents, Instant timestamp) implements PaymentState {}
    record Authorized(String paymentId, long amountCents, String authCode, Instant timestamp) implements PaymentState {}
    record Captured(String paymentId, long amountCents, String captureRef, Instant timestamp) implements PaymentState {}
    record Refunded(String paymentId, long refundAmountCents, String reason, Instant timestamp) implements PaymentState {}
    record Failed(String paymentId, String errorCode, String errorMessage, Instant timestamp) implements PaymentState {}
}
