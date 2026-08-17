package com.example.bankbff.dto;

import java.math.BigDecimal;

/** Mirrors bankapi's TransferResultDto exactly. Used for transfer/payment/deposit/withdrawal responses. */
public record TransferResultDto(
        String transferId,
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount,
        String status
) {}
