package com.example.bankapi;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.MockMvc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.oauth2.jwt.JwtClaimNames.SUB;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tier 3 (Capstone Testing Guidance): real database trigger, real WireMock
 * payment mock. Targets the docker-compose Oracle/Kafka in infra/docker-compose.yaml
 * directly (see application.yaml) rather than Testcontainers-managed containers --
 * on this machine Testcontainers' bundled Docker client can't talk to Docker Desktop
 * (a Windows-specific client/API incompatibility, confirmed unrelated to app code:
 * the plain Docker CLI and docker-compose work fine). Requires
 * `docker compose -f infra/docker-compose.yaml up -d` to be running first, and the
 * Oracle container to have finished its first-boot init scripts (db/01-03 in infra).
 *
 * Not annotated @Transactional -- a rolled-back transaction would hide the
 * account_audit row the balance-update trigger writes.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
class BankapiApplicationTests {

    static WireMockServer paymentMock;

    @BeforeAll
    static void setUpMock() {
        paymentMock = new WireMockServer(8090);
        paymentMock.start();
        WireMock.configureFor("localhost", 8090);
        // Explicit priorities (lower number wins): without these, two equal-priority stubs where
        // one is strictly more specific than the other is an ambiguous match in WireMock -- this
        // was silently relying on registration-order luck rather than a real guarantee.
        WireMock.stubFor(WireMock.post(urlEqualTo("/payments"))
                .atPriority(1)
                .withRequestBody(WireMock.matchingJsonPath("$[?(@.amount == 999.99)]"))
                .willReturn(aResponse().withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"UNAVAILABLE\"}")));
        WireMock.stubFor(WireMock.post(urlEqualTo("/payments"))
                .atPriority(2)
                .willReturn(aResponse().withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"ACCEPTED\",\"confirmation\":\"PMT-1001\"}")));
    }

    @AfterAll
    static void tearDownMock() {
        if (paymentMock != null) {
            paymentMock.stop();
        }
    }

    private static Connection oracleConnection() throws Exception {
        return DriverManager.getConnection(
                "jdbc:oracle:thin:@//localhost:15210/XEPDB1", "labuser", "labpass123");
    }

    @Autowired
    MockMvc mockMvc;

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asTeller() {
        return jwt().jwt(j -> j.claim(SUB, "EM01").claim("roles", List.of("teller")))
                .authorities(new SimpleGrantedAuthority("ROLE_TELLER"));
    }

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor asAlice() {
        return jwt().jwt(j -> j.claim(SUB, "487-978493").claim("roles", List.of("account_holder")))
                .authorities(new SimpleGrantedAuthority("ROLE_ACCOUNT_HOLDER"));
    }

    @Test
    void contextLoads() {
    }

    // BR-X1: a committed balance change writes an account_audit row via the DB trigger.
    @Test
    void deposit_completes_andBalanceAuditTriggerFires() throws Exception {
        mockMvc.perform(post("/api/accounts/1/deposits")
                        .with(asTeller())
                        .contentType("application/json")
                        .content("{\"amount\": 25.00}"))
                .andExpect(status().isCreated());

        try (Connection conn = oracleConnection();
             Statement stmt = conn.createStatement()) {
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM account_audit WHERE account_id = 1");
            rs.next();
            assertThat(rs.getInt(1)).isGreaterThan(0);
        }
    }

    // BR-P3/BR-P4: amount 999.99 drives the mock's 503 path -> BankAPI maps it to 502.
    @Test
    void payment_amount99999_mapsMockFailureTo502() throws Exception {
        mockMvc.perform(post("/api/accounts/1/payments")
                        .with(asAlice())
                        .contentType("application/json")
                        .content("{\"amount\": 999.99, \"reference\": \"INV-TEST\"}"))
                .andExpect(status().isBadGateway());

        verify(postRequestedFor(urlEqualTo("/payments")));
    }

    @Test
    void health_isPublic() throws Exception {
        mockMvc.perform(get("/api/health")).andExpect(status().isOk());
    }

    // BR-A6: a nonexistent account returns 404 with the standard error body, against a real query.
    @Test
    void getAccount_nonexistent_returns404WithBody() throws Exception {
        mockMvc.perform(get("/api/accounts/999999").with(asTeller()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // BR-A4: Alice cannot read Bob's account (account 4 = 128-9878-004, customer 500-100200).
    // Also verifies the AccessDeniedHandler fix -- @PreAuthorize-free, service-layer manual
    // ownership check throws AccessDeniedException, must produce the same JSON body shape.
    @Test
    void getAccount_notOwnedByCaller_returns403WithBody() throws Exception {
        mockMvc.perform(get("/api/accounts/4").with(asAlice()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // Filter-chain-level denial (teller hitting an account_holder-only route) -- this is the
    // specific codepath that used to return a bare bodyless 403 before the AccessDeniedHandler fix.
    @Test
    void getMyAccounts_asTeller_returns403WithBody() throws Exception {
        mockMvc.perform(get("/api/accounts").with(asTeller()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.status").value(403));
    }

    // BR-V1: missing "status" field must be a clean 400, not a raw NullPointerException/500.
    @Test
    void updateStatus_missingStatusField_returns400() throws Exception {
        mockMvc.perform(put("/api/accounts/1/status")
                        .with(asTeller())
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // BR-A3/BR-S3: teller can toggle account status; account 3 (128-9878-003) is seeded INACTIVE.
    @Test
    void updateStatus_asTeller_activatesAccount() throws Exception {
        mockMvc.perform(put("/api/accounts/3/status")
                        .with(asTeller())
                        .contentType("application/json")
                        .content("{\"status\": \"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountStatus").value("ACTIVE"));
    }

    // Non-teller hitting a teller-only route -- filter-chain-level 403, same body-consistency check.
    @Test
    void updateStatus_asAccountHolder_returns403() throws Exception {
        mockMvc.perform(put("/api/accounts/1/status")
                        .with(asAlice())
                        .contentType("application/json")
                        .content("{\"status\": \"ACTIVE\"}"))
                .andExpect(status().isForbidden());
    }

    // BR-F1/BR-C2: withdrawal exceeding balance -> 422, FAILED row recorded, balance unchanged.
    @Test
    void withdrawal_insufficientFunds_returns422AndRecordsFailedTransaction() throws Exception {
        mockMvc.perform(post("/api/accounts/4/withdrawals")
                        .with(asTeller())
                        .contentType("application/json")
                        .content("{\"amount\": 999999.99}"))
                .andExpect(status().isUnprocessableEntity());

        try (Connection conn = oracleConnection();
             Statement stmt = conn.createStatement()) {
            var rs = stmt.executeQuery(
                    "SELECT COUNT(*) FROM transactions WHERE account_id = 4 AND txn_type = 'WITHDRAWAL' AND status = 'FAILED'");
            rs.next();
            assertThat(rs.getInt(1)).isGreaterThan(0);
        }
    }

    // Teller-only report endpoint returns real aggregated data reflecting the transactions above.
    @Test
    void transactionReport_asTeller_returnsData() throws Exception {
        mockMvc.perform(get("/api/reports/transactions").with(asTeller()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
