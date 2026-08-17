package com.example.bankapi.repository;

import com.example.bankapi.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    List<Account> findByCustomer_CustomerNumber(String customerNumber);

    Optional<Account> findByAccountNumber(String accountNumber);
}
