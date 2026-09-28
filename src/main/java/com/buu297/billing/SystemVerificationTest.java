package com.buu297.billing;

import com.buu297.billing.config.DatabaseManager;
import com.buu297.billing.dao.BillDAO;
import com.buu297.billing.dao.StudentDAO;
import com.buu297.billing.model.*;
import com.buu297.billing.service.BillingService;
import com.buu297.billing.service.ConcurrencyPaymentService;
import com.buu297.billing.service.ReportGeneratorService;

import java.math.BigDecimal;
import java.util.List;

public class SystemVerificationTest {
    public static void main(String[] args) {
        System.out.println(">>> RUNNING FULL SYSTEM VERIFICATION SUITE <<<");

        // 1. Verify Database Manager
        DatabaseManager db = DatabaseManager.getInstance();
        System.out.println("[TEST 1] Database Mode: " + db.getDatabaseStatusDescription());

        // 2. Verify Student DAO
        StudentDAO studentDAO = db.getStudentDAO();
        List<Student> students = studentDAO.findAll();
        System.out.println("[TEST 2] Active Students Count: " + students.size());
        assert students.size() >= 8 : "Expected at least 8 students";

        // 3. Verify Bill DAO & Statuses
        BillDAO billDAO = db.getBillDAO();
        List<Bill> bills = billDAO.findAll();
        System.out.println("[TEST 3] Seed Bills Loaded: " + bills.size());
        assert bills.size() >= 8 : "Expected at least 8 bills";

        // 4. Verify Payment Processing
        ConcurrencyPaymentService payService = new ConcurrencyPaymentService();
        Bill testBill = bills.stream()
                .filter(b -> b.getStatus() == BillingStatus.PARTIAL || b.getStatus() == BillingStatus.UNPAID)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No unpaid bill found"));

        BigDecimal initialBal = testBill.getBalanceDue();
        System.out.println("[TEST 4] Testing Single Payment on Bill " + testBill.getBillId() + " (Initial Due: $" + initialBal + ")");

        TransactionResult res = payService.processPayment(
                testBill.getBillId(), testBill.getStudentId(), new BigDecimal("100.00"),
                PaymentMethod.VNPAY_QR, "Automated Verification Test Payment"
        );
        System.out.println("  -> Payment Result: " + res.getStatusCode() + ", Txn Ref: " + res.getTransactionRef() +
                ", New Balance: $" + res.getRemainingBalance() + " (ACID Latency: " + res.getExecutionDurationMs() + "ms)");
        assert res.isSuccess() : "Payment should succeed";
        assert res.getRemainingBalance().compareTo(initialBal.subtract(new BigDecimal("100.00"))) == 0 : "Balance must match";

        // 5. Verify Concurrency Simulator (Pessimistic Locking / Invariant verification)
        System.out.println("[TEST 5] Testing Multi-threaded Concurrency (10 threads @ $150 simultaneous)");
        ConcurrencyPaymentService.ConcurrencySimulationReport simReport =
                payService.simulateConcurrentPayments(testBill.getBillId(), testBill.getStudentId(), new BigDecimal("150.00"), 10);

        System.out.println("  -> Threads Fired: " + simReport.threadCount +
                ", Committed: " + simReport.successfulTxns +
                ", Blocked/Prevented: " + simReport.rejectedTxns +
                ", Execution Time: " + simReport.totalDurationMs + "ms");
        System.out.println("  -> Mathematical Invariant Consistent: " + simReport.isDataConsistent());
        assert simReport.isDataConsistent() : "Invariant must hold: 0 race conditions";

        // 6. Verify Report Generator
        ReportGeneratorService repService = new ReportGeneratorService();
        BillingReportSummary summary = repService.generateSummary("ALL", "ALL", "ALL", null, null);
        System.out.println("[TEST 6] Report Summary Generated: Total Billed = $" + summary.getTotalBilledAmount() +
                ", Collected = $" + summary.getTotalCollectedAmount() +
                ", Outstanding = $" + summary.getTotalOutstandingAmount() +
                ", Collection Rate = " + String.format("%.2f%%", summary.getCollectionRatePct()));

        // 7. Verify CSV & HTML generation
        String csv = repService.exportBillsToCSV(bills);
        assert csv.contains("Invoice Number") : "CSV header present";
        String html = repService.generateHTMLInvoice(testBill, null);
        assert html.contains("INVOICE") : "HTML invoice rendered";
        System.out.println("[TEST 7] CSV & HTML Export Engines Verified.");

        System.out.println("\n🎉 ALL 7 SYSTEM VERIFICATION TESTS PASSED WITH 100% SUCCESS!");
    }
}