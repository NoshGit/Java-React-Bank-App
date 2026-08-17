CREATE OR REPLACE TRIGGER trg_account_balance_audit
AFTER UPDATE OF balance ON accounts
FOR EACH ROW
WHEN (NVL(OLD.balance, -1) != NVL(NEW.balance, -1))
BEGIN
  INSERT INTO account_audit (account_id, old_balance, new_balance)
  VALUES (:OLD.account_id, :OLD.balance, :NEW.balance);
END;
