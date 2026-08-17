package com.example.bankapi.repository;

import com.example.bankapi.entity.Transaction;
import com.example.bankapi.entity.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, String> {

    List<Transaction> findByAccount_AccountIdOrderByTxnDateDesc(Long accountId);

    @Query("select t.txnType as txnType, count(t) as count, sum(t.amount) as total "
            + "from Transaction t where t.status = :status group by t.txnType")
    List<TransactionTypeSummary> summarizeByType(TransactionStatus status);

    interface TransactionTypeSummary {
        com.example.bankapi.entity.TransactionType getTxnType();
        long getCount();
        java.math.BigDecimal getTotal();
    }
}
