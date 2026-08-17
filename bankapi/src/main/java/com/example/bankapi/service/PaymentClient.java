package com.example.bankapi.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

/**
 * Calls the external payment mock (Capstone/Wiremock/Setting Up the Payment Mock.md).
 * POST /payments {amount, reference} -> 201 ACCEPTED, or 503 UNAVAILABLE.
 * amount == 999.99 is the deterministic failure switch (BR-P4).
 */
@Component
public class PaymentClient {

    private static final Logger log = LoggerFactory.getLogger(PaymentClient.class);

    private final RestClient restClient;

    public PaymentClient(RestClient paymentServiceRestClient) {
        this.restClient = paymentServiceRestClient;
    }

    public boolean submitPayment(BigDecimal amount, String reference) {
        try {
            restClient.post()
                    .uri("/payments")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new PaymentMockRequest(amount, reference))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception ex) {
            // BR-P3 treats any failure here (including the mock being unreachable) as an
            // external service failure; logging it is the only way to tell "mock said no"
            // apart from "we never actually reached the mock" during live debugging.
            log.warn("Payment mock call failed for amount={}: {}", amount, ex.toString());
            return false;
        }
    }

    private record PaymentMockRequest(BigDecimal amount, String reference) {
    }
}
