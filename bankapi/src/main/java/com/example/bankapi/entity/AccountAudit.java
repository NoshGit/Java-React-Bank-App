package com.example.bankapi.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Read-only view of rows written by the database trigger trg_account_balance_audit.
 * The application never inserts into this table directly.
 */
@Entity
@Table(name = "account_audit")
public class AccountAudit {

    @Id
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "account_id")
    private Long accountId;

    @Column(name = "old_balance")
    private BigDecimal oldBalance;

    @Column(name = "new_balance")
    private BigDecimal newBalance;

    @Column(name = "changed_at")
    private LocalDateTime changedAt;

    protected AccountAudit() {
    }

    public Long getAuditId() {
        return auditId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public BigDecimal getOldBalance() {
        return oldBalance;
    }

    public BigDecimal getNewBalance() {
        return newBalance;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }
}
