package com.example.bankapi.controller;

import com.example.bankapi.dto.AccountDto;
import com.example.bankapi.dto.AccountStatusUpdateDto;
import com.example.bankapi.dto.TransactionDto;
import com.example.bankapi.service.AccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountControllerTest {

    @Mock
    AccountService accountService;

    @InjectMocks
    AccountController controller;

    Authentication asAlice;

    @BeforeEach
    void setUp() {
        asAlice = new TestingAuthenticationToken("487-978493", "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_ACCOUNT_HOLDER")));
    }

    @Test
    void getMyAccounts_delegatesToService() {
        AccountDto dto = new AccountDto(1L, "acc-1", "487-978493", "CHECKING", "ACTIVE", new BigDecimal("10.00"), LocalDate.now(), "Alice Smith");
        when(accountService.getMyAccounts(asAlice)).thenReturn(List.of(dto));

        var list = controller.getMyAccounts(asAlice);
        assertThat(list).hasSize(1);
        assertThat(list.get(0).accountId()).isEqualTo(1L);
    }

    @Test
    void updateStatus_callsService_and_returnsResponseEntity() {
        AccountStatusUpdateDto body = new AccountStatusUpdateDto("ACTIVE");
        AccountDto updated = new AccountDto(1L, "acc-1", "487-978493", "CHECKING", "ACTIVE", new BigDecimal("10.00"), LocalDate.now(), "Alice Smith");
        when(accountService.updateStatus(1L, com.example.bankapi.entity.AccountStatus.ACTIVE)).thenReturn(updated);

        ResponseEntity<AccountDto> r = controller.updateStatus(1L, body);
        assertThat(r.getStatusCodeValue()).isEqualTo(200);
        assertThat(r.getBody().accountStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void getTransactionReport_delegates() {
        when(accountService.getTransactionReport()).thenReturn(List.of());
        var res = controller.getTransactionReport();
        assertThat(res).isEmpty();
    }
}
