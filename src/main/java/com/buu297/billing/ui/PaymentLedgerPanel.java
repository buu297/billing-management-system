package com.buu297.billing.ui;

import com.buu297.billing.config.DatabaseManager;
import com.buu297.billing.dao.BillDAO;
import com.buu297.billing.dao.PaymentDAO;
import com.buu297.billing.dao.StudentDAO;
import com.buu297.billing.model.*;
import com.buu297.billing.service.ConcurrencyPaymentService;
import com.buu297.billing.service.ReportGeneratorService;
import com.buu297.billing.ui.components.ModernTable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.util.List;

public class PaymentLedgerPanel extends JPanel {
    private final PaymentDAO paymentDAO;
    private final BillDAO billDAO;
    private final StudentDAO studentDAO;
    private final ConcurrencyPaymentService paymentService;
    private final ReportGeneratorService reportService;

    private final DefaultTableModel tableModel;
    private final ModernTable payTable;

    public PaymentLedgerPanel() {
        setLayout(new BorderLayout(16, 16));
        setBackground(Theme.BG_MAIN);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        this.paymentDAO = DatabaseManager.getInstance().getPaymentDAO();
        this.billDAO = DatabaseManager.getInstance().getBillDAO();
        this.studentDAO = DatabaseManager.getInstance().getStudentDAO();
        this.paymentService = new ConcurrencyPaymentService();
        this.reportService = new ReportGeneratorService();

        // 1. Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel title = new JLabel("Payment Transactions & Audit Ledger");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_MAIN);

        JLabel subtitle = new JLabel("Record counter payments, verify transaction proofs, and print official bursar receipts.");
        subtitle.setFont(Theme.FONT_SUBTITLE);
        subtitle.setForeground(Theme.TEXT_MUTED);

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBox.setOpaque(false);
        titleBox.add(title);
        titleBox.add(subtitle);

        JPanel actionBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionBox.setOpaque(false);

        JButton btnRecordPay = Theme.createPrimaryButton("+ Record Counter Payment");
        btnRecordPay.addActionListener(e -> showRecordPaymentDialog());

        JButton btnViewReceipt = Theme.createSecondaryButton("Print Official Receipt");
        btnViewReceipt.addActionListener(e -> showReceiptDialog());

        actionBox.add(btnViewReceipt);
        actionBox.add(btnRecordPay);

        headerPanel.add(titleBox, BorderLayout.WEST);
        headerPanel.add(actionBox, BorderLayout.EAST);
        add(headerPanel, BorderLayout.NORTH);

        // 2. Table
        String[] cols = {"Txn Reference", "Payment ID", "Bill ID", "Student ID", "Student Name", "Amount ($)", "Method", "Status", "Date / Time"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        payTable = new ModernTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(payTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        add(scrollPane, BorderLayout.CENTER);

        loadData();
    }

    public void loadData() {
        tableModel.setRowCount(0);
        List<Payment> list = paymentDAO.findAll();
        for (Payment p : list) {
            tableModel.addRow(new Object[]{
                    p.getTransactionRef(),
                    p.getPaymentId(),
                    p.getBillId(),
                    p.getStudentId(),
                    p.getStudentName() != null ? p.getStudentName() : "",
                    "$" + String.format(java.util.Locale.US, "%,.2f", p.getAmount()),
                    p.getPaymentMethod().getDisplayName(),
                    p.getStatus(),
                    p.getPaymentDate() != null ? p.getPaymentDate().toString().replace("T", " ") : ""
            });
        }
    }

    private void showRecordPaymentDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Record In-Person / Counter Tuition Payment", true);
        dialog.setSize(520, 380);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(15, 15));

        JPanel form = new JPanel(new GridLayout(5, 2, 10, 12));
        form.setBorder(new EmptyBorder(20, 20, 10, 20));

        List<Bill> unsettledBills = billDAO.filterBills(null, null, null, null);
        // filter out paid
        List<Bill> payable = new java.util.ArrayList<>();
        for (Bill b : unsettledBills) {
            if (b.getStatus() != BillingStatus.PAID && b.getBalanceDue().compareTo(BigDecimal.ZERO) > 0) {
                payable.add(b);
            }
        }

        JComboBox<Bill> billCombo = new JComboBox<>(payable.toArray(new Bill[0]));
        billCombo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof Bill) {
                    Bill b = (Bill) value;
                    setText(b.getInvoiceNumber() + " - " + b.getStudentName() + " (Due: $" + b.getBalanceDue() + ")");
                }
                return this;
            }
        });

        JTextField tfAmount = new JTextField();
        if (!payable.isEmpty()) {
            tfAmount.setText(payable.get(0).getBalanceDue().toString());
        }

        billCombo.addActionListener(e -> {
            Bill b = (Bill) billCombo.getSelectedItem();
            if (b != null) {
                tfAmount.setText(b.getBalanceDue().toString());
            }
        });

        JComboBox<PaymentMethod> methodCombo = new JComboBox<>(new PaymentMethod[]{
                PaymentMethod.CASH,
                PaymentMethod.CREDIT_CARD,
                PaymentMethod.BANK_TRANSFER,
                PaymentMethod.VNPAY_QR,
                PaymentMethod.FINANCIAL_AID
        });

        JTextField tfNotes = new JTextField("Bursar counter transaction");

        form.add(new JLabel("Select Bill to Pay:"));
        form.add(billCombo);
        form.add(new JLabel("Payment Amount ($):"));
        form.add(tfAmount);
        form.add(new JLabel("Payment Method:"));
        form.add(methodCombo);
        form.add(new JLabel("Transaction Memo / Notes:"));
        form.add(tfNotes);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 15));
        JButton btnSubmit = Theme.createPrimaryButton("Process Payment");
        JButton btnCancel = Theme.createSecondaryButton("Cancel");
        btnCancel.addActionListener(e -> dialog.dispose());

        btnSubmit.addActionListener(e -> {
            Bill selectedBill = (Bill) billCombo.getSelectedItem();
            if (selectedBill == null) {
                JOptionPane.showMessageDialog(dialog, "No unpaid bill selected.", "Error", JOptionPane.WARNING_MESSAGE);
                return;
            }

            try {
                BigDecimal amt = new BigDecimal(tfAmount.getText().trim());
                PaymentMethod method = (PaymentMethod) methodCombo.getSelectedItem();
                String notes = tfNotes.getText().trim();

                TransactionResult res = paymentService.processPayment(
                        selectedBill.getBillId(), selectedBill.getStudentId(), amt, method, notes
                );

                if (res.isSuccess()) {
                    JOptionPane.showMessageDialog(dialog,
                            "Payment Successfully Processed!\n\n" +
                            "Ref: " + res.getTransactionRef() + "\n" +
                            "Amount Paid: $" + res.getPaidAmount() + "\n" +
                            "Remaining Balance: $" + res.getRemainingBalance() + "\n" +
                            "Status: " + res.getUpdatedStatus().getDisplayName() + "\n" +
                            "ACID Duration: " + res.getExecutionDurationMs() + "ms",
                            "Transaction Approved", JOptionPane.INFORMATION_MESSAGE);
                    dialog.dispose();
                    loadData();
                } else {
                    JOptionPane.showMessageDialog(dialog,
                            "Payment Declined: " + res.getMessage(),
                            "Transaction Failed", JOptionPane.ERROR_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Invalid amount: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnSubmit);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void showReceiptDialog() {
        int row = payTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a payment transaction from the table first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String billId = (String) tableModel.getValueAt(row, 2);
        Bill bill = billDAO.findById(billId).orElse(null);
        if (bill == null) return;

        List<Payment> billPayments = paymentDAO.findByBillId(billId);
        String html = reportService.generateHTMLInvoice(bill, billPayments);

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Official Bursar Receipt: " + bill.getInvoiceNumber(), true);
        dialog.setSize(750, 650);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JEditorPane editorPane = new JEditorPane();
        editorPane.setEditable(false);
        editorPane.setContentType("text/html");
        editorPane.setText(html);
        editorPane.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(editorPane);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        JButton btnPrint = Theme.createPrimaryButton("Print Document");
        btnPrint.addActionListener(e -> {
            try {
                editorPane.print();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Printing error: " + ex.getMessage());
            }
        });

        JButton btnClose = Theme.createSecondaryButton("Close");
        btnClose.addActionListener(e -> dialog.dispose());

        bottom.add(btnPrint);
        bottom.add(btnClose);

        dialog.add(scroll, BorderLayout.CENTER);
        dialog.add(bottom, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
}
