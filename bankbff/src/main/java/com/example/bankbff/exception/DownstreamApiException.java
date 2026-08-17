package com.example.bankbff.exception;

import org.springframework.http.HttpStatusCode;

/** Carries bankapi's real status code and response body so they can be relayed unchanged. */
public class DownstreamApiException extends RuntimeException {

    private final HttpStatusCode status;
    private final String body;

    public DownstreamApiException(HttpStatusCode status, String body) {
        super("Downstream bankapi call failed: " + status);
        this.status = status;
        this.body = body;
    }

    public HttpStatusCode getStatus() {
        return status;
    }

    public String getBody() {
        return body;
    }
}
