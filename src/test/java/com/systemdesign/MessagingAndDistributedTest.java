package com.systemdesign;

import com.systemdesign.dataintegrity.PaymentState;
import com.systemdesign.dataintegrity.PaymentStateMachine;
import com.systemdesign.distributed.SagaOrchestrator;
import com.systemdesign.messaging.IdempotentConsumer;
import com.systemdesign.messaging.OutboxEvent;
import com.systemdesign.messaging.OutboxEventRepository;
import com.systemdesign.messaging.OutboxPublisher;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class MessagingAndDistributedTest {

    @Autowired
    private OutboxEventRepository outboxRepository;

    @Autowired
    private OutboxPublisher outboxPublisher;

    @Autowired
    private IdempotentConsumer idempotentConsumer;

    @Test
    @DisplayName("OutboxPublisher reads pending events and transitions status to PUBLISHED")
    void testOutboxPattern() {
        OutboxEvent event = new OutboxEvent("ACCOUNT", "ACC-999", "ACCOUNT_DEBITED", "{\"amount\":1000}");
        outboxRepository.save(event);

        outboxPublisher.publishPendingEvents();

        OutboxEvent refreshed = outboxRepository.findById(event.getId()).orElseThrow();
        assertThat(refreshed.getStatus()).isEqualTo(OutboxEvent.Status.PUBLISHED);
        assertThat(refreshed.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("IdempotentConsumer processes first delivery and skips duplicate deliveries")
    void testIdempotentConsumer() {
        String messageId = "msg-unique-555";
        AtomicBoolean processed = new AtomicBoolean(false);

        // First attempt: processed
        boolean firstAttempt = idempotentConsumer.process(messageId, "payment-group", id -> processed.set(true));
        assertThat(firstAttempt).isTrue();
        assertThat(processed.get()).isTrue();

        // Second attempt: skipped
        processed.set(false);
        boolean secondAttempt = idempotentConsumer.process(messageId, "payment-group", id -> processed.set(true));
        assertThat(secondAttempt).isFalse();
        assertThat(processed.get()).isFalse();
    }

    @Test
    @DisplayName("SagaOrchestrator rolls back completed steps in reverse order on failure")
    void testSagaOrchestratorRollback() {
        List<String> log = new ArrayList<>();

        var step1 = new SagaOrchestrator.SagaStep(
                "ReserveCredit",
                () -> log.add("CreditReserved"),
                () -> log.add("CreditCompensated")
        );

        var step2 = new SagaOrchestrator.SagaStep(
                "AuthorizeCard",
                () -> log.add("CardAuthorized"),
                () -> log.add("CardCompensated")
        );

        var step3 = new SagaOrchestrator.SagaStep(
                "BookOrder",
                () -> { throw new RuntimeException("Inventory unavailable"); },
                () -> log.add("OrderCompensated")
        );

        assertThatThrownBy(() -> SagaOrchestrator.executeSaga("saga-order-1", List.of(step1, step2, step3)))
                .isInstanceOf(SagaOrchestrator.SagaExecutionException.class);

        // Verify compensation occurred in reverse order: CardCompensated then CreditCompensated
        assertThat(log).containsExactly("CreditReserved", "CardAuthorized", "CardCompensated", "CreditCompensated");
    }

    @Test
    @DisplayName("PaymentStateMachine strictly enforces legal transitions on sealed state records")
    void testPaymentStateMachine() {
        PaymentStateMachine psm = new PaymentStateMachine();

        PaymentState init = new PaymentState.Initiated("PAY-1", 5000, Instant.now());

        // Initiated -> Authorized
        PaymentState auth = psm.transition(init, "AUTHORIZE", "AUTH_CODE_OK");
        assertThat(auth).isInstanceOf(PaymentState.Authorized.class);

        // Authorized -> Captured
        PaymentState captured = psm.transition(auth, "CAPTURE", "CAP_REF_123");
        assertThat(captured).isInstanceOf(PaymentState.Captured.class);

        // Captured -> Refunded
        PaymentState refunded = psm.transition(captured, "REFUND", "CUSTOMER_REQUEST");
        assertThat(refunded).isInstanceOf(PaymentState.Refunded.class);

        // Illegal transition: Cannot transition a Refunded payment
        assertThatThrownBy(() -> psm.transition(refunded, "CAPTURE", "REF_AGAIN"))
                .isInstanceOf(IllegalStateException.class);
    }
}
