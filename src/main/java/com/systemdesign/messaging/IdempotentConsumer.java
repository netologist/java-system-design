package com.systemdesign.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Consumer;

/**
 * Idempotent Consumer / Inbox Pattern Implementation (Category 15).
 * Guarantees exactly-once processing side-effects by checking message ID before execution.
 */
@Service
public class IdempotentConsumer {

    private static final Logger log = LoggerFactory.getLogger(IdempotentConsumer.class);
    private final ProcessedMessageRepository repository;

    public IdempotentConsumer(ProcessedMessageRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public boolean process(String messageId, String consumerGroup, Consumer<String> handler) {
        if (repository.existsById(messageId)) {
            log.info("Message [{}] already processed by [{}]. Skipping (Idempotent).", messageId, consumerGroup);
            return false;
        }

        try {
            // Record processed message ID
            repository.save(new ProcessedMessage(messageId, consumerGroup));
            // Execute business logic
            handler.accept(messageId);
            return true;
        } catch (DataIntegrityViolationException ex) {
            // Concurrent delivery of the same message ID
            log.warn("Concurrent duplicate message [{}] caught via DB constraint. Skipping.", messageId);
            return false;
        }
    }
}
