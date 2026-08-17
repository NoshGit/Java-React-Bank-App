package com.example.bankbff.dto;

import java.math.BigDecimal;

public record TransactionReportLineDto(
        String txnType,
        long count,
        BigDecimal totalAmount
) {}
