package com.buu297.billing.dao.mysql;

import com.buu297.billing.config.DatabaseManager;
import com.buu297.billing.dao.BillDAO;
import com.buu297.billing.model.Bill;
import com.buu297.billing.model.BillItem;
import com.buu297.billing.model.BillingStatus;
import com.buu297.billing.model.ItemType;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MySQLBillDAO implements BillDAO {
    private final DatabaseManager dbManager;

    public MySQLBillDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public Optional<Bill> findById(String billId) {
        String sql = "SELECT b.*, CONCAT(s.last_name, ' ', s.first_name) AS student_name, s.department, sem.term_name AS semester_name " +
                     "FROM bills b " +
                     "JOIN students s ON b.student_id = s.student_id " +
                     "JOIN semesters sem ON b.semester_id = sem.semester_id " +
                     "WHERE b.bill_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, billId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Bill b = mapResultSet(rs);
                    b.setItems(getItemsByBillId(billId));
                    return Optional.of(b);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public Optional<Bill> findByInvoiceNumber(String invoiceNumber) {
        String sql = "SELECT b.*, CONCAT(s.last_name, ' ', s.first_name) AS student_name, s.department, sem.term_name AS semester_name " +
                     "FROM bills b " +
                     "JOIN students s ON b.student_id = s.student_id " +
                     "JOIN semesters sem ON b.semester_id = sem.semester_id " +
                     "WHERE b.invoice_number = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, invoiceNumber);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Bill b = mapResultSet(rs);
                    b.setItems(getItemsByBillId(b.getBillId()));
                    return Optional.of(b);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public List<Bill> findByStudentId(String studentId) {
        List<Bill> list = new ArrayList<>();
        // Uses composite index idx_bills_student_semester
        String sql = "SELECT b.*, CONCAT(s.last_name, ' ', s.first_name) AS student_name, s.department, sem.term_name AS semester_name " +
                     "FROM bills b " +
                     "JOIN students s ON b.student_id = s.student_id " +
                     "JOIN semesters sem ON b.semester_id = sem.semester_id " +
                     "WHERE b.student_id = ? " +
                     "ORDER BY b.created_at DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, studentId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Bill b = mapResultSet(rs);
                    b.setItems(getItemsByBillId(b.getBillId()));
                    list.add(b);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    public List<Bill> findAll() {
        return filterBills(null, null, null, null);
    }

    @Override
    public List<Bill> filterBills(String semesterId, String department, BillingStatus status, String searchQuery) {
        List<Bill> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
                "SELECT b.*, CONCAT(s.last_name, ' ', s.first_name) AS student_name, s.department, sem.term_name AS semester_name " +
                "FROM bills b " +
                "JOIN students s ON b.student_id = s.student_id " +
                "JOIN semesters sem ON b.semester_id = sem.semester_id " +
                "WHERE 1=1 ");

        List<Object> params = new ArrayList<>();
        if (semesterId != null && !semesterId.isEmpty() && !semesterId.equalsIgnoreCase("ALL")) {
            sql.append("AND b.semester_id = ? ");
            params.add(semesterId);
        }
        if (department != null && !department.isEmpty() && !department.equalsIgnoreCase("ALL")) {
            sql.append("AND s.department = ? ");
            params.add(department);
        }
        if (status != null) {
            sql.append("AND b.status = ? ");
            params.add(status.name());
        }
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            sql.append("AND (b.invoice_number LIKE ? OR b.student_id LIKE ? OR s.first_name LIKE ? OR s.last_name LIKE ?) ");
            String pat = "%" + searchQuery.trim() + "%";
            params.add(pat); params.add(pat); params.add(pat); params.add(pat);
        }
        sql.append("ORDER BY b.created_at DESC");

        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
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
    public void save(Bill b) {
        String sql = "INSERT INTO bills (bill_id, invoice_number, student_id, semester_id, total_amount, discount_amount, final_amount, paid_amount, balance_due, due_date, status, version) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, b.getBillId());
            ps.setString(2, b.getInvoiceNumber());
            ps.setString(3, b.getStudentId());
            ps.setString(4, b.getSemesterId());
            ps.setBigDecimal(5, b.getTotalAmount());
            ps.setBigDecimal(6, b.getDiscountAmount());
            ps.setBigDecimal(7, b.getFinalAmount());
            ps.setBigDecimal(8, b.getPaidAmount());
            ps.setBigDecimal(9, b.getBalanceDue());
            ps.setDate(10, Date.valueOf(b.getDueDate()));
            ps.setString(11, b.getStatus().name());
            ps.setInt(12, b.getVersion());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void update(Bill b) {
        String sql = "UPDATE bills SET total_amount=?, discount_amount=?, final_amount=?, paid_amount=?, balance_due=?, due_date=?, status=?, version=version+1 " +
                     "WHERE bill_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, b.getTotalAmount());
            ps.setBigDecimal(2, b.getDiscountAmount());
            ps.setBigDecimal(3, b.getFinalAmount());
            ps.setBigDecimal(4, b.getPaidAmount());
            ps.setBigDecimal(5, b.getBalanceDue());
            ps.setDate(6, Date.valueOf(b.getDueDate()));
            ps.setString(7, b.getStatus().name());
            ps.setString(8, b.getBillId());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void addItem(BillItem item) {
        String sql = "INSERT INTO bill_items (item_id, bill_id, item_type, description, amount) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, item.getItemId());
            ps.setString(2, item.getBillId());
            ps.setString(3, item.getItemType().name());
            ps.setString(4, item.getDescription());
            ps.setBigDecimal(5, item.getAmount());
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<BillItem> getItemsByBillId(String billId) {
        List<BillItem> list = new ArrayList<>();
        String sql = "SELECT * FROM bill_items WHERE bill_id = ? ORDER BY created_at ASC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, billId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BillItem item = new BillItem();
                    item.setItemId(rs.getString("item_id"));
                    item.setBillId(rs.getString("bill_id"));
                    item.setItemType(ItemType.valueOf(rs.getString("item_type")));
                    item.setDescription(rs.getString("description"));
                    item.setAmount(rs.getBigDecimal("amount"));
                    list.add(item);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    private Bill mapResultSet(ResultSet rs) throws SQLException {
        Bill b = new Bill();
        b.setBillId(rs.getString("bill_id"));
        b.setInvoiceNumber(rs.getString("invoice_number"));
        b.setStudentId(rs.getString("student_id"));
        b.setSemesterId(rs.getString("semester_id"));
        b.setTotalAmount(rs.getBigDecimal("total_amount"));
        b.setDiscountAmount(rs.getBigDecimal("discount_amount"));
        b.setFinalAmount(rs.getBigDecimal("final_amount"));
        b.setPaidAmount(rs.getBigDecimal("paid_amount"));
        b.setBalanceDue(rs.getBigDecimal("balance_due"));
        Date d = rs.getDate("due_date");
        if (d != null) b.setDueDate(d.toLocalDate());
        b.setStatus(BillingStatus.fromString(rs.getString("status")));
        b.setVersion(rs.getInt("version"));
        try {
            b.setStudentName(rs.getString("student_name"));
            b.setDepartment(rs.getString("department"));
            b.setSemesterName(rs.getString("semester_name"));
        } catch (SQLException ignored) {}
        return b;
    }
}
