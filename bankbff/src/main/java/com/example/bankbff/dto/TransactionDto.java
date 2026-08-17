package com.example.bankbff.dto;

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
) {}
