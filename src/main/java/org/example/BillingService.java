package org.example;

import java.math.BigDecimal;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


public class BillingService {

    private final InvoiceDAO invoiceDAO;
    private final InvoiceItemDAO invoiceItemDAO;
    private final PaymentDAO paymentDAO;
    private final ProductDAO productDAO;

    public BillingService() {
        this.invoiceDAO = new InvoiceDAO();
        this.invoiceItemDAO = new InvoiceItemDAO();
        this.paymentDAO = new PaymentDAO();
        this.productDAO = new ProductDAO();
    }

    // Check whether a product has enough stock
    public boolean hasSufficientStock(
            String sku, int requestedQuantity) {

        if (sku == null || sku.isBlank()
                || requestedQuantity <= 0) {
            return false;
        }

        Product product = productDAO.getProductBySku(sku);

        return product != null
                && product.getCurrentStock() >= requestedQuantity;
    }

    // Calculate a percentage discount
    public BigDecimal calculateDiscount(
            BigDecimal subtotal, BigDecimal discountPercent) {

        return InvoiceCalculator.calculatePercentageDiscount(
                subtotal, discountPercent);
    }

    // Calculate the invoice grand total
    public BigDecimal calculateTotal(
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal taxAmount) {

        return InvoiceCalculator.calculateGrandTotal(
                subtotal, discountAmount, taxAmount);
    }


    // Calculate the discount amount and final invoice total
    public BigDecimal[] calculateInvoiceAmounts(
            BigDecimal subtotal,
            BigDecimal discountPercent,
            BigDecimal taxAmount) {

        if (subtotal == null
                || discountPercent == null
                || taxAmount == null) {
            throw new IllegalArgumentException(
                    "Invoice amounts cannot be null.");
        }

        BigDecimal discountAmount =
                calculateDiscount(subtotal, discountPercent);

        BigDecimal grandTotal =
                calculateTotal(
                        subtotal,
                        discountAmount,
                        taxAmount
                );

        return new BigDecimal[]{
                discountAmount,
                grandTotal
        };
    }

    // Calculate and save totals for a draft invoice
    public boolean updateDraftInvoiceTotals(
            int invoiceId,
            BigDecimal discountPercent,
            BigDecimal taxAmount) {

        if (invoiceId <= 0
                || discountPercent == null
                || taxAmount == null) {
            return false;
        }

        Invoice invoice = invoiceDAO.getInvoiceById(invoiceId);

        if (invoice == null
                || !"DRAFT".equals(invoice.getStatus())) {
            return false;
        }

        BigDecimal subtotal =
                invoiceItemDAO.calculateSubtotal(invoiceId);

        BigDecimal discountAmount =
                calculateDiscount(subtotal, discountPercent);

        BigDecimal grandTotal =
                calculateTotal(subtotal, discountAmount, taxAmount);

        return invoiceDAO.updateInvoiceTotals(
                invoiceId,
                subtotal,
                discountPercent,
                discountAmount,
                taxAmount,
                grandTotal
        );
    }

    public boolean completeInvoice(int invoiceId) {
        if (invoiceId <= 0) {
            System.out.println("Invalid invoice ID.");
            return false;
        }

        String invoiceQuery =
                "SELECT grand_total, status FROM invoices " +
                        "WHERE invoice_id = ? FOR UPDATE";

        String paymentQuery =
                "SELECT COALESCE(SUM(amount), 0) AS paid " +
                        "FROM payments " +
                        "WHERE invoice_id = ? AND payment_status = 'COMPLETED'";

        String itemQuery =
                "SELECT product_id, SUM(quantity) AS total_quantity " +
                        "FROM invoice_items WHERE invoice_id = ? " +
                        "GROUP BY product_id ORDER BY product_id";

        String stockQuery =
                "UPDATE products SET current_stock = current_stock - ? " +
                        "WHERE product_id = ? AND current_stock >= ?";

        String movementQuery =
                "INSERT INTO stock_movements " +
                        "(product_id, movement_type, quantity, reference_type, " +
                        "reference_id, notes, movement_date) " +
                        "VALUES (?, 'OUT', ?, 'SALE', ?, ?, ?)";

        String completeQuery =
                "UPDATE invoices SET status = 'COMPLETED' " +
                        "WHERE invoice_id = ? AND status = 'DRAFT'";

        try (Connection connection = DBConnection.getConnection()) {
            connection.setAutoCommit(false);

            try {
                BigDecimal grandTotal;
                String status;

                try (PreparedStatement statement =
                             connection.prepareStatement(invoiceQuery)) {
                    statement.setInt(1, invoiceId);

                    try (ResultSet rs = statement.executeQuery()) {
                        if (!rs.next()) {
                            throw new SQLException("Invoice not found.");
                        }

                        grandTotal = rs.getBigDecimal("grand_total");
                        status = rs.getString("status");
                    }
                }

                if (!"DRAFT".equals(status)) {
                    throw new SQLException(
                            "Only draft invoices can be completed.");
                }

                BigDecimal paid;

                try (PreparedStatement statement =
                             connection.prepareStatement(paymentQuery)) {
                    statement.setInt(1, invoiceId);

                    try (ResultSet rs = statement.executeQuery()) {
                        rs.next();
                        paid = rs.getBigDecimal("paid");
                    }
                }

                if (paid.compareTo(grandTotal) != 0) {
                    throw new SQLException(
                            "Full payment required. Invoice total: " +
                                    grandTotal + ", completed payments: " + paid);
                }

                List<int[]> items = new ArrayList<>();

                try (PreparedStatement statement =
                             connection.prepareStatement(itemQuery)) {
                    statement.setInt(1, invoiceId);

                    try (ResultSet rs = statement.executeQuery()) {
                        while (rs.next()) {
                            items.add(new int[]{
                                    rs.getInt("product_id"),
                                    rs.getInt("total_quantity")
                            });
                        }
                    }
                }

                if (items.isEmpty()) {
                    throw new SQLException(
                            "Cannot complete an invoice without items.");
                }

                for (int[] item : items) {
                    try (PreparedStatement statement =
                                 connection.prepareStatement(stockQuery)) {
                        statement.setInt(1, item[1]);
                        statement.setInt(2, item[0]);
                        statement.setInt(3, item[1]);

                        if (statement.executeUpdate() != 1) {
                            throw new SQLException(
                                    "Insufficient stock for product ID " +
                                            item[0] + ". No changes were saved.");
                        }
                    }

                    try (PreparedStatement statement =
                                 connection.prepareStatement(movementQuery)) {
                        statement.setInt(1, item[0]);
                        statement.setInt(2, item[1]);
                        statement.setInt(3, invoiceId);
                        statement.setString(4,
                                "Stock deducted for completed invoice");
                        statement.setTimestamp(5,
                                Timestamp.valueOf(LocalDateTime.now()));
                        statement.executeUpdate();
                    }
                }

                try (PreparedStatement statement =
                             connection.prepareStatement(completeQuery)) {
                    statement.setInt(1, invoiceId);

                    if (statement.executeUpdate() != 1) {
                        throw new SQLException(
                                "Could not complete the invoice.");
                    }
                }

                connection.commit();
                System.out.println(
                        "Invoice completed. Payment verified and stock updated.");
                return true;

            } catch (Exception e) {
                connection.rollback();
                System.out.println("Checkout failed: " + e.getMessage());
                System.out.println("All checkout changes were rolled back.");
                return false;

            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException | IOException e) {
            System.out.println("Checkout database error: " + e.getMessage());
            return false;
        }
    }

}