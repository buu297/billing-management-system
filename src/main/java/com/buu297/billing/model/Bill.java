package com.buu297.billing.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Bill {
    private String billId;
    private String invoiceNumber;
    private String studentId;
    private String studentName;
    private String department;
    private String semesterId;
    private String semesterName;
    private BigDecimal totalAmount = BigDecimal.ZERO;
    private BigDecimal discountAmount = BigDecimal.ZERO;
    private BigDecimal finalAmount = BigDecimal.ZERO;
    private BigDecimal paidAmount = BigDecimal.ZERO;
    private BigDecimal balanceDue = BigDecimal.ZERO;
    private LocalDate dueDate;
    private BillingStatus status = BillingStatus.UNPAID;
    private int version = 1;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<BillItem> items = new ArrayList<>();

    public Bill() {}

    public Bill(String billId, String invoiceNumber, String studentId, String semesterId,
                BigDecimal totalAmount, BigDecimal discountAmount, BigDecimal paidAmount,
                LocalDate dueDate, BillingStatus status) {
        this.billId = billId;
        this.invoiceNumber = invoiceNumber;
        this.studentId = studentId;
        this.semesterId = semesterId;
        this.totalAmount = totalAmount;
        this.discountAmount = discountAmount;
        this.finalAmount = totalAmount.subtract(discountAmount);
        this.paidAmount = paidAmount;
        this.balanceDue = this.finalAmount.subtract(paidAmount);
        this.dueDate = dueDate;
        this.status = status;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void recalculateAmounts() {
        BigDecimal sumTotal = BigDecimal.ZERO;
        BigDecimal sumDiscount = BigDecimal.ZERO;

        for (BillItem item : items) {
            if (item.getItemType() == ItemType.SCHOLARSHIP_DISCOUNT) {
                sumDiscount = sumDiscount.add(item.getAmount().abs());
            } else {
                sumTotal = sumTotal.add(item.getAmount());
            }
        }
        this.totalAmount = sumTotal;
        this.discountAmount = sumDiscount;
        this.finalAmount = sumTotal.subtract(sumDiscount);
        if (this.finalAmount.compareTo(BigDecimal.ZERO) < 0) {
            this.finalAmount = BigDecimal.ZERO;
        }
        this.balanceDue = this.finalAmount.subtract(this.paidAmount);
        if (this.balanceDue.compareTo(BigDecimal.ZERO) < 0) {
            this.balanceDue = BigDecimal.ZERO;
        }

        // Determine status
        if (this.balanceDue.compareTo(BigDecimal.ZERO) == 0 && this.finalAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.status = BillingStatus.PAID;
        } else if (this.paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.status = BillingStatus.PARTIAL;
        } else if (this.dueDate != null && this.dueDate.isBefore(LocalDate.now())) {
            this.status = BillingStatus.OVERDUE;
        } else {
            this.status = BillingStatus.UNPAID;
        }
    }

    public String getBillId() { return billId; }
    public void setBillId(String billId) { this.billId = billId; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getStudentName() { return studentName; }
    public void setStudentName(String studentName) { this.studentName = studentName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getSemesterId() { return semesterId; }
    public void setSemesterId(String semesterId) { this.semesterId = semesterId; }

    public String getSemesterName() { return semesterName; }
    public void setSemesterName(String semesterName) { this.semesterName = semesterName; }

    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }

    public BigDecimal getFinalAmount() { return finalAmount; }
    public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }

    public BigDecimal getPaidAmount() { return paidAmount; }
    public void setPaidAmount(BigDecimal paidAmount) { this.paidAmount = paidAmount; }

    public BigDecimal getBalanceDue() { return balanceDue; }
    public void setBalanceDue(BigDecimal balanceDue) { this.balanceDue = balanceDue; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public BillingStatus getStatus() { return status; }
    public void setStatus(BillingStatus status) { this.status = status; }

    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public List<BillItem> getItems() { return items; }
    public void setItems(List<BillItem> items) { this.items = items; }
}
