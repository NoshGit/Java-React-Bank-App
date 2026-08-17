package com.example.bankapi.kafka;

import com.example.bankapi.entity.Transaction;
import com.example.bankapi.entity.TransactionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * BR-K1: every COMPLETED transaction is published to the Kafka statistics topic. FAILED
 * transactions publish nothing. Per the Kafka Message Schema doc, publishing happens "after
 * database commit" -- calling kafkaTemplate.send() directly from inside the caller's still-open
 * @Transactional method would publish before the commit actually happens (or even if it later
 * fails), so the send is deferred via a commit-time synchronization instead.
 */
@Slf4j
@Component
public class TransactionStatsPublisher {

    private final KafkaTemplate<String, TransactionStatsMessage> kafkaTemplate;
    private final String topic;

    public TransactionStatsPublisher(KafkaTemplate<String, TransactionStatsMessage> kafkaTemplate,
                                      @Value("${app.kafka.transaction-stats-topic}") String topic) {
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    public void publishIfCompleted(Transaction txn) {
        if (txn.getStatus() != TransactionStatus.COMPLETED) {
            log.debug("Transaction {} status is {}, skipping Kafka publish", txn.getTxnId(), txn.getStatus());
            return;
        }
        String key = txn.getTxnType().name();
        TransactionStatsMessage message = new TransactionStatsMessage(key, txn.getAmount());
        log.info("Publishing completed transaction {} to Kafka topic: {}", txn.getTxnId(), topic);

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    kafkaTemplate.send(topic, key, message);
                    log.debug("Transaction {} successfully published to Kafka after commit", txn.getTxnId());
                }
            });
        } else {
            kafkaTemplate.send(topic, key, message);
            log.debug("Transaction {} published to Kafka immediately", txn.getTxnId());
        }
    }
}
