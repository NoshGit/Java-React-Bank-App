package com.example.bankapi.controller;

import com.example.bankapi.dto.AccountDto;
import com.example.bankapi.dto.AccountStatusUpdateDto;
import com.example.bankapi.dto.TransactionDto;
import com.example.bankapi.dto.TransactionReportLineDto;
import com.example.bankapi.entity.AccountStatus;
import com.example.bankapi.service.AccountService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    /** BR-A4: account_holder role only; teller gets 403. */
    @GetMapping("/api/accounts")
    public List<AccountDto> getMyAccounts(Authentication authentication) {
        return  accountService.getMyAccounts(authentication);
    }

    /** Teller only; BR-A6: 404 if customer missing. */
    @GetMapping("/api/customers/{customerNumber}/accounts")
    public List<AccountDto> getAccountsForCustomer(@PathVariable String customerNumber) {
        return accountService.getAccountsForCustomer(customerNumber);
    }

    /** account_holder or teller; ownership enforced for account_holder. */
    @GetMapping("/api/accounts/{accountId}")
    public AccountDto getAccount(@PathVariable Long accountId, Authentication authentication) {
        log.info("Fetching account {} for user: {}", accountId, authentication.getName());
        AccountDto account = accountService.getAccount(accountId, authentication);
        log.debug("Account {} retrieved successfully", accountId);
        return account;
    }

    @GetMapping("/api/accounts/{accountId}/transactions")
    public List<TransactionDto> getTransactions(@PathVariable Long accountId, Authentication authentication) {
        return accountService.getTransactions(accountId, authentication);
    }

    /** Teller only (BR-A3, BR-S3). */
    @PutMapping("/api/accounts/{accountId}/status")
    public ResponseEntity<AccountDto> updateStatus(@PathVariable Long accountId,
                                                     @Valid @RequestBody AccountStatusUpdateDto body) {
        AccountDto updated = accountService.updateStatus(accountId, AccountStatus.valueOf(body.status()));
        return ResponseEntity.ok(updated);
    }

    /** Teller only (BR-A3). */
    @GetMapping("/api/reports/transactions")
    public List<TransactionReportLineDto> getTransactionReport() {
        return accountService.getTransactionReport();
    }
}
