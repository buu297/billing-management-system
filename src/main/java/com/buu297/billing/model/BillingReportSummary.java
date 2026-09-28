package com.buu297.billing.model;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class BillingReportSummary {
    private String filterSemester;
    private String filterDepartment;
    private String filterStatus;
    private int totalBillsCount = 0;
    private int totalStudentsCount = 0;
    private BigDecimal totalBilledAmount = BigDecimal.ZERO;
    private BigDecimal totalCollectedAmount = BigDecimal.ZERO;
    private BigDecimal totalOutstandingAmount = BigDecimal.ZERO;
    private double collectionRatePct = 0.0;

    private int countPaid = 0;
    private int countPartial = 0;
    private int countUnpaid = 0;
    private int countOverdue = 0;

    private Map<String, BigDecimal> revenueByDepartment = new HashMap<>();
    private Map<String, BigDecimal> revenueBySemester = new HashMap<>();

    public BillingReportSummary() {}

    public void calculateRates() {
        if (totalBilledAmount != null && totalBilledAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.collectionRatePct = totalCollectedAmount
                    .divide(totalBilledAmount, 4, java.math.RoundingMode.HALF_UP)
                    .doubleValue() * 100.0;
        } else {
            this.collectionRatePct = 0.0;
        }
    }

    public String getFilterSemester() { return filterSemester; }
    public void setFilterSemester(String filterSemester) { this.filterSemester = filterSemester; }

    public String getFilterDepartment() { return filterDepartment; }
    public void setFilterDepartment(String filterDepartment) { this.filterDepartment = filterDepartment; }

    public String getFilterStatus() { return filterStatus; }
    public void setFilterStatus(String filterStatus) { this.filterStatus = filterStatus; }

    public int getTotalBillsCount() { return totalBillsCount; }
    public void setTotalBillsCount(int totalBillsCount) { this.totalBillsCount = totalBillsCount; }

    public int getTotalStudentsCount() { return totalStudentsCount; }
    public void setTotalStudentsCount(int totalStudentsCount) { this.totalStudentsCount = totalStudentsCount; }

    public BigDecimal getTotalBilledAmount() { return totalBilledAmount; }
    public void setTotalBilledAmount(BigDecimal totalBilledAmount) { this.totalBilledAmount = totalBilledAmount; }

    public BigDecimal getTotalCollectedAmount() { return totalCollectedAmount; }
    public void setTotalCollectedAmount(BigDecimal totalCollectedAmount) { this.totalCollectedAmount = totalCollectedAmount; }

    public BigDecimal getTotalOutstandingAmount() { return totalOutstandingAmount; }
    public void setTotalOutstandingAmount(BigDecimal totalOutstandingAmount) { this.totalOutstandingAmount = totalOutstandingAmount; }

    public double getCollectionRatePct() { return collectionRatePct; }
    public void setCollectionRatePct(double collectionRatePct) { this.collectionRatePct = collectionRatePct; }

    public int getCountPaid() { return countPaid; }
    public void setCountPaid(int countPaid) { this.countPaid = countPaid; }

    public int getCountPartial() { return countPartial; }
    public void setCountPartial(int countPartial) { this.countPartial = countPartial; }

    public int getCountUnpaid() { return countUnpaid; }
    public void setCountUnpaid(int countUnpaid) { this.countUnpaid = countUnpaid; }

    public int getCountOverdue() { return countOverdue; }
    public void setCountOverdue(int countOverdue) { this.countOverdue = countOverdue; }

    public Map<String, BigDecimal> getRevenueByDepartment() { return revenueByDepartment; }
    public void setRevenueByDepartment(Map<String, BigDecimal> revenueByDepartment) { this.revenueByDepartment = revenueByDepartment; }

    public Map<String, BigDecimal> getRevenueBySemester() { return revenueBySemester; }
    public void setRevenueBySemester(Map<String, BigDecimal> revenueBySemester) { this.revenueBySemester = revenueBySemester; }
}
