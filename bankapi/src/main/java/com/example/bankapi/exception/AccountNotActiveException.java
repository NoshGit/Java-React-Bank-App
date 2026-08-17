package com.example.bankapi.exception;

/** BR-S1: money movement is prohibited on INACTIVE accounts; no transaction row is created. */
public class AccountNotActiveException extends RuntimeException {
    public AccountNotActiveException(String message) {
        super(message);
    }
}
