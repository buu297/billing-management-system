package com.buu297.billing.ui;

import com.buu297.billing.config.DatabaseManager;
import com.buu297.billing.dao.BillDAO;
import com.buu297.billing.dao.PaymentDAO;
import com.buu297.billing.dao.StudentDAO;
import com.buu297.billing.model.Bill;
import com.buu297.billing.model.Payment;
import com.buu297.billing.ui.components.ModernTable;
import com.buu297.billing.ui.components.StatCard;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

public class DashboardPanel extends JPanel {
    private final StatCard cardBilled;
    private final StatCard cardCollected;
    private final StatCard cardOutstanding;
    private final StatCard cardStudents;

    private final DefaultTableModel recentTxnModel;
    private final BillDAO billDAO;
    private final PaymentDAO paymentDAO;
    private final StudentDAO studentDAO;

    public DashboardPanel(Runnable onNavigateToBilling, Runnable onNavigateToPayment, Runnable onNavigateToConcurrency) {
        setLayout(new BorderLayout(20, 20));
        setBackground(Theme.BG_MAIN);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        this.billDAO = DatabaseManager.getInstance().getBillDAO();
        this.paymentDAO = DatabaseManager.getInstance().getPaymentDAO();
        this.studentDAO = DatabaseManager.getInstance().getStudentDAO();

        // 1. Header with title and quick actions
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel title = new JLabel("Financial Billing Dashboard");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_MAIN);

        JLabel subtitle = new JLabel("Real-time Tuition Processing & Financial Summary (Fall 2026)");
        subtitle.setFont(Theme.FONT_SUBTITLE);
        subtitle.setForeground(Theme.TEXT_MUTED);

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBox.setOpaque(false);
        titleBox.add(title);
        titleBox.add(subtitle);

        JPanel actionBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionBox.setOpaque(false);

        JButton btnNewBill = Theme.createPrimaryButton("+ Create New Bill");
        btnNewBill.addActionListener(e -> onNavigateToBilling.run());

        JButton btnRecordPay = Theme.createSecondaryButton("Record Payment");
        btnRecordPay.addActionListener(e -> onNavigateToPayment.run());

        JButton btnStressTest = Theme.createSecondaryButton("⚡ Concurrency Lab");
        btnStressTest.addActionListener(e -> onNavigateToConcurrency.run());

        actionBox.add(btnRecordPay);
        actionBox.add(btnStressTest);
        actionBox.add(btnNewBill);

        headerPanel.add(titleBox, BorderLayout.WEST);
        headerPanel.add(actionBox, BorderLayout.EAST);

        // 2. Metric Cards Grid
        JPanel metricsGrid = new JPanel(new GridLayout(1, 4, 16, 0));
        metricsGrid.setOpaque(false);
        metricsGrid.setPreferredSize(new Dimension(800, 100));

        cardBilled = new StatCard("Total Invoiced", "$0.00", "Across all enrolled terms", Theme.PRIMARY);
        cardCollected = new StatCard("Total Collected", "$0.00", "Tuition revenue realized", Theme.SUCCESS);
        cardOutstanding = new StatCard("Outstanding Balance", "$0.00", "Pending & Overdue fees", Theme.DANGER);
        cardStudents = new StatCard("Active Students", "0", "Enrolled this term", Theme.ACCENT);

        metricsGrid.add(cardBilled);
        metricsGrid.add(cardCollected);
        metricsGrid.add(cardOutstanding);
        metricsGrid.add(cardStudents);

        // Top Container
        JPanel topContainer = new JPanel(new BorderLayout(0, 16));
        topContainer.setOpaque(false);
        topContainer.add(headerPanel, BorderLayout.NORTH);
        topContainer.add(metricsGrid, BorderLayout.CENTER);
        add(topContainer, BorderLayout.NORTH);

        // 3. Recent Transactions Table Section
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 10));
        bottomPanel.setBackground(Theme.BG_CARD);
        bottomPanel.setBorder(Theme.createCardBorder());

        JLabel sectionTitle = new JLabel("Recent Financial Transactions Ledger");
        sectionTitle.setFont(Theme.FONT_HEADER);
        sectionTitle.setForeground(Theme.TEXT_MAIN);
        bottomPanel.add(sectionTitle, BorderLayout.NORTH);

        String[] cols = {"Txn Reference", "Student", "Bill ID", "Amount ($)", "Payment Method", "Status", "Date / Time"};
        recentTxnModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };

        ModernTable txnTable = new ModernTable(recentTxnModel);
        JScrollPane scrollPane = new JScrollPane(txnTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        bottomPanel.add(scrollPane, BorderLayout.CENTER);

        add(bottomPanel, BorderLayout.CENTER);

        refreshData();
    }

    public void refreshData() {
        List<Bill> bills = billDAO.findAll();
        BigDecimal totalBilled = BigDecimal.ZERO;
        BigDecimal totalCollected = BigDecimal.ZERO;
        BigDecimal totalOutstanding = BigDecimal.ZERO;

        for (Bill b : bills) {
            totalBilled = totalBilled.add(b.getFinalAmount());
            totalCollected = totalCollected.add(b.getPaidAmount());
            totalOutstanding = totalOutstanding.add(b.getBalanceDue());
        }

        int activeStudents = studentDAO.countActiveStudents();

        cardBilled.setValue("$" + String.format(java.util.Locale.US, "%,.2f", totalBilled));
        cardCollected.setValue("$" + String.format(java.util.Locale.US, "%,.2f", totalCollected));
        cardOutstanding.setValue("$" + String.format(java.util.Locale.US, "%,.2f", totalOutstanding));
        cardStudents.setValue(String.valueOf(activeStudents));

        // Refresh Recent Txns
        recentTxnModel.setRowCount(0);
        List<Payment> recentPayments = paymentDAO.findRecent(15);
        for (Payment p : recentPayments) {
            recentTxnModel.addRow(new Object[]{
                    p.getTransactionRef(),
                    p.getStudentName() != null ? p.getStudentName() : p.getStudentId(),
                    p.getBillId(),
                    "$" + String.format(java.util.Locale.US, "%,.2f", p.getAmount()),
                    p.getPaymentMethod().getDisplayName(),
                    p.getStatus(),
                    p.getPaymentDate() != null ? p.getPaymentDate().toString().replace("T", " ") : "Just now"
            });
        }
    }
}
