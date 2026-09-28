package com.buu297.billing.dao;

import com.buu297.billing.model.Payment;
import java.util.List;
import java.util.Optional;

public interface PaymentDAO {
    Optional<Payment> findById(String paymentId);
    Optional<Payment> findByTransactionRef(String transactionRef);
    List<Payment> findByBillId(String billId);
    List<Payment> findByStudentId(String studentId);
    List<Payment> findAll();
    List<Payment> findRecent(int limit);
    void recordPayment(Payment payment);
}
