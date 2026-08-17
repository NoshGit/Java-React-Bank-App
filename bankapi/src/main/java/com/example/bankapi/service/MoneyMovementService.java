package com.example.bankapi.service;

import com.example.bankapi.dto.TransferResultDto;
import com.example.bankapi.entity.Account;
import com.example.bankapi.entity.AccountStatus;
import com.example.bankapi.entity.Transaction;
import com.example.bankapi.entity.TransactionStatus;
import com.example.bankapi.entity.TransactionType;
import com.example.bankapi.entity.Transfer;
import com.example.bankapi.exception.AccountNotActiveException;
import com.example.bankapi.exception.AccountNotFoundException;
import com.example.bankapi.exception.ExternalPaymentException;
import com.example.bankapi.exception.InsufficientFundsException;
import com.example.bankapi.exception.InvalidRequestException;
import com.example.bankapi.kafka.TransactionStatsPublisher;
import com.example.bankapi.repository.AccountRepository;
import com.example.bankapi.repository.TransactionRepository;
import com.example.bankapi.repository.TransferRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * All money-movement business rules (Capstone Business Rules: BR-S1, BR-F1/F2,
 * BR-T1..T3, BR-P1..P4, BR-C1..C3, BR-X1, BR-K1) live here so the controller
 * stays a thin pass-through, per the JPA Persistence layering rule.
 */
@Slf4j
@Service
public class MoneyMovementService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransferRepository transferRepository;
    private final TransactionStatsPublisher statsPublisher;
    private final PaymentClient paymentClient;

    public MoneyMovementService(AccountRepository accountRepository,
                                 TransactionRepository transactionRepository,
                                 TransferRepository transferRepository,
                                 TransactionStatsPublisher statsPublisher,
                                 PaymentClient paymentClient) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transferRepository = transferRepository;
        this.statsPublisher = statsPublisher;
        this.paymentClient = paymentClient;
    }

    /**
     * POST /api/accounts/{accountId}/transfers -- BR-T1, BR-T2, BR-T3, BR-A4, BR-A5.
     * noRollbackFor is required: Spring rolls back @Transactional methods on any unchecked
     * exception by default, which would silently undo the FAILED-transaction row BR-F1/BR-T3
     * require us to keep even though the request itself failed.
     */
    @Transactional(noRollbackFor = InsufficientFundsException.class)
    public TransferResultDto transfer(Long accountId, Long toAccountId, BigDecimal amount,
                                       Authentication authentication) {
        log.info("Transfer initiated from account {} to account {} amount: {} by user: {}", 
                accountId, toAccountId, amount, authentication.getName());
        
        if (toAccountId == null || toAccountId.equals(accountId)) {
            log.warn("Invalid transfer request - toAccountId must be present and differ from source");
            throw new InvalidRequestException("toAccountId must be present and differ from the source account");
        }

        Account source = loadAccountOrThrow(accountId);
        Account destination = loadAccountOrThrow(toAccountId);

        boolean isTeller = AccountService.hasRole(authentication, "TELLER");
        if (isTeller) {
            // BR-A5: teller transfers must stay within one customer.
            if (!source.getCustomer().getCustomerNumber().equals(destination.getCustomer().getCustomerNumber())) {
                log.error("Teller transfer denied - accounts belong to different customers");
                throw new AccessDeniedException("Teller transfers must stay within the same customer");
            }
        } else {
            // BR-A4: account_holder transfers are limited to their own accounts, both legs.
            String caller = authentication.getName();
            boolean ownsSource = source.getCustomer().getCustomerNumber().equals(caller);
            boolean ownsDestination = destination.getCustomer().getCustomerNumber().equals(caller);
            if (!ownsSource || !ownsDestination) {
                log.error("Transfer denied - accounts do not belong to caller");
                throw new AccessDeniedException("Transfer accounts must belong to the caller");
            }
        }

        // BR-S1: no transaction row at all for an inactive account.
        if (!source.isActive() || !destination.isActive()) {
            log.error("Transfer denied - one or both accounts inactive");
            throw new AccountNotActiveException("Both accounts must be ACTIVE to transfer");
        }

        if (source.getBalance().compareTo(amount) < 0) {
            // BR-F1 / BR-T3: insufficient funds -> one FAILED TRANSFER_OUT row, no transfers row.
            log.warn("Transfer failed - insufficient funds in account {}", accountId);
            Transaction failedDebit = new Transaction(source, TransactionType.TRANSFER_OUT, amount,
                    TransactionStatus.FAILED, "Insufficient funds for transfer to account " + toAccountId);
            transactionRepository.save(failedDebit);
            throw new InsufficientFundsException("Insufficient funds in account " + accountId);
        }

        // BR-T1: debit source, credit destination, two transactions + one transfers row.
        source.setBalance(source.getBalance().subtract(amount));
        destination.setBalance(destination.getBalance().add(amount));

        Transaction debit = transactionRepository.save(new Transaction(source, TransactionType.TRANSFER_OUT,
                amount, TransactionStatus.COMPLETED, "Transfer to account " + toAccountId));
        Transaction credit = transactionRepository.save(new Transaction(destination, TransactionType.TRANSFER_IN,
                amount, TransactionStatus.COMPLETED, "Transfer from account " + accountId));
        transferRepository.save(new Transfer(debit, credit));

        statsPublisher.publishIfCompleted(debit);
        statsPublisher.publishIfCompleted(credit);

        log.info("Transfer completed successfully from account {} to account {} amount: {}", 
                accountId, toAccountId, amount);
        return new TransferResultDto(null, accountId, toAccountId, amount, "COMPLETED");
    }

    /** POST /api/accounts/{accountId}/payments -- BR-P1..P4, BR-A4. See transfer() for why noRollbackFor is needed. */
    @Transactional(noRollbackFor = {InsufficientFundsException.class, ExternalPaymentException.class})
    public TransferResultDto payment(Long accountId, BigDecimal amount, String reference,
                                      Authentication authentication) {
        log.info("Payment initiated on account {} amount: {} ref: {} by user: {}", 
                accountId, amount, reference, authentication.getName());
        
        Account account = loadAccountOrThrow(accountId);
        requireOwnerOrTeller(account, authentication);

        // BR-S1: inactive accounts get no transaction row at all.
        if (!account.isActive()) {
            log.error("Payment failed - account {} is not ACTIVE", accountId);
            throw new AccountNotActiveException("Account " + accountId + " is not ACTIVE");
        }

        // BR-P2: pre-conditions (active, funds) are checked before the external call.
        if (account.getBalance().compareTo(amount) < 0) {
            log.warn("Payment failed - insufficient funds in account {}", accountId);
            Transaction failed = new Transaction(account, TransactionType.PAYMENT, amount,
                    TransactionStatus.FAILED, "Insufficient funds for payment ref " + reference);
            transactionRepository.save(failed);
            throw new InsufficientFundsException("Insufficient funds in account " + accountId);
        }

        boolean accepted = paymentClient.submitPayment(amount, reference);

        if (!accepted) {
            // BR-P3: external failure -> FAILED PAYMENT recorded, no debit, 502 to client.
            log.error("Payment failed - external payment service rejected ref: {}", reference);
            Transaction failed = new Transaction(account, TransactionType.PAYMENT, amount,
                    TransactionStatus.FAILED, "External payment service rejected ref " + reference);
            transactionRepository.save(failed);
            throw new ExternalPaymentException("Payment service unavailable");
        }

        account.setBalance(account.getBalance().subtract(amount));
        Transaction completed = transactionRepository.save(new Transaction(account, TransactionType.PAYMENT,
                amount, TransactionStatus.COMPLETED, "Payment ref " + reference));
        statsPublisher.publishIfCompleted(completed);

        log.info("Payment completed successfully on account {} amount: {} ref: {}", accountId, amount, reference);
        return new TransferResultDto(null, accountId, null, amount, "COMPLETED");
    }

    /** POST /api/accounts/{accountId}/deposits -- BR-C1, BR-C3, teller only. */
    @Transactional
    @PreAuthorize("hasRole('TELLER')")
    public TransferResultDto deposit(Long accountId, BigDecimal amount) {
        log.info("Deposit initiated on account {} amount: {}", accountId, amount);
        Account account = loadAccountOrThrow(accountId);

        if (!account.isActive()) {
            log.error("Deposit failed - account {} is not ACTIVE", accountId);
            throw new AccountNotActiveException("Account " + accountId + " is not ACTIVE");
        }

        account.setBalance(account.getBalance().add(amount));
        Transaction completed = transactionRepository.save(new Transaction(account, TransactionType.DEPOSIT,
                amount, TransactionStatus.COMPLETED, "Teller deposit"));
        statsPublisher.publishIfCompleted(completed);

        log.info("Deposit completed successfully on account {} amount: {}", accountId, amount);
        return new TransferResultDto(null, accountId, null, amount, "COMPLETED");
    }

    /** POST /api/accounts/{accountId}/withdrawals -- BR-C2, BR-C3, teller only. See transfer() for why noRollbackFor is needed. */
    @Transactional(noRollbackFor = InsufficientFundsException.class)
    @PreAuthorize("hasRole('TELLER')")
    public TransferResultDto withdrawal(Long accountId, BigDecimal amount) {
        log.info("Withdrawal initiated on account {} amount: {}", accountId, amount);
        Account account = loadAccountOrThrow(accountId);

        if (!account.isActive()) {
            log.error("Withdrawal failed - account {} is not ACTIVE", accountId);
            throw new AccountNotActiveException("Account " + accountId + " is not ACTIVE");
        }

        if (account.getBalance().compareTo(amount) < 0) {
            log.warn("Withdrawal failed - insufficient funds in account {}", accountId);
            Transaction failed = new Transaction(account, TransactionType.WITHDRAWAL, amount,
                    TransactionStatus.FAILED, "Insufficient funds for withdrawal");
            transactionRepository.save(failed);
            throw new InsufficientFundsException("Insufficient funds in account " + accountId);
        }

        account.setBalance(account.getBalance().subtract(amount));
        Transaction completed = transactionRepository.save(new Transaction(account, TransactionType.WITHDRAWAL,
                amount, TransactionStatus.COMPLETED, "Teller withdrawal"));
        statsPublisher.publishIfCompleted(completed);

        log.info("Withdrawal completed successfully on account {} amount: {}", accountId, amount);
        return new TransferResultDto(null, accountId, null, amount, "COMPLETED");
    }

    private Account loadAccountOrThrow(Long accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException("No such account: " + accountId));
    }

    private void requireOwnerOrTeller(Account account, Authentication authentication) {
        if (AccountService.hasRole(authentication, "TELLER")) {
            return;
        }
        boolean owns = account.getCustomer().getCustomerNumber().equals(authentication.getName());
        if (!owns) {
            throw new AccessDeniedException("Account does not belong to caller");
        }
    }
}
