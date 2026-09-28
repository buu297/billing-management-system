package com.buu297.billing.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Payment {
    private String paymentId;
    private String transactionRef;
    private String billId;
    private String studentId;
    private String studentName;
    private BigDecimal amount;
    private PaymentMethod paymentMethod;
    private String status;
    private String notes;
    private LocalDateTime paymentDate;

    public Payment() {}

    public Payment(String paymentId, String transactionRef, String billId, String studentId,
                   BigDecimal amount, PaymentMethod paymentMethod, String status, String notes) {
        this.paymentId = paymentId;
        this.transactionRef = transactionRef;
        this.billId = billId;
        this.studentId = studentId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.status = status;
        this.notes = notes;
        this.paymentDate = LocalDateTime.now();
    }

    public String getPaymentId() { return paymentId; }
    public void setPaymentId(String paymentId) { this.paymentId = paymentId; }

    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }

    public String getBillId() { return billId; }
    public void setBillId(String billId) { this.billId = billId; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public PaymentMethod getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(PaymentMethod paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
}
