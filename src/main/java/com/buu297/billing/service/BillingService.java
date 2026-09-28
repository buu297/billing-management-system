package com.buu297.billing.service;

import com.buu297.billing.config.DatabaseManager;
import com.buu297.billing.dao.BillDAO;
import com.buu297.billing.dao.StudentDAO;
import com.buu297.billing.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class BillingService {
    private final BillDAO billDAO;
    private final StudentDAO studentDAO;

    public BillingService() {
        this.billDAO = DatabaseManager.getInstance().getBillDAO();
        this.studentDAO = DatabaseManager.getInstance().getStudentDAO();
    }

    public List<Bill> getAllBills() {
        return billDAO.findAll();
    }

    public Optional<Bill> getBillById(String billId) {
        return billDAO.findById(billId);
    }

    public Optional<Bill> getBillByInvoice(String invoiceNumber) {
        return billDAO.findByInvoiceNumber(invoiceNumber);
    }

    public List<Bill> getBillsForStudent(String studentId) {
        return billDAO.findByStudentId(studentId);
    }

    public List<Bill> filterBills(String semesterId, String department, BillingStatus status, String query) {
        return billDAO.filterBills(semesterId, department, status, query);
    }

    public Bill createNewBill(String studentId, String semesterId, LocalDate dueDate, List<BillItem> items) {
        Student s = studentDAO.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));

        String billId = "BILL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String invoiceNumber = "INV-" + LocalDate.now().getYear() + "-" + (int)(10000 + Math.random() * 90000);

        Bill bill = new Bill();
        bill.setBillId(billId);
        bill.setInvoiceNumber(invoiceNumber);
        bill.setStudentId(studentId);
        bill.setStudentName(s.getFullName());
        bill.setDepartment(s.getDepartment());
        bill.setSemesterId(semesterId);
        bill.setDueDate(dueDate != null ? dueDate : LocalDate.now().plusDays(30));
        bill.setStatus(BillingStatus.UNPAID);

        for (BillItem item : items) {
            item.setItemId("ITEM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            item.setBillId(billId);
        }
        bill.setItems(items);
        bill.recalculateAmounts();

        billDAO.save(bill);
        for (BillItem item : items) {
            billDAO.addItem(item);
        }

        return bill;
    }

    public void addLineItemToBill(String billId, ItemType type, String description, BigDecimal amount) {
        Bill bill = billDAO.findById(billId)
                .orElseThrow(() -> new IllegalArgumentException("Bill not found: " + billId));

        String itemId = "ITEM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        BillItem item = new BillItem(itemId, billId, type, description, amount);

        billDAO.addItem(item);
        bill.getItems().add(item);
        bill.recalculateAmounts();
        billDAO.update(bill);
    }

    public void updateOverdueStatuses() {
        List<Bill> all = billDAO.findAll();
        LocalDate now = LocalDate.now();
        for (Bill b : all) {
            if ((b.getStatus() == BillingStatus.UNPAID || b.getStatus() == BillingStatus.PARTIAL)
                    && b.getDueDate() != null && b.getDueDate().isBefore(now)) {
                b.setStatus(BillingStatus.OVERDUE);
                billDAO.update(b);
            }
        }
    }
}
