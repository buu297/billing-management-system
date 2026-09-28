package com.buu297.billing.ui;

import com.buu297.billing.config.DatabaseManager;
import com.buu297.billing.dao.BillDAO;
import com.buu297.billing.model.Bill;
import com.buu297.billing.model.BillingStatus;
import com.buu297.billing.model.TransactionResult;
import com.buu297.billing.service.ConcurrencyPaymentService;
import com.buu297.billing.ui.components.ModernTable;
import com.buu297.billing.ui.components.StatCard;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

public class ConcurrencySimulatorPanel extends JPanel {
    private final BillDAO billDAO;
    private final ConcurrencyPaymentService concurrencyService;

    private final JComboBox<Bill> cbBills;
    private final JComboBox<Integer> cbThreads;
    private final JTextField tfAmountPerThread;

    private final StatCard cardSuccessful;
    private final StatCard cardRejected;
    private final StatCard cardConsistency;
    private final StatCard cardTotalDuration;

    private final DefaultTableModel threadLogModel;
    private final JButton btnRunTest;
    private final JLabel lblInvariantStatus;

    public ConcurrencySimulatorPanel() {
        setLayout(new BorderLayout(16, 16));
        setBackground(Theme.BG_MAIN);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        this.billDAO = DatabaseManager.getInstance().getBillDAO();
        this.concurrencyService = new ConcurrencyPaymentService();

        // 1. Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel title = new JLabel("⚡ Concurrency & ACID Transaction Stress Lab");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_MAIN);

        JLabel subtitle = new JLabel("Pessimistic Row-Level Locking (SELECT ... FOR UPDATE) Benchmark & Race Condition Protection.");
        subtitle.setFont(Theme.FONT_SUBTITLE);
        subtitle.setForeground(Theme.TEXT_MUTED);

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBox.setOpaque(false);
        titleBox.add(title);
        titleBox.add(subtitle);

        headerPanel.add(titleBox, BorderLayout.WEST);

        // 2. Control Bar
        JPanel controlCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 10));
        controlCard.setBackground(Theme.BG_CARD);
        controlCard.setBorder(Theme.createCardBorder());

        controlCard.add(new JLabel("Target Bill:"));
        List<Bill> allBills = billDAO.findAll();
        cbBills = new JComboBox<>(allBills.toArray(new Bill[0]));
        cbBills.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Bill) {
                    Bill b = (Bill) value;
                    setText(b.getInvoiceNumber() + " - " + b.getStudentName() + " (Bal: $" + b.getBalanceDue() + ")");
                }
                return this;
            }
        });
        controlCard.add(cbBills);

        controlCard.add(new JLabel("Concurrent Threads:"));
        cbThreads = new JComboBox<>(new Integer[]{5, 8, 10, 15, 20});
        controlCard.add(cbThreads);

        controlCard.add(new JLabel("Payment / Thread ($):"));
        tfAmountPerThread = new JTextField("500.00", 7);
        controlCard.add(tfAmountPerThread);

        btnRunTest = Theme.createPrimaryButton("⚡ Launch Multi-Thread Stress Test");
        btnRunTest.addActionListener(e -> runSimulation());
        controlCard.add(btnRunTest);

        // 3. Metric KPI Cards
        JPanel metricsGrid = new JPanel(new GridLayout(1, 4, 16, 0));
        metricsGrid.setOpaque(false);
        metricsGrid.setPreferredSize(new Dimension(800, 100));

        cardSuccessful = new StatCard("Committed Txns", "0", "ACID locks verified", Theme.SUCCESS);
        cardRejected = new StatCard("Prevented Overpayments", "0", "Exceeded balance guard", Theme.WARNING);
        cardConsistency = new StatCard("Data Integrity", "AWAITING", "Zero balance drift", Theme.PRIMARY);
        cardTotalDuration = new StatCard("Execution Latency", "0 ms", "Parallel threads", Theme.INFO);

        metricsGrid.add(cardSuccessful);
        metricsGrid.add(cardRejected);
        metricsGrid.add(cardConsistency);
        metricsGrid.add(cardTotalDuration);

        JPanel topContainer = new JPanel(new BorderLayout(0, 12));
        topContainer.setOpaque(false);
        topContainer.add(headerPanel, BorderLayout.NORTH);
        topContainer.add(controlCard, BorderLayout.CENTER);
        topContainer.add(metricsGrid, BorderLayout.SOUTH);
        add(topContainer, BorderLayout.NORTH);

        // 4. Center Log Table & Invariant Bar
        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setBackground(Theme.BG_CARD);
        centerPanel.setBorder(Theme.createCardBorder());

        lblInvariantStatus = new JLabel("Ready: Select a bill and thread count to initiate concurrency validation.");
        lblInvariantStatus.setFont(Theme.FONT_BODY_BOLD);
        lblInvariantStatus.setForeground(Theme.TEXT_MUTED);
        centerPanel.add(lblInvariantStatus, BorderLayout.NORTH);

        String[] cols = {"Thread #", "Txn Reference", "Payment Amount", "Previous Bal", "New Bal", "Result Status", "Lock Duration", "Message"};
        threadLogModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        ModernTable logTable = new ModernTable(threadLogModel);
        JScrollPane scroll = new JScrollPane(logTable);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        centerPanel.add(scroll, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);
    }

    private void runSimulation() {
        Bill selectedBill = (Bill) cbBills.getSelectedItem();
        if (selectedBill == null) {
            JOptionPane.showMessageDialog(this, "Please select a bill to test.");
            return;
        }

        // Refresh bill from DB
        Bill freshBill = billDAO.findById(selectedBill.getBillId()).orElse(selectedBill);
        if (freshBill.getBalanceDue().compareTo(BigDecimal.ZERO) <= 0) {
            JOptionPane.showMessageDialog(this, "The selected bill already has a $0.00 balance. Please choose an unpaid bill.");
            return;
        }

        int threads = (Integer) cbThreads.getSelectedItem();
        BigDecimal amt;
        try {
            amt = new BigDecimal(tfAmountPerThread.getText().trim());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Invalid payment amount.");
            return;
        }

        btnRunTest.setEnabled(false);
        lblInvariantStatus.setText("Running multi-threaded stress test with " + threads + " concurrent workers...");
        threadLogModel.setRowCount(0);

        SwingWorker<ConcurrencyPaymentService.ConcurrencySimulationReport, Void> worker =
                new SwingWorker<ConcurrencyPaymentService.ConcurrencySimulationReport, Void>() {
                    @Override
                    protected ConcurrencyPaymentService.ConcurrencySimulationReport doInBackground() {
                        return concurrencyService.simulateConcurrentPayments(
                                freshBill.getBillId(), freshBill.getStudentId(), amt, threads
                        );
                    }

                    @Override
                    protected void done() {
                        btnRunTest.setEnabled(true);
                        try {
                            ConcurrencyPaymentService.ConcurrencySimulationReport rep = get();
                            displayResults(rep);
                        } catch (Exception e) {
                            lblInvariantStatus.setText("Error during simulation: " + e.getMessage());
                        }
                    }
                };
        worker.execute();
    }

    private void displayResults(ConcurrencyPaymentService.ConcurrencySimulationReport rep) {
        cardSuccessful.setValue(rep.successfulTxns + " / " + rep.threadCount);
        cardRejected.setValue(rep.rejectedTxns + " blocked");
        cardTotalDuration.setValue(rep.totalDurationMs + " ms");

        boolean consistent = rep.isDataConsistent();
        if (consistent) {
            cardConsistency.setValue("100% ACID");
            cardConsistency.setSubtitle("Mathematically consistent");
            lblInvariantStatus.setText(String.format(
                    "✔ VERIFIED: Initial Balance: $%s -> Total Paid: $%s -> Remaining Balance: $%s. [0 Race Conditions, 0 Lost Updates]",
                    rep.initialBalance, rep.totalPaidInSim, rep.finalBalance
            ));
            lblInvariantStatus.setForeground(Theme.SUCCESS);
        } else {
            cardConsistency.setValue("INCONSISTENT");
            lblInvariantStatus.setText("❌ Anomaly detected in concurrent ledger calculations.");
            lblInvariantStatus.setForeground(Theme.DANGER);
        }

        int idx = 1;
        for (TransactionResult res : rep.results) {
            threadLogModel.addRow(new Object[]{
                    "Worker #" + idx++,
                    res.getTransactionRef() != null ? res.getTransactionRef() : "N/A",
                    "$" + rep.paymentPerThread,
                    "$" + res.getPreviousBalance(),
                    "$" + res.getRemainingBalance(),
                    res.isSuccess() ? "COMMITTED" : "REJECTED (" + res.getStatusCode() + ")",
                    res.getExecutionDurationMs() + "ms",
                    res.getMessage()
            });
        }
    }
}
