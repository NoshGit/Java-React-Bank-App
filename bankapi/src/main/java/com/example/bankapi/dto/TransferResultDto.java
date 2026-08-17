package com.example.bankapi.dto;

import java.math.BigDecimal;

public record TransferResultDto(
        String transferId,
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount,
        String status
) {
}
