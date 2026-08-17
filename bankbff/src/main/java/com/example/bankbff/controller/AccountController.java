package com.example.bankbff.controller;

import com.example.bankbff.client.BankingApiClient;
import com.example.bankbff.dto.AccountDto;
import com.example.bankbff.dto.AccountStatusUpdateDto;
import com.example.bankbff.dto.AmountRequestDto;
import com.example.bankbff.dto.PaymentRequestDto;
import com.example.bankbff.dto.TransactionDto;
import com.example.bankbff.dto.TransactionReportLineDto;
import com.example.bankbff.dto.TransferRequestDto;
import com.example.bankbff.dto.TransferResultDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AccountController {

    private final BankingApiClient bankingApiClient;

    public AccountController(BankingApiClient bankingApiClient) {
        this.bankingApiClient = bankingApiClient;
    }

    @GetMapping("/accounts")
    public List<AccountDto> getMyAccounts() {
        return bankingApiClient.getMyAccounts();
    }

    @GetMapping("/customers/{customerNumber}/accounts")
    public List<AccountDto> getAccountsForCustomer(@PathVariable String customerNumber) {
        return bankingApiClient.getAccountsForCustomer(customerNumber);
    }

    @GetMapping("/accounts/{accountId}")
    public AccountDto getAccount(@PathVariable Long accountId) {
        return bankingApiClient.getAccount(accountId);
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public List<TransactionDto> getTransactions(@PathVariable Long accountId) {
        return bankingApiClient.getTransactions(accountId);
    }

    // bankapi returns 201 Created for all four of these; Spring MVC defaults to 200 for a plain
    // returned object, so the status has to be set explicitly to actually relay bankapi's contract.
    @PostMapping("/accounts/{accountId}/transfers")
    public ResponseEntity<TransferResultDto> transfer(@PathVariable Long accountId, @RequestBody TransferRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bankingApiClient.transfer(accountId, request));
    }

    @PostMapping("/accounts/{accountId}/payments")
    public ResponseEntity<TransferResultDto> payment(@PathVariable Long accountId, @RequestBody PaymentRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bankingApiClient.payment(accountId, request));
    }

    @PostMapping("/accounts/{accountId}/deposits")
    public ResponseEntity<TransferResultDto> deposit(@PathVariable Long accountId, @RequestBody AmountRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bankingApiClient.deposit(accountId, request));
    }

    @PostMapping("/accounts/{accountId}/withdrawals")
    public ResponseEntity<TransferResultDto> withdrawal(@PathVariable Long accountId, @RequestBody AmountRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bankingApiClient.withdrawal(accountId, request));
    }

    @PutMapping("/accounts/{accountId}/status")
    public AccountDto updateStatus(@PathVariable Long accountId, @RequestBody AccountStatusUpdateDto request) {
        return bankingApiClient.updateStatus(accountId, request);
    }

    @GetMapping("/reports/transactions")
    public List<TransactionReportLineDto> getTransactionReport() {
        return bankingApiClient.getTransactionReport();
    }
}
