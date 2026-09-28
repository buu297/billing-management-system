-- =====================================================================
-- Seed Data: Student Billing Management System
-- Stack: HTML, CSS, JavaScript, Java Swing, MySQL
-- =====================================================================

USE billing_db;

-- 1. Insert Semesters
INSERT INTO semesters (semester_id, term_name, academic_year, start_date, end_date, is_active) VALUES
('SEM-2026-FALL',   'Fall Semester 2026',   2026, '2026-09-01', '2026-12-31', TRUE),
('SEM-2026-SPRING', 'Spring Semester 2026', 2026, '2026-01-15', '2026-05-30', FALSE),
('SEM-2026-SUMMER', 'Summer Term 2026',     2026, '2026-06-15', '2026-08-20', FALSE);

-- 2. Insert Students
INSERT INTO students (student_id, first_name, last_name, email, phone, department, major, academic_year, status) VALUES
('STU202601', 'Nguyen', 'Van An',     'an.nguyen@university.edu.vn',   '+84 901 234 567', 'Computer Science',       'Software Engineering',    2026, 'ACTIVE'),
('STU202602', 'Tran',   'Thi Bich',   'bich.tran@university.edu.vn',   '+84 912 345 678', 'Information Systems',    'Data Analytics',          2026, 'ACTIVE'),
('STU202603', 'Le',     'Quoc Cuong', 'cuong.le@university.edu.vn',    '+84 923 456 789', 'Computer Science',       'Artificial Intelligence', 2025, 'ACTIVE'),
('STU202604', 'Pham',   'Minh Duc',   'duc.pham@university.edu.vn',    '+84 934 567 890', 'Business Administration', 'International Business', 2026, 'ACTIVE'),
('STU202605', 'Hoang',  'Gia Bao',    'bao.hoang@university.edu.vn',   '+84 945 678 901', 'Electrical Engineering',  'Embedded Systems',        2024, 'ACTIVE'),
('STU202606', 'Vu',     'Thanh Ha',   'ha.vu@university.edu.vn',       '+84 956 789 012', 'Computer Science',       'Cybersecurity',           2025, 'ACTIVE'),
('STU202607', 'Dang',   'Tuan Kiet',  'kiet.dang@university.edu.vn',   '+84 967 890 123', 'Business Administration', 'Finance & Banking',       2026, 'ACTIVE'),
('STU202608', 'Do',     'Ngoc Linh',  'linh.do@university.edu.vn',     '+84 978 901 234', 'Information Systems',    'E-Commerce',              2026, 'ACTIVE');

-- 3. Insert Invoices / Bills
INSERT INTO bills (bill_id, invoice_number, student_id, semester_id, total_amount, discount_amount, final_amount, paid_amount, balance_due, due_date, status, version) VALUES
('BILL-001', 'INV-2026-00101', 'STU202601', 'SEM-2026-FALL',   2450.00,  200.00, 2250.00, 2250.00,    0.00, '2026-10-15', 'PAID',    2),
('BILL-002', 'INV-2026-00102', 'STU202602', 'SEM-2026-FALL',   2100.00,    0.00, 2100.00, 1000.00, 1100.00, '2026-10-15', 'PARTIAL', 2),
('BILL-003', 'INV-2026-00103', 'STU202603', 'SEM-2026-FALL',   2800.00,  500.00, 2300.00,    0.00, 2300.00, '2026-10-15', 'UNPAID',  1),
('BILL-004', 'INV-2026-00104', 'STU202604', 'SEM-2026-FALL',   1950.00,    0.00, 1950.00,    0.00, 1950.00, '2026-09-15', 'OVERDUE', 1),
('BILL-005', 'INV-2026-00105', 'STU202605', 'SEM-2026-FALL',   2600.00,  300.00, 2300.00, 1500.00,  800.00, '2026-10-15', 'PARTIAL', 2),
('BILL-006', 'INV-2026-00106', 'STU202606', 'SEM-2026-FALL',   2500.00,    0.00, 2500.00, 2500.00,    0.00, '2026-10-15', 'PAID',    2),
('BILL-007', 'INV-2026-00107', 'STU202607', 'SEM-2026-FALL',   2200.00,    0.00, 2200.00,    0.00, 2200.00, '2026-09-20', 'OVERDUE', 1),
('BILL-008', 'INV-2026-00108', 'STU202608', 'SEM-2026-FALL',   2100.00,  150.00, 1950.00, 1950.00,    0.00, '2026-10-15', 'PAID',    2),
-- Previous Semester Bills (Spring 2026) for historical reporting
('BILL-009', 'INV-2026-00050', 'STU202601', 'SEM-2026-SPRING', 2400.00,    0.00, 2400.00, 2400.00,    0.00, '2026-03-15', 'PAID',    2),
('BILL-010', 'INV-2026-00051', 'STU202603', 'SEM-2026-SPRING', 2500.00,  200.00, 2300.00, 2300.00,    0.00, '2026-03-15', 'PAID',    2);

-- 4. Insert Bill Line Items
INSERT INTO bill_items (item_id, bill_id, item_type, description, amount) VALUES
-- BILL-001 items
('ITEM-101', 'BILL-001', 'TUITION_COURSE',       'Core Courses: Algorithms, Architecture, Database Systems (12 credits)', 1800.00),
('ITEM-102', 'BILL-001', 'LAB_FEE',              'Computing Lab & Cloud Infrastructure Access',                            350.00),
('ITEM-103', 'BILL-001', 'LIBRARY_FEE',          'Digital Library & ACM/IEEE Portal Access',                              100.00),
('ITEM-104', 'BILL-001', 'HEALTH_INSURANCE',     'Annual Student Medical and Accident Protection',                        200.00),
('ITEM-105', 'BILL-001', 'SCHOLARSHIP_DISCOUNT', 'Academic Merit Scholarship Award (Dean List)',                         -200.00),

-- BILL-002 items
('ITEM-201', 'BILL-002', 'TUITION_COURSE',       'Information Systems Courses (12 credits)',                             1700.00),
('ITEM-202', 'BILL-002', 'LAB_FEE',              'Data Analytics & BI Workshop Fee',                                       200.00),
('ITEM-203', 'BILL-002', 'HEALTH_INSURANCE',     'Annual Student Medical and Accident Protection',                        200.00),

-- BILL-003 items
('ITEM-301', 'BILL-003', 'TUITION_COURSE',       'AI Specialization Courses: Deep Learning, NLP (15 credits)',            2200.00),
('ITEM-302', 'BILL-003', 'LAB_FEE',              'GPU High-Performance Computing Cluster Lab Fee',                         400.00),
('ITEM-303', 'BILL-003', 'HEALTH_INSURANCE',     'Annual Student Medical and Accident Protection',                        200.00),
('ITEM-304', 'BILL-003', 'SCHOLARSHIP_DISCOUNT', 'AI Excellence Fellowship Grant',                                       -500.00),

-- BILL-004 items (OVERDUE)
('ITEM-401', 'BILL-004', 'TUITION_COURSE',       'Business Administration Core Modules (12 credits)',                    1650.00),
('ITEM-402', 'BILL-004', 'CAMPUS_FACILITY',      'Campus Amenities & Career Center Placement Support',                    100.00),
('ITEM-403', 'BILL-004', 'HEALTH_INSURANCE',     'Annual Student Medical and Accident Protection',                        200.00),

-- BILL-005 items
('ITEM-501', 'BILL-005', 'TUITION_COURSE',       'Electrical Engineering Modules (14 credits)',                           1950.00),
('ITEM-502', 'BILL-005', 'LAB_FEE',              'Microcontroller & Hardware Fabrication Lab',                             450.00),
('ITEM-503', 'BILL-005', 'HEALTH_INSURANCE',     'Annual Student Medical and Accident Protection',                        200.00),
('ITEM-504', 'BILL-005', 'SCHOLARSHIP_DISCOUNT', 'STEM Talent Subsidy',                                                  -300.00);

-- 5. Insert Payment Ledger Records
INSERT INTO payments (payment_id, transaction_ref, bill_id, student_id, amount, payment_method, status, notes, payment_date) VALUES
('PAY-001', 'TXN-20260905-8812', 'BILL-001', 'STU202601', 2250.00, 'BANK_TRANSFER', 'SUCCESS', 'Tuition full settlement via Vietcombank iBanking', '2026-09-05 10:14:22'),
('PAY-002', 'TXN-20260910-3341', 'BILL-002', 'STU202602', 1000.00, 'CREDIT_CARD',   'SUCCESS', 'First installment 50% tuition payment via Visa',     '2026-09-10 14:32:05'),
('PAY-003', 'TXN-20260912-9904', 'BILL-005', 'STU202605', 1500.00, 'VNPAY_QR',      'SUCCESS', 'Tuition installment via VNPay QR mobile app',         '2026-09-12 16:45:18'),
('PAY-004', 'TXN-20260908-1120', 'BILL-006', 'STU202606', 2500.00, 'CASH',          'SUCCESS', 'Full cash payment at university Bursar counter',      '2026-09-08 09:20:11'),
('PAY-005', 'TXN-20260914-7749', 'BILL-008', 'STU202608', 1950.00, 'CREDIT_CARD',   'SUCCESS', 'Full online payment with Mastercard',                 '2026-09-14 11:05:43');

-- 6. Insert Initial Audit Logs
INSERT INTO audit_logs (entity_name, entity_id, action, details, performed_by) VALUES
('SYSTEM', 'INIT', 'DATABASE_SEED', 'Initial system seeding completed with 8 students and 10 bills.', 'SETUP_SCRIPT');
