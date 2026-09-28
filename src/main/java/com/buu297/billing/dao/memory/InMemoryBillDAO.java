package com.buu297.billing.dao.memory;

import com.buu297.billing.dao.BillDAO;
import com.buu297.billing.model.Bill;
import com.buu297.billing.model.BillItem;
import com.buu297.billing.model.BillingStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class InMemoryBillDAO implements BillDAO {
    private final InMemoryDatabase db = InMemoryDatabase.getInstance();

    @Override
    public Optional<Bill> findById(String billId) {
        Bill b = db.bills.get(billId);
        if (b != null) {
            b.setItems(db.billItems.getOrDefault(billId, new ArrayList<>()));
        }
        return Optional.ofNullable(b);
    }

    @Override
    public Optional<Bill> findByInvoiceNumber(String invoiceNumber) {
        return db.bills.values().stream()
                .filter(b -> b.getInvoiceNumber().equalsIgnoreCase(invoiceNumber))
                .findFirst();
    }

    @Override
    public List<Bill> findByStudentId(String studentId) {
        return db.bills.values().stream()
                .filter(b -> b.getStudentId().equalsIgnoreCase(studentId))
                .sorted(Comparator.comparing(Bill::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public List<Bill> findAll() {
        return new ArrayList<>(db.bills.values());
    }

    @Override
    public List<Bill> filterBills(String semesterId, String department, BillingStatus status, String searchQuery) {
        return db.bills.values().stream()
                .filter(b -> semesterId == null || semesterId.isEmpty() || semesterId.equalsIgnoreCase("ALL") || b.getSemesterId().equalsIgnoreCase(semesterId))
                .filter(b -> department == null || department.isEmpty() || department.equalsIgnoreCase("ALL") || (b.getDepartment() != null && b.getDepartment().equalsIgnoreCase(department)))
                .filter(b -> status == null || b.getStatus() == status)
                .filter(b -> {
                    if (searchQuery == null || searchQuery.trim().isEmpty()) return true;
                    String q = searchQuery.toLowerCase().trim();
                    return b.getInvoiceNumber().toLowerCase().contains(q)
                            || b.getStudentId().toLowerCase().contains(q)
                            || (b.getStudentName() != null && b.getStudentName().toLowerCase().contains(q));
                })
                .sorted(Comparator.comparing(Bill::getCreatedAt).reversed())
                .collect(Collectors.toList());
    }

    @Override
    public void save(Bill bill) {
        db.bills.put(bill.getBillId(), bill);
        if (bill.getItems() != null) {
            db.billItems.put(bill.getBillId(), new ArrayList<>(bill.getItems()));
        }
    }

    @Override
    public void update(Bill bill) {
        bill.setUpdatedAt(LocalDateTime.now());
        db.bills.put(bill.getBillId(), bill);
    }

    @Override
    public void addItem(BillItem item) {
        db.billItems.computeIfAbsent(item.getBillId(), k -> new ArrayList<>()).add(item);
        Bill b = db.bills.get(item.getBillId());
        if (b != null) {
            b.setItems(db.billItems.get(item.getBillId()));
            b.recalculateAmounts();
        }
    }

    @Override
    public List<BillItem> getItemsByBillId(String billId) {
        return new ArrayList<>(db.billItems.getOrDefault(billId, new ArrayList<>()));
    }
}