package com.buu297.billing.service;

import com.buu297.billing.config.DatabaseManager;
import com.buu297.billing.dao.BillDAO;
import com.buu297.billing.dao.ReportDAO;
import com.buu297.billing.model.Bill;
import com.buu297.billing.model.BillItem;
import com.buu297.billing.model.BillingReportSummary;
import com.buu297.billing.model.Payment;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ReportGeneratorService {
    private final ReportDAO reportDAO;
    private final BillDAO billDAO;

    public ReportGeneratorService() {
        this.reportDAO = DatabaseManager.getInstance().getReportDAO();
        this.billDAO = DatabaseManager.getInstance().getBillDAO();
    }

    public BillingReportSummary generateSummary(String semesterId, String department, String status, LocalDate fromDate, LocalDate toDate) {
        return reportDAO.generateSummary(semesterId, department, status, fromDate, toDate);
    }

    public String exportBillsToCSV(List<Bill> bills) {
        StringBuilder sb = new StringBuilder();
        sb.append("Invoice Number,Student ID,Student Name,Department,Semester,Total ($),Discount ($),Final ($),Paid ($),Balance Due ($),Due Date,Status\n");

        for (Bill b : bills) {
            sb.append(escapeCsv(b.getInvoiceNumber())).append(",")
              .append(escapeCsv(b.getStudentId())).append(",")
              .append(escapeCsv(b.getStudentName() != null ? b.getStudentName() : "")).append(",")
              .append(escapeCsv(b.getDepartment() != null ? b.getDepartment() : "")).append(",")
              .append(escapeCsv(b.getSemesterId())).append(",")
              .append(b.getTotalAmount()).append(",")
              .append(b.getDiscountAmount()).append(",")
              .append(b.getFinalAmount()).append(",")
              .append(b.getPaidAmount()).append(",")
              .append(b.getBalanceDue()).append(",")
              .append(b.getDueDate() != null ? b.getDueDate().toString() : "").append(",")
              .append(b.getStatus().name()).append("\n");
        }
        return sb.toString();
    }

    public void saveCSVToFile(String csvContent, File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(csvContent);
        }
    }

    public String generateHTMLInvoice(Bill bill, List<Payment> paymentHistory) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html>\n<html>\n<head>\n<meta charset='utf-8'>\n");
        html.append("<title>Invoice ").append(bill.getInvoiceNumber()).append("</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: 'Segoe UI', Tahoma, sans-serif; margin: 40px; color: #1e293b; background: #fff; }\n");
        html.append(".header { display: flex; justify-content: space-between; border-bottom: 2px solid #2563eb; padding-bottom: 20px; margin-bottom: 25px; }\n");
        html.append(".brand h1 { margin: 0; color: #1e3a8a; font-size: 24px; }\n");
        html.append(".brand p { margin: 4px 0 0 0; color: #64748b; font-size: 13px; }\n");
        html.append(".inv-meta { text-align: right; font-size: 14px; }\n");
        html.append(".inv-title { font-size: 22px; font-weight: bold; color: #2563eb; }\n");
        html.append(".grid { display: grid; grid-template-columns: 1fr 1fr; gap: 20px; margin-bottom: 25px; font-size: 14px; }\n");
        html.append(".box { background: #f8fafc; padding: 15px; border-radius: 8px; border: 1px solid #e2e8f0; }\n");
        html.append("table { width: 100%; border-collapse: collapse; margin-bottom: 25px; }\n");
        html.append("th { background: #f1f5f9; padding: 10px; text-align: left; font-size: 13px; border-bottom: 2px solid #cbd5e1; }\n");
        html.append("td { padding: 10px; border-bottom: 1px solid #e2e8f0; font-size: 13px; }\n");
        html.append(".total-box { margin-left: auto; width: 300px; font-size: 14px; line-height: 1.8; }\n");
        html.append(".total-row { display: flex; justify-content: space-between; }\n");
        html.append(".grand-total { font-weight: bold; font-size: 16px; border-top: 2px solid #cbd5e1; padding-top: 8px; margin-top: 8px; }\n");
        html.append(".badge { display: inline-block; padding: 4px 10px; border-radius: 9999px; font-weight: bold; font-size: 12px; }\n");
        html.append(".badge-PAID { background: #dcfce7; color: #15803d; }\n");
        html.append(".badge-PARTIAL { background: #fef3c7; color: #b45309; }\n");
        html.append(".badge-UNPAID { background: #fee2e2; color: #b91c1c; }\n");
        html.append(".badge-OVERDUE { background: #ffe4e6; color: #e11d48; }\n");
        html.append(".footer { margin-top: 50px; text-align: center; color: #94a3b8; font-size: 12px; border-top: 1px solid #e2e8f0; padding-top: 15px; }\n");
        html.append("</style>\n</head>\n<body>\n");

        html.append("<div class='header'>\n");
        html.append("  <div class='brand'>\n");
        html.append("    <h1>STUDENT BILLING MANAGEMENT SYSTEM</h1>\n");
        html.append("    <p>University Financial Services & Bursar Division</p>\n");
        html.append("  </div>\n");
        html.append("  <div class='inv-meta'>\n");
        html.append("    <div class='inv-title'>INVOICE</div>\n");
        html.append("    <div><strong>No:</strong> ").append(bill.getInvoiceNumber()).append("</div>\n");
        html.append("    <div><strong>Date:</strong> ").append(LocalDate.now().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))).append("</div>\n");
        html.append("    <div><strong>Status:</strong> <span class='badge badge-").append(bill.getStatus().name()).append("'>").append(bill.getStatus().getDisplayName()).append("</span></div>\n");
        html.append("  </div>\n");
        html.append("</div>\n");

        html.append("<div class='grid'>\n");
        html.append("  <div class='box'>\n");
        html.append("    <strong>Student Details:</strong><br>\n");
        html.append("    ID: ").append(bill.getStudentId()).append("<br>\n");
        html.append("    Name: ").append(bill.getStudentName() != null ? bill.getStudentName() : "").append("<br>\n");
        html.append("    Department: ").append(bill.getDepartment() != null ? bill.getDepartment() : "General").append("<br>\n");
        html.append("  </div>\n");
        html.append("  <div class='box'>\n");
        html.append("    <strong>Billing Term:</strong><br>\n");
        html.append("    Semester: ").append(bill.getSemesterName() != null ? bill.getSemesterName() : bill.getSemesterId()).append("<br>\n");
        html.append("    Due Date: <strong>").append(bill.getDueDate() != null ? bill.getDueDate().toString() : "N/A").append("</strong><br>\n");
        html.append("  </div>\n");
        html.append("</div>\n");

        html.append("<h3>Itemized Tuition & Fee Breakdown</h3>\n");
        html.append("<table>\n<thead><tr><th>Category</th><th>Description</th><th style='text-align:right;'>Amount ($)</th></tr></thead>\n<tbody>\n");
        for (BillItem item : bill.getItems()) {
            html.append("<tr>\n");
            html.append("  <td>").append(item.getItemType().getDisplayName()).append("</td>\n");
            html.append("  <td>").append(item.getDescription()).append("</td>\n");
            html.append("  <td style='text-align:right;'>$").append(item.getAmount()).append("</td>\n");
            html.append("</tr>\n");
        }
        html.append("</tbody>\n</table>\n");

        html.append("<div class='total-box'>\n");
        html.append("  <div class='total-row'><span>Total Gross Billed:</span><span>$").append(bill.getTotalAmount()).append("</span></div>\n");
        html.append("  <div class='total-row'><span>Scholarship / Waiver:</span><span>-$").append(bill.getDiscountAmount()).append("</span></div>\n");
        html.append("  <div class='total-row grand-total'><span>Net Payable Amount:</span><span>$").append(bill.getFinalAmount()).append("</span></div>\n");
        html.append("  <div class='total-row' style='color:#15803d;'><span>Amount Paid:</span><span>$").append(bill.getPaidAmount()).append("</span></div>\n");
        html.append("  <div class='total-row' style='color:#b91c1c; font-weight:bold;'><span>Balance Remaining:</span><span>$").append(bill.getBalanceDue()).append("</span></div>\n");
        html.append("</div>\n");

        if (paymentHistory != null && !paymentHistory.isEmpty()) {
            html.append("<h3 style='margin-top:30px;'>Payment History</h3>\n");
            html.append("<table>\n<thead><tr><th>Date</th><th>Ref Code</th><th>Method</th><th style='text-align:right;'>Amount</th><th>Status</th></tr></thead>\n<tbody>\n");
            for (Payment p : paymentHistory) {
                html.append("<tr>\n");
                html.append("  <td>").append(p.getPaymentDate() != null ? p.getPaymentDate().toString() : "").append("</td>\n");
                html.append("  <td><code>").append(p.getTransactionRef()).append("</code></td>\n");
                html.append("  <td>").append(p.getPaymentMethod().getDisplayName()).append("</td>\n");
                html.append("  <td style='text-align:right; font-weight:bold;'>$").append(p.getAmount()).append("</td>\n");
                html.append("  <td>").append(p.getStatus()).append("</td>\n");
                html.append("</tr>\n");
            }
            html.append("</tbody>\n</table>\n");
        }

        html.append("<div class='footer'>\n");
        html.append("  <p>Thank you for your payment. For inquiries, please contact bursar@university.edu.vn.</p>\n");
        html.append("  <p>Generated by Student Billing Management System (github.com/buu297/billing-management-system)</p>\n");
        html.append("</div>\n");
        html.append("</body>\n</html>\n");

        return html.toString();
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
