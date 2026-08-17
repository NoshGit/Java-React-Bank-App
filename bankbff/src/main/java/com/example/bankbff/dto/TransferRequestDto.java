package com.example.bankbff.dto;

import java.math.BigDecimal;

/** The source account comes from the URL path (POST /api/accounts/{accountId}/transfers), not this body. */
public record TransferRequestDto(
        Long toAccountId,
        BigDecimal amount
) {}
