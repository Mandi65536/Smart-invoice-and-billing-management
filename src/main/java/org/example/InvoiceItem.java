
package org.example;

public class InvoiceItem {

    private int invoiceItemId;
    private int invoiceId;
    private int productId;
    private int quantity;
    private double unitPrice;
    private double discountPercent;
    private double discountAmount;
    private double lineTotal;

    public InvoiceItem(
            int invoiceItemId,
            int invoiceId,
            int productId,
            int quantity,
            double unitPrice,
            double discountPercent,
            double discountAmount,
            double lineTotal) {

        if (invoiceId < 0 || productId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid invoice ID or product ID.");
        }

        if (quantity <= 0) {
            throw new IllegalArgumentException(
                    "Quantity must be greater than zero.");
        }

        if (!Double.isFinite(unitPrice)
                || unitPrice < 0
                || !Double.isFinite(discountPercent)
                || discountPercent < 0
                || discountPercent > 100
                || !Double.isFinite(discountAmount)
                || discountAmount < 0
                || !Double.isFinite(lineTotal)
                || lineTotal < 0) {
            throw new IllegalArgumentException(
                    "Invalid price, discount, or line total.");
        }

        this.invoiceItemId = invoiceItemId;
        this.invoiceId = invoiceId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.discountPercent = discountPercent;
        this.discountAmount = discountAmount;
        this.lineTotal = lineTotal;
    }

    public int getInvoiceItemId() {
        return invoiceItemId;
    }

    public int getInvoiceId() {
        return invoiceId;
    }

    public int getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public double getDiscountPercent() {
        return discountPercent;
    }

    public double getDiscountAmount() {
        return discountAmount;
    }

    public double getLineTotal() {
        return lineTotal;
    }
}
