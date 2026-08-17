package com.example.bankapi.exception;

/** BR-F1: debit only succeeds if balance >= amount. A FAILED transaction has already been recorded when this is thrown. */
public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String message) {
        super(message);
    }
}
