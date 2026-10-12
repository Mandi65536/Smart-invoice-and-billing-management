
package org.example;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InvoiceItemDAO {

    // Add a product to an invoice
    public boolean addInvoiceItem(InvoiceItem item) {

        String query =
                "INSERT INTO invoice_items " +
                        "(invoice_id, product_id, quantity, unit_price, " +
                        "discount_percent, discount_amount, line_total) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(query)
        ) {
            statement.setInt(1, item.getInvoiceId());
            statement.setInt(2, item.getProductId());
            statement.setInt(3, item.getQuantity());
            statement.setBigDecimal(
                    4, BigDecimal.valueOf(item.getUnitPrice()));
            statement.setBigDecimal(
                    5, BigDecimal.valueOf(item.getDiscountPercent()));
            statement.setBigDecimal(
                    6, BigDecimal.valueOf(item.getDiscountAmount()));
            statement.setBigDecimal(
                    7, BigDecimal.valueOf(item.getLineTotal()));

            return statement.executeUpdate() > 0;

        } catch (SQLException | IOException e) {
            System.out.println("Unable to add invoice item.");
            System.out.println("Reason: " + e.getMessage());
            return false;
        }
    }

    // Retrieve all items belonging to an invoice
    public List<InvoiceItem> getItemsByInvoiceId(int invoiceId) {

        List<InvoiceItem> items = new ArrayList<>();

        String query =
                "SELECT * FROM invoice_items " +
                        "WHERE invoice_id = ? ORDER BY invoice_item_id";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(query)
        ) {
            statement.setInt(1, invoiceId);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {

                    items.add(new InvoiceItem(
                            resultSet.getInt("invoice_item_id"),
                            resultSet.getInt("invoice_id"),
                            resultSet.getInt("product_id"),
                            resultSet.getInt("quantity"),
                            resultSet.getBigDecimal("unit_price")
                                    .doubleValue(),
                            resultSet.getBigDecimal("discount_percent")
                                    .doubleValue(),
                            resultSet.getBigDecimal("discount_amount")
                                    .doubleValue(),
                            resultSet.getBigDecimal("line_total")
                                    .doubleValue()
                    ));
                }
            }

        } catch (SQLException | IOException e) {
            System.out.println("Unable to retrieve invoice items.");
            System.out.println("Reason: " + e.getMessage());
        }

        return items;
    }

    // Delete one invoice item by its ID
    public boolean deleteInvoiceItem(int invoiceItemId) {

        String query =
                "DELETE FROM invoice_items WHERE invoice_item_id = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(query)
        ) {
            statement.setInt(1, invoiceItemId);
            return statement.executeUpdate() > 0;

        } catch (SQLException | IOException e) {
            System.out.println("Unable to delete invoice item.");
            System.out.println("Reason: " + e.getMessage());
            return false;
        }
    }

    // Calculate the subtotal of all items in an invoice
    public java.math.BigDecimal calculateSubtotal(int invoiceId) {

        String query =
                "SELECT COALESCE(SUM(line_total), 0) AS subtotal " +
                        "FROM invoice_items WHERE invoice_id = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(query)
        ) {
            statement.setInt(1, invoiceId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getBigDecimal("subtotal");
                }
            }

        } catch (SQLException | IOException e) {
            System.out.println("Unable to calculate invoice subtotal.");
            System.out.println("Reason: " + e.getMessage());
        }

        return java.math.BigDecimal.ZERO;
    }

}
