--SELECT table_name FROM user_tables ORDER BY table_name;
--Table 1

CREATE TABLE customers (
                           customer_id       NUMBER GENERATED AS IDENTITY PRIMARY KEY,
                           customer_number   VARCHAR2(10)   NOT NULL,
                           full_name         VARCHAR2(100)  NOT NULL,
                           email             VARCHAR2(150)  NOT NULL,
                           created_date      DATE           DEFAULT SYSDATE NOT NULL,
                           CONSTRAINT uq_customers_number UNIQUE (customer_number),
                           CONSTRAINT uq_customers_email  UNIQUE (email)
);

CREATE TABLE accounts (
                          account_id      NUMBER GENERATED AS IDENTITY PRIMARY KEY,
                          account_number  VARCHAR2(12)   NOT NULL,
                          customer_id     NUMBER         NOT NULL,
                          account_type    VARCHAR2(20)   NOT NULL
                              CHECK (account_type IN ('CHECKING','SAVINGS')),
                          account_status  VARCHAR2(8)    DEFAULT 'INACTIVE' NOT NULL
                              CHECK (account_status IN ('ACTIVE','INACTIVE')),
                          balance         NUMBER(15,2)   DEFAULT 0 NOT NULL,
                          opened_date     DATE           DEFAULT SYSDATE NOT NULL,
                          CONSTRAINT uq_accounts_number UNIQUE (account_number),
                          CONSTRAINT fk_acct_cust FOREIGN KEY (customer_id)
                              REFERENCES customers(customer_id)
);


----
CREATE TABLE transactions (
                              txn_id       VARCHAR2(36)  PRIMARY KEY,
                              account_id   NUMBER        NOT NULL,
                              txn_type     VARCHAR2(12)  NOT NULL
                                  CHECK (txn_type IN ('DEPOSIT','WITHDRAWAL','TRANSFER_IN','TRANSFER_OUT','PAYMENT')),
                              amount       NUMBER(15,2)  NOT NULL CHECK (amount > 0),
                              status       VARCHAR2(10)  DEFAULT 'COMPLETED' NOT NULL
                                  CHECK (status IN ('COMPLETED','FAILED')),
                              txn_date     TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
                              description  VARCHAR2(255),
                              CONSTRAINT fk_txn_acct FOREIGN KEY (account_id)
                                  REFERENCES accounts(account_id)
);

----

CREATE TABLE transfers (
                           transfer_id   VARCHAR2(36)  PRIMARY KEY,
                           debit_txn_id  VARCHAR2(36)  NOT NULL,
                           credit_txn_id VARCHAR2(36)  NOT NULL,
                           created_date  TIMESTAMP     DEFAULT SYSTIMESTAMP NOT NULL,
                           CONSTRAINT fk_transfer_debit  FOREIGN KEY (debit_txn_id)  REFERENCES transactions(txn_id),
                           CONSTRAINT fk_transfer_credit FOREIGN KEY (credit_txn_id) REFERENCES transactions(txn_id),
                           CONSTRAINT uq_transfer_debit  UNIQUE (debit_txn_id),
                           CONSTRAINT uq_transfer_credit UNIQUE (credit_txn_id),
                           CONSTRAINT chk_transfer_legs  CHECK (debit_txn_id <> credit_txn_id)
);

---

CREATE TABLE account_audit (
                               audit_id      NUMBER GENERATED AS IDENTITY PRIMARY KEY,
                               account_id    NUMBER  NOT NULL,
                               old_balance   NUMBER(15,2),
                               new_balance   NUMBER(15,2),
                               changed_at    TIMESTAMP      DEFAULT SYSTIMESTAMP NOT NULL
);


--Insert sample data into customers table
INSERT INTO customers (customer_number, full_name, email)
VALUES ('487-978493', 'Alice Eke', 'alice@example.com');
INSERT INTO customers (customer_number, full_name, email)
VALUES ('500-100200', 'Bob Banala', 'bob@example.com');

--select * from customers;

---Insert sample data into accounts table
-- Accounts: all ACTIVE except one INACTIVE for Alice.
-- customer_id is resolved by customer_number so we never assume identity values.
INSERT INTO accounts (account_number, customer_id, account_type, account_status, balance)
VALUES ('128-9878-001',
        (SELECT customer_id FROM customers WHERE customer_number = '487-978493'),
        'CHECKING', 'ACTIVE', 5000.00);
INSERT INTO accounts (account_number, customer_id, account_type, account_status, balance)
VALUES ('128-9878-002',
        (SELECT customer_id FROM customers WHERE customer_number = '487-978493'),
        'SAVINGS', 'ACTIVE', 10000.00);
INSERT INTO accounts (account_number, customer_id, account_type, account_status, balance)
VALUES ('128-9878-003',
        (SELECT customer_id FROM customers WHERE customer_number = '487-978493'),
        'CHECKING', 'INACTIVE', 0.00);
INSERT INTO accounts (account_number, customer_id, account_type, account_status, balance)
VALUES ('128-9878-004',
        (SELECT customer_id FROM customers WHERE customer_number = '500-100200'),
        'CHECKING', 'ACTIVE', 2500.00);


--select * from ACCOUNTS;