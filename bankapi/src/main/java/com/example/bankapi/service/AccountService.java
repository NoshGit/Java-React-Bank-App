package com.example.bankapi.service;

import com.example.bankapi.dto.AccountDto;
import com.example.bankapi.dto.TransactionDto;
import com.example.bankapi.dto.TransactionReportLineDto;
import com.example.bankapi.entity.Account;
import com.example.bankapi.entity.AccountStatus;
import com.example.bankapi.entity.Customer;
import com.example.bankapi.exception.AccountNotFoundException;
import com.example.bankapi.exception.CustomerNotFoundException;
import com.example.bankapi.repository.AccountRepository;
import com.example.bankapi.repository.CustomerRepository;
import com.example.bankapi.repository.TransactionRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
public class AccountService {

    private final AccountRepository accountRepository;
    private final CustomerRepository customerRepository;
    private final TransactionRepository transactionRepository;

    public AccountService(AccountRepository accountRepository,
                           CustomerRepository customerRepository,
                           TransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.customerRepository = customerRepository;
        this.transactionRepository = transactionRepository;
    }

    /** GET /api/accounts -- BR-A4: account_holder only sees their own accounts. */
    @PreAuthorize("hasRole('ACCOUNT_HOLDER')")
    public List<AccountDto> getMyAccounts(Authentication authentication) {
        log.info("Fetching accounts for customer: {}", authentication.getName());
        List<AccountDto> accounts= accountRepository.findByCustomer_CustomerNumber(authentication.getName())
                .stream().map(AccountDto::from).toList();
        log.debug("Retrieved {} accounts for customer: {}", accounts.size(), authentication.getName());
        return accounts;
    }

    /** GET /api/customers/{customerNumber}/accounts -- teller only, BR-A6: 404 if customer missing. */
    @PreAuthorize("hasRole('TELLER')")
    public List<AccountDto> getAccountsForCustomer(String customerNumber) {
        Customer customer = customerRepository.findByCustomerNumber(customerNumber)
                .orElseThrow(() -> new CustomerNotFoundException("No such customer: " + customerNumber));
        return accountRepository.findByCustomer_CustomerNumber(customer.getCustomerNumber())
                .stream().map(AccountDto::from).toList();
    }

    /**
     * GET /api/accounts/{accountId} -- account_holder or teller, ownership enforced.
     * 404 must win over 403 (BR-A6), so this is checked manually rather than via @PreAuthorize,
     * which would evaluate before we know whether the account even exists.
     */
    public AccountDto getAccount(Long accountId, Authentication authentication) {
        Account account = loadAccountOrThrow(accountId);
        requireReadAccess(account, authentication);
        return AccountDto.from(account);
    }

    /** GET /api/accounts/{accountId}/transactions -- same ownership rule as getAccount. */
    public List<TransactionDto> getTransactions(Long accountId, Authentication authentication) {
        Account account = loadAccountOrThrow(accountId);
        requireReadAccess(account, authentication);
        return transactionRepository.findByAccount_AccountIdOrderByTxnDateDesc(accountId)
                .stream().map(TransactionDto::from).toList();
    }

    /** PUT /api/accounts/{accountId}/status -- teller only (BR-A3, BR-S3). */
    @Transactional
    @PreAuthorize("hasRole('TELLER')")
    public AccountDto updateStatus(Long accountId, AccountStatus newStatus) {
        log.info("Updating account {} status to: {}", accountId, newStatus);
        Account account = loadAccountOrThrow(accountId);
        account.setAccountStatus(newStatus);
        log.debug("Account {} status updated successfully to: {}", accountId, newStatus);
        return AccountDto.from(account);
    }

    /** GET /api/reports/transactions -- teller only (BR-A3). Counts and totals of COMPLETED transactions by type. */
    @PreAuthorize("hasRole('TELLER')")
    public List<TransactionReportLineDto> getTransactionReport() {
        return transactionRepository.summarizeByType(com.example.bankapi.entity.TransactionStatus.COMPLETED)
                .stream()
                .map(row -> new TransactionReportLineDto(row.getTxnType().name(), row.getCount(), row.getTotal()))
                .toList();
    }

    private Account loadAccountOrThrow(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("No such account: " + accountId));
    }

    /** BR-A4: account_holder is only permitted for their own account; teller may read any account. */
    private void requireReadAccess(Account account, Authentication authentication) {
        boolean isTeller = hasRole(authentication, "TELLER");
        if (isTeller) {
            return;
        }
        boolean owns = account.getCustomer().getCustomerNumber().equals(authentication.getName());
        if (!owns) {
            throw new AccessDeniedException("Account does not belong to caller");
        }
    }

    static boolean hasRole(Authentication authentication, String role) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_" + role));
    }
}
