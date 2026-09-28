package com.buu297.billing.dao.mysql;

import com.buu297.billing.config.DatabaseManager;
import com.buu297.billing.dao.ReportDAO;
import com.buu297.billing.model.BillingReportSummary;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

public class MySQLReportDAO implements ReportDAO {
    private final DatabaseManager dbManager;

    public MySQLReportDAO(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    @Override
    public BillingReportSummary generateSummary(String semesterId, String department, String status, LocalDate fromDate, LocalDate toDate) {
        BillingReportSummary summary = new BillingReportSummary();
        summary.setFilterSemester(semesterId);
        summary.setFilterDepartment(department);
        summary.setFilterStatus(status);

        StringBuilder sql = new StringBuilder(
                "SELECT " +
                "  COUNT(b.bill_id) AS total_bills, " +
                "  COUNT(DISTINCT b.student_id) AS total_students, " +
                "  COALESCE(SUM(b.final_amount), 0) AS total_billed, " +
                "  COALESCE(SUM(b.paid_amount), 0) AS total_collected, " +
                "  COALESCE(SUM(b.balance_due), 0) AS total_outstanding, " +
                "  COUNT(CASE WHEN b.status = 'PAID' THEN 1 END) AS count_paid, " +
                "  COUNT(CASE WHEN b.status = 'PARTIAL' THEN 1 END) AS count_partial, " +
                "  COUNT(CASE WHEN b.status = 'UNPAID' THEN 1 END) AS count_unpaid, " +
                "  COUNT(CASE WHEN b.status = 'OVERDUE' THEN 1 END) AS count_overdue " +
                "FROM bills b " +
                "JOIN students s ON b.student_id = s.student_id " +
                "WHERE 1=1 ");

        if (semesterId != null && !semesterId.isEmpty() && !semesterId.equalsIgnoreCase("ALL")) {
            sql.append("AND b.semester_id = '").append(semesterId).append("' ");
        }
        if (department != null && !department.isEmpty() && !department.equalsIgnoreCase("ALL")) {
            sql.append("AND s.department = '").append(department).append("' ");
        }
        if (status != null && !status.isEmpty() && !status.equalsIgnoreCase("ALL")) {
            sql.append("AND b.status = '").append(status).append("' ");
        }

        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql.toString())) {
            if (rs.next()) {
                summary.setTotalBillsCount(rs.getInt("total_bills"));
                summary.setTotalStudentsCount(rs.getInt("total_students"));
                summary.setTotalBilledAmount(rs.getBigDecimal("total_billed"));
                summary.setTotalCollectedAmount(rs.getBigDecimal("total_collected"));
                summary.setTotalOutstandingAmount(rs.getBigDecimal("total_outstanding"));
                summary.setCountPaid(rs.getInt("count_paid"));
                summary.setCountPartial(rs.getInt("count_partial"));
                summary.setCountUnpaid(rs.getInt("count_unpaid"));
                summary.setCountOverdue(rs.getInt("count_overdue"));
                summary.calculateRates();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // Department breakdown
        String deptSql = "SELECT s.department, SUM(b.paid_amount) AS revenue FROM bills b JOIN students s ON b.student_id = s.student_id GROUP BY s.department";
        Map<String, BigDecimal> deptRev = new HashMap<>();
        try (Connection conn = dbManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(deptSql)) {
            while (rs.next()) {
                deptRev.put(rs.getString("department"), rs.getBigDecimal("revenue"));
            }
        } catch (SQLException ignored) {}
        summary.setRevenueByDepartment(deptRev);

        return summary;
    }
}
