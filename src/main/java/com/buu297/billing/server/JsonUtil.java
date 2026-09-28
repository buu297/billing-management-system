package com.buu297.billing.server;

import com.buu297.billing.model.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class JsonUtil {

    public static String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < ' ') {
                        String t = "000" + Integer.toHexString(c);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.toString();
    }

    public static String studentToJson(Student s) {
        return "{" +
                "\"studentId\":\"" + escape(s.getStudentId()) + "\"," +
                "\"firstName\":\"" + escape(s.getFirstName()) + "\"," +
                "\"lastName\":\"" + escape(s.getLastName()) + "\"," +
                "\"fullName\":\"" + escape(s.getFullName()) + "\"," +
                "\"email\":\"" + escape(s.getEmail()) + "\"," +
                "\"phone\":\"" + escape(s.getPhone()) + "\"," +
                "\"department\":\"" + escape(s.getDepartment()) + "\"," +
                "\"major\":\"" + escape(s.getMajor()) + "\"," +
                "\"academicYear\":" + s.getAcademicYear() + "," +
                "\"status\":\"" + escape(s.getStatus()) + "\"" +
                "}";
    }

    public static String studentsToJson(List<Student> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(studentToJson(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    public static String billItemToJson(BillItem item) {
        return "{" +
                "\"itemId\":\"" + escape(item.getItemId()) + "\"," +
                "\"billId\":\"" + escape(item.getBillId()) + "\"," +
                "\"itemType\":\"" + escape(item.getItemType().name()) + "\"," +
                "\"itemTypeName\":\"" + escape(item.getItemType().getDisplayName()) + "\"," +
                "\"description\":\"" + escape(item.getDescription()) + "\"," +
                "\"amount\":" + item.getAmount() +
                "}";
    }

    public static String billToJson(Bill b) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"billId\":\"").append(escape(b.getBillId())).append("\",");
        sb.append("\"invoiceNumber\":\"").append(escape(b.getInvoiceNumber())).append("\",");
        sb.append("\"studentId\":\"").append(escape(b.getStudentId())).append("\",");
        sb.append("\"studentName\":\"").append(escape(b.getStudentName() != null ? b.getStudentName() : "")).append("\",");
        sb.append("\"department\":\"").append(escape(b.getDepartment() != null ? b.getDepartment() : "")).append("\",");
        sb.append("\"semesterId\":\"").append(escape(b.getSemesterId())).append("\",");
        sb.append("\"semesterName\":\"").append(escape(b.getSemesterName() != null ? b.getSemesterName() : b.getSemesterId())).append("\",");
        sb.append("\"totalAmount\":").append(b.getTotalAmount()).append(",");
        sb.append("\"discountAmount\":").append(b.getDiscountAmount()).append(",");
        sb.append("\"finalAmount\":").append(b.getFinalAmount()).append(",");
        sb.append("\"paidAmount\":").append(b.getPaidAmount()).append(",");
        sb.append("\"balanceDue\":").append(b.getBalanceDue()).append(",");
        sb.append("\"dueDate\":\"").append(b.getDueDate() != null ? b.getDueDate().toString() : "").append("\",");
        sb.append("\"status\":\"").append(escape(b.getStatus().name())).append("\",");
        sb.append("\"statusDisplayName\":\"").append(escape(b.getStatus().getDisplayName())).append("\",");
        sb.append("\"version\":").append(b.getVersion()).append(",");

        sb.append("\"items\":[");
        List<BillItem> items = b.getItems();
        if (items != null) {
            for (int i = 0; i < items.size(); i++) {
                if (i > 0) sb.append(",");
                sb.append(billItemToJson(items.get(i)));
            }
        }
        sb.append("]}");
        return sb.toString();
    }

    public static String billsToJson(List<Bill> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(billToJson(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    public static String paymentToJson(Payment p) {
        return "{" +
                "\"paymentId\":\"" + escape(p.getPaymentId()) + "\"," +
                "\"transactionRef\":\"" + escape(p.getTransactionRef()) + "\"," +
                "\"billId\":\"" + escape(p.getBillId()) + "\"," +
                "\"studentId\":\"" + escape(p.getStudentId()) + "\"," +
                "\"studentName\":\"" + escape(p.getStudentName() != null ? p.getStudentName() : "") + "\"," +
                "\"amount\":" + p.getAmount() + "," +
                "\"paymentMethod\":\"" + escape(p.getPaymentMethod().name()) + "\"," +
                "\"paymentMethodName\":\"" + escape(p.getPaymentMethod().getDisplayName()) + "\"," +
                "\"status\":\"" + escape(p.getStatus()) + "\"," +
                "\"notes\":\"" + escape(p.getNotes() != null ? p.getNotes() : "") + "\"," +
                "\"paymentDate\":\"" + (p.getPaymentDate() != null ? p.getPaymentDate().toString() : "") + "\"" +
                "}";
    }

    public static String paymentsToJson(List<Payment> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(paymentToJson(list.get(i)));
        }
        sb.append("]");
        return sb.toString();
    }

    public static String transactionResultToJson(TransactionResult tr) {
        return "{" +
                "\"success\":" + tr.isSuccess() + "," +
                "\"statusCode\":\"" + escape(tr.getStatusCode()) + "\"," +
                "\"message\":\"" + escape(tr.getMessage()) + "\"," +
                "\"transactionRef\":\"" + escape(tr.getTransactionRef() != null ? tr.getTransactionRef() : "") + "\"," +
                "\"paidAmount\":" + tr.getPaidAmount() + "," +
                "\"previousBalance\":" + tr.getPreviousBalance() + "," +
                "\"remainingBalance\":" + tr.getRemainingBalance() + "," +
                "\"updatedStatus\":\"" + (tr.getUpdatedStatus() != null ? tr.getUpdatedStatus().name() : "") + "\"," +
                "\"executionDurationMs\":" + tr.getExecutionDurationMs() +
                "}";
    }

    public static String reportSummaryToJson(BillingReportSummary rep) {
        StringBuilder sb = new StringBuilder("{");
        sb.append("\"totalBillsCount\":").append(rep.getTotalBillsCount()).append(",");
        sb.append("\"totalStudentsCount\":").append(rep.getTotalStudentsCount()).append(",");
        sb.append("\"totalBilledAmount\":").append(rep.getTotalBilledAmount()).append(",");
        sb.append("\"totalCollectedAmount\":").append(rep.getTotalCollectedAmount()).append(",");
        sb.append("\"totalOutstandingAmount\":").append(rep.getTotalOutstandingAmount()).append(",");
        sb.append("\"collectionRatePct\":").append(String.format(java.util.Locale.US, "%.2f", rep.getCollectionRatePct())).append(",");
        sb.append("\"countPaid\":").append(rep.getCountPaid()).append(",");
        sb.append("\"countPartial\":").append(rep.getCountPartial()).append(",");
        sb.append("\"countUnpaid\":").append(rep.getCountUnpaid()).append(",");
        sb.append("\"countOverdue\":").append(rep.getCountOverdue()).append(",");

        sb.append("\"revenueByDepartment\":{");
        int i = 0;
        for (Map.Entry<String, BigDecimal> e : rep.getRevenueByDepartment().entrySet()) {
            if (i++ > 0) sb.append(",");
            sb.append("\"").append(escape(e.getKey())).append("\":").append(e.getValue());
        }
        sb.append("}}");
        return sb.toString();
    }
}
