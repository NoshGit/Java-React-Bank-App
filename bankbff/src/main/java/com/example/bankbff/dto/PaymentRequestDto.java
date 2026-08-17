package com.example.bankbff.dto;

import java.math.BigDecimal;

public record PaymentRequestDto(
        BigDecimal amount,
        String reference
) {}
