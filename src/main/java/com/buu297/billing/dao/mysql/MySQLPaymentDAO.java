package com.buu297.billing.dao.mysql;

import com.buu297.billing.config.DatabaseManager;
import com.buu297.billing.dao.PaymentDAO;
import com.buu297.billing.model.Payment;
import com.buu297.billing.model.PaymentMethod;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MySQLPaymentDAO implements PaymentDAO {
    private final DatabaseManager dbManager;

    public MySQLPaymentDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Optional<Payment> findById(String paymentId) {
        String sql = "SELECT p.*, CONCAT(s.last_name, ' ', s.first_name) AS student_name " +
                     "FROM payments p " +
                     "JOIN students s ON p.student_id = s.student_id " +
                     "WHERE p.payment_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, paymentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public Optional<Payment> findByTransactionRef(String transactionRef) {
        String sql = "SELECT p.*, CONCAT(s.last_name, ' ', s.first_name) AS student_name " +
                     "FROM payments p " +
                     "JOIN students s ON p.student_id = s.student_id " +
                     "WHERE p.transaction_ref = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, transactionRef);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<Payment> findByBillId(String billId) {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT p.*, CONCAT(s.last_name, ' ', s.first_name) AS student_name " +
                     "FROM payments p " +
                     "JOIN students s ON p.student_id = s.student_id " +
                     "WHERE p.bill_id = ? " +
                     "ORDER BY p.payment_date DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, billId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<Payment> findByStudentId(String studentId) {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT p.*, CONCAT(s.last_name, ' ', s.first_name) AS student_name " +
                     "FROM payments p " +
                     "JOIN students s ON p.student_id = s.student_id " +
                     "WHERE p.student_id = ? " +
                     "ORDER BY p.payment_date DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<Payment> findAll() {
        return findRecent(500);
    }

    @Override
    public List<Payment> findRecent(int limit) {
        List<Payment> list = new ArrayList<>();
        String sql = "SELECT p.*, CONCAT(s.last_name, ' ', s.first_name) AS student_name " +
                     "FROM payments p " +
                     "JOIN students s ON p.student_id = s.student_id " +
                     "ORDER BY p.payment_date DESC LIMIT ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSet(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public void recordPayment(Payment p) {
        String sql = "INSERT INTO payments (payment_id, transaction_ref, bill_id, student_id, amount, payment_method, status, notes, payment_date) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW())";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, p.getPaymentId());
            ps.setString(2, p.getTransactionRef());
            ps.setString(3, p.getBillId());
            ps.setString(4, p.getStudentId());
            ps.setBigDecimal(5, p.getAmount());
            ps.setString(6, p.getPaymentMethod().name());
            ps.setString(7, p.getStatus());
            ps.setString(8, p.getNotes());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Payment mapResultSet(ResultSet rs) throws SQLException {
        Payment p = new Payment();
        p.setPaymentId(rs.getString("payment_id"));
        p.setTransactionRef(rs.getString("transaction_ref"));
        p.setBillId(rs.getString("bill_id"));
        p.setStudentId(rs.getString("student_id"));
        p.setAmount(rs.getBigDecimal("amount"));
        p.setPaymentMethod(PaymentMethod.fromString(rs.getString("payment_method")));
        p.setStatus(rs.getString("status"));
        p.setNotes(rs.getString("notes"));
        Timestamp ts = rs.getTimestamp("payment_date");
        if (ts != null) p.setPaymentDate(ts.toLocalDateTime());
        try {
            p.setStudentName(rs.getString("student_name"));
        } catch (SQLException ignored) {}
        return p;
    }
}
