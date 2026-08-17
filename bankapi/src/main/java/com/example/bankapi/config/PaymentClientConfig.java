package com.example.bankapi.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class PaymentClientConfig {

    /**
     * Builds from Spring Boot's auto-configured RestClient.Builder bean (not the static
     * RestClient.builder() factory) so this client inherits the application's actual Jackson
     * ObjectMapper and message converters. Building from the static factory instead was silently
     * sending empty request bodies to the payment mock -- WireMock still matched and returned 201
     * because the stub didn't check the body, so this was invisible until something asserted on
     * request content.
     */
    @Bean
    public RestClient paymentServiceRestClient(RestClient.Builder builder,
                                                @Value("${app.payment-service.base-url}") String baseUrl) {
        return builder.baseUrl(baseUrl).build();
    }
}
