package com.buu297.billing.dao.memory;

import com.buu297.billing.dao.PaymentDAO;
import com.buu297.billing.model.Payment;
import com.buu297.billing.model.Student;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class InMemoryPaymentDAO implements PaymentDAO {
    private final InMemoryDatabase db = InMemoryDatabase.getInstance();

    @Override
    public Optional<Payment> findById(String paymentId) {
        synchronized (db.payments) {
            return db.payments.stream().filter(p -> p.getPaymentId().equals(paymentId)).findFirst();
        }
    }

    @Override
    public Optional<Payment> findByTransactionRef(String transactionRef) {
        synchronized (db.payments) {
            return db.payments.stream().filter(p -> p.getTransactionRef().equalsIgnoreCase(transactionRef)).findFirst();
        }
    }

    @Override
    public List<Payment> findByBillId(String billId) {
        synchronized (db.payments) {
            return db.payments.stream()
                    .filter(p -> p.getBillId().equalsIgnoreCase(billId))
                    .sorted(Comparator.comparing(Payment::getPaymentDate).reversed())
                    .collect(Collectors.toList());
        }
    }

    @Override
    public List<Payment> findByStudentId(String studentId) {
        synchronized (db.payments) {
            return db.payments.stream()
                    .filter(p -> p.getStudentId().equalsIgnoreCase(studentId))
                    .sorted(Comparator.comparing(Payment::getPaymentDate).reversed())
                    .collect(Collectors.toList());
        }
    }

    @Override
    public List<Payment> findAll() {
        synchronized (db.payments) {
            return new ArrayList<>(db.payments);
        }
    }

    @Override
    public List<Payment> findRecent(int limit) {
        synchronized (db.payments) {
            return db.payments.stream()
                    .sorted(Comparator.comparing(Payment::getPaymentDate).reversed())
                    .limit(limit)
                    .collect(Collectors.toList());
        }
    }

    @Override
    public void recordPayment(Payment payment) {
        Student s = db.students.get(payment.getStudentId());
        if (s != null) {
            payment.setStudentName(s.getFullName());
        }
        db.payments.add(payment);
    }
}