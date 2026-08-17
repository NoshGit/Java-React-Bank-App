package com.example.bankbff;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Login;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies bankbff's own logic (paths, DTO field mapping, error relaying) against a WireMock
 * stand-in for bankapi, matching bankapi's real contract exactly. Real end-to-end verification
 * (bankbff -> real bankapi with a real login-issued token) needs a running authserver + a
 * scripted OAuth2 authorization-code flow, which is a separate, heavier check -- this instead
 * targets exactly the bugs that existed here: wrong endpoint paths, wrong JSON field names, and
 * bankapi's real status codes/bodies being swallowed into a generic 500.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class BankbffApplicationTests {

    static WireMockServer bankApiMock;

    @BeforeAll
    static void startMock() {
        bankApiMock = new WireMockServer(18081);
        bankApiMock.start();
    }

    @AfterAll
    static void stopMock() {
        if (bankApiMock != null) {
            bankApiMock.stop();
        }
    }

    @BeforeEach
    void resetMock() {
        bankApiMock.resetAll();
    }

    @DynamicPropertySource
    static void wireBankApiBaseUrl(DynamicPropertyRegistry registry) {
        registry.add("banking.resource-server.base-url", () -> "http://localhost:18081");
    }

    @Autowired
    MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    // Confirms the path fix: bankbff must call /api/accounts, not the old /api/v1/accounts,
    // and must map bankapi's real field names (accountId, customerNumber, ...) correctly.
    @Test
    void getMyAccounts_callsCorrectPathAndMapsFields() throws Exception {
        WireMock.configureFor("localhost", 18081);
        WireMock.stubFor(WireMock.get(urlEqualTo("/api/accounts"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                [{"accountId":1,"accountNumber":"128-9878-001","customerNumber":"487-978493",
                                  "accountType":"CHECKING","accountStatus":"ACTIVE","balance":5000.00,"openedDate":"2026-01-01"}]
                                """)));

        mockMvc.perform(get("/api/accounts").with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountId").value(1))
                .andExpect(jsonPath("$[0].customerNumber").value("487-978493"))
                .andExpect(jsonPath("$[0].balance").value(5000.00));

        verify(WireMock.getRequestedFor(urlEqualTo("/api/accounts")));
    }

    // Confirms the request-shape fix: {toAccountId, amount} in the body, accountId in the URL --
    // not the old flat {fromAccountId, toAccountId, amount}.
    @Test
    void transfer_postsCorrectPathAndBody() throws Exception {
        WireMock.configureFor("localhost", 18081);
        WireMock.stubFor(WireMock.post(urlEqualTo("/api/accounts/1/transfers"))
                .willReturn(aResponse().withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"transferId":null,"fromAccountId":1,"toAccountId":2,"amount":50.00,"status":"COMPLETED"}
                                """)));

        mockMvc.perform(post("/api/accounts/1/transfers")
                        .with(oauth2Login())
                        .contentType("application/json")
                        .content("{\"toAccountId\": 2, \"amount\": 50.00}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        verify(postRequestedFor(urlEqualTo("/api/accounts/1/transfers"))
                .withRequestBody(equalToJson("{\"toAccountId\": 2, \"amount\": 50.00}")));
    }

    @Test
    void deposit_postsToNewEndpoint() throws Exception {
        WireMock.configureFor("localhost", 18081);
        WireMock.stubFor(WireMock.post(urlEqualTo("/api/accounts/1/deposits"))
                .willReturn(aResponse().withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"transferId\":null,\"fromAccountId\":1,\"toAccountId\":null,\"amount\":25.00,\"status\":\"COMPLETED\"}")));

        mockMvc.perform(post("/api/accounts/1/deposits")
                        .with(oauth2Login())
                        .contentType("application/json")
                        .content("{\"amount\": 25.00}"))
                .andExpect(status().isCreated());

        verify(postRequestedFor(urlEqualTo("/api/accounts/1/deposits")));
    }

    @Test
    void updateStatus_putsToNewEndpoint() throws Exception {
        WireMock.configureFor("localhost", 18081);
        WireMock.stubFor(WireMock.put(urlEqualTo("/api/accounts/3/status"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"accountId":3,"accountNumber":"128-9878-003","customerNumber":"487-978493",
                                 "accountType":"CHECKING","accountStatus":"ACTIVE","balance":0.00,"openedDate":"2026-01-01"}
                                """)));

        mockMvc.perform(put("/api/accounts/3/status")
                        .with(oauth2Login())
                        .contentType("application/json")
                        .content("{\"status\": \"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));
    }

    @Test
    void transactionReport_callsNewEndpoint() throws Exception {
        WireMock.configureFor("localhost", 18081);
        WireMock.stubFor(WireMock.get(urlEqualTo("/api/reports/transactions"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"txnType\":\"DEPOSIT\",\"count\":3,\"totalAmount\":75.00}]")));

        mockMvc.perform(get("/api/reports/transactions").with(oauth2Login()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].txnType").value("DEPOSIT"));
    }

    // The core error-relay fix: bankapi's real status code and body must reach the caller
    // unchanged, not collapse into a generic 500.
    @Test
    void payment_relaysDownstream422Unchanged() throws Exception {
        WireMock.configureFor("localhost", 18081);
        WireMock.stubFor(WireMock.post(urlEqualTo("/api/accounts/1/payments"))
                .willReturn(aResponse().withStatus(422)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"timestamp\":\"2026-08-06T00:00:00Z\",\"status\":422,\"error\":\"Unprocessable Entity\",\"message\":\"Insufficient funds in account 1\"}")));

        mockMvc.perform(post("/api/accounts/1/payments")
                        .with(oauth2Login())
                        .contentType("application/json")
                        .content("{\"amount\": 999999.99, \"reference\": \"INV-1\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Insufficient funds in account 1"));
    }

    @Test
    void payment_relaysDownstream502Unchanged() throws Exception {
        WireMock.configureFor("localhost", 18081);
        WireMock.stubFor(WireMock.post(urlEqualTo("/api/accounts/1/payments"))
                .willReturn(aResponse().withStatus(502)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"timestamp\":\"2026-08-06T00:00:00Z\",\"status\":502,\"error\":\"Bad Gateway\",\"message\":\"Payment service unavailable\"}")));

        mockMvc.perform(post("/api/accounts/1/payments")
                        .with(oauth2Login())
                        .contentType("application/json")
                        .content("{\"amount\": 999.99, \"reference\": \"INV-2\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502));
    }

    @Test
    void getAccount_relaysDownstream404Unchanged() throws Exception {
        WireMock.configureFor("localhost", 18081);
        WireMock.stubFor(WireMock.get(urlEqualTo("/api/accounts/999"))
                .willReturn(aResponse().withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"timestamp\":\"2026-08-06T00:00:00Z\",\"status\":404,\"error\":\"Not Found\",\"message\":\"No such account: 999\"}")));

        mockMvc.perform(get("/api/accounts/999").with(oauth2Login()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
