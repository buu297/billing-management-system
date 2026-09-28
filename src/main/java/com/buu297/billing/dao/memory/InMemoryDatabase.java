package com.buu297.billing.dao.memory;

import com.buu297.billing.model.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryDatabase {
    private static final InMemoryDatabase INSTANCE = new InMemoryDatabase();

    public static InMemoryDatabase getInstance() {
        return INSTANCE;
    }

    public final Map<String, Student> students = new ConcurrentHashMap<>();
    public final Map<String, Semester> semesters = new ConcurrentHashMap<>();
    public final Map<String, Bill> bills = new ConcurrentHashMap<>();
    public final Map<String, List<BillItem>> billItems = new ConcurrentHashMap<>();
    public final List<Payment> payments = Collections.synchronizedList(new ArrayList<>());
    public final List<AuditLog> auditLogs = Collections.synchronizedList(new ArrayList<>());

    private final ConcurrentHashMap<String, Object> billRowLocks = new ConcurrentHashMap<>();

    private InMemoryDatabase() {
        seedInitialData();
    }

    public Object getRowLock(String billId) {
        return billRowLocks.computeIfAbsent(billId, k -> new Object());
    }

    private void seedInitialData() {
        semesters.put("SEM-2026-FALL", new Semester("SEM-2026-FALL", "Fall Semester 2026", 2026, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 12, 31), true));
        semesters.put("SEM-2026-SPRING", new Semester("SEM-2026-SPRING", "Spring Semester 2026", 2026, LocalDate.of(2026, 1, 15), LocalDate.of(2026, 5, 30), false));
        semesters.put("SEM-2026-SUMMER", new Semester("SEM-2026-SUMMER", "Summer Term 2026", 2026, LocalDate.of(2026, 6, 15), LocalDate.of(2026, 8, 20), false));

        addStudent(new Student("STU202601", "An", "Nguyen Van", "an.nguyen@university.edu.vn", "+84 901 234 567", "Computer Science", "Software Engineering", 2026, "ACTIVE"));
        addStudent(new Student("STU202602", "Bich", "Tran Thi", "bich.tran@university.edu.vn", "+84 912 345 678", "Information Systems", "Data Analytics", 2026, "ACTIVE"));
        addStudent(new Student("STU202603", "Cuong", "Le Quoc", "cuong.le@university.edu.vn", "+84 923 456 789", "Computer Science", "Artificial Intelligence", 2025, "ACTIVE"));
        addStudent(new Student("STU202604", "Duc", "Pham Minh", "duc.pham@university.edu.vn", "+84 934 567 890", "Business Administration", "International Business", 2026, "ACTIVE"));
        addStudent(new Student("STU202605", "Bao", "Hoang Gia", "bao.hoang@university.edu.vn", "+84 945 678 901", "Electrical Engineering", "Embedded Systems", 2024, "ACTIVE"));
        addStudent(new Student("STU202606", "Ha", "Vu Thanh", "ha.vu@university.edu.vn", "+84 956 789 012", "Computer Science", "Cybersecurity", 2025, "ACTIVE"));
        addStudent(new Student("STU202607", "Kiet", "Dang Tuan", "kiet.dang@university.edu.vn", "+84 967 890 123", "Business Administration", "Finance & Banking", 2026, "ACTIVE"));
        addStudent(new Student("STU202608", "Linh", "Do Ngoc", "linh.do@university.edu.vn", "+84 978 901 234", "Information Systems", "E-Commerce", 2026, "ACTIVE"));

        createInitialBill("BILL-001", "INV-2026-00101", "STU202601", "SEM-2026-FALL", new BigDecimal("2450.00"), new BigDecimal("200.00"), new BigDecimal("2250.00"), LocalDate.of(2026, 10, 15), BillingStatus.PAID,
                Arrays.asList(
                        new BillItem("ITEM-101", "BILL-001", ItemType.TUITION_COURSE, "Core Courses: Algorithms, Architecture, Database Systems (12 credits)", new BigDecimal("1800.00")),
                        new BillItem("ITEM-102", "BILL-001", ItemType.LAB_FEE, "Computing Lab & Cloud Infrastructure Access", new BigDecimal("350.00")),
                        new BillItem("ITEM-103", "BILL-001", ItemType.LIBRARY_FEE, "Digital Library & ACM/IEEE Portal Access", new BigDecimal("100.00")),
                        new BillItem("ITEM-104", "BILL-001", ItemType.HEALTH_INSURANCE, "Annual Student Medical and Accident Protection", new BigDecimal("200.00")),
                        new BillItem("ITEM-105", "BILL-001", ItemType.SCHOLARSHIP_DISCOUNT, "Academic Merit Scholarship Award (Dean List)", new BigDecimal("-200.00"))
                ));

        createInitialBill("BILL-002", "INV-2026-00102", "STU202602", "SEM-2026-FALL", new BigDecimal("2100.00"), BigDecimal.ZERO, new BigDecimal("1000.00"), LocalDate.of(2026, 10, 15), BillingStatus.PARTIAL,
                Arrays.asList(
                        new BillItem("ITEM-201", "BILL-002", ItemType.TUITION_COURSE, "Information Systems Courses (12 credits)", new BigDecimal("1700.00")),
                        new BillItem("ITEM-202", "BILL-002", ItemType.LAB_FEE, "Data Analytics & BI Workshop Fee", new BigDecimal("200.00")),
                        new BillItem("ITEM-203", "BILL-002", ItemType.HEALTH_INSURANCE, "Annual Student Medical and Accident Protection", new BigDecimal("200.00"))
                ));

        createInitialBill("BILL-003", "INV-2026-00103", "STU202603", "SEM-2026-FALL", new BigDecimal("2800.00"), new BigDecimal("500.00"), BigDecimal.ZERO, LocalDate.of(2026, 10, 15), BillingStatus.UNPAID,
                Arrays.asList(
                        new BillItem("ITEM-301", "BILL-003", ItemType.TUITION_COURSE, "AI Specialization Courses: Deep Learning, NLP (15 credits)", new BigDecimal("2200.00")),
                        new BillItem("ITEM-302", "BILL-003", ItemType.LAB_FEE, "GPU High-Performance Computing Cluster Lab Fee", new BigDecimal("400.00")),
                        new BillItem("ITEM-303", "BILL-003", ItemType.HEALTH_INSURANCE, "Annual Student Medical and Accident Protection", new BigDecimal("200.00")),
                        new BillItem("ITEM-304", "BILL-003", ItemType.SCHOLARSHIP_DISCOUNT, "AI Excellence Fellowship Grant", new BigDecimal("-500.00"))
                ));

        createInitialBill("BILL-004", "INV-2026-00104", "STU202604", "SEM-2026-FALL", new BigDecimal("1950.00"), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.of(2026, 9, 15), BillingStatus.OVERDUE,
                Arrays.asList(
                        new BillItem("ITEM-401", "BILL-004", ItemType.TUITION_COURSE, "Business Administration Core Modules (12 credits)", new BigDecimal("1650.00")),
                        new BillItem("ITEM-402", "BILL-004", ItemType.CAMPUS_FACILITY, "Campus Amenities & Career Center Placement Support", new BigDecimal("100.00")),
                        new BillItem("ITEM-403", "BILL-004", ItemType.HEALTH_INSURANCE, "Annual Student Medical and Accident Protection", new BigDecimal("200.00"))
                ));

        createInitialBill("BILL-005", "INV-2026-00105", "STU202605", "SEM-2026-FALL", new BigDecimal("2600.00"), new BigDecimal("300.00"), new BigDecimal("1500.00"), LocalDate.of(2026, 10, 15), BillingStatus.PARTIAL,
                Arrays.asList(
                        new BillItem("ITEM-501", "BILL-005", ItemType.TUITION_COURSE, "Electrical Engineering Modules (14 credits)", new BigDecimal("1950.00")),
                        new BillItem("ITEM-502", "BILL-005", ItemType.LAB_FEE, "Microcontroller & Hardware Fabrication Lab", new BigDecimal("450.00")),
                        new BillItem("ITEM-503", "BILL-005", ItemType.HEALTH_INSURANCE, "Annual Student Medical and Accident Protection", new BigDecimal("200.00")),
                        new BillItem("ITEM-504", "BILL-005", ItemType.SCHOLARSHIP_DISCOUNT, "STEM Talent Subsidy", new BigDecimal("-300.00"))
                ));

        createInitialBill("BILL-006", "INV-2026-00106", "STU202606", "SEM-2026-FALL", new BigDecimal("2500.00"), BigDecimal.ZERO, new BigDecimal("2500.00"), LocalDate.of(2026, 10, 15), BillingStatus.PAID,
                Collections.singletonList(
                        new BillItem("ITEM-601", "BILL-006", ItemType.TUITION_COURSE, "Cybersecurity Core & Ethical Hacking Lab (12 credits)", new BigDecimal("2500.00"))
                ));

        createInitialBill("BILL-007", "INV-2026-00107", "STU202607", "SEM-2026-FALL", new BigDecimal("2200.00"), BigDecimal.ZERO, BigDecimal.ZERO, LocalDate.of(2026, 9, 20), BillingStatus.OVERDUE,
                Collections.singletonList(
                        new BillItem("ITEM-701", "BILL-007", ItemType.TUITION_COURSE, "Finance & Banking Modules (12 credits)", new BigDecimal("2200.00"))
                ));

        createInitialBill("BILL-008", "INV-2026-00108", "STU202608", "SEM-2026-FALL", new BigDecimal("2100.00"), new BigDecimal("150.00"), new BigDecimal("1950.00"), LocalDate.of(2026, 10, 15), BillingStatus.PAID,
                Collections.singletonList(
                        new BillItem("ITEM-801", "BILL-008", ItemType.TUITION_COURSE, "E-Commerce Systems (12 credits)", new BigDecimal("1950.00"))
                ));

        payments.add(new Payment("PAY-001", "TXN-20260905-8812", "BILL-001", "STU202601", new BigDecimal("2250.00"), PaymentMethod.BANK_TRANSFER, "SUCCESS", "Tuition full settlement via Vietcombank iBanking"));
        payments.add(new Payment("PAY-002", "TXN-20260910-3341", "BILL-002", "STU202602", new BigDecimal("1000.00"), PaymentMethod.CREDIT_CARD, "SUCCESS", "First installment 50% tuition payment via Visa"));
        payments.add(new Payment("PAY-003", "TXN-20260912-9904", "BILL-005", "STU202605", new BigDecimal("1500.00"), PaymentMethod.VNPAY_QR, "SUCCESS", "Tuition installment via VNPay QR mobile app"));
        payments.add(new Payment("PAY-004", "TXN-20260908-1120", "BILL-006", "STU202606", new BigDecimal("2500.00"), PaymentMethod.CASH, "SUCCESS", "Full cash payment at university Bursar counter"));
        payments.add(new Payment("PAY-005", "TXN-20260914-7749", "BILL-008", "STU202608", new BigDecimal("1950.00"), PaymentMethod.CREDIT_CARD, "SUCCESS", "Full online payment with Mastercard"));

        for (Payment p : payments) {
            Student s = students.get(p.getStudentId());
            if (s != null) p.setStudentName(s.getFullName());
        }
    }

    private void addStudent(Student s) {
        students.put(s.getStudentId(), s);
    }

    private void createInitialBill(String billId, String inv, String studentId, String semesterId,
                                  BigDecimal total, BigDecimal discount, BigDecimal paid,
                                  LocalDate dueDate, BillingStatus status, List<BillItem> items) {
        Bill b = new Bill(billId, inv, studentId, semesterId, total, discount, paid, dueDate, status);
        Student s = students.get(studentId);
        if (s != null) {
            b.setStudentName(s.getFullName());
            b.setDepartment(s.getDepartment());
        }
        Semester sem = semesters.get(semesterId);
        if (sem != null) {
            b.setSemesterName(sem.getTermName());
        }
        b.setItems(new ArrayList<>(items));
        bills.put(billId, b);
        billItems.put(billId, new ArrayList<>(items));
    }

    public void addAuditLog(AuditLog log) {
        auditLogs.add(log);
    }

    public List<AuditLog> getAuditLogs() {
        return new ArrayList<>(auditLogs);
    }
}