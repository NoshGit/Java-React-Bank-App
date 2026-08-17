package com.example.bankapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "transfers")
public class Transfer {

    @Id
    @UuidGenerator
    @Column(name = "transfer_id", length = 36)
    private String transferId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "debit_txn_id", nullable = false, unique = true)
    private Transaction debitTransaction;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "credit_txn_id", nullable = false, unique = true)
    private Transaction creditTransaction;

    @Column(name = "created_date", nullable = false)
    private LocalDateTime createdDate;

    protected Transfer() {
    }

    public Transfer(Transaction debitTransaction, Transaction creditTransaction) {
        this.debitTransaction = debitTransaction;
        this.creditTransaction = creditTransaction;
        this.createdDate = LocalDateTime.now();
    }

    public String getTransferId() {
        return transferId;
    }

    public Transaction getDebitTransaction() {
        return debitTransaction;
    }

    public Transaction getCreditTransaction() {
        return creditTransaction;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }
}
