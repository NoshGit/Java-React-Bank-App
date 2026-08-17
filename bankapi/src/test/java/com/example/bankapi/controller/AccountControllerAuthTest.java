package com.example.bankapi.controller;

import com.example.bankapi.config.SecurityConfig;
import com.example.bankapi.service.AccountService;
import com.example.bankapi.service.MoneyMovementService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tier 2 (Capstone Testing Guidance): web/security slice. Service layer is
 * mocked; this only proves routing and the 401 unauthenticated path, which
 * belongs in the filter chain, not the service layer. Imports the real
 * SecurityConfig so this actually exercises production rules, not Spring's
 * slice-test default deny-all.
 */
@WebMvcTest(AccountController.class)
@Import(SecurityConfig.class)
class AccountControllerAuthTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    AccountService accountService;

    @MockBean
    MoneyMovementService moneyMovementService;

    @Test
    void getMyAccounts_withoutToken_returns401() throws Exception {
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isUnauthorized());
    }
}
