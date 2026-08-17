package com.example.bankapi.exception;

import com.example.bankapi.dto.ErrorResponseDto;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({AccountNotFoundException.class, CustomerNotFoundException.class})
    public ResponseEntity<ErrorResponseDto> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResponseDto.of(404, "Not Found", ex.getMessage()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDto> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponseDto.of(403, "Forbidden", "Not authorized for this resource"));
    }

    @ExceptionHandler({AccountNotActiveException.class, InsufficientFundsException.class})
    public ResponseEntity<ErrorResponseDto> handleBusinessRuleViolation(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponseDto.of(422, "Unprocessable Entity", ex.getMessage()));
    }

    @ExceptionHandler(ExternalPaymentException.class)
    public ResponseEntity<ErrorResponseDto> handleExternalPaymentFailure(ExternalPaymentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ErrorResponseDto.of(502, "Bad Gateway", ex.getMessage()));
    }

    @ExceptionHandler({InvalidRequestException.class,
            MethodArgumentNotValidException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            IllegalArgumentException.class})
    public ResponseEntity<ErrorResponseDto> handleBadRequest(Exception ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponseDto.of(400, "Bad Request", ex.getMessage()));
    }

    @ExceptionHandler({DataIntegrityViolationException.class, CannotAcquireLockException.class})
    public ResponseEntity<ErrorResponseDto> handleDataAccess(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponseDto.of(409, "Conflict", "The request could not be completed due to a data conflict"));
    }

    /** Keeps unexpected failures in the same response shape as everything else, instead of
     *  falling through to Spring Boot's default /error body. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> handleUnexpected(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponseDto.of(500, "Internal Server Error", "An unexpected error occurred"));
    }
}
