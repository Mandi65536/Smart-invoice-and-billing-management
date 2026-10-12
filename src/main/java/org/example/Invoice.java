
package org.example;

import java.time.LocalDate;

public class Invoice {

    private int invoiceId;
    private String invoiceNumber;
    private int customerId;
    private LocalDate invoiceDate;
    private double subtotal;
    private String discountType;
    private double discountValue;
    private double discountAmount;
    private double taxAmount;
    private double grandTotal;
    private String status;


    public Invoice(
            int invoiceId,
            String invoiceNumber,
            int customerId,
            LocalDate invoiceDate,
            double subtotal,
            String discountType,
            double discountValue,
            double discountAmount,
            double taxAmount,
            double grandTotal,
            String status) {

        this.invoiceId = invoiceId;
        this.invoiceNumber = invoiceNumber;
        this.customerId = customerId;
        this.invoiceDate = invoiceDate;
        this.subtotal = subtotal;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.discountAmount = discountAmount;
        this.taxAmount = taxAmount;
        this.grandTotal = grandTotal;
        this.status = status;
    }

    public int getInvoiceId() {
        return invoiceId;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public int getCustomerId() {
        return customerId;
    }

    public LocalDate getInvoiceDate() {
        return invoiceDate;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public String getDiscountType() {
        return discountType;
    }

    public double getDiscountValue() {
        return discountValue;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public double getTaxAmount() {
        return taxAmount;
    }

    public double getGrandTotal() {
        return grandTotal;
    }

    public String getStatus() {
        return status;
    }

}
