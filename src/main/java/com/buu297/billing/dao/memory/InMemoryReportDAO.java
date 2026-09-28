package com.buu297.billing.dao.memory;

import com.buu297.billing.dao.ReportDAO;
import com.buu297.billing.model.Bill;
import com.buu297.billing.model.BillingReportSummary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class InMemoryReportDAO implements ReportDAO {
    private final InMemoryDatabase db = InMemoryDatabase.getInstance();

    @Override
    public BillingReportSummary generateSummary(String semesterId, String department, String status, LocalDate fromDate, LocalDate toDate) {
        BillingReportSummary summary = new BillingReportSummary();
        summary.setFilterSemester(semesterId);
        summary.setFilterDepartment(department);
        summary.setFilterStatus(status);

        List<Bill> filtered = db.bills.values().stream()
                .filter(b -> semesterId == null || semesterId.isEmpty() || semesterId.equalsIgnoreCase("ALL") || b.getSemesterId().equalsIgnoreCase(semesterId))
                .filter(b -> department == null || department.isEmpty() || department.equalsIgnoreCase("ALL") || (b.getDepartment() != null && b.getDepartment().equalsIgnoreCase(department)))
                .filter(b -> status == null || status.isEmpty() || status.equalsIgnoreCase("ALL") || b.getStatus().name().equalsIgnoreCase(status))
                .collect(Collectors.toList());

        summary.setTotalBillsCount(filtered.size());
        Set<String> uniqueStudents = filtered.stream().map(Bill::getStudentId).collect(Collectors.toSet());
        summary.setTotalStudentsCount(uniqueStudents.size());

        BigDecimal totalBilled = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalOutstanding = BigDecimal.ZERO;

        int paidCount = 0;
        int partialCount = 0;
        int unpaidCount = 0;
        int overdueCount = 0;

        Map<String, BigDecimal> deptRev = new HashMap<>();
        Map<String, BigDecimal> semRev = new HashMap<>();

        for (Bill b : filtered) {
            totalBilled = totalBilled.add(b.getFinalAmount());
            totalPaid = totalPaid.add(b.getPaidAmount());
            totalOutstanding = totalOutstanding.add(b.getBalanceDue());

            switch (b.getStatus()) {
                case PAID: paidCount++; break;
                case PARTIAL: partialCount++; break;
                case UNPAID: unpaidCount++; break;
                case OVERDUE: overdueCount++; break;
            }

            String d = b.getDepartment() != null ? b.getDepartment() : "General";
            deptRev.put(d, deptRev.getOrDefault(d, BigDecimal.ZERO).add(b.getPaidAmount()));

            String sem = b.getSemesterName() != null ? b.getSemesterName() : b.getSemesterId();
            semRev.put(sem, semRev.getOrDefault(sem, BigDecimal.ZERO).add(b.getPaidAmount()));
        }

        summary.setTotalBilledAmount(totalBilled);
        summary.setTotalCollectedAmount(totalPaid);
        summary.setTotalOutstandingAmount(totalOutstanding);
        summary.setCountPaid(paidCount);
        summary.setCountPartial(partialCount);
        summary.setCountUnpaid(unpaidCount);
        summary.setCountOverdue(overdueCount);
        summary.setRevenueByDepartment(deptRev);
        summary.setRevenueBySemester(semRev);
        summary.calculateRates();

        return summary;
    }
}