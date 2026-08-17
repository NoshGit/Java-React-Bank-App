package com.example.bankapi.dto;

import com.example.bankapi.entity.Transaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionDto(
        String txnId,
        Long accountId,
        String txnType,
        BigDecimal amount,
        String status,
        LocalDateTime txnDate,
        String description
) {
    public static TransactionDto from(Transaction txn) {
        return new TransactionDto(
                txn.getTxnId(),
                txn.getAccount().getAccountId(),
                txn.getTxnType().name(),
                txn.getAmount(),
                txn.getStatus().name(),
                txn.getTxnDate(),
                txn.getDescription()
        );
    }
}
