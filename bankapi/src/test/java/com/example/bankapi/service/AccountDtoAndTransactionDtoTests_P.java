package com.example.bankapi.dto;

import com.example.bankapi.entity.Account;
import com.example.bankapi.entity.AccountStatus;
import com.example.bankapi.entity.AccountType;
import com.example.bankapi.entity.Customer;
import com.example.bankapi.entity.Transaction;
import com.example.bankapi.entity.TransactionStatus;
import com.example.bankapi.entity.TransactionType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class AccountDtoAndTransactionDtoTests_P {

    @Test
    void accountDto_from_mapsAllFields() throws Exception {
        Customer c = new Customer("C-1", "Name", "n@example.com");
        var constructor = Account.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        Account a = constructor.newInstance();
        setField(a, "accountId", 7L);
        setField(a, "accountNumber", "ACC-7");
        setField(a, "customer", c);
        setField(a, "accountType", AccountType.CHECKING);
        setField(a, "accountStatus", AccountStatus.ACTIVE);
        setField(a, "balance", new BigDecimal("50.00"));
        setField(a, "openedDate", LocalDate.of(2020,1,1));

        com.example.bankapi.dto.AccountDto dto = com.example.bankapi.dto.AccountDto.from(a);
        assertThat(dto.accountId()).isEqualTo(7L);
        assertThat(dto.customerNumber()).isEqualTo("C-1");
        assertThat(dto.balance()).isEqualByComparingTo("50.00");
    }

    @Test
    void transactionDto_from_mapsFieldsCorrectly() throws Exception {
        Account a = Account.class.getDeclaredConstructor().newInstance();
        setField(a, "accountId", 9L);

        Transaction t = new Transaction(a, TransactionType.PAYMENT, new BigDecimal("12.34"), TransactionStatus.COMPLETED, "pmt");
        Field id = Transaction.class.getDeclaredField("txnId");
        id.setAccessible(true);
        id.set(t, "TX-1");

        com.example.bankapi.dto.TransactionDto dto = com.example.bankapi.dto.TransactionDto.from(t);
        assertThat(dto.txnId()).isEqualTo("TX-1");
        assertThat(dto.accountId()).isEqualTo(9L);
        assertThat(dto.amount()).isEqualByComparingTo("12.34");
        assertThat(dto.status()).isEqualTo("COMPLETED");
    }

    private static void setField(Object target, String fieldName, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
