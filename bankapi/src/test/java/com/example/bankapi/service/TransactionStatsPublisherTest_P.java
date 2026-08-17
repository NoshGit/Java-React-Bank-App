package com.example.bankapi.kafka;

import com.example.bankapi.entity.Transaction;
import com.example.bankapi.entity.TransactionStatus;
import com.example.bankapi.entity.TransactionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionStatsPublisherTest_P {

    @Mock
    KafkaTemplate<String, com.example.bankapi.kafka.TransactionStatsMessage> kafkaTemplate;

    private com.example.bankapi.kafka.TransactionStatsPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new com.example.bankapi.kafka.TransactionStatsPublisher(kafkaTemplate, "tx-topic");
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void doesNotPublishWhenNotCompleted() {
        Transaction t = new Transaction(null, TransactionType.DEPOSIT, new BigDecimal("10.00"), TransactionStatus.FAILED, "x");

        publisher.publishIfCompleted(t);

        verify(kafkaTemplate, never()).send(eq("tx-topic"), eq("DEPOSIT"), org.mockito.Mockito.any());
    }

    @Test
    void publishesImmediatelyWhenNoTransactionSyncActive() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }

        Transaction t = new Transaction(null, TransactionType.DEPOSIT, new BigDecimal("10.00"), TransactionStatus.COMPLETED, "x");

        publisher.publishIfCompleted(t);

        verify(kafkaTemplate).send(eq("tx-topic"), eq("DEPOSIT"), org.mockito.Mockito.any());
    }

    @Test
    void defersWhenTransactionSyncActive() {
        TransactionSynchronizationManager.initSynchronization();

        Transaction t = new Transaction(null, TransactionType.DEPOSIT, new BigDecimal("10.00"), TransactionStatus.COMPLETED, "x");

        publisher.publishIfCompleted(t);

        verify(kafkaTemplate, never()).send(eq("tx-topic"), eq("DEPOSIT"), org.mockito.Mockito.any());

        TransactionSynchronizationManager.clearSynchronization();
    }
}
