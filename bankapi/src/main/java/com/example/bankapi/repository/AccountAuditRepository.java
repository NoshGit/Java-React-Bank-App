package com.example.bankapi.repository;

import com.example.bankapi.entity.AccountAudit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccountAuditRepository extends JpaRepository<AccountAudit, Long> {
    List<AccountAudit> findByAccountId(Long accountId);

    long countByAccountId(Long accountId);
}
