
package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InvoiceDAO {

    // Add a new invoice
    public boolean addInvoice(Invoice invoice) {

        String query =
                "INSERT INTO invoices " +
                        "(invoice_number, customer_id, invoice_date, " +
                        "subtotal, discount_type, discount_value, " +
                        "discount_amount, tax_amount, grand_total, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(query)
        ) {

            statement.setString(1, invoice.getInvoiceNumber());
            statement.setInt(2, invoice.getCustomerId());
            statement.setDate(3, Date.valueOf(invoice.getInvoiceDate()));
            statement.setBigDecimal(4,
                    java.math.BigDecimal.valueOf(invoice.getSubtotal()));
            statement.setString(5, invoice.getDiscountType());
            statement.setBigDecimal(6,
                    java.math.BigDecimal.valueOf(invoice.getDiscountValue()));
            statement.setBigDecimal(7,
                    java.math.BigDecimal.valueOf(invoice.getDiscountAmount()));
            statement.setBigDecimal(8,
                    java.math.BigDecimal.valueOf(invoice.getTaxAmount()));
            statement.setBigDecimal(9,
                    java.math.BigDecimal.valueOf(invoice.getGrandTotal()));
            statement.setString(10, invoice.getStatus());

            return statement.executeUpdate() > 0;

        } catch (SQLException | IOException e) {
            System.out.println("Unable to add invoice.");
            System.out.println("Reason: " + e.getMessage());
            return false;
        }
    }

    // Retrieve all invoices
    public List<Invoice> getAllInvoices() {

        List<Invoice> invoices = new ArrayList<>();

        String query = "SELECT * FROM invoices ORDER BY invoice_id";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(query);
                ResultSet resultSet = statement.executeQuery()
        ) {

            while (resultSet.next()) {
                invoices.add(mapResultSetToInvoice(resultSet));
            }

        } catch (SQLException | IOException e) {
            System.out.println("Unable to retrieve invoices.");
            System.out.println("Reason: " + e.getMessage());
        }

        return invoices;
    }

    // Find an invoice by its database ID
    public Invoice getInvoiceById(int invoiceId) {

        String query =
                "SELECT * FROM invoices WHERE invoice_id = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(query)
        ) {

            statement.setInt(1, invoiceId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToInvoice(resultSet);
                }
            }

        } catch (SQLException | IOException e) {
            System.out.println("Unable to retrieve invoice.");
            System.out.println("Reason: " + e.getMessage());
        }

        return null;
    }

    // Find an invoice by its invoice number
    public Invoice getInvoiceByNumber(String invoiceNumber) {

        String query =
                "SELECT * FROM invoices WHERE invoice_number = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(query)
        ) {

            statement.setString(1, invoiceNumber);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return mapResultSetToInvoice(resultSet);
                }
            }

        } catch (SQLException | IOException e) {
            System.out.println("Unable to retrieve invoice.");
            System.out.println("Reason: " + e.getMessage());
        }

        return null;
    }

    // Update the invoice status
    public boolean updateInvoiceStatus(int invoiceId, String status) {

        if (invoiceId <= 0 || status == null
                || (!status.equals("DRAFT")
                && !status.equals("COMPLETED")
                && !status.equals("CANCELLED"))) {
            System.out.println("Invalid invoice ID or status.");
            return false;
        }

        if ("DRAFT".equals(status)) {
            System.out.println(
                    "An invoice cannot be changed back to DRAFT.");
            return false;
        }

        String query =
                "UPDATE invoices SET status = ? " +
                        "WHERE invoice_id = ? AND status = 'DRAFT'";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(query)
        ) {

            statement.setString(1, status);
            statement.setInt(2, invoiceId);

            return statement.executeUpdate() > 0;

        } catch (SQLException | IOException e) {
            System.out.println("Unable to update invoice status.");
            System.out.println("Reason: " + e.getMessage());
            return false;
        }
    }

    // Delete a draft invoice
    public boolean deleteDraftInvoice(int invoiceId) {

        String query =
                "DELETE FROM invoices " +
                        "WHERE invoice_id = ? AND status = 'DRAFT'";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(query)
        ) {

            statement.setInt(1, invoiceId);

            return statement.executeUpdate() > 0;

        } catch (SQLException | IOException e) {
            System.out.println("Unable to delete draft invoice.");
            System.out.println("Reason: " + e.getMessage());
            return false;
        }
    }

    // Convert a database row into an Invoice object
    private Invoice mapResultSetToInvoice(ResultSet resultSet)
            throws SQLException {

        return new Invoice(
                resultSet.getInt("invoice_id"),
                resultSet.getString("invoice_number"),
                resultSet.getInt("customer_id"),
                resultSet.getDate("invoice_date").toLocalDate(),
                resultSet.getBigDecimal("subtotal").doubleValue(),
                resultSet.getString("discount_type"),
                resultSet.getBigDecimal("discount_value").doubleValue(),
                resultSet.getBigDecimal("discount_amount").doubleValue(),
                resultSet.getBigDecimal("tax_amount").doubleValue(),
                resultSet.getBigDecimal("grand_total").doubleValue(),
                resultSet.getString("status")
        );
    }

    // Update the financial totals of a draft invoice
    public boolean updateInvoiceTotals(
            int invoiceId,
            java.math.BigDecimal subtotal,
            java.math.BigDecimal discountValue,
            java.math.BigDecimal discountAmount,
            java.math.BigDecimal taxAmount,
            java.math.BigDecimal grandTotal) {

        if (invoiceId <= 0
                || subtotal == null || subtotal.signum() < 0
                || discountValue == null || discountValue.signum() < 0
                || discountAmount == null || discountAmount.signum() < 0
                || taxAmount == null || taxAmount.signum() < 0
                || grandTotal == null || grandTotal.signum() < 0) {
            return false;
        }

        String query =
                "UPDATE invoices SET subtotal = ?, " +
                        "discount_value = ?, discount_amount = ?, " +
                        "tax_amount = ?, grand_total = ? " +
                        "WHERE invoice_id = ? AND status = 'DRAFT'";

        try (
                java.sql.Connection connection =
                        DBConnection.getConnection();
                java.sql.PreparedStatement statement =
                        connection.prepareStatement(query)
        ) {
            statement.setBigDecimal(1, subtotal);
            statement.setBigDecimal(2, discountValue);
            statement.setBigDecimal(3, discountAmount);
            statement.setBigDecimal(4, taxAmount);
            statement.setBigDecimal(5, grandTotal);
            statement.setInt(6, invoiceId);

            return statement.executeUpdate() > 0;

        } catch (java.sql.SQLException | java.io.IOException e) {
            System.out.println("Unable to update invoice totals.");
            System.out.println("Reason: " + e.getMessage());
            return false;
        }
    }

}
