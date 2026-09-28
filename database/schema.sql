-- =====================================================================
-- Database Schema: Student Billing Management System
-- Stack: HTML, CSS, JavaScript, Java Swing, MySQL
-- Author: buu297 (github.com/buu297/billing-management-system)
-- =====================================================================

DROP DATABASE IF EXISTS billing_db;
CREATE DATABASE billing_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE billing_db;

-- ---------------------------------------------------------------------
-- 1. Table: students
-- Stores student demographic and enrollment information.
-- ---------------------------------------------------------------------
CREATE TABLE students (
    student_id VARCHAR(20) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20),
    department VARCHAR(80) NOT NULL,
    major VARCHAR(80) NOT NULL,
    academic_year INT NOT NULL,
    status ENUM('ACTIVE', 'SUSPENDED', 'GRADUATED') DEFAULT 'ACTIVE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Optimized indexes for student search and department aggregation
CREATE INDEX idx_students_dept ON students(department, major);
CREATE INDEX idx_students_status ON students(status);

-- ---------------------------------------------------------------------
-- 2. Table: semesters
-- Academic terms (e.g., Fall 2026, Spring 2026).
-- ---------------------------------------------------------------------
CREATE TABLE semesters (
    semester_id VARCHAR(20) NOT NULL,
    term_name VARCHAR(50) NOT NULL,
    academic_year INT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    PRIMARY KEY (semester_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ---------------------------------------------------------------------
-- 3. Table: bills
-- Financial invoices issued to students for tuition and associated fees.
-- Includes optimistic concurrency version and decimal precision.
-- ---------------------------------------------------------------------
CREATE TABLE bills (
    bill_id VARCHAR(36) NOT NULL,
    invoice_number VARCHAR(30) NOT NULL UNIQUE,
    student_id VARCHAR(20) NOT NULL,
    semester_id VARCHAR(20) NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    discount_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    final_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    paid_amount DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    balance_due DECIMAL(12, 2) NOT NULL DEFAULT 0.00,
    due_date DATE NOT NULL,
    status ENUM('UNPAID', 'PARTIAL', 'PAID', 'OVERDUE') DEFAULT 'UNPAID',
    version INT NOT NULL DEFAULT 1, -- Optimistic concurrency control counter
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (bill_id),
    CONSTRAINT fk_bills_student FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE RESTRICT,
    CONSTRAINT fk_bills_semester FOREIGN KEY (semester_id) REFERENCES semesters(semester_id) ON DELETE RESTRICT,
    CONSTRAINT chk_bills_amount CHECK (final_amount >= 0 AND paid_amount >= 0 AND balance_due >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- High-performance indexes:
-- 1. Accelerates querying all bills of a student for a specific semester
CREATE INDEX idx_bills_student_semester ON bills(student_id, semester_id);
-- 2. Optimized for overdue alerts & status filtering in admin report generation
CREATE INDEX idx_bills_status_due ON bills(status, due_date);
-- 3. Invoice lookup index
CREATE INDEX idx_bills_invoice_num ON bills(invoice_number);

-- ---------------------------------------------------------------------
-- 4. Table: bill_items
-- Itemized breakdown of fees comprising each tuition bill.
-- ---------------------------------------------------------------------
CREATE TABLE bill_items (
    item_id VARCHAR(36) NOT NULL,
    bill_id VARCHAR(36) NOT NULL,
    item_type ENUM(
        'TUITION_COURSE',
        'LAB_FEE',
        'LIBRARY_FEE',
        'HEALTH_INSURANCE',
        'CAMPUS_FACILITY',
        'SCHOLARSHIP_DISCOUNT',
        'LATE_PENALTY'
    ) NOT NULL,
    description VARCHAR(255) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (item_id),
    CONSTRAINT fk_bill_items_bill FOREIGN KEY (bill_id) REFERENCES bills(bill_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_bill_items_bill ON bill_items(bill_id);

-- ---------------------------------------------------------------------
-- 5. Table: payments
-- Financial transaction ledger recording student payments.
-- ---------------------------------------------------------------------
CREATE TABLE payments (
    payment_id VARCHAR(36) NOT NULL,
    transaction_ref VARCHAR(40) NOT NULL UNIQUE,
    bill_id VARCHAR(36) NOT NULL,
    student_id VARCHAR(20) NOT NULL,
    amount DECIMAL(12, 2) NOT NULL,
    payment_method ENUM('CREDIT_CARD', 'BANK_TRANSFER', 'VNPAY_QR', 'CASH', 'FINANCIAL_AID') NOT NULL,
    status ENUM('SUCCESS', 'PENDING', 'FAILED') DEFAULT 'SUCCESS',
    notes VARCHAR(255),
    payment_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (payment_id),
    CONSTRAINT fk_payments_bill FOREIGN KEY (bill_id) REFERENCES bills(bill_id) ON DELETE RESTRICT,
    CONSTRAINT fk_payments_student FOREIGN KEY (student_id) REFERENCES students(student_id) ON DELETE RESTRICT,
    CONSTRAINT chk_payment_amount CHECK (amount > 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Composite index to optimize transaction ledger pagination and audits
CREATE INDEX idx_payments_bill_date ON payments(bill_id, payment_date DESC);
CREATE INDEX idx_payments_student_date ON payments(student_id, payment_date DESC);
CREATE INDEX idx_payments_ref ON payments(transaction_ref);

-- ---------------------------------------------------------------------
-- 6. Table: audit_logs
-- Immutable audit trail for financial events and status transitions.
-- ---------------------------------------------------------------------
CREATE TABLE audit_logs (
    log_id INT AUTO_INCREMENT PRIMARY KEY,
    entity_name VARCHAR(50) NOT NULL,
    entity_id VARCHAR(36) NOT NULL,
    action VARCHAR(50) NOT NULL,
    details TEXT,
    performed_by VARCHAR(50) DEFAULT 'SYSTEM',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_audit_entity ON audit_logs(entity_name, entity_id);

-- =====================================================================
-- STORED PROCEDURES & ACID CONCURRENCY CONTROL
-- =====================================================================

DELIMITER //

-- ---------------------------------------------------------------------
-- Stored Procedure: sp_process_tuition_payment
-- Implements pessimistic row-level locking (SELECT ... FOR UPDATE)
-- to ensure strict ACID consistency under concurrent payment requests.
-- Prevents race conditions, double charging, and negative balance due.
-- ---------------------------------------------------------------------
CREATE PROCEDURE sp_process_tuition_payment(
    IN p_bill_id VARCHAR(36),
    IN p_student_id VARCHAR(20),
    IN p_amount DECIMAL(12, 2),
    IN p_payment_method VARCHAR(30),
    IN p_transaction_ref VARCHAR(40),
    IN p_notes VARCHAR(255),
    OUT p_status_code VARCHAR(20),
    OUT p_message VARCHAR(255)
)
proc_label: BEGIN
    DECLARE v_current_balance DECIMAL(12, 2);
    DECLARE v_current_paid DECIMAL(12, 2);
    DECLARE v_bill_status VARCHAR(20);
    DECLARE v_new_balance DECIMAL(12, 2);
    DECLARE v_new_paid DECIMAL(12, 2);
    DECLARE v_new_status VARCHAR(20);
    DECLARE v_new_payment_id VARCHAR(36);

    -- Exit on SQL Exception and rollback
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        SET p_status_code = 'ERROR';
        SET p_message = 'Database error occurred during payment processing. Transaction rolled back.';
    END;

    -- Basic input validation
    IF p_amount <= 0 THEN
        SET p_status_code = 'INVALID_AMOUNT';
        SET p_message = 'Payment amount must be strictly greater than 0.';
        LEAVE proc_label;
    END IF;

    -- START TRANSACTION
    START TRANSACTION;

    -- PESSIMISTIC LOCK: Lock the bill row for update to prevent concurrent updates
    SELECT balance_due, paid_amount, status
    INTO v_current_balance, v_current_paid, v_bill_status
    FROM bills
    WHERE bill_id = p_bill_id
    FOR UPDATE;

    -- Check if bill exists
    IF v_current_balance IS NULL THEN
        ROLLBACK;
        SET p_status_code = 'NOT_FOUND';
        SET p_message = 'Specified bill was not found.';
        LEAVE proc_label;
    END IF;

    -- Check if bill is already settled
    IF v_bill_status = 'PAID' OR v_current_balance <= 0 THEN
        ROLLBACK;
        SET p_status_code = 'ALREADY_PAID';
        SET p_message = 'This bill has already been fully paid.';
        LEAVE proc_label;
    END IF;

    -- Check for overpayment beyond balance due
    IF p_amount > v_current_balance THEN
        ROLLBACK;
        SET p_status_code = 'EXCEEDS_BALANCE';
        SET p_message = CONCAT('Payment amount ($', p_amount, ') exceeds remaining balance ($', v_current_balance, ').');
        LEAVE proc_label;
    END IF;

    -- Calculate updated amounts and determine new status
    SET v_new_paid = v_current_paid + p_amount;
    SET v_new_balance = v_current_balance - p_amount;

    IF v_new_balance = 0 THEN
        SET v_new_status = 'PAID';
    ELSE
        SET v_new_status = 'PARTIAL';
    END IF;

    -- Insert payment ledger record
    SET v_new_payment_id = UUID();
    INSERT INTO payments (
        payment_id, transaction_ref, bill_id, student_id, amount, payment_method, status, notes, payment_date
    ) VALUES (
        v_new_payment_id, p_transaction_ref, p_bill_id, p_student_id, p_amount, p_payment_method, 'SUCCESS', p_notes, NOW()
    );

    -- Update bill record with version increment
    UPDATE bills
    SET paid_amount = v_new_paid,
        balance_due = v_new_balance,
        status = v_new_status,
        version = version + 1,
        updated_at = NOW()
    WHERE bill_id = p_bill_id;

    -- Record immutable audit log
    INSERT INTO audit_logs (entity_name, entity_id, action, details, performed_by)
    VALUES (
        'BILL',
        p_bill_id,
        'PAYMENT_PROCESSED',
        CONCAT('Processed payment ref: ', p_transaction_ref, ', amount: $', p_amount, ', new status: ', v_new_status),
        'SYSTEM_GATEWAY'
    );

    -- COMMIT TRANSACTION
    COMMIT;

    SET p_status_code = 'SUCCESS';
    SET p_message = CONCAT('Payment of $', p_amount, ' successfully applied. Remaining balance: $', v_new_balance);
END //

-- ---------------------------------------------------------------------
-- Stored Procedure: sp_generate_billing_summary
-- Generates aggregate financial reporting grouped by department/semester.
-- Uses optimized indexes for aggregation.
-- ---------------------------------------------------------------------
CREATE PROCEDURE sp_generate_billing_summary(
    IN p_semester_id VARCHAR(20),
    IN p_department VARCHAR(80)
)
BEGIN
    SELECT 
        s.department,
        b.semester_id,
        COUNT(b.bill_id) AS total_bills_issued,
        SUM(b.final_amount) AS total_billed_amount,
        SUM(b.paid_amount) AS total_collected_amount,
        SUM(b.balance_due) AS total_outstanding_amount,
        ROUND((SUM(b.paid_amount) / NULLIF(SUM(b.final_amount), 0)) * 100, 2) AS collection_rate_pct,
        COUNT(CASE WHEN b.status = 'PAID' THEN 1 END) AS count_paid,
        COUNT(CASE WHEN b.status = 'PARTIAL' THEN 1 END) AS count_partial,
        COUNT(CASE WHEN b.status = 'UNPAID' THEN 1 END) AS count_unpaid,
        COUNT(CASE WHEN b.status = 'OVERDUE' THEN 1 END) AS count_overdue
    FROM bills b
    JOIN students s ON b.student_id = s.student_id
    WHERE (p_semester_id IS NULL OR b.semester_id = p_semester_id)
      AND (p_department IS NULL OR s.department = p_department)
    GROUP BY s.department, b.semester_id
    ORDER BY total_outstanding_amount DESC;
END //

DELIMITER ;
