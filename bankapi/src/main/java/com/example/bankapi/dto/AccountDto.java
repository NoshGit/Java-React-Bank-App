package com.example.bankapi.dto;

import com.example.bankapi.entity.Account;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AccountDto(
        Long accountId,
        String accountNumber,
        String customerNumber,
        String accountType,
        String accountStatus,
        BigDecimal balance,
        LocalDate openedDate,
        String fullName
) {
    public static AccountDto from(Account account) {
        return new AccountDto(
                account.getAccountId(),
                account.getAccountNumber(),
                account.getCustomer().getCustomerNumber(),
                account.getAccountType().name(),
                account.getAccountStatus().name(),
                account.getBalance(),
                account.getOpenedDate(),
                account.getCustomer().getFullName()
        );
    }
}
