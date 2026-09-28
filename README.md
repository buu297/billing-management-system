# 🎓 Student Billing Management System

[![Java](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://openjdk.org/)
[![GUI](https://img.shields.io/badge/GUI-Java%20Swing-blue.svg)](https://docs.oracle.com/javase/tutorial/uiswing/)
[![Web Dashboard](https://img.shields.io/badge/Web%20Dashboard-HTML5%20%7C%20CSS3%20%7C%20ES6+-yellow.svg)]()
[![Database](https://img.shields.io/badge/Database-MySQL%208.0%20(InnoDB)-00758F.svg)](https://www.mysql.com/)
[![Transactions](https://img.shields.io/badge/Concurrency-ACID%20Row%20Locking%20(FOR%20UPDATE)-success.svg)]()
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)]()

A comprehensive, enterprise-grade financial tracking and tuition payment management system built with **Java Swing**, **HTML/CSS/JavaScript**, and **MySQL**. 

This system was engineered to solve high-concurrency billing challenges in academic institutions, featuring **dual interfaces** (an online **Student Payment Web Dashboard** and a desktop **Admin Billing Report Generator & Management Console**) backed by an **ACID-compliant transactional core with optimized SQL queries**.

---

## 🌟 Key Engineering Highlights

- **Dual-Interface Architecture**:
  - 🌐 **Student Payment Dashboard (Web)**: Responsive web portal built in HTML5, CSS3, and modern JavaScript allowing students to review itemized tuition fees, check real-time billing statuses (`PAID`, `PARTIAL`, `UNPAID`, `OVERDUE`), process online payments (VietQR / VNPay QR, Credit/Debit Card, Bank Transfer), and download formatted invoices.
  - 🖥️ **Admin Billing Report Generator (Java Swing)**: Executive desktop console equipped with real-time financial KPI cards, student billing ledger, counter cash transaction recording, aggregate analytics, and one-click export to CSV and formatted printable HTML reports.
- **Financial Tracking & Status Lifecycle**:
  - Dynamic invoice fee composition supporting tuition courses, computing laboratory fees, digital library access, health insurance, and academic scholarship waivers.
  - Automated status state machine dynamically transitioning invoices through `UNPAID` ➔ `PARTIAL` ➔ `PAID` or flagged as `OVERDUE` based on maturity dates.
- **Optimized SQL Queries & High-Concurrency Protection**:
  - Implements **pessimistic row-level locking (`SELECT ... FOR UPDATE`)** inside atomic database transactions to completely prevent race conditions, lost updates, and duplicate payments under concurrent access.
  - Index-tuned database schema featuring composite indexes on `(student_id, semester_id)`, `(status, due_date)`, and `(bill_id, payment_date)` to maximize query throughput and minimize disk I/O.
- **Interactive Concurrency Stress Lab**:
  - Built-in multi-threaded benchmarking tool in the Admin Console that fires up to 20 parallel threads attempting concurrent payments against the same invoice, mathematically proving zero balance drift and 100% ACID consistency.
- **Zero-Friction Dual Execution Engine**:
  - Connects out of the box to production **MySQL** (InnoDB engine).
  - Automatically falls back to an embedded high-performance transactional engine if MySQL is offline, enabling **1-click execution** with zero configuration required!

---

## 🏛️ System Architecture

```
+---------------------------------------------------------------------------------------------------+
|                                 STUDENT BILLING MANAGEMENT SYSTEM                                 |
+---------------------------------------------------------------------------------------------------+
|                                                                                                   |
|    [ STUDENT TOUCHPOINT ]                                       [ BURSAR / ADMIN CONSOLE ]        |
|  +---------------------------+                                +------------------------------+    |
|  |  Student Payment Web App  |                                |    Java Swing Admin Desk     |    |
|  |  HTML5 / CSS3 / ES6+ JS   |                                | Modern Look & Feel (Cards)   |    |
|  +---------------------------+                                +------------------------------+    |
|         |               |                                                     |                   |
|  (REST API / HTTP)  (Direct HTML)                                     (Desktop Event Bus)         |
|         |               |                                                     |                   |
|         v               v                                                     v                   |
|  +-------------------------------------------------------------------------------------------+    |
|  |                           CORE APPLICATION ENGINE (Java 21 LTS)                           |    |
|  |                                                                                           |    |
|  |  +---------------------+   +-------------------------+   +-----------------------------+  |    |
|  |  | Embedded HTTP Server|   | ConcurrencyPaymentServ. |   | ReportGeneratorService      |  |    |
|  |  | (Port 8080 REST)    |   | [ACID Transaction Gate] |   | [CSV Export & HTML Receipt] |  |    |
|  |  +---------------------+   +-------------------------+   +-----------------------------+  |    |
|  |                                         |                                                 |    |
|  |                            +-------------------------+                                    |    |
|  |                            |     DatabaseManager     |                                    |    |
|  |                            +-------------------------+                                    |    |
|  +-----------------------------------------|-------------------------------------------------+    |
|                                            |                                                      |
|                   +------------------------+------------------------+                             |
|                   |                                                 |                             |
|                   v                                                 v                             |
|     +---------------------------+                     +---------------------------+               |
|     |     MySQL 8.0 (InnoDB)    |                     |   Embedded ACID Engine    |               |
|     |  * SELECT ... FOR UPDATE  |                     |  * Thread-Safe Locks      |               |
|     |  * Composite Indexes      |                     |  * Pre-loaded Seed Data   |               |
|     |  * Stored Procedures      |                     |  * Fallback Zero-Setup    |               |
|     +---------------------------+                     +---------------------------+               |
|                                                                                                   |
+---------------------------------------------------------------------------------------------------+
```

---

## 🔒 Concurrency & SQL Query Optimization

### 1. The Lost Update & Double-Deduction Problem
In an educational institution, tuition payment can occur simultaneously from multiple touchpoints:
1. The student paying online via the **Student Web Dashboard** (e.g., via VNPay / Credit Card).
2. A bursar clerk processing cash payment or financial aid at the **University Counter**.

Without strict concurrency control, two parallel processes reading a `$1,000` balance due could both apply `$600` payments concurrently:
```
Thread 1 (Web):      Reads Balance = $1,000
Thread 2 (Counter):  Reads Balance = $1,000
Thread 1 (Web):      Calculates: 1000 - 600 = $400  ==>  Writes Balance = $400
Thread 2 (Counter):  Calculates: 1000 - 600 = $400  ==>  Writes Balance = $400  (LOST UPDATE!)
Result: Student paid $1,200 total, but database records only $600 received!
```

### 2. The Solution: Pessimistic Row Locking (`SELECT ... FOR UPDATE`)
Our transaction engine locks the specific bill row inside an atomic transaction:

```sql
START TRANSACTION;

-- PESSIMISTIC LOCK: Exclusively locks the row; concurrent transactions block here
SELECT balance_due, paid_amount, status 
FROM bills 
WHERE bill_id = ? 
FOR UPDATE;

-- Validate payment amount does not exceed remaining balance
-- Insert immutable payment transaction record
INSERT INTO payments (payment_id, transaction_ref, bill_id, student_id, amount, payment_method, status, notes, payment_date)
VALUES (?, ?, ?, ?, ?, ?, 'SUCCESS', ?, NOW());

-- Atomic update with optimistic version counter increment
UPDATE bills 
SET paid_amount = paid_amount + ?,
    balance_due = balance_due - ?,
    status = CASE WHEN balance_due - ? <= 0 THEN 'PAID' ELSE 'PARTIAL' END,
    version = version + 1
WHERE bill_id = ?;

COMMIT;
```

### 3. Composite Indexing Strategies
To prevent slow table scans under thousands of concurrent students:

| Index Name | Indexed Columns | Optimized Business Operation | Scan Type |
|---|---|---|---|
| `idx_bills_student_semester` | `(student_id, semester_id)` | Student dashboard tuition lookup | Index Range Scan $O(\log N)$ |
| `idx_bills_status_due` | `(status, due_date)` | Automated delinquent notice & overdue tracking | Direct Index Scan $O(\log N)$ |
| `idx_payments_bill_date` | `(bill_id, payment_date DESC)`| Payment ledger pagination & receipts | Covering Index Scan |
| `idx_payments_ref` | `(transaction_ref)` (UNIQUE) | Idempotent transaction execution | Unique Key Lookup $O(1)$ |

---

## 📊 Database Schema (Relational DDL)

```
  +--------------------+         +--------------------+         +--------------------+
  |      students      | 1     * |       bills        | 1     * |     bill_items     |
  +--------------------+---------+--------------------+---------+--------------------+
  | PK  student_id     |         | PK  bill_id        |         | PK  item_id        |
  |     first_name     |         |     invoice_number |         | FK  bill_id        |
  |     last_name      |         | FK  student_id     |         |     item_type      |
  |     email (UQ)     |         | FK  semester_id    |         |     description    |
  |     phone          |         |     total_amount   |         |     amount         |
  |     department     |         |     discount_amount|         +--------------------+
  |     major          |         |     final_amount   |
  |     academic_year  |         |     paid_amount    |
  |     status         |         |     balance_due    |
  +--------------------+         |     due_date       |
                                 |     status         |
                                 |     version        |
                                 +--------------------+
                                           | 1
                                           |
                                           | *
                                 +--------------------+
                                 |      payments      |
                                 +--------------------+
                                 | PK  payment_id     |
                                 | UQ  transaction_ref|
                                 | FK  bill_id        |
                                 | FK  student_id     |
                                 |     amount         |
                                 |     payment_method |
                                 |     status         |
                                 |     payment_date   |
                                 +--------------------+
```

---

## 🖥️ User Interfaces

### 1. Student Payment Web Dashboard
- **Active Student Switcher**: Instantly switch between sample student accounts (e.g., *Nguyen Van An*, *Tran Thi Bich*, *Le Quoc Cuong*) to test different account states.
- **Tuition Breakdown Modal**: Inspect fee components (Course Credits, Laboratory Fees, Library Subscriptions, Medical Coverage, and Merit Scholarships).
- **Online Checkout Modal**:
  - **VietQR / VNPay QR Code**: Generates simulated banking QR code for instant scan-to-pay.
  - **Credit / Debit Card**: Real-time card payment processing.
  - **Bank Transfer**: University treasury account details.
- **Printable E-Invoice**: One-click printable PDF / receipt generation directly from the browser.

### 2. Admin Billing Report Generator (Java Swing)
- **Financial KPI Cards**: Instant calculation of Gross Invoiced, Total Collected, Outstanding Receivables, and Collection Rate %.
- **Billing Management Panel**: Search and filter by semester, department, or status; issue new bills with custom fee items.
- **Payment Ledger & Audit Trail**: Full transaction history with official receipt viewer and counter payment recorder.
- **Admin Report Generator**: Filter data by department and semester, review collection efficiency, export directly to **CSV** or print formatted **HTML summary reports**.
- **Concurrency Stress Lab**: Live visual simulation executing concurrent thread pools against database invoices to test ACID compliance.

---

## 🚀 Quick Start Guide

### Prerequisites
- **Java JDK 21+** installed and available in `PATH`.
- (Optional) **MySQL 8.0+** running on `localhost:3306` (The system automatically falls back to its embedded engine if MySQL is offline).

### 1-Click Launch (Windows)

| File | Description |
|---|---|
| **`run-all.bat`** | Launches both the Web Server (port 8080) and the Java Swing Admin Console simultaneously. |
| **`run-admin.bat`** | Launches the desktop **Java Swing Admin Console & Report Generator**. |
| **`run-web.bat`** | Starts the embedded HTTP server and opens the **Student Web Dashboard** in your default browser. |
| **`compile.bat`** | Compiles all Java source code using standalone `javac`. |

### Launching via Command Line
```bash
# 1. Compile the project
compile.bat

# 2. Run the application
java -cp "bin;lib/*;src/main/resources" com.buu297.billing.Main
```

### Running with Apache Maven
```bash
mvn clean compile
mvn exec:java -Dexec.mainClass="com.buu297.billing.Main"
```

---

## 🗄️ MySQL Database Setup (Optional)

If you wish to use a local MySQL server:
```bash
# Using the automated setup script:
cd database
setup_mysql.bat

# Or manually via MySQL CLI:
mysql -u root -p < database/schema.sql
mysql -u root -p < database/seed_data.sql
```
Credentials can be configured in `src/main/resources/application.properties`:
```properties
db.url=jdbc:mysql://localhost:3306/billing_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=root
db.password=
server.port=8080
```

---

## 🧪 Automated System Verification Suite

Run the built-in system verification suite to validate all 7 subsystem components:
```bash
java -ea -cp "bin;lib/*;src/main/resources" com.buu297.billing.SystemVerificationTest
```

**Verification Results**:
- ✔ Database Manager connection & fallback detection
- ✔ Student DAO lookup & search
- ✔ Bill DAO calculations & itemized aggregations
- ✔ Single tuition payment processing with ACID commit
- ✔ Multi-threaded Concurrency Stress Test (10 concurrent threads @ $150 simultaneous, 0 race conditions, zero balance drift)
- ✔ Financial Report Generator & Department Revenue summary
- ✔ CSV & HTML invoice templating engines

---

## 📂 Project Structure

```
billing-management-system/
├── database/
│   ├── schema.sql              # MySQL DDL, indexes, constraints & stored procedures
│   ├── seed_data.sql           # Realistic students, semesters, bills & payments
│   ├── queries_analysis.sql    # Concurrency analysis & EXPLAIN optimization
│   ├── setup_mysql.bat         # Automated MySQL setup script
│   └── README.md               # Database architecture guide
├── web/                        # Dual Interface 1: Student Payment Dashboard
│   ├── index.html              # Modern student portal layout
│   ├── css/
│   │   └── style.css           # Modern banking UI design
│   └── js/
│       ├── app.js              # Portal logic, payment processing & invoice print
│       └── mockData.js         # Offline fallback data
├── lib/
│   └── mysql-connector-j-8.3.0.jar # MySQL JDBC Driver
├── src/
│   ├── main/
│   │   ├── java/com/buu297/billing/
│   │   │   ├── Main.java                   # Unified application entry point
│   │   │   ├── SystemVerificationTest.java # Automated verification suite
│   │   │   ├── config/
│   │   │   │   ├── AppConfig.java          # Configuration properties loader
│   │   │   │   └── DatabaseManager.java    # Connection manager with fallback
│   │   │   ├── model/                      # Domain models & enums
│   │   │   │   ├── Student.java
│   │   │   │   ├── Bill.java
│   │   │   │   ├── BillItem.java
│   │   │   │   ├── Payment.java
│   │   │   │   ├── BillingStatus.java
│   │   │   │   ├── PaymentMethod.java
│   │   │   │   ├── ItemType.java
│   │   │   │   ├── TransactionResult.java
│   │   │   │   └── BillingReportSummary.java
│   │   │   ├── dao/                        # Data Access Object pattern
│   │   │   │   ├── StudentDAO.java
│   │   │   │   ├── BillDAO.java
│   │   │   │   ├── PaymentDAO.java
│   │   │   │   ├── ReportDAO.java
│   │   │   │   ├── memory/                 # Thread-safe in-memory DAOs
│   │   │   │   └── mysql/                  # MySQL JDBC DAOs
│   │   │   ├── service/                    # Business logic
│   │   │   │   ├── BillingService.java
│   │   │   │   ├── ConcurrencyPaymentService.java
│   │   │   │   └── ReportGeneratorService.java
│   │   │   ├── server/                     # Embedded Web & REST API Server
│   │   │   │   ├── StudentApiServer.java
│   │   │   │   ├── StaticFileHandler.java
│   │   │   │   └── JsonUtil.java
│   │   │   └── ui/                         # Dual Interface 2: Java Swing Admin App
│   │   │       ├── Theme.java
│   │   │       ├── AdminBillingApp.java    # Main JFrame with navigation sidebar
│   │   │       ├── DashboardPanel.java     # Financial KPI cards & recent txns
│   │   │       ├── BillingManagementPanel.java # Invoice management & line items
│   │   │       ├── PaymentLedgerPanel.java # Counter payment & receipt dialog
│   │   │       ├── ReportGeneratorPanel.java   # Filterable reports & CSV export
│   │   │       ├── ConcurrencySimulatorPanel.java # Live ACID stress test
│   │   │       └── components/
│   │   │           ├── StatCard.java
│   │   │           └── ModernTable.java
│   │   └── resources/
│   │       └── application.properties
├── pom.xml                     # Maven project descriptor
├── compile.bat                 # Direct compilation script
├── run-all.bat                 # Dual-mode launch script
├── run-admin.bat               # Desktop admin console launch
├── run-web.bat                 # Web dashboard launch
├── .gitignore
└── README.md
```

---

## 👨‍💻 Author

- **GitHub**: [@buu297](https://github.com/buu297)
- **Repository**: [https://github.com/buu297/billing-management-system](https://github.com/buu297/billing-management-system)