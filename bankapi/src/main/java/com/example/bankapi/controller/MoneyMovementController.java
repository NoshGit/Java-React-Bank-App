package com.example.bankapi.controller;

import com.example.bankapi.dto.AmountRequestDto;
import com.example.bankapi.dto.PaymentRequestDto;
import com.example.bankapi.dto.TransferRequestDto;
import com.example.bankapi.dto.TransferResultDto;
import com.example.bankapi.service.MoneyMovementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MoneyMovementController {

    private final MoneyMovementService moneyMovementService;

    public MoneyMovementController(MoneyMovementService moneyMovementService) {
        this.moneyMovementService = moneyMovementService;
    }

    @PostMapping("/api/accounts/{accountId}/transfers")
    public ResponseEntity<TransferResultDto> transfer(@PathVariable Long accountId,
                                                        @Valid @RequestBody TransferRequestDto request,
                                                        Authentication authentication) {
        TransferResultDto result = moneyMovementService.transfer(
                accountId, request.toAccountId(), request.amount(), authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/api/accounts/{accountId}/payments")
    public ResponseEntity<TransferResultDto> payment(@PathVariable Long accountId,
                                                       @Valid @RequestBody PaymentRequestDto request,
                                                       Authentication authentication) {
        TransferResultDto result = moneyMovementService.payment(
                accountId, request.amount(), request.reference(), authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /** Teller only. */
    @PostMapping("/api/accounts/{accountId}/deposits")
    public ResponseEntity<TransferResultDto> deposit(@PathVariable Long accountId,
                                                       @Valid @RequestBody AmountRequestDto request) {
        TransferResultDto result = moneyMovementService.deposit(accountId, request.amount());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /** Teller only. */
    @PostMapping("/api/accounts/{accountId}/withdrawals")
    public ResponseEntity<TransferResultDto> withdrawal(@PathVariable Long accountId,
                                                          @Valid @RequestBody AmountRequestDto request) {
        TransferResultDto result = moneyMovementService.withdrawal(accountId, request.amount());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
