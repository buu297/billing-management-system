package com.buu297.billing.model;

import java.math.BigDecimal;

public class TransactionResult {
    private final boolean success;
    private final String statusCode;
    private final String message;
    private final String transactionRef;
    private final BigDecimal paidAmount;
    private final BigDecimal previousBalance;
    private final BigDecimal remainingBalance;
    private final BillingStatus updatedStatus;
    private final long executionDurationMs;

    public TransactionResult(boolean success, String statusCode, String message, String transactionRef,
                             BigDecimal paidAmount, BigDecimal previousBalance, BigDecimal remainingBalance,
                             BillingStatus updatedStatus, long executionDurationMs) {
        this.success = success;
        this.statusCode = statusCode;
        this.message = message;
        this.transactionRef = transactionRef;
        this.paidAmount = paidAmount;
        this.previousBalance = previousBalance;
        this.remainingBalance = remainingBalance;
        this.updatedStatus = updatedStatus;
        this.executionDurationMs = executionDurationMs;
    }

    public static TransactionResult failure(String statusCode, String message, BigDecimal currentBalance) {
        return new TransactionResult(false, statusCode, message, null, BigDecimal.ZERO, currentBalance, currentBalance, null, 0);
    }

    public boolean isSuccess() { return success; }
    public String getStatusCode() { return statusCode; }
    public String getMessage() { return message; }
    public String getTransactionRef() { return transactionRef; }
    public BigDecimal getPaidAmount() { return paidAmount; }
    public BigDecimal getPreviousBalance() { return previousBalance; }
    public BigDecimal getRemainingBalance() { return remainingBalance; }
    public BillingStatus getUpdatedStatus() { return updatedStatus; }
    public long getExecutionDurationMs() { return executionDurationMs; }
}
