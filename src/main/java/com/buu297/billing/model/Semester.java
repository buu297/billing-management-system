package com.buu297.billing.model;

import java.time.LocalDate;

public class Semester {
    private String semesterId;
    private String termName;
    private int academicYear;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean active;

    public Semester() {}

    public Semester(String semesterId, String termName, int academicYear, LocalDate startDate, LocalDate endDate, boolean active) {
        this.semesterId = semesterId;
        this.termName = termName;
        this.academicYear = academicYear;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = active;
    }

    public String getSemesterId() { return semesterId; }
    public void setSemesterId(String semesterId) { this.semesterId = semesterId; }

    public String getTermName() { return termName; }
    public void setTermName(String termName) { this.termName = termName; }

    public int getAcademicYear() { return academicYear; }
    public void setAcademicYear(int academicYear) { this.academicYear = academicYear; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Override
    public String toString() {
        return termName;
    }
}
