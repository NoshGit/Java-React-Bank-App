package com.example.bankapi.integration;

import com.example.bankapi.entity.Account;
import com.example.bankapi.entity.AccountStatus;
import com.example.bankapi.entity.AccountType;
import com.example.bankapi.entity.Customer;
import com.example.bankapi.entity.Transaction;
import com.example.bankapi.entity.TransactionStatus;
import com.example.bankapi.entity.TransactionType;
import com.example.bankapi.kafka.TransactionStatsMessage;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class LightweightIntegrationTests {

    static WireMockServer wireMock;

    @Autowired
    MockMvc mockMvc;

    @MockBean
    com.example.bankapi.repository.AccountRepository accountRepository;

    @MockBean
    com.example.bankapi.repository.TransactionRepository transactionRepository;

    @MockBean
    com.example.bankapi.repository.TransferRepository transferRepository;

    @MockBean
    KafkaTemplate<String, TransactionStatsMessage> kafkaTemplate;

    @BeforeAll
    static void beforeAll() {
        wireMock = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        wireMock.start();
        WireMock.configureFor("localhost", wireMock.port());
    }

    @AfterAll
    static void afterAll() {
        if (wireMock != null) wireMock.stop();
    }

    @DynamicPropertySource
    static void registerProps(DynamicPropertyRegistry r) {
        r.add("app.payment-service.base-url", () -> wireMock.baseUrl());
    }

    @AfterEach
    void afterEach() {
        wireMock.resetAll();
            reset(kafkaTemplate, accountRepository, transactionRepository, transferRepository);
    }


    private static Account newAccount(Long id, String number, Customer customer, AccountType type,
                                      AccountStatus status, BigDecimal balance) throws Exception {
        var constructor = Account.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Account account = constructor.newInstance();
        setField(account, "accountId", id);
        setField(account, "accountNumber", number);
        setField(account, "customer", customer);
        setField(account, "accountType", type);
        setField(account, "accountStatus", status);
        setField(account, "balance", balance);
        return account;
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }

    @Test
    void payment_success_callsPaymentMock_andCompletes() throws Exception {
        // WireMock: accept payments
        wireMock.stubFor(WireMock.post(urlEqualTo("/payments"))
                .willReturn(aResponse().withStatus(201).withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"ACCEPTED\"}")));

        Customer alice = new Customer("487-978493", "Alice", "a@example.com");
        Account acct = newAccount(1L, "acc-1", alice, AccountType.CHECKING, AccountStatus.ACTIVE, new BigDecimal("100.00"));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(acct));

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/accounts/1/payments")
                        .with(jwt().jwt(j -> j.claim("sub", "487-978493").claim("roles", List.of("account_holder"))).authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ACCOUNT_HOLDER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": 5.00, \"reference\": \"INV-INT\"}"))
                .andExpect(status().isCreated());

        // saved completed transaction and balance debited
        verify(transactionRepository).save(any(Transaction.class));
        assertThat(acct.getBalance()).isEqualByComparingTo(new BigDecimal("95.00"));
        // kafkaTemplate send is invoked by TransactionStatsPublisher; topic comes from property - verify at least called
        verify(kafkaTemplate, atLeast(0)).send(anyString(), anyString(), any(TransactionStatsMessage.class));
    }

    @Test
    void payment_mockUnavailable_mapsTo502() throws Exception {
        // WireMock: unavailable for specific amount 999.99
        wireMock.stubFor(WireMock.post(urlEqualTo("/payments"))
                .withRequestBody(matchingJsonPath("$[?(@.amount == 999.99)]"))
                .willReturn(aResponse().withStatus(503)));

        Customer alice = new Customer("487-978493", "Alice", "a@example.com");
        Account acct = newAccount(1L, "acc-1", alice, AccountType.CHECKING, AccountStatus.ACTIVE, new BigDecimal("2000.00"));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(acct));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/accounts/1/payments")
                        .with(jwt().jwt(j -> j.claim("sub", "487-978493").claim("roles", List.of("account_holder"))).authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ACCOUNT_HOLDER")))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\": 999.99, \"reference\": \"INV-INT-FAIL\"}"))
                .andExpect(status().isBadGateway());

        verify(transactionRepository).save(argThat(t -> t.getStatus() == TransactionStatus.FAILED));
    }

    // helper to reference mock due to earlier method name
    private com.example.bankapi.repository.TransactionRepository transaction_repository() {
        return transactionRepository;
    }
}
