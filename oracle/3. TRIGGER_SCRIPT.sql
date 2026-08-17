-- ============================================================
-- TEST STATEMENTS
-- ============================================================
-- Run the following to verify the trigger works correctly:
-- ============================================================

INSERT INTO accounts (ACCOUNT_NUMBER, CUSTOMER_ID, ACCOUNT_STATUS, ACCOUNT_TYPE, BALANCE)
VALUES('128-9878-006',2,'ACTIVE', 'SAVINGS',  100)
COMMIT;

UPDATE accounts SET balance = 950.00 WHERE account_id = 7;
COMMIT;

===============================================================================

CREATE OR REPLACE TRIGGER trg_accounts_audit
    BEFORE INSERT OR UPDATE OR DELETE ON accounts
    FOR EACH ROW
BEGIN
    -- Handle INSERT operation
    IF INSERTING THEN
        INSERT INTO account_audit (account_id, old_balance, new_balance)
        VALUES (:NEW.account_id, :OLD.balance, :NEW.balance);

        -- Handle UPDATE operation
    ELSIF UPDATING THEN
        INSERT INTO account_audit (account_id, old_balance, new_balance)
        VALUES (:NEW.account_id, :OLD.balance, :NEW.balance);

        -- Handle DELETE operation
    ELSIF DELETING THEN
        INSERT INTO account_audit (account_id, old_balance, new_balance)
        VALUES (:OLD.account_id, :OLD.balance, :NEW.balance);

    END IF;
END;
/
===============================================================================

CREATE OR REPLACE VIEW v_Account_Balance AS
(SELECT FULL_NAME,
        CUSTOMER_NUMBER,
        ACCOUNT_TYPE,
        ACCOUNT_NUMBER,
        SUM(BALANCE) AS BALANCE
FROM (
         SELECT c.FULL_NAME,
                c.CUSTOMER_NUMBER,
                a.ACCOUNT_TYPE,
                a.ACCOUNT_NUMBER,
                CASE t.txn_type
                    WHEN 'DEPOSIT' THEN t.AMOUNT
                    WHEN 'WITHDRAWAL' THEN -t.AMOUNT
                    WHEN 'TRANSFER_IN' THEN t.AMOUNT
                    WHEN 'TRANSFER_OUT' THEN -t.AMOUNT
                    WHEN 'PAYMENT' THEN -t.AMOUNT
                    ELSE 0
                    END AS BALANCE
         FROM CUSTOMERS c JOIN ACCOUNTS a ON c.CUSTOMER_ID = A.CUSTOMER_ID
                          JOIN TRANSACTIONS t ON a.ACCOUNT_ID = T.ACCOUNT_ID
         WHERE t.STATUS = 'COMPLETED'
         AND a.ACCOUNT_STATUS = 'ACTIVE')
GROUP BY FULL_NAME,
         CUSTOMER_NUMBER,
         ACCOUNT_TYPE,
         ACCOUNT_NUMBER)
