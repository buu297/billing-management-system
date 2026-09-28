# Database Architecture & Query Optimization Guide

This module contains the relational schema, sample dataset, and concurrency optimization benchmarks for the **Student Billing Management System**.

## Schema Components

- **`schema.sql`**: Full DDL defining 6 core tables (`students`, `semesters`, `bills`, `bill_items`, `payments`, `audit_logs`), check constraints, foreign keys, composite indexes, and the ACID transaction stored procedure `sp_process_tuition_payment`.
- **`seed_data.sql`**: Comprehensive test dataset containing multiple academic terms, students across diverse departments, itemized tuition fees, and realistic payment histories.
- **`queries_analysis.sql`**: In-depth SQL benchmarks comparing indexing strategies, row-level locking (`SELECT ... FOR UPDATE`), and EXPLAIN execution plans.

## Concurrency & ACID Integrity

### 1. The Lost Update Problem
In an educational institution, tuition payment can occur simultaneously from multiple touchpoints:
1. The student paying online via the **Student Web Dashboard**.
2. A bursar clerk processing a scholarship or in-person cash payment via the **Java Swing Admin Console**.

If both systems read a bill with remaining balance of `$1,000` and each processes a `$600` payment concurrently without locking:
- Both read `balance_due = $1000`.
- Process A writes `paid = $600, balance = $400`.
- Process B overwrites with `paid = $600, balance = $400`.
- The student paid `$1,200` total, but the database records only `$600` received, and the university loses money or creates an accounting discrepancy.

### 2. The Solution: Pessimistic Row-Level Locking (`SELECT ... FOR UPDATE`)
Our transaction engine locks the specific bill row inside an atomic transaction:
```sql
START TRANSACTION;
SELECT balance_due, paid_amount, status 
FROM bills 
WHERE bill_id = ? 
FOR UPDATE;
```
Any competing transaction on the same `bill_id` blocks until the active transaction commits or rolls back, guaranteeing zero lost updates and preventing any overpayment beyond the current balance.

## Setup Instructions

1. Ensure MySQL Server 8.0+ is running.
2. Run `setup_mysql.bat` or execute in MySQL CLI:
   ```bash
   mysql -u root -p < database/schema.sql
   mysql -u root -p < database/seed_data.sql
   ```
