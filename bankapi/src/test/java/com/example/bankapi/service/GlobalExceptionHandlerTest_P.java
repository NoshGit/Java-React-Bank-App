package com.example.bankapi.exception;

import com.example.bankapi.dto.ErrorResponseDto;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest_P {

    GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleNotFound_returns404() {
        ResponseEntity<ErrorResponseDto> r = handler.handleNotFound(new RuntimeException("nope"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(r.getBody().message()).contains("nope");
    }

    @Test
    void handleAccessDenied_returns403() {
        ResponseEntity<ErrorResponseDto> r = handler.handleAccessDenied(new AccessDeniedException("x"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(r.getBody().status()).isEqualTo(403);
    }

    @Test
    void handleBusinessRuleViolation_returns422() {
        ResponseEntity<ErrorResponseDto> r = handler.handleBusinessRuleViolation(new RuntimeException("bad"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void handleExternalPaymentFailure_returns502() {
        ResponseEntity<ErrorResponseDto> r = handler.handleExternalPaymentFailure(new com.example.bankapi.exception.ExternalPaymentException("boom"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void handleBadRequest_returns400() {
        ResponseEntity<ErrorResponseDto> r = handler.handleBadRequest(new IllegalArgumentException("bad arg"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void handleDataAccess_returns409() {
        ResponseEntity<ErrorResponseDto> r = handler.handleDataAccess(new DataIntegrityViolationException("dup"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void handleUnexpected_returns500() {
        ResponseEntity<ErrorResponseDto> r = handler.handleUnexpected(new Exception("oops"));
        assertThat(r.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
