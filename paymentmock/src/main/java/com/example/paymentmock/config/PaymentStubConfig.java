package com.example.paymentmock.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@Configuration
public class PaymentStubConfig {

    private static final Logger log = LoggerFactory.getLogger(PaymentStubConfig.class);
    private static final int PORT = 8090;

    private WireMockServer wireMockServer;

    @EventListener(ContextRefreshedEvent.class)
    public void start() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            return;
        }
        wireMockServer = new WireMockServer(
                WireMockConfiguration.wireMockConfig().port(PORT));
        wireMockServer.start();
        WireMock.configureFor("localhost", PORT);
        registerStubs();
        log.info("Payment mock started on port {}. Admin: http://localhost:{}/__admin/mappings",
                PORT, PORT);
    }

    @PreDestroy
    public void stop() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
            log.info("Payment mock stopped.");
        }
    }

    private void registerStubs() {

        // Catch-all success: every payment is accepted with 201.
        // Registered FIRST so the failure stub below takes priority. WireMock
        // evaluates stubs in reverse registration order (last registered wins),
        // the same ordering rule as the auth-required stubs in Lab 2-3.
        stubFor(post(urlEqualTo("/payments"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"ACCEPTED\",\"confirmation\":\"PMT-1001\"}")));

        // Failure trigger: amount == 999.99 returns 503.
        // Registered AFTER the catch-all so WireMock checks it first.
        // The JSONPath filter matches the amount numerically, so it is not
        // sensitive to how the number is formatted on the wire.
        stubFor(post(urlEqualTo("/payments"))
                .withRequestBody(matchingJsonPath("$[?(@.amount == 999.99)]"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"UNAVAILABLE\"}")));
    }
}