package com.example.bankapi.exception;

/** BR-P3: external payment service failure. A FAILED PAYMENT transaction has already been recorded when this is thrown. */
public class ExternalPaymentException extends RuntimeException {
    public ExternalPaymentException(String message) {
        super(message);
    }
}
