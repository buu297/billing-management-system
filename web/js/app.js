const API_BASE = window.location.protocol.startsWith("http") ? "" : null;

let currentStudentId = "STU202602"; // Default to student with partial balance
let studentsList = [];
let studentBills = [];
let studentPayments = [];
let activePayingBill = null;

document.addEventListener("DOMContentLoaded", async () => {
  await loadStudents();
  setupEventListeners();
});

async function loadStudents() {
  try {
    if (API_BASE !== null) {
      const res = await fetch("/api/students");
      if (res.ok) {
        studentsList = await res.json();
      }
    }
  } catch (e) {
    console.warn("Using offline mock data for students:", e);
  }

  if (!studentsList || studentsList.length === 0) {
    studentsList = window.MOCK_DATA.students;
  }

  const select = document.getElementById("studentSelect");
  select.innerHTML = "";
  studentsList.forEach(s => {
    const opt = document.createElement("option");
    opt.value = s.studentId;
    opt.textContent = `${s.fullName} (${s.studentId})`;
    if (s.studentId === currentStudentId) opt.selected = true;
    select.appendChild(opt);
  });

  select.addEventListener("change", (e) => {
    currentStudentId = e.target.value;
    loadStudentData();
  });

  await loadStudentData();
}

async function loadStudentData() {
  const currentStudent = studentsList.find(s => s.studentId === currentStudentId) || studentsList[0];
  
  // Update banner
  document.getElementById("studentName").textContent = currentStudent.fullName;
  document.getElementById("studentIdDisplay").textContent = currentStudent.studentId;
  document.getElementById("studentDept").textContent = currentStudent.department;
  document.getElementById("studentMajor").textContent = currentStudent.major;
  document.getElementById("studentEmail").textContent = currentStudent.email;

  // Load Bills
  try {
    if (API_BASE !== null) {
      const res = await fetch(`/api/bills?studentId=${currentStudentId}`);
      if (res.ok) {
        studentBills = await res.json();
      }
      const payRes = await fetch(`/api/payments?studentId=${currentStudentId}`);
      if (payRes.ok) {
        studentPayments = await payRes.json();
      }
    }
  } catch (e) {
    console.warn("Using offline mock bills:", e);
  }

  if (!studentBills || studentBills.length === 0) {
    studentBills = window.MOCK_DATA.bills.filter(b => b.studentId === currentStudentId);
  }
  if (!studentPayments) {
    studentPayments = window.MOCK_DATA.payments.filter(p => p.studentId === currentStudentId);
  }

  renderMetrics();
  renderInvoices();
  renderPayments();
}

function renderMetrics() {
  let totalBilled = 0;
  let totalPaid = 0;
  let totalOutstanding = 0;

  studentBills.forEach(b => {
    totalBilled += parseFloat(b.finalAmount || 0);
    totalPaid += parseFloat(b.paidAmount || 0);
    totalOutstanding += parseFloat(b.balanceDue || 0);
  });

  document.getElementById("metricOutstanding").textContent = "$" + formatMoney(totalOutstanding);
  document.getElementById("metricBilled").textContent = "$" + formatMoney(totalBilled);
  document.getElementById("metricPaid").textContent = "$" + formatMoney(totalPaid);

  const statusPill = document.getElementById("overallStatusPill");
  if (totalOutstanding === 0 && totalBilled > 0) {
    statusPill.textContent = "ACCOUNT SETTLED";
    statusPill.className = "badge badge-PAID";
  } else if (totalPaid > 0) {
    statusPill.textContent = "PARTIAL BALANCE";
    statusPill.className = "badge badge-PARTIAL";
  } else {
    statusPill.textContent = "UNPAID BALANCE";
    statusPill.className = "badge badge-UNPAID";
  }
}

function renderInvoices() {
  const container = document.getElementById("invoicesGrid");
  container.innerHTML = "";

  if (studentBills.length === 0) {
    container.innerHTML = `<p style="color:var(--text-muted); padding:20px;">No tuition invoices found for this term.</p>`;
    return;
  }

  studentBills.forEach(bill => {
    const card = document.createElement("div");
    const statusClass = (bill.status || "UNPAID").toLowerCase();
    card.className = `invoice-card ${statusClass}`;

    const isPaid = (bill.status === "PAID" || parseFloat(bill.balanceDue) <= 0);

    card.innerHTML = `
      <div class="inv-top">
        <div>
          <div class="inv-no">${bill.invoiceNumber}</div>
          <div class="inv-term">${bill.semesterName || bill.semesterId}</div>
        </div>
        <span class="badge badge-${bill.status}">${bill.statusDisplayName || bill.status}</span>
      </div>
      <div class="inv-body">
        <div class="inv-row">
          <span>Gross Billed:</span>
          <span>$${formatMoney(bill.totalAmount)}</span>
        </div>
        <div class="inv-row">
          <span>Scholarship / Waiver:</span>
          <span style="color:var(--success);">-$${formatMoney(bill.discountAmount)}</span>
        </div>
        <div class="inv-row">
          <span>Net Payable:</span>
          <strong>$${formatMoney(bill.finalAmount)}</strong>
        </div>
        <div class="inv-row">
          <span>Paid So Far:</span>
          <span style="color:var(--success); font-weight:600;">$${formatMoney(bill.paidAmount)}</span>
        </div>
        <div class="inv-row highlight">
          <span>Remaining Balance:</span>
          <span style="color:${isPaid ? 'var(--success)' : 'var(--danger)'};">$${formatMoney(bill.balanceDue)}</span>
        </div>
        <div class="due-notice ${bill.status === 'OVERDUE' ? 'alert' : ''}">
          ${bill.status === 'OVERDUE' ? '⚠️ Payment Overdue! Due date was:' : 'Payment Due Date:'} 
          <strong>${bill.dueDate || 'N/A'}</strong>
        </div>
      </div>
      <div class="inv-actions">
        <button class="btn btn-secondary" onclick="openBreakdownModal('${bill.billId}')">Details</button>
        <button class="btn btn-secondary" onclick="printInvoice('${bill.billId}')">Invoice</button>
        ${!isPaid ? `<button class="btn btn-primary" onclick="openPaymentModal('${bill.billId}')">💳 Pay Now</button>` : ''}
      </div>
    `;
    container.appendChild(card);
  });
}

function renderPayments() {
  const tbody = document.getElementById("paymentsTableBody");
  tbody.innerHTML = "";

  if (studentPayments.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" style="text-align:center; color:var(--text-muted);">No payment transactions found.</td></tr>`;
    return;
  }

  studentPayments.forEach(p => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td><code>${p.transactionRef}</code></td>
      <td>${p.billId}</td>
      <td><strong>$${formatMoney(p.amount)}</strong></td>
      <td>${p.paymentMethodName || p.paymentMethod}</td>
      <td><span class="badge badge-PAID">${p.status}</span></td>
      <td>${p.paymentDate ? p.paymentDate.replace("T", " ") : "N/A"}</td>
    `;
    tbody.appendChild(tr);
  });
}

function openPaymentModal(billId) {
  activePayingBill = studentBills.find(b => b.billId === billId);
  if (!activePayingBill) return;

  document.getElementById("payInvNumber").textContent = activePayingBill.invoiceNumber;
  document.getElementById("payBalanceDue").textContent = "$" + formatMoney(activePayingBill.balanceDue);
  document.getElementById("payAmountInput").value = activePayingBill.balanceDue;
  document.getElementById("qrAmount").textContent = "$" + formatMoney(activePayingBill.balanceDue);
  document.getElementById("qrRef").textContent = "TUITION-" + activePayingBill.invoiceNumber;

  document.getElementById("paymentModal").classList.add("active");
}

function closePaymentModal() {
  document.getElementById("paymentModal").classList.remove("active");
  activePayingBill = null;
}

function openBreakdownModal(billId) {
  const bill = studentBills.find(b => b.billId === billId);
  if (!bill) return;

  document.getElementById("breakdownInvNumber").textContent = bill.invoiceNumber;
  const tbody = document.getElementById("breakdownItemsBody");
  tbody.innerHTML = "";

  const items = bill.items || [];
  items.forEach(it => {
    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>${it.itemTypeName || it.itemType || 'Fee'}</td>
      <td>${it.description}</td>
      <td style="text-align:right; font-weight:600;">$${formatMoney(it.amount)}</td>
    `;
    tbody.appendChild(tr);
  });

  document.getElementById("breakdownModal").classList.add("active");
}

function closeBreakdownModal() {
  document.getElementById("breakdownModal").classList.remove("active");
}

async function submitPayment() {
  if (!activePayingBill) return;

  const amt = parseFloat(document.getElementById("payAmountInput").value);
  if (isNaN(amt) || amt <= 0) {
    alert("Please enter a valid payment amount greater than zero.");
    return;
  }

  const activeTab = document.querySelector(".method-tab.active").dataset.method;
  const notes = document.getElementById("payNotesInput").value || "Student portal web payment";

  const btn = document.getElementById("btnSubmitPay");
  btn.disabled = true;
  btn.textContent = "Processing Transaction...";

  try {
    if (API_BASE !== null) {
      const res = await fetch("/api/payments", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          billId: activePayingBill.billId,
          studentId: currentStudentId,
          amount: amt,
          method: activeTab,
          notes: notes
        })
      });
      const data = await res.json();
      if (data.success) {
        showSuccessModal(data);
      } else {
        alert("Payment Error: " + data.message);
      }
    } else {
      // Mock payment fallback
      const ref = "TXN-" + Date.now().toString().slice(-6);
      activePayingBill.paidAmount = parseFloat(activePayingBill.paidAmount) + amt;
      activePayingBill.balanceDue = Math.max(0, parseFloat(activePayingBill.balanceDue) - amt);
      if (activePayingBill.balanceDue === 0) activePayingBill.status = "PAID";
      else activePayingBill.status = "PARTIAL";

      studentPayments.unshift({
        paymentId: "PAY-" + Math.floor(Math.random()*1000),
        transactionRef: ref,
        billId: activePayingBill.billId,
        studentId: currentStudentId,
        amount: amt,
        paymentMethodName: activeTab,
        status: "SUCCESS",
        paymentDate: new Date().toISOString()
      });

      showSuccessModal({
        transactionRef: ref,
        paidAmount: amt,
        remainingBalance: activePayingBill.balanceDue,
        executionDurationMs: 14
      });
    }
  } catch (err) {
    alert("Network error: " + err.message);
  } finally {
    btn.disabled = false;
    btn.textContent = "Confirm & Submit Payment";
    closePaymentModal();
    loadStudentData();
  }
}

function showSuccessModal(data) {
  document.getElementById("successRef").textContent = data.transactionRef;
  document.getElementById("successAmount").textContent = "$" + formatMoney(data.paidAmount);
  document.getElementById("successRemaining").textContent = "$" + formatMoney(data.remainingBalance);
  document.getElementById("successDuration").textContent = data.executionDurationMs + " ms";
  document.getElementById("successModal").classList.add("active");
}

function closeSuccessModal() {
  document.getElementById("successModal").classList.remove("active");
}

function printInvoice(billId) {
  const bill = studentBills.find(b => b.billId === billId);
  if (!bill) return;

  const currentStudent = studentsList.find(s => s.studentId === currentStudentId);

  const printWindow = window.open('', '_blank');
  printWindow.document.write(`
    <!DOCTYPE html>
    <html>
    <head>
      <title>Tuition Invoice - ${bill.invoiceNumber}</title>
      <style>
        body { font-family: 'Segoe UI', Arial, sans-serif; padding: 40px; color: #0f172a; }
        .header { display: flex; justify-content: space-between; border-bottom: 2px solid #2563eb; padding-bottom: 20px; }
        h1 { margin: 0; color: #1e3a8a; font-size: 24px; }
        table { width: 100%; border-collapse: collapse; margin-top: 25px; }
        th, td { border-bottom: 1px solid #e2e8f0; padding: 10px; text-align: left; }
        th { background: #f8fafc; }
        .total-box { margin-top: 20px; width: 300px; margin-left: auto; line-height: 1.8; }
        .badge { background: #dcfce7; color: #15803d; padding: 4px 10px; border-radius: 9999px; font-weight: bold; }
      </style>
    </head>
    <body onload="window.print()">
      <div class="header">
        <div>
          <h1>STUDENT BILLING INVOICE</h1>
          <p>University Financial Services & Bursar</p>
        </div>
        <div style="text-align:right;">
          <p><strong>Invoice:</strong> ${bill.invoiceNumber}</p>
          <p><strong>Date:</strong> ${new Date().toLocaleDateString()}</p>
          <p><strong>Status:</strong> <span class="badge">${bill.status}</span></p>
        </div>
      </div>
      <p style="margin-top:20px;">
        <strong>Student:</strong> ${currentStudent ? currentStudent.fullName : ''} (${bill.studentId})<br>
        <strong>Term:</strong> ${bill.semesterName || bill.semesterId}<br>
        <strong>Due Date:</strong> ${bill.dueDate || 'N/A'}
      </p>
      <table>
        <thead><tr><th>Fee Category</th><th>Description</th><th style="text-align:right;">Amount</th></tr></thead>
        <tbody>
          ${(bill.items || []).map(it => `<tr><td>${it.itemTypeName || 'Fee'}</td><td>${it.description}</td><td style="text-align:right;">$${formatMoney(it.amount)}</td></tr>`).join('')}
        </tbody>
      </table>
      <div class="total-box">
        <div>Gross Amount: <strong>$${formatMoney(bill.totalAmount)}</strong></div>
        <div>Scholarship Waiver: <strong>-$${formatMoney(bill.discountAmount)}</strong></div>
        <div>Net Payable: <strong>$${formatMoney(bill.finalAmount)}</strong></div>
        <div>Amount Paid: <strong>$${formatMoney(bill.paidAmount)}</strong></div>
        <div style="font-size:16px; border-top:1px solid #cbd5e1; padding-top:6px;">Remaining Balance: <strong>$${formatMoney(bill.balanceDue)}</strong></div>
      </div>
    </body>
    </html>
  `);
  printWindow.document.close();
}

function setupEventListeners() {
  document.querySelectorAll(".method-tab").forEach(tab => {
    tab.addEventListener("click", () => {
      document.querySelectorAll(".method-tab").forEach(t => t.classList.remove("active"));
      tab.classList.add("active");

      const method = tab.dataset.method;
      document.getElementById("qrSection").style.display = (method === "VNPAY_QR") ? "block" : "none";
      document.getElementById("cardSection").style.display = (method === "CREDIT_CARD") ? "block" : "none";
      document.getElementById("bankSection").style.display = (method === "BANK_TRANSFER") ? "block" : "none";
    });
  });
}

function formatMoney(num) {
  return parseFloat(num || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}
