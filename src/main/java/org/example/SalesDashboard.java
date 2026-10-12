
package org.example;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SalesDashboard {

    public void displayDashboard() {

        String invoiceQuery =
                "SELECT COUNT(*) AS total_invoices, " +
                        "COALESCE(SUM(CASE WHEN status = 'COMPLETED' " +
                        "THEN grand_total ELSE 0 END), 0) AS completed_sales, " +
                        "COALESCE(SUM(CASE WHEN status = 'DRAFT' " +
                        "THEN grand_total ELSE 0 END), 0) AS draft_value " +
                        "FROM invoices";

        String paymentQuery =
                "SELECT COALESCE(SUM(amount), 0) AS payments_received " +
                        "FROM payments WHERE payment_status = 'COMPLETED'";

        String productsSoldQuery =
                "SELECT COALESCE(SUM(ii.quantity), 0) AS products_sold " +
                        "FROM invoice_items ii " +
                        "JOIN invoices i ON ii.invoice_id = i.invoice_id " +
                        "WHERE i.status = 'COMPLETED'";

        try (Connection connection = DBConnection.getConnection()) {

            System.out.println("\n========================================");
            System.out.println("          SALES DASHBOARD");
            System.out.println("========================================");

            try (PreparedStatement statement =
                         connection.prepareStatement(invoiceQuery);
                 ResultSet rs = statement.executeQuery()) {

                if (rs.next()) {
                    System.out.println("Total invoices: "
                            + rs.getInt("total_invoices"));

                    System.out.println("Completed sales: ₹"
                            + rs.getBigDecimal("completed_sales")
                            .setScale(2));

                    System.out.println("Draft invoice value: ₹"
                            + rs.getBigDecimal("draft_value")
                            .setScale(2));
                }
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(paymentQuery);
                 ResultSet rs = statement.executeQuery()) {

                if (rs.next()) {
                    System.out.println("Payments received: ₹"
                            + rs.getBigDecimal("payments_received")
                            .setScale(2));
                }
            }

            try (PreparedStatement statement =
                         connection.prepareStatement(productsSoldQuery);
                 ResultSet rs = statement.executeQuery()) {

                if (rs.next()) {
                    System.out.println("Products sold: "
                            + rs.getLong("products_sold"));
                }
            }

            System.out.println("========================================");

        } catch (SQLException | IOException e) {
            System.out.println("Unable to display sales dashboard: "
                    + e.getMessage());
        }
    }
    public static void main(String[] args) {
        SalesDashboard dashboard = new SalesDashboard();
        dashboard.displayDashboard();
    }
}
