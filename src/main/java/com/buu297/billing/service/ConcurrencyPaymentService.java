package com.buu297.billing.service;

import com.buu297.billing.config.DatabaseManager;
import com.buu297.billing.dao.BillDAO;
import com.buu297.billing.dao.PaymentDAO;
import com.buu297.billing.dao.StudentDAO;
import com.buu297.billing.dao.memory.InMemoryDatabase;
import com.buu297.billing.model.*;

import java.math.BigDecimal;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import java.util.concurrent.*;

public class ConcurrencyPaymentService {
    private final DatabaseManager dbManager;
    private final BillDAO billDAO;
    private final PaymentDAO paymentDAO;
    private final StudentDAO studentDAO;

    public ConcurrencyPaymentService() {
        this.dbManager = DatabaseManager.getInstance();
        this.billDAO = dbManager.getBillDAO();
        this.paymentDAO = dbManager.getPaymentDAO();
        this.studentDAO = dbManager.getStudentDAO();
    }

    /**
     * Processes a tuition payment with strict ACID transaction semantics.
     * Prevents race conditions and double-spending using row-level locking.
     */
    public TransactionResult processPayment(String billId, String studentId, BigDecimal amount,
                                           PaymentMethod method, String notes) {
        long startTime = System.currentTimeMillis();

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return TransactionResult.failure("INVALID_AMOUNT", "Payment amount must be greater than zero.", BigDecimal.ZERO);
        }

        String txnRef = "TXN-" + System.currentTimeMillis() + "-" + (int)(1000 + Math.random() * 9000);

        if (dbManager.isUsingMySQL()) {
            return processPaymentMySQL(billId, studentId, amount, method, txnRef, notes, startTime);
        } else {
            return processPaymentInMemory(billId, studentId, amount, method, txnRef, notes, startTime);
        }
    }

    /**
     * MySQL Implementation using InnoDB transaction and row-level pessimistic locking (SELECT ... FOR UPDATE)
     */
    private TransactionResult processPaymentMySQL(String billId, String studentId, BigDecimal amount,
                                                  PaymentMethod method, String txnRef, String notes, long startTime) {
        String lockSql = "SELECT balance_due, paid_amount, status FROM bills WHERE bill_id = ? FOR UPDATE";
        String insertPaySql = "INSERT INTO payments (payment_id, transaction_ref, bill_id, student_id, amount, payment_method, status, notes, payment_date) " +
                              "VALUES (?, ?, ?, ?, ?, ?, 'SUCCESS', ?, NOW())";
        String updateBillSql = "UPDATE bills SET paid_amount = ?, balance_due = ?, status = ?, version = version + 1 WHERE bill_id = ?";

        Connection conn = null;
        try {
            conn = dbManager.getConnection();
            conn.setAutoCommit(false); // Begin ACID Transaction

            BigDecimal currentBalance = BigDecimal.ZERO;
            BigDecimal currentPaid = BigDecimal.ZERO;
            String currentStatus = "UNPAID";

            try (PreparedStatement ps = conn.prepareStatement(lockSql)) {
                ps.setString(1, billId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        return TransactionResult.failure("BILL_NOT_FOUND", "Bill ID not found in database.", BigDecimal.ZERO);
                    }
                    currentBalance = rs.getBigDecimal("balance_due");
                    currentPaid = rs.getBigDecimal("paid_amount");
                    currentStatus = rs.getString("status");
                }
            }

            if ("PAID".equalsIgnoreCase(currentStatus) || currentBalance.compareTo(BigDecimal.ZERO) <= 0) {
                conn.rollback();
                return TransactionResult.failure("ALREADY_PAID", "Invoice is already fully settled.", currentBalance);
            }

            if (amount.compareTo(currentBalance) > 0) {
                conn.rollback();
                return TransactionResult.failure("EXCEEDS_BALANCE",
                        "Payment amount ($" + amount + ") exceeds remaining balance ($" + currentBalance + ").", currentBalance);
            }

            BigDecimal newPaid = currentPaid.add(amount);
            BigDecimal newBalance = currentBalance.subtract(amount);
            BillingStatus newStatus = (newBalance.compareTo(BigDecimal.ZERO) == 0) ? BillingStatus.PAID : BillingStatus.PARTIAL;

            String payId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            try (PreparedStatement ps = conn.prepareStatement(insertPaySql)) {
                ps.setString(1, payId);
                ps.setString(2, txnRef);
                ps.setString(3, billId);
                ps.setString(4, studentId);
                ps.setBigDecimal(5, amount);
                ps.setString(6, method.name());
                ps.setString(7, notes != null ? notes : "Tuition Payment");
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement(updateBillSql)) {
                ps.setBigDecimal(1, newPaid);
                ps.setBigDecimal(2, newBalance);
                ps.setString(3, newStatus.name());
                ps.setString(4, billId);
                ps.executeUpdate();
            }

            conn.commit(); // Commit Transaction

            long duration = System.currentTimeMillis() - startTime;
            return new TransactionResult(true, "SUCCESS",
                    "Payment of $" + amount + " successfully processed.", txnRef, amount, currentBalance, newBalance, newStatus, duration);

        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            long duration = System.currentTimeMillis() - startTime;
            return TransactionResult.failure("SQL_ERROR", "Transaction failed: " + e.getMessage(), BigDecimal.ZERO);
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ignored) {}
            }
        }
    }

    /**
     * In-Memory Implementation using synchronized Row-Lock Mutex to emulate pessimistic locking
     */
    private TransactionResult processPaymentInMemory(String billId, String studentId, BigDecimal amount,
                                                    PaymentMethod method, String txnRef, String notes, long startTime) {
        InMemoryDatabase memDb = InMemoryDatabase.getInstance();
        Object rowLock = memDb.getRowLock(billId);

        // Synchronize on the specific bill ID mutex (simulates row-level lock)
        synchronized (rowLock) {
            Optional<Bill> optBill = billDAO.findById(billId);
            if (!optBill.isPresent()) {
                return TransactionResult.failure("BILL_NOT_FOUND", "Bill ID not found.", BigDecimal.ZERO);
            }

            Bill bill = optBill.get();
            BigDecimal currentBalance = bill.getBalanceDue();

            if (bill.getStatus() == BillingStatus.PAID || currentBalance.compareTo(BigDecimal.ZERO) <= 0) {
                return TransactionResult.failure("ALREADY_PAID", "Invoice is already fully settled.", currentBalance);
            }

            if (amount.compareTo(currentBalance) > 0) {
                return TransactionResult.failure("EXCEEDS_BALANCE",
                        "Payment amount ($" + amount + ") exceeds remaining balance ($" + currentBalance + ").", currentBalance);
            }

            BigDecimal previousBalance = currentBalance;
            BigDecimal newPaid = bill.getPaidAmount().add(amount);
            BigDecimal newBalance = currentBalance.subtract(amount);
            bill.setPaidAmount(newPaid);
            bill.setBalanceDue(newBalance);

            if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
                bill.setStatus(BillingStatus.PAID);
            } else {
                bill.setStatus(BillingStatus.PARTIAL);
            }
            bill.setVersion(bill.getVersion() + 1);
            billDAO.update(bill);

            String payId = "PAY-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            Payment payment = new Payment(payId, txnRef, billId, studentId, amount, method, "SUCCESS", notes);
            paymentDAO.recordPayment(payment);

            memDb.addAuditLog(new AuditLog("BILL", billId, "PAYMENT_PROCESSED",
                    "Payment of $" + amount + " recorded. Remaining: $" + newBalance, "CONCURRENCY_ENGINE"));

            long duration = System.currentTimeMillis() - startTime;
            return new TransactionResult(true, "SUCCESS",
                    "Payment of $" + amount + " successfully processed.",
                    txnRef, amount, previousBalance, newBalance, bill.getStatus(), duration);
        }
    }

    /**
     * Simulates concurrent multi-threaded payment requests hitting the same bill row simultaneously.
     * Demonstrates concurrency isolation, row locking, and mathematical balance consistency.
     */
    public ConcurrencySimulationReport simulateConcurrentPayments(String billId, String studentId,
                                                                 BigDecimal paymentPerThread, int threadCount) {
        Optional<Bill> optBill = billDAO.findById(billId);
        if (!optBill.isPresent()) {
            throw new IllegalArgumentException("Bill not found: " + billId);
        }
        Bill initialBill = optBill.get();
        BigDecimal initialBalance = initialBill.getBalanceDue();

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneGate = new CountDownLatch(threadCount);

        List<TransactionResult> results = Collections.synchronizedList(new ArrayList<>());
        long simStart = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            final int threadIdx = i + 1;
            executor.submit(() -> {
                try {
                    startGate.await(); // Hold all threads until the pistol fires
                    TransactionResult res = processPayment(billId, studentId, paymentPerThread,
                            PaymentMethod.CREDIT_CARD, "Simulated Concurrent Payment Thread #" + threadIdx);
                    results.add(res);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneGate.countDown();
                }
            });
        }

        // Fire all threads simultaneously
        startGate.countDown();

        try {
            doneGate.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
        }

        long simTotalDuration = System.currentTimeMillis() - simStart;

        Bill finalBill = billDAO.findById(billId).orElse(initialBill);
        BigDecimal finalBalance = finalBill.getBalanceDue();
        BigDecimal totalPaidInSim = initialBalance.subtract(finalBalance);

        int successfulTxns = (int) results.stream().filter(TransactionResult::isSuccess).count();
        int rejectedTxns = threadCount - successfulTxns;

        return new ConcurrencySimulationReport(
                billId, threadCount, paymentPerThread, initialBalance, finalBalance,
                totalPaidInSim, successfulTxns, rejectedTxns, simTotalDuration, results
        );
    }

    public static class ConcurrencySimulationReport {
        public final String billId;
        public final int threadCount;
        public final BigDecimal paymentPerThread;
        public final BigDecimal initialBalance;
        public final BigDecimal finalBalance;
        public final BigDecimal totalPaidInSim;
        public final int successfulTxns;
        public final int rejectedTxns;
        public final long totalDurationMs;
        public final List<TransactionResult> results;

        public ConcurrencySimulationReport(String billId, int threadCount, BigDecimal paymentPerThread,
                                         BigDecimal initialBalance, BigDecimal finalBalance,
                                         BigDecimal totalPaidInSim, int successfulTxns, int rejectedTxns,
                                         long totalDurationMs, List<TransactionResult> results) {
            this.billId = billId;
            this.threadCount = threadCount;
            this.paymentPerThread = paymentPerThread;
            this.initialBalance = initialBalance;
            this.finalBalance = finalBalance;
            this.totalPaidInSim = totalPaidInSim;
            this.successfulTxns = successfulTxns;
            this.rejectedTxns = rejectedTxns;
            this.totalDurationMs = totalDurationMs;
            this.results = results;
        }

        public boolean isDataConsistent() {
            // Balance must never drop below 0
            if (finalBalance.compareTo(BigDecimal.ZERO) < 0) return false;
            // Total amount paid must equal successful transactions * paymentPerThread
            BigDecimal expectedPaid = paymentPerThread.multiply(BigDecimal.valueOf(successfulTxns));
            return totalPaidInSim.compareTo(expectedPaid) == 0;
        }
    }
}
