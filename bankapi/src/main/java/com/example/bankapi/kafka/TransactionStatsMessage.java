package com.example.bankapi.kafka;

import java.math.BigDecimal;

/**
 * Deliberately omits txn id, account id, customer, and timestamp so the
 * stream supports aggregate analytics without being traceable to an
 * individual or an account (Capstone Kafka Message Schema).
 */
public record TransactionStatsMessage(String type, BigDecimal amount) {
}
