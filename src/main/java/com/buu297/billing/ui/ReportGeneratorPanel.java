package com.buu297.billing.ui;

import com.buu297.billing.config.DatabaseManager;
import com.buu297.billing.dao.BillDAO;
import com.buu297.billing.dao.ReportDAO;
import com.buu297.billing.model.Bill;
import com.buu297.billing.model.BillingReportSummary;
import com.buu297.billing.service.ReportGeneratorService;
import com.buu297.billing.ui.components.ModernTable;
import com.buu297.billing.ui.components.StatCard;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class ReportGeneratorPanel extends JPanel {
    private final ReportDAO reportDAO;
    private final BillDAO billDAO;
    private final ReportGeneratorService reportService;

    private final JComboBox<String> cbSemester;
    private final JComboBox<String> cbDepartment;
    private final JComboBox<String> cbStatus;

    private final StatCard cardBilled;
    private final StatCard cardCollected;
    private final StatCard cardOutstanding;
    private final StatCard cardRate;

    private final DefaultTableModel breakdownModel;
    private BillingReportSummary currentSummary;

    public ReportGeneratorPanel() {
        setLayout(new BorderLayout(16, 16));
        setBackground(Theme.BG_MAIN);
        setBorder(new EmptyBorder(20, 24, 20, 24));

        this.reportDAO = DatabaseManager.getInstance().getReportDAO();
        this.billDAO = DatabaseManager.getInstance().getBillDAO();
        this.reportService = new ReportGeneratorService();

        // 1. Header
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);

        JLabel title = new JLabel("Admin Billing Report Generator");
        title.setFont(Theme.FONT_TITLE);
        title.setForeground(Theme.TEXT_MAIN);

        JLabel subtitle = new JLabel("Aggregate financial analytics, collection rates, and accounting exports.");
        subtitle.setFont(Theme.FONT_SUBTITLE);
        subtitle.setForeground(Theme.TEXT_MUTED);

        JPanel titleBox = new JPanel(new GridLayout(2, 1, 0, 2));
        titleBox.setOpaque(false);
        titleBox.add(title);
        titleBox.add(subtitle);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setOpaque(false);

        JButton btnExportCSV = Theme.createPrimaryButton("Export to CSV");
        btnExportCSV.addActionListener(e -> exportCSV());

        JButton btnHTMLReport = Theme.createSecondaryButton("HTML Report / Print");
        btnHTMLReport.addActionListener(e -> previewHTMLReport());

        actions.add(btnHTMLReport);
        actions.add(btnExportCSV);

        headerPanel.add(titleBox, BorderLayout.WEST);
        headerPanel.add(actions, BorderLayout.EAST);

        // 2. Filter Bar
        JPanel filterCard = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        filterCard.setBackground(Theme.BG_CARD);
        filterCard.setBorder(Theme.createCardBorder());

        filterCard.add(new JLabel("Term:"));
        cbSemester = new JComboBox<>(new String[]{"ALL", "SEM-2026-FALL", "SEM-2026-SPRING", "SEM-2026-SUMMER"});
        filterCard.add(cbSemester);

        filterCard.add(new JLabel("Department:"));
        cbDepartment = new JComboBox<>(new String[]{"ALL", "Computer Science", "Information Systems", "Business Administration", "Electrical Engineering"});
        filterCard.add(cbDepartment);

        filterCard.add(new JLabel("Status:"));
        cbStatus = new JComboBox<>(new String[]{"ALL", "PAID", "PARTIAL", "UNPAID", "OVERDUE"});
        filterCard.add(cbStatus);

        JButton btnGenerate = Theme.createPrimaryButton("Generate Report");
        btnGenerate.addActionListener(e -> generateReport());
        filterCard.add(btnGenerate);

        // 3. Metrics Summary Grid
        JPanel metricsGrid = new JPanel(new GridLayout(1, 4, 16, 0));
        metricsGrid.setOpaque(false);
        metricsGrid.setPreferredSize(new Dimension(800, 100));

        cardBilled = new StatCard("Report Billed", "$0.00", "Gross amount", Theme.PRIMARY);
        cardCollected = new StatCard("Collected Revenue", "$0.00", "Realized receipts", Theme.SUCCESS);
        cardOutstanding = new StatCard("Unpaid Receivables", "$0.00", "Delinquent / Pending", Theme.DANGER);
        cardRate = new StatCard("Collection Rate", "0.0%", "Realized / Invoiced", Theme.INFO);

        metricsGrid.add(cardBilled);
        metricsGrid.add(cardCollected);
        metricsGrid.add(cardOutstanding);
        metricsGrid.add(cardRate);

        JPanel topContainer = new JPanel(new BorderLayout(0, 12));
        topContainer.setOpaque(false);
        topContainer.add(headerPanel, BorderLayout.NORTH);
        topContainer.add(filterCard, BorderLayout.CENTER);
        topContainer.add(metricsGrid, BorderLayout.SOUTH);
        add(topContainer, BorderLayout.NORTH);

        // 4. Breakdown Table
        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setBackground(Theme.BG_CARD);
        centerPanel.setBorder(Theme.createCardBorder());

        JLabel sectionTitle = new JLabel("Departmental Revenue Breakdown & Bill Status Distribution");
        sectionTitle.setFont(Theme.FONT_HEADER);
        sectionTitle.setForeground(Theme.TEXT_MAIN);
        centerPanel.add(sectionTitle, BorderLayout.NORTH);

        String[] cols = {"Department / Program", "Collected Tuition ($)", "Status Breakdown", "Share of Total"};
        breakdownModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        ModernTable breakdownTable = new ModernTable(breakdownModel);
        JScrollPane scroll = new JScrollPane(breakdownTable);
        scroll.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
        centerPanel.add(scroll, BorderLayout.CENTER);

        add(centerPanel, BorderLayout.CENTER);

        generateReport();
    }

    public void generateReport() {
        String sem = (String) cbSemester.getSelectedItem();
        String dept = (String) cbDepartment.getSelectedItem();
        String status = (String) cbStatus.getSelectedItem();

        currentSummary = reportService.generateSummary(sem, dept, status, null, null);

        cardBilled.setValue("$" + String.format(java.util.Locale.US, "%,.2f", currentSummary.getTotalBilledAmount()));
        cardCollected.setValue("$" + String.format(java.util.Locale.US, "%,.2f", currentSummary.getTotalCollectedAmount()));
        cardOutstanding.setValue("$" + String.format(java.util.Locale.US, "%,.2f", currentSummary.getTotalOutstandingAmount()));
        cardRate.setValue(String.format(java.util.Locale.US, "%.1f%%", currentSummary.getCollectionRatePct()));
        cardRate.setSubtitle(currentSummary.getCountPaid() + " Paid, " + currentSummary.getCountPartial() + " Partial, " + currentSummary.getCountOverdue() + " Overdue");

        breakdownModel.setRowCount(0);
        BigDecimal totalColl = currentSummary.getTotalCollectedAmount();

        for (Map.Entry<String, BigDecimal> entry : currentSummary.getRevenueByDepartment().entrySet()) {
            double share = 0.0;
            if (totalColl.compareTo(BigDecimal.ZERO) > 0) {
                share = entry.getValue().divide(totalColl, 4, java.math.RoundingMode.HALF_UP).doubleValue() * 100.0;
            }

            breakdownModel.addRow(new Object[]{
                    entry.getKey(),
                    "$" + String.format(java.util.Locale.US, "%,.2f", entry.getValue()),
                    "Active Invoices: " + currentSummary.getTotalBillsCount(),
                    String.format(java.util.Locale.US, "%.1f%%", share)
            });
        }
    }

    private void exportCSV() {
        String sem = (String) cbSemester.getSelectedItem();
        String dept = (String) cbDepartment.getSelectedItem();
        String statusStr = (String) cbStatus.getSelectedItem();
        com.buu297.billing.model.BillingStatus status = "ALL".equalsIgnoreCase(statusStr) ? null : com.buu297.billing.model.BillingStatus.valueOf(statusStr);

        List<Bill> bills = billDAO.filterBills(sem, dept, status, null);
        String csv = reportService.exportBillsToCSV(bills);

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("student_billing_report_" + System.currentTimeMillis() + ".csv"));
        int res = fileChooser.showSaveDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            try {
                reportService.saveCSVToFile(csv, fileChooser.getSelectedFile());
                JOptionPane.showMessageDialog(this, "CSV Report successfully exported to:\n" + fileChooser.getSelectedFile().getAbsolutePath(),
                        "Export Complete", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error saving CSV: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void previewHTMLReport() {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='utf-8'><title>Admin Billing Report</title>");
        html.append("<style>body{font-family:'Segoe UI',Arial,sans-serif;margin:30px;color:#1e293b;}");
        html.append("h1{color:#1e3a8a;margin-bottom:4px;} table{width:100%;border-collapse:collapse;margin-top:20px;}");
        html.append("th,td{border:1px solid #cbd5e1;padding:8px 12px;text-align:left;font-size:13px;}");
        html.append("th{background:#f1f5f9;font-weight:bold;} .metric-box{display:flex;gap:20px;margin-top:20px;}");
        html.append(".card{background:#f8fafc;padding:15px;border:1px solid #e2e8f0;border-radius:6px;flex:1;}");
        html.append(".val{font-size:20px;font-weight:bold;color:#2563eb;margin-top:5px;}</style></head><body>");

        html.append("<h1>STUDENT BILLING SUMMARY REPORT</h1>");
        html.append("<p><strong>Generated:</strong> ").append(java.time.LocalDateTime.now().toString()).append("</p>");

        html.append("<div class='metric-box'>");
        html.append("<div class='card'><div>Total Billed</div><div class='val'>$").append(currentSummary.getTotalBilledAmount()).append("</div></div>");
        html.append("<div class='card'><div>Total Collected</div><div class='val' style='color:#15803d;'>$").append(currentSummary.getTotalCollectedAmount()).append("</div></div>");
        html.append("<div class='card'><div>Outstanding Balance</div><div class='val' style='color:#b91c1c;'>$").append(currentSummary.getTotalOutstandingAmount()).append("</div></div>");
        html.append("<div class='card'><div>Collection Rate</div><div class='val' style='color:#4f46e5;'>").append(String.format(java.util.Locale.US, "%.1f%%", currentSummary.getCollectionRatePct())).append("</div></div>");
        html.append("</div>");

        html.append("<h3>Bills & Status Distribution</h3>");
        html.append("<p>Paid: <strong>").append(currentSummary.getCountPaid()).append("</strong> | Partial: <strong>").append(currentSummary.getCountPartial()).append("</strong> | Unpaid: <strong>").append(currentSummary.getCountUnpaid()).append("</strong> | Overdue: <strong>").append(currentSummary.getCountOverdue()).append("</strong></p>");

        html.append("<h3>Departmental Revenue Ledger</h3>");
        html.append("<table><thead><tr><th>Department</th><th>Tuition Collected ($)</th></tr></thead><tbody>");
        for (Map.Entry<String, BigDecimal> e : currentSummary.getRevenueByDepartment().entrySet()) {
            html.append("<tr><td>").append(e.getKey()).append("</td><td>$").append(e.getValue()).append("</td></tr>");
        }
        html.append("</tbody></table></body></html>");

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Billing Summary Report", true);
        dialog.setSize(750, 600);
        dialog.setLocationRelativeTo(this);
        dialog.setLayout(new BorderLayout());

        JEditorPane pane = new JEditorPane("text/html", html.toString());
        pane.setEditable(false);
        pane.setCaretPosition(0);

        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));
        JButton btnPrint = Theme.createPrimaryButton("Print Report");
        btnPrint.addActionListener(e -> {
            try { pane.print(); } catch (Exception ex) { JOptionPane.showMessageDialog(dialog, ex.getMessage()); }
        });
        JButton btnClose = Theme.createSecondaryButton("Close");
        btnClose.addActionListener(e -> dialog.dispose());
        bottom.add(btnPrint);
        bottom.add(btnClose);

        dialog.add(new JScrollPane(pane), BorderLayout.CENTER);
        dialog.add(bottom, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }
}
