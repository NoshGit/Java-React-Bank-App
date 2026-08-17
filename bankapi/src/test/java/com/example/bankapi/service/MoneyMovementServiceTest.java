package com.example.bankapi.service;

import com.example.bankapi.entity.Account;
import com.example.bankapi.entity.AccountStatus;
import com.example.bankapi.entity.AccountType;
import com.example.bankapi.entity.Customer;
import com.example.bankapi.entity.Transaction;
import com.example.bankapi.entity.TransactionStatus;
import com.example.bankapi.entity.TransactionType;
import com.example.bankapi.exception.AccountNotActiveException;
import com.example.bankapi.exception.ExternalPaymentException;
import com.example.bankapi.exception.InsufficientFundsException;
import com.example.bankapi.kafka.TransactionStatsPublisher;
import com.example.bankapi.repository.AccountRepository;
import com.example.bankapi.repository.TransactionRepository;
import com.example.bankapi.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tier 1 unit tests (Capstone Testing Guidance): repositories, Kafka, and the
 * payment client are mocked so these run in milliseconds and exercise the
 * business rules in isolation.
 */
@ExtendWith(MockitoExtension.class)
class MoneyMovementServiceTest {

    @Mock
    AccountRepository accountRepository;
    @Mock
    TransactionRepository transactionRepository;
    @Mock
    TransferRepository transferRepository;
    @Mock
    TransactionStatsPublisher statsPublisher;
    @Mock
    PaymentClient paymentClient;

    MoneyMovementService service;

    Customer alice;
    Account activeAccount;
    Account inactiveAccount;

    Authentication asAlice;
    Authentication asTeller;

    @BeforeEach
    void setUp() throws Exception {
        service = new MoneyMovementService(accountRepository, transactionRepository,
                transferRepository, statsPublisher, paymentClient);

        alice = new Customer("487-978493", "Alice Customer", "alice@example.com");

        activeAccount = newAccount(1L, "128-9878-001", alice, AccountType.CHECKING,
                AccountStatus.ACTIVE, new BigDecimal("100.00"));
        inactiveAccount = newAccount(2L, "128-9878-003", alice, AccountType.CHECKING,
                AccountStatus.INACTIVE, new BigDecimal("0.00"));

        asAlice = new TestingAuthenticationToken("487-978493", "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_ACCOUNT_HOLDER")));
        asTeller = new TestingAuthenticationToken("EM01", "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_TELLER")));
    }

    // BR-F1 / BR-C2: insufficient funds on withdrawal -> FAILED row written, 422-mapped
    // exception thrown, no balance change, nothing published to Kafka.
    @Test
    void withdrawal_insufficientFunds_recordsFailedTransactionAndThrows() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(activeAccount));

        assertThatThrownBy(() -> service.withdrawal(1L, new BigDecimal("500.00")))
                .isInstanceOf(InsufficientFundsException.class);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        Transaction saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(saved.getTxnType()).isEqualTo(TransactionType.WITHDRAWAL);
        assertThat(activeAccount.getBalance()).isEqualByComparingTo("100.00");
        verify(statsPublisher, never()).publishIfCompleted(any());
    }

    // BR-S1: inactive account blocks money movement entirely -- no transaction row at all.
    @Test
    void deposit_inactiveAccount_writesNoTransactionRow() {
        when(accountRepository.findById(2L)).thenReturn(Optional.of(inactiveAccount));

        assertThatThrownBy(() -> service.deposit(2L, new BigDecimal("50.00")))
                .isInstanceOf(AccountNotActiveException.class);

        verify(transactionRepository, never()).save(any());
        verify(statsPublisher, never()).publishIfCompleted(any());
    }

    // BR-P3: external payment failure -> FAILED PAYMENT recorded, no debit, 502-mapped exception.
    @Test
    void payment_externalServiceFails_recordsFailedPaymentAndThrows() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(activeAccount));
        when(paymentClient.submitPayment(any(), any())).thenReturn(false);

        assertThatThrownBy(() -> service.payment(1L, new BigDecimal("25.00"), "INV-1", asAlice))
                .isInstanceOf(ExternalPaymentException.class);

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(TransactionStatus.FAILED);
        assertThat(activeAccount.getBalance()).isEqualByComparingTo("100.00");
    }

    // BR-P4: amount 999.99 is the deterministic mock failure switch -- proven via the mocked client here.
    @Test
    void payment_amount99999_isRejectedByMockAndAccountUnchanged() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(activeAccount));
        when(paymentClient.submitPayment(new BigDecimal("999.99"), "INV-2")).thenReturn(false);
        activeAccount.setBalance(new BigDecimal("2000.00"));

        assertThatThrownBy(() -> service.payment(1L, new BigDecimal("999.99"), "INV-2", asAlice))
                .isInstanceOf(ExternalPaymentException.class);

        assertThat(activeAccount.getBalance()).isEqualByComparingTo("2000.00");
    }

    // BR-A4: account_holder cannot act on an account they don't own.
    @Test
    void payment_notOwnedByAccountHolder_isDenied() throws Exception {
        Customer bob = new Customer("500-100200", "Bob Customer", "bob@example.com");
        Account bobsAccount = newAccount(3L, "128-9878-004", bob, AccountType.CHECKING,
                AccountStatus.ACTIVE, new BigDecimal("500.00"));
        when(accountRepository.findById(3L)).thenReturn(Optional.of(bobsAccount));

        assertThatThrownBy(() -> service.payment(3L, new BigDecimal("10.00"), "INV-3", asAlice))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

        verify(transactionRepository, never()).save(any());
    }

    // BR-A5: teller transfers must stay within one customer.
    @Test
    void transfer_tellerCrossCustomer_isDenied() throws Exception {
        Customer bob = new Customer("500-100200", "Bob Customer", "bob@example.com");
        Account bobsAccount = newAccount(3L, "128-9878-004", bob, AccountType.CHECKING,
                AccountStatus.ACTIVE, new BigDecimal("500.00"));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(activeAccount));
        when(accountRepository.findById(3L)).thenReturn(Optional.of(bobsAccount));

        assertThatThrownBy(() -> service.transfer(1L, 3L, new BigDecimal("10.00"), asTeller))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

        verify(transactionRepository, never()).save(any());
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
}
