package com.example.bankapi.service;

import com.example.bankapi.dto.AccountDto;
import com.example.bankapi.dto.TransactionDto;
import com.example.bankapi.entity.Account;
import com.example.bankapi.entity.AccountStatus;
import com.example.bankapi.entity.AccountType;
import com.example.bankapi.entity.Customer;
import com.example.bankapi.entity.Transaction;
import com.example.bankapi.entity.TransactionStatus;
import com.example.bankapi.entity.TransactionType;
import com.example.bankapi.exception.CustomerNotFoundException;
import com.example.bankapi.repository.AccountRepository;
import com.example.bankapi.repository.CustomerRepository;
import com.example.bankapi.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private TransactionRepository transactionRepository;

    private AccountService accountService;

    private Customer alice;
    private Account aliceAccount;
    private Authentication asAlice;

    @BeforeEach
    void setUp() throws Exception {
        accountService = new AccountService(accountRepository, customerRepository, transactionRepository);

        alice = new Customer("487-978493", "Alice Customer", "alice@example.com");
        aliceAccount = newAccount(1L, "128-9878-001", alice, AccountType.CHECKING,
                AccountStatus.ACTIVE, new BigDecimal("100.00"));

        asAlice = new TestingAuthenticationToken("487-978493", "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_ACCOUNT_HOLDER")));
    }

    @Test
    void getMyAccounts_returnsMappedDtos() {
        when(accountRepository.findByCustomer_CustomerNumber("487-978493")).thenReturn(List.of(aliceAccount));

        var result = accountService.getMyAccounts(asAlice);

        assertThat(result).hasSize(1);
        AccountDto dto = result.get(0);
        assertThat(dto.accountId()).isEqualTo(1L);
        assertThat(dto.customerNumber()).isEqualTo("487-978493");
        assertThat(dto.balance()).isEqualByComparingTo("100.00");
    }

    @Test
    void getAccountsForCustomer_missingCustomer_throws() {
        when(customerRepository.findByCustomerNumber("X-123")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccountsForCustomer("X-123"))
                .isInstanceOf(CustomerNotFoundException.class);
    }

    @Test
    void getAccount_ownerAccess_returnsDto() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(aliceAccount));

        var dto = accountService.getAccount(1L, asAlice);

        assertThat(dto.accountId()).isEqualTo(1L);
        assertThat(dto.customerNumber()).isEqualTo("487-978493");
    }

    @Test
    void getAccount_nonOwner_throwsAccessDenied() throws Exception {
        Customer bob = new Customer("500-100200", "Bob", "bob@example.com");
        Account bobs = newAccount(2L, "128-9000-002", bob, AccountType.CHECKING, AccountStatus.ACTIVE, new BigDecimal("20.00"));
        when(accountRepository.findById(2L)).thenReturn(Optional.of(bobs));

        assertThatThrownBy(() -> accountService.getAccount(2L, asAlice))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    void getTransactions_returnsMappedDtos() {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(aliceAccount));
        Transaction t = new Transaction(aliceAccount, TransactionType.DEPOSIT, new BigDecimal("10.00"), TransactionStatus.COMPLETED, "desc");
        when(transactionRepository.findByAccount_AccountIdOrderByTxnDateDesc(1L)).thenReturn(List.of(t));

        var list = accountService.getTransactions(1L, asAlice);
        assertThat(list).hasSize(1);
        TransactionDto dto = list.get(0);
        assertThat(dto.accountId()).isEqualTo(1L);
        assertThat(dto.amount()).isEqualByComparingTo("10.00");
    }

    @Test
    void updateStatus_changesStatusAndReturnsDto() throws Exception {
        when(accountRepository.findById(1L)).thenReturn(Optional.of(aliceAccount));

        var dto = accountService.updateStatus(1L, AccountStatus.INACTIVE);
        assertThat(dto.accountStatus()).isEqualTo("INACTIVE");
        assertThat(aliceAccount.getAccountStatus()).isEqualTo(AccountStatus.INACTIVE);
    }

    // helpers
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
