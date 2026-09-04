package com.systemdesign.messaging;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Inbox Pattern / Idempotent Consumer Entity (Category 15).
 * Records consumed message IDs to ensure deduplication.
 */
@Entity
@Table(name = "inbox_processed_messages")
public class ProcessedMessage {

    @Id
    @Column(nullable = false, unique = true)
    private String messageId;

    @Column(nullable = false)
    private String consumerGroup;

    @Column(nullable = false)
    private Instant processedAt = Instant.now();

    protected ProcessedMessage() {}

    public ProcessedMessage(String messageId, String consumerGroup) {
        this.messageId = messageId;
        this.consumerGroup = consumerGroup;
        this.processedAt = Instant.now();
    }

    public String getMessageId() { return messageId; }
    public String getConsumerGroup() { return consumerGroup; }
    public Instant getProcessedAt() { return processedAt; }
}
