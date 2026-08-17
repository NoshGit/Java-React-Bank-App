package com.example.bankapi.controller;

import com.example.bankapi.dto.AmountRequestDto;
import com.example.bankapi.dto.PaymentRequestDto;
import com.example.bankapi.dto.TransferRequestDto;
import com.example.bankapi.dto.TransferResultDto;
import com.example.bankapi.service.MoneyMovementService;
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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MoneyMovementControllerTest {

    @Mock
    MoneyMovementService moneyMovementService;

    @InjectMocks
    MoneyMovementController controller;

    Authentication asAlice;

    @BeforeEach
    void setUp() {
        asAlice = new TestingAuthenticationToken("487-978493", "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_ACCOUNT_HOLDER")));
    }

    @Test
    void transfer_returnsCreatedAndBody() {
        var req = new TransferRequestDto(2L, new BigDecimal("10.00"));
        var expected = new TransferResultDto(null, 1L, 2L, new BigDecimal("10.00"), "COMPLETED");
        when(moneyMovementService.transfer(1L, 2L, new BigDecimal("10.00"), asAlice)).thenReturn(expected);

        ResponseEntity<TransferResultDto> r = controller.transfer(1L, req, asAlice);
        assertThat(r.getStatusCodeValue()).isEqualTo(201);
        assertThat(r.getBody().status()).isEqualTo("COMPLETED");
    }

    @Test
    void payment_returnsCreatedAndBody() {
        var req = new PaymentRequestDto(new BigDecimal("5.00"), "REF-1");
        var expected = new TransferResultDto(null, 1L, null, new BigDecimal("5.00"), "COMPLETED");
        when(moneyMovementService.payment(1L, req.amount(), req.reference(), asAlice)).thenReturn(expected);

        ResponseEntity<TransferResultDto> r = controller.payment(1L, req, asAlice);
        assertThat(r.getStatusCodeValue()).isEqualTo(201);
        assertThat(r.getBody().fromAccountId()).isEqualTo(1L);
    }

    @Test
    void deposit_and_withdrawal_returnCreated() {
        var amt = new AmountRequestDto(new BigDecimal("7.00"));
        var res = new TransferResultDto(null, 1L, null, amt.amount(), "COMPLETED");
        when(moneyMovementService.deposit(1L, amt.amount())).thenReturn(res);
        when(moneyMovementService.withdrawal(1L, amt.amount())).thenReturn(res);

        ResponseEntity<TransferResultDto> d = controller.deposit(1L, amt);
        ResponseEntity<TransferResultDto> w = controller.withdrawal(1L, amt);

        assertThat(d.getStatusCodeValue()).isEqualTo(201);
        assertThat(w.getStatusCodeValue()).isEqualTo(201);
    }
}
