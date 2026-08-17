package com.example.statsconsumer.model;

import java.math.BigDecimal;

public record TransactionStats(
        String type,
        BigDecimal amount
) {}