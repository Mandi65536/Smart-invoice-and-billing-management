package org.example;

import java.time.LocalDateTime;

public class Payment {

    private int paymentId;
    private int invoiceId;
    private double amount;
    private String paymentMethod;
    private LocalDateTime paymentDate;
    private String status;

    public Payment(
            int paymentId,
            int invoiceId,
            double amount,
            String paymentMethod,
            LocalDateTime paymentDate,
            String status) {

        if (invoiceId <= 0) {
            throw new IllegalArgumentException(
                    "Invoice ID must be positive.");
        }

        if (!Double.isFinite(amount) || amount <= 0) {
            throw new IllegalArgumentException(
                    "Payment amount must be positive and finite.");
        }

        if (paymentMethod == null
                || !paymentMethod.matches(
                "CASH|CARD|UPI|BANK_TRANSFER")) {
            throw new IllegalArgumentException(
                    "Invalid payment method.");
        }

        if (paymentDate == null) {
            throw new IllegalArgumentException(
                    "Payment date cannot be null.");
        }

        if (status == null
                || !status.matches("COMPLETED|PENDING|REFUNDED")) {
            throw new IllegalArgumentException(
                    "Invalid payment status.");
        }

        this.paymentId = paymentId;
        this.invoiceId = invoiceId;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.paymentDate = paymentDate;
        this.status = status;
    }

    public int getPaymentId() {
        return paymentId;
    }

    public int getInvoiceId() {
        return invoiceId;
    }

    public double getAmount() {
        return amount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public String getStatus() {
        return status;
    }
}