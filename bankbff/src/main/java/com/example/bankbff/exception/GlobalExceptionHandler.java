package com.example.bankbff.exception;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Relays bankapi's real status code and JSON body back to the caller unchanged, instead of
 * letting an unhandled WebClient exception collapse into a generic 500. Without this, banking-ui
 * would never see the 403/422/502/404 bankapi actually returned.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DownstreamApiException.class)
    public ResponseEntity<String> handleDownstreamApiException(DownstreamApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(ex.getBody());
    }
}
