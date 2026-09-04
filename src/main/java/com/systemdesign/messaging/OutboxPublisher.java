package com.systemdesign.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Reliable Outbox Event Publisher (Category 15 & 26).
 * Polls uncommitted pending outbox events and dispatches them to message broker.
 */
@Service
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxEventRepository outboxEventRepository;

    public OutboxPublisher(OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;
    }

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findTop50ByStatusOrderByCreatedAtAsc(OutboxEvent.Status.PENDING);
        if (pending.isEmpty()) return;

        for (OutboxEvent event : pending) {
            try {
                // Simulate publishing to Kafka / RabbitMQ broker
                log.info("Dispatched Outbox Event [{}] type: {} for aggregate: {}", 
                        event.getId(), event.getEventType(), event.getAggregateId());
                event.markPublished();
            } catch (Exception ex) {
                log.error("Failed to publish outbox event [{}]: {}", event.getId(), ex.getMessage());
                event.incrementRetry();
            }
        }
        outboxEventRepository.saveAll(pending);
    }
}
