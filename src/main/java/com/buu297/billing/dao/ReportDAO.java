package com.buu297.billing.dao;

import com.buu297.billing.model.BillingReportSummary;
import java.time.LocalDate;

public interface ReportDAO {
    BillingReportSummary generateSummary(String semesterId, String department, String status, LocalDate fromDate, LocalDate toDate);
}
