-- =====================================================================
-- Query Optimization & Concurrency Analysis
-- Topic: "Optimized SQL queries for financial transactions, ensuring data consistency under concurrent access"
-- =====================================================================

USE billing_db;

-- ---------------------------------------------------------------------
-- 1. Index Optimization Analysis: Composite Index on (student_id, semester_id)
-- ---------------------------------------------------------------------
-- Problem: When a student logs in, the dashboard frequently queries all bills for that student in the current semester.
-- Without index: Full table scan O(N).
-- With composite index `idx_bills_student_semester`: Index Range Scan O(log N).

EXPLAIN SELECT bill_id, invoice_number, total_amount, paid_amount, balance_due, status, due_date
FROM bills
WHERE student_id = 'STU202601' AND semester_id = 'SEM-2026-FALL';

-- ---------------------------------------------------------------------
-- 2. Index Optimization Analysis: Status & Due Date Filtering for Overdue Bills
-- ---------------------------------------------------------------------
-- Problem: Automated batch jobs and admin alerts constantly monitor delinquent tuition.
-- Using `idx_bills_status_due` (status, due_date): Direct index lookup.

EXPLAIN SELECT bill_id, student_id, balance_due, due_date
FROM bills
WHERE status IN ('UNPAID', 'PARTIAL') AND due_date < CURRENT_DATE();

-- ---------------------------------------------------------------------
-- 3. Concurrent Transaction Safety: Pessimistic Locking with SELECT ... FOR UPDATE
-- ---------------------------------------------------------------------
-- Scenario: Student triggers payment on Web Dashboard while Registrar processes cash at Bursar counter.
-- Without locking: Lost update anomaly (both read balance = $1000, both apply payments concurrently, final balance corrupt).
-- With row-level lock: Second transaction waits until the first commits, reading the updated balance atomically.

-- Thread 1:
-- START TRANSACTION;
-- SELECT balance_due, paid_amount FROM bills WHERE bill_id = 'BILL-002' FOR UPDATE;
-- (Row is locked exclusively; Thread 2 blocks here until Thread 1 executes COMMIT)
-- UPDATE bills SET paid_amount = paid_amount + 500, balance_due = balance_due - 500 WHERE bill_id = 'BILL-002';
-- COMMIT;

-- ---------------------------------------------------------------------
-- 4. Optimistic Concurrency Control Alternative (Version Checking)
-- ---------------------------------------------------------------------
-- Used for web APIs to prevent stale overwrite without long-lived DB locks:
-- UPDATE bills
-- SET paid_amount = paid_amount + 500.00,
--     balance_due = balance_due - 500.00,
--     version = version + 1
-- WHERE bill_id = 'BILL-002' AND version = 1;
-- If row count returned is 0, another concurrent transaction updated the record, trigger automatic retry.

-- ---------------------------------------------------------------------
-- 5. Optimized Admin Aggregation Report Query
-- ---------------------------------------------------------------------
-- Leverages covering indexes and conditional aggregation (CASE WHEN)
-- to avoid multiple round-trips to the database.

EXPLAIN SELECT 
    s.department,
    COUNT(b.bill_id) AS total_bills,
    COALESCE(SUM(b.final_amount), 0) AS total_billed,
    COALESCE(SUM(b.paid_amount), 0) AS total_collected,
    COALESCE(SUM(b.balance_due), 0) AS total_outstanding,
    ROUND((SUM(b.paid_amount) / NULLIF(SUM(b.final_amount), 0)) * 100, 2) AS collection_percentage,
    COUNT(CASE WHEN b.status = 'PAID' THEN 1 END) AS count_paid,
    COUNT(CASE WHEN b.status = 'OVERDUE' THEN 1 END) AS count_overdue
FROM students s
LEFT JOIN bills b ON s.student_id = b.student_id AND b.semester_id = 'SEM-2026-FALL'
GROUP BY s.department
ORDER BY total_outstanding DESC;
