SELECT table_name FROM user_tables ORDER BY table_name;

--Alter all table names to append with '_archived'.
-- Handle the case where there is foreign key constraints that reference the tables being renamed.
BEGIN
    FOR rec IN (SELECT table_name FROM user_tables) LOOP
        EXECUTE IMMEDIATE 'ALTER TABLE ' || rec.table_name || ' RENAME TO ' || rec.table_name || '_archived';
    END LOOP;
END;

--Rename all constraints to append with '_archived'.
BEGIN
    FOR rec IN (SELECT constraint_name, table_name FROM user_constraints WHERE constraint_type IN ('P', 'R', 'U')) LOOP
        EXECUTE IMMEDIATE 'ALTER TABLE ' || rec.table_name || ' RENAME CONSTRAINT ' || rec.constraint_name || ' TO ' || rec.constraint_name || '_archived';
    END LOOP;
END;

--Show list of constraint
SELECT constraint_name, table_name FROM user_constraints WHERE constraint_type IN ('P', 'R', 'U');