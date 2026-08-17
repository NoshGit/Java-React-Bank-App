package com.example.bankapi.exception;

/** BR-V2/V3/V4: request shape is valid JSON but violates a business input rule. */
public class InvalidRequestException extends RuntimeException {
    public InvalidRequestException(String message) {
        super(message);
    }
}
