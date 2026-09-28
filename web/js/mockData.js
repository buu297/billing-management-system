// Mock Data fallback for offline / standalone file:// access
window.MOCK_DATA = {
  students: [
    {
      studentId: "STU202601",
      firstName: "An",
      lastName: "Nguyen Van",
      fullName: "Nguyen Van An",
      email: "an.nguyen@university.edu.vn",
      phone: "+84 901 234 567",
      department: "Computer Science",
      major: "Software Engineering",
      academicYear: 2026,
      status: "ACTIVE"
    },
    {
      studentId: "STU202602",
      firstName: "Bich",
      lastName: "Tran Thi",
      fullName: "Tran Thi Bich",
      email: "bich.tran@university.edu.vn",
      phone: "+84 912 345 678",
      department: "Information Systems",
      major: "Data Analytics",
      academicYear: 2026,
      status: "ACTIVE"
    },
    {
      studentId: "STU202603",
      firstName: "Cuong",
      lastName: "Le Quoc",
      fullName: "Le Quoc Cuong",
      email: "cuong.le@university.edu.vn",
      phone: "+84 923 456 789",
      department: "Computer Science",
      major: "Artificial Intelligence",
      academicYear: 2025,
      status: "ACTIVE"
    },
    {
      studentId: "STU202604",
      firstName: "Duc",
      lastName: "Pham Minh",
      fullName: "Pham Minh Duc",
      email: "duc.pham@university.edu.vn",
      phone: "+84 934 567 890",
      department: "Business Administration",
      major: "International Business",
      academicYear: 2026,
      status: "ACTIVE"
    }
  ],
  bills: [
    {
      billId: "BILL-001",
      invoiceNumber: "INV-2026-00101",
      studentId: "STU202601",
      semesterId: "SEM-2026-FALL",
      semesterName: "Fall Semester 2026",
      totalAmount: 2450.00,
      discountAmount: 200.00,
      finalAmount: 2250.00,
      paidAmount: 2250.00,
      balanceDue: 0.00,
      dueDate: "2026-10-15",
      status: "PAID",
      items: [
        { itemId: "ITEM-101", description: "Core Courses: Algorithms, Architecture, Database Systems (12 credits)", amount: 1800.00, itemTypeName: "Tuition Course Fee" },
        { itemId: "ITEM-102", description: "Computing Lab & Cloud Infrastructure Access", amount: 350.00, itemTypeName: "Laboratory Fee" },
        { itemId: "ITEM-103", description: "Digital Library & ACM/IEEE Portal Access", amount: 100.00, itemTypeName: "Library Fee" },
        { itemId: "ITEM-104", description: "Annual Student Medical and Accident Protection", amount: 200.00, itemTypeName: "Student Health Insurance" },
        { itemId: "ITEM-105", description: "Academic Merit Scholarship Award (Dean List)", amount: -200.00, itemTypeName: "Scholarship / Waiver" }
      ]
    },
    {
      billId: "BILL-002",
      invoiceNumber: "INV-2026-00102",
      studentId: "STU202602",
      semesterId: "SEM-2026-FALL",
      semesterName: "Fall Semester 2026",
      totalAmount: 2100.00,
      discountAmount: 0.00,
      finalAmount: 2100.00,
      paidAmount: 1000.00,
      balanceDue: 1100.00,
      dueDate: "2026-10-15",
      status: "PARTIAL",
      items: [
        { itemId: "ITEM-201", description: "Information Systems Courses (12 credits)", amount: 1700.00, itemTypeName: "Tuition Course Fee" },
        { itemId: "ITEM-202", description: "Data Analytics & BI Workshop Fee", amount: 200.00, itemTypeName: "Laboratory Fee" },
        { itemId: "ITEM-203", description: "Annual Student Medical and Accident Protection", amount: 200.00, itemTypeName: "Student Health Insurance" }
      ]
    },
    {
      billId: "BILL-003",
      invoiceNumber: "INV-2026-00103",
      studentId: "STU202603",
      semesterId: "SEM-2026-FALL",
      semesterName: "Fall Semester 2026",
      totalAmount: 2800.00,
      discountAmount: 500.00,
      finalAmount: 2300.00,
      paidAmount: 0.00,
      balanceDue: 2300.00,
      dueDate: "2026-10-15",
      status: "UNPAID",
      items: [
        { itemId: "ITEM-301", description: "AI Specialization Courses: Deep Learning, NLP (15 credits)", amount: 2200.00, itemTypeName: "Tuition Course Fee" },
        { itemId: "ITEM-302", description: "GPU High-Performance Computing Cluster Lab Fee", amount: 400.00, itemTypeName: "Laboratory Fee" },
        { itemId: "ITEM-303", description: "Annual Student Medical and Accident Protection", amount: 200.00, itemTypeName: "Student Health Insurance" },
        { itemId: "ITEM-304", description: "AI Excellence Fellowship Grant", amount: -500.00, itemTypeName: "Scholarship / Waiver" }
      ]
    },
    {
      billId: "BILL-004",
      invoiceNumber: "INV-2026-00104",
      studentId: "STU202604",
      semesterId: "SEM-2026-FALL",
      semesterName: "Fall Semester 2026",
      totalAmount: 1950.00,
      discountAmount: 0.00,
      finalAmount: 1950.00,
      paidAmount: 0.00,
      balanceDue: 1950.00,
      dueDate: "2026-09-15",
      status: "OVERDUE",
      items: [
        { itemId: "ITEM-401", description: "Business Administration Core Modules (12 credits)", amount: 1650.00, itemTypeName: "Tuition Course Fee" },
        { itemId: "ITEM-402", description: "Campus Amenities & Career Center Placement Support", amount: 100.00, itemTypeName: "Campus Facility Fee" },
        { itemId: "ITEM-403", description: "Annual Student Medical and Accident Protection", amount: 200.00, itemTypeName: "Student Health Insurance" }
      ]
    }
  ],
  payments: [
    {
      paymentId: "PAY-001",
      transactionRef: "TXN-20260905-8812",
      billId: "BILL-001",
      studentId: "STU202601",
      amount: 2250.00,
      paymentMethodName: "Bank Transfer",
      status: "SUCCESS",
      notes: "Tuition full settlement via Vietcombank iBanking",
      paymentDate: "2026-09-05 10:14:22"
    },
    {
      paymentId: "PAY-002",
      transactionRef: "TXN-20260910-3341",
      billId: "BILL-002",
      studentId: "STU202602",
      amount: 1000.00,
      paymentMethodName: "Credit / Debit Card",
      status: "SUCCESS",
      notes: "First installment 50% tuition payment via Visa",
      paymentDate: "2026-09-10 14:32:05"
    }
  ]
};
