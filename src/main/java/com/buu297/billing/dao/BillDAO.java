package com.buu297.billing.dao;

import com.buu297.billing.model.Bill;
import com.buu297.billing.model.BillItem;
import com.buu297.billing.model.BillingStatus;
import java.util.List;
import java.util.Optional;

public interface BillDAO {
    Optional<Bill> findById(String billId);
    Optional<Bill> findByInvoiceNumber(String invoiceNumber);
    List<Bill> findByStudentId(String studentId);
    List<Bill> findAll();
    List<Bill> filterBills(String semesterId, String department, BillingStatus status, String searchQuery);
    void save(Bill bill);
    void update(Bill bill);
    void addItem(BillItem item);
    List<BillItem> getItemsByBillId(String billId);
}
