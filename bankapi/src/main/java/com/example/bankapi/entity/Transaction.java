package com.example.bankapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @UuidGenerator
    @Column(name = "txn_id", length = 36)
    private String txnId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Enumerated(EnumType.STRING)
    @Column(name = "txn_type", nullable = false)
    private TransactionType txnType;

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransactionStatus status;

    @Column(name = "txn_date", nullable = false)
    private LocalDateTime txnDate;

    @Column(name = "description")
    private String description;

    protected Transaction() {
    }

    public Transaction(Account account, TransactionType txnType, BigDecimal amount,
                        TransactionStatus status, String description) {
        this.account = account;
        this.txnType = txnType;
        this.amount = amount;
        this.status = status;
        this.description = description;
        this.txnDate = LocalDateTime.now();
    }

    public String getTxnId() {
        return txnId;
    }

    public Account getAccount() {
        return account;
    }

    public TransactionType getTxnType() {
        return txnType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public LocalDateTime getTxnDate() {
        return txnDate;
    }

    public String getDescription() {
        return description;
    }
}
