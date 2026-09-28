package com.buu297.billing.ui;

import com.buu297.billing.config.DatabaseManager;
import com.buu297.billing.dao.BillDAO;
import com.buu297.billing.dao.StudentDAO;
import com.buu297.billing.model.*;
import com.buu297.billing.service.BillingService;
import com.buu297.billing.ui.components.ModernTable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BillingManagementPanel extends JPanel {
    private final BillDAO billDAO;
    private final StudentDAO studentDAO;
    private final BillingService billingService;

    private final JComboBox<String> cbSemester;
    private final JComboBox<String> cbDepartment;
    private final JComboBox<String> cbStatus;
    private final JTextField tfSearch;

    private final DefaultTableModel tableModel;
    private final ModernTable billTable;

    public BillingManagementPanel() {
        setLayout(new BorderLayout(16, 16));
        setBackground(Theme.BG_MAIN);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        this.billDAO = DatabaseManager.getInstance().getBillDAO();
        this.studentDAO = DatabaseManager.getInstance().getStudentDAO();
        this.billingService = new BillingService();

        // 1. Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel title = new JLabel("Tuition & Billing Management");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_MAIN);

        JLabel subtitle = new JLabel("Issue tuition invoices, track line items, and adjust billing accounts.");
        subtitle.setFont(Theme.FONT_SUBTITLE);
        subtitle.setForeground(Theme.TEXT_MUTED);

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBox.setOpaque(false);
        titleBox.add(title);
        titleBox.add(subtitle);

        JPanel topActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        topActions.setOpaque(false);

        JButton btnCreateBill = Theme.createPrimaryButton("+ Create Bill");
        btnCreateBill.addActionListener(e -> showCreateBillDialog());

        JButton btnViewItems = Theme.createSecondaryButton("View Itemized Breakdown");
        btnViewItems.addActionListener(e -> showSelectedBillItems());

        topActions.add(btnViewItems);
        topActions.add(btnCreateBill);

        headerPanel.add(titleBox, BorderLayout.WEST);
        headerPanel.add(topActions, BorderLayout.EAST);

        // 2. Filter Bar
        JPanel filterCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        filterCard.setBackground(Theme.BG_CARD);
        filterCard.setBorder(Theme.createCardBorder());

        filterCard.add(new JLabel("Semester:"));
        cbSemester = new JComboBox<>(new String[]{"ALL", "SEM-2026-FALL", "SEM-2026-SPRING", "SEM-2026-SUMMER"});
        filterCard.add(cbSemester);

        filterCard.add(new JLabel("Department:"));
        cbDepartment = new JComboBox<>(new String[]{"ALL", "Computer Science", "Information Systems", "Business Administration", "Electrical Engineering"});
        filterCard.add(cbDepartment);

        filterCard.add(new JLabel("Status:"));
        cbStatus = new JComboBox<>(new String[]{"ALL", "UNPAID", "PARTIAL", "PAID", "OVERDUE"});
        filterCard.add(cbStatus);

        filterCard.add(new JLabel("Search:"));
        tfSearch = new JTextField(15);
        tfSearch.putClientProperty("JTextField.placeholderText", "Student ID, Name, Inv #");
        filterCard.add(tfSearch);

        JButton btnSearch = Theme.createPrimaryButton("Filter");
        btnSearch.addActionListener(e -> loadFilteredData());
        filterCard.add(btnSearch);

        JButton btnReset = Theme.createSecondaryButton("Reset");
        btnReset.addActionListener(e -> {
            cbSemester.setSelectedIndex(0);
            cbDepartment.setSelectedIndex(0);
            cbStatus.setSelectedIndex(0);
            tfSearch.setText("");
            loadFilteredData();
        });
        filterCard.add(btnReset);

        JPanel topContainer = new JPanel(new BorderLayout(0, 12));
        topContainer.setOpaque(false);
        topContainer.add(headerPanel, BorderLayout.NORTH);
        topContainer.add(filterCard, BorderLayout.CENTER);
        add(topContainer, BorderLayout.NORTH);

        // 3. Table
        String[] cols = {"Bill ID", "Invoice #", "Student ID", "Student Name", "Department", "Due Date", "Gross ($)", "Paid ($)", "Balance Due ($)", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
            @Override
            public Class<?> getColumnClass(int col) {
                if (col == 9) return BillingStatus.class;
                return Object.class;
            }
        };

        billTable = new ModernTable(tableModel);
        JScrollPane scrollPane = new JScrollPane(billTable);
        scrollPane.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        add(scrollPane, BorderLayout.CENTER);

        loadFilteredData();
    }

    public void loadFilteredData() {
        String sem = (String) cbSemester.getSelectedItem();
        String dept = (String) cbDepartment.getSelectedItem();
        String statusStr = (String) cbStatus.getSelectedItem();
        BillingStatus status = "ALL".equalsIgnoreCase(statusStr) ? null : BillingStatus.valueOf(statusStr);
        String q = tfSearch.getText();

        List<Bill> list = billDAO.filterBills(sem, dept, status, q);
        tableModel.setRowCount(0);

        for (Bill b : list) {
            tableModel.addRow(new Object[]{
                    b.getBillId(),
                    b.getInvoiceNumber(),
                    b.getStudentId(),
                    b.getStudentName() != null ? b.getStudentName() : "",
                    b.getDepartment() != null ? b.getDepartment() : "",
                    b.getDueDate() != null ? b.getDueDate().toString() : "N/A",
                    "$" + String.format(java.util.Locale.US, "%,.2f", b.getFinalAmount()),
                    "$" + String.format(java.util.Locale.US, "%,.2f", b.getPaidAmount()),
                    "$" + String.format(java.util.Locale.US, "%,.2f", b.getBalanceDue()),
                    b.getStatus()
            });
        }
    }

    private void showCreateBillDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Create New Tuition Bill", true);
        dialog.setSize(550, 480);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(15, 15));

        JPanel form = new JPanel(new GridLayout(6, 2, 10, 12));
        form.setBorder(new EmptyBorder(20, 20, 10, 20));

        List<Student> students = studentDAO.findAll();
        JComboBox<Student> studentCombo = new JComboBox<>(students.toArray(new Student[0]));

        JComboBox<String> semCombo = new JComboBox<>(new String[]{"SEM-2026-FALL", "SEM-2026-SPRING", "SEM-2026-SUMMER"});
        JTextField tfTuition = new JTextField("1800.00");
        JTextField tfLabFee = new JTextField("300.00");
        JTextField tfDiscount = new JTextField("0.00");
        JTextField tfDueDate = new JTextField(LocalDate.now().plusDays(30).toString());

        form.add(new JLabel("Select Student:"));
        form.add(studentCombo);
        form.add(new JLabel("Academic Term:"));
        form.add(semCombo);
        form.add(new JLabel("Tuition Fee ($):"));
        form.add(tfTuition);
        form.add(new JLabel("Laboratory Fee ($):"));
        form.add(tfLabFee);
        form.add(new JLabel("Scholarship / Waiver ($):"));
        form.add(tfDiscount);
        form.add(new JLabel("Payment Due Date (YYYY-MM-DD):"));
        form.add(tfDueDate);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 15));
        JButton btnSubmit = Theme.createPrimaryButton("Create Invoice");
        JButton btnCancel = Theme.createSecondaryButton("Cancel");
        btnCancel.addActionListener(e -> dialog.dispose());

        btnSubmit.addActionListener(e -> {
            try {
                Student selStudent = (Student) studentCombo.getSelectedItem();
                String sem = (String) semCombo.getSelectedItem();
                BigDecimal tuition = new BigDecimal(tfTuition.getText().trim());
                BigDecimal lab = new BigDecimal(tfLabFee.getText().trim());
                BigDecimal discount = new BigDecimal(tfDiscount.getText().trim());
                LocalDate dueDate = LocalDate.parse(tfDueDate.getText().trim());

                List<BillItem> items = new ArrayList<>();
                items.add(new BillItem("", "", ItemType.TUITION_COURSE, "Semester Course Registration (12 credits)", tuition));
                if (lab.compareTo(BigDecimal.ZERO) > 0) {
                    items.add(new BillItem("", "", ItemType.LAB_FEE, "Department Lab & Cloud Usage", lab));
                }
                if (discount.compareTo(BigDecimal.ZERO) > 0) {
                    items.add(new BillItem("", "", ItemType.SCHOLARSHIP_DISCOUNT, "Tuition Grant / Financial Aid", discount.negate()));
                }

                billingService.createNewBill(selStudent.getStudentId(), sem, dueDate, items);
                JOptionPane.showMessageDialog(dialog, "Tuition Bill successfully generated!", "Success", JOptionPane.INFORMATION_MESSAGE);
                dialog.dispose();
                loadFilteredData();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Error creating bill: " + ex.getMessage(), "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnSubmit);

        dialog.add(form, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void showSelectedBillItems() {
        int row = billTable.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Please select a bill from the table first.", "Info", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String billId = (String) tableModel.getValueAt(row, 0);
        Bill bill = billDAO.findById(billId).orElse(null);
        if (bill == null) return;

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Invoice Breakdown: " + bill.getInvoiceNumber(), true);
        dialog.setSize(600, 420);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout(10, 10));

        JPanel info = new JPanel(new GridLayout(3, 2, 10, 6));
        info.setBorder(new EmptyBorder(15, 15, 5, 15));
        info.add(new JLabel("Student: " + bill.getStudentName() + " (" + bill.getStudentId() + ")"));
        info.add(new JLabel("Term: " + bill.getSemesterName()));
        info.add(new JLabel("Net Payable: $" + bill.getFinalAmount()));
        info.add(new JLabel("Amount Paid: $" + bill.getPaidAmount()));
        info.add(new JLabel("Balance Remaining: $" + bill.getBalanceDue()));
        info.add(new JLabel("Status: " + bill.getStatus().getDisplayName()));

        String[] itemCols = {"Category", "Description", "Amount ($)"};
        DefaultTableModel itemModel = new DefaultTableModel(itemCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        for (BillItem item : bill.getItems()) {
            itemModel.addRow(new Object[]{
                    item.getItemType().getDisplayName(),
                    item.getDescription(),
                    "$" + item.getAmount()
            });
        }

        ModernTable itemTable = new ModernTable(itemModel);
        JScrollPane scroll = new JScrollPane(itemTable);

        JPanel closePanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        JButton btnClose = Theme.createSecondaryButton("Close");
        btnClose.addActionListener(e -> dialog.dispose());
        closePanel.add(btnClose);

        dialog.add(info, BorderLayout.NORTH);
        dialog.add(scroll, BorderLayout.CENTER);
        dialog.add(closePanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
}
