package com.example.bankbff.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Mirrors bankapi's AccountDto exactly -- field names and types must match its JSON. */
public record AccountDto(
        Long accountId,
        String accountNumber,
        String customerNumber,
        String accountType,
        String accountStatus,
        BigDecimal balance,
        LocalDate openedDate,
        String fullName
) {}
