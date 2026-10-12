
package org.example;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class PaymentDAO {

    // Record a new payment
    public boolean addPayment(Payment payment) {

        if (payment == null) {
            System.out.println("Payment cannot be null.");
            return false;
        }

        int invoiceId = payment.getInvoiceId();

        if (invoiceId <= 0) {
            System.out.println("Invalid invoice ID.");
            return false;
        }

        InvoiceDAO invoiceDAO = new InvoiceDAO();
        Invoice invoice = invoiceDAO.getInvoiceById(invoiceId);

        if (invoice == null) {
            System.out.println("Invoice not found.");
            return false;
        }

        if (!"DRAFT".equals(invoice.getStatus())) {
            System.out.println("Payments can only be added to draft invoices.");
            return false;
        }

        BigDecimal amount = BigDecimal.valueOf(payment.getAmount());

        BigDecimal invoiceTotal =
                BigDecimal.valueOf(invoice.getGrandTotal())
                        .setScale(2, java.math.RoundingMode.HALF_UP);

        if (amount.signum() <= 0) {
            System.out.println("Payment amount must be positive.");
            return false;
        }

        BigDecimal completedTotal = getCompletedPaymentTotal(invoiceId);

        if ("COMPLETED".equals(payment.getStatus())
                && completedTotal.add(amount).compareTo(invoiceTotal) > 0) {
            System.out.println("Payment exceeds the outstanding invoice balance.");
            return false;
        }

        String query =
                "INSERT INTO payments " +
                        "(invoice_id, amount, payment_method, payment_date, payment_status) " +
                        "VALUES (?, ?, ?, ?, ?)";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(query)
        ) {
            statement.setInt(1, invoiceId);
            statement.setBigDecimal(2, amount);
            statement.setString(3, payment.getPaymentMethod());
            statement.setTimestamp(
                    4, Timestamp.valueOf(payment.getPaymentDate()));
            statement.setString(5, payment.getStatus());

            return statement.executeUpdate() > 0;

        } catch (SQLException | IOException e) {
            System.out.println("Unable to record payment.");
            System.out.println("Reason: " + e.getMessage());
            return false;
        }
    }

    // Retrieve all payments for a specific invoice
    public List<Payment> getPaymentsByInvoiceId(int invoiceId) {

        List<Payment> payments = new ArrayList<>();

        String query =
                "SELECT * FROM payments " +
                        "WHERE invoice_id = ? ORDER BY payment_id";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(query)
        ) {
            statement.setInt(1, invoiceId);

            try (ResultSet resultSet = statement.executeQuery()) {

                while (resultSet.next()) {

                    payments.add(new Payment(
                            resultSet.getInt("payment_id"),
                            resultSet.getInt("invoice_id"),
                            resultSet.getBigDecimal("amount")
                                    .doubleValue(),
                            resultSet.getString("payment_method"),
                            resultSet.getTimestamp("payment_date")
                                    .toLocalDateTime(),
                            resultSet.getString("payment_status")
                    ));
                }
            }

        } catch (SQLException | IOException e) {
            System.out.println("Unable to retrieve payments.");
            System.out.println("Reason: " + e.getMessage());
        }

        return payments;
    }

    public BigDecimal getCompletedPaymentTotal(int invoiceId) {
        String query =
                "SELECT COALESCE(SUM(amount), 0) AS total " +
                        "FROM payments " +
                        "WHERE invoice_id = ? " +
                        "AND payment_status = 'COMPLETED'";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(query)
        ) {
            statement.setInt(1, invoiceId);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getBigDecimal("total");
                }
            }

        } catch (SQLException | IOException e) {
            System.out.println("Unable to calculate completed payment total.");
            System.out.println("Reason: " + e.getMessage());
            throw new RuntimeException(
                    "Payment total calculation failed.", e);
        }

        return BigDecimal.ZERO;
    }


    // Retrieve one payment by its database ID
    public Payment getPaymentById(int paymentId) {

        String query =
                "SELECT * FROM payments WHERE payment_id = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(query)
        ) {
            statement.setInt(1, paymentId);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {

                    return new Payment(
                            resultSet.getInt("payment_id"),
                            resultSet.getInt("invoice_id"),
                            resultSet.getBigDecimal("amount")
                                    .doubleValue(),
                            resultSet.getString("payment_method"),
                            resultSet.getTimestamp("payment_date")
                                    .toLocalDateTime(),
                            resultSet.getString("payment_status")
                    );
                }
            }

        } catch (SQLException | IOException e) {
            System.out.println("Unable to retrieve payment.");
            System.out.println("Reason: " + e.getMessage());
        }

        return null;
    }

    public void displayPaymentSummary() {
        String query =
                "SELECT payment_method, " +
                        "COALESCE(SUM(CASE WHEN payment_status = 'COMPLETED' " +
                        "THEN amount ELSE 0 END), 0) AS completed_total, " +
                        "COALESCE(SUM(CASE WHEN payment_status = 'PENDING' " +
                        "THEN amount ELSE 0 END), 0) AS pending_total " +
                        "FROM payments " +
                        "GROUP BY payment_method " +
                        "ORDER BY payment_method";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            System.out.println("\n--- PAYMENT SUMMARY ---");
            System.out.printf("%-18s %-20s %-20s%n",
                    "Payment Method", "Completed", "Pending");

            BigDecimal completedGrandTotal = BigDecimal.ZERO;
            BigDecimal pendingGrandTotal = BigDecimal.ZERO;
            boolean found = false;

            while (resultSet.next()) {
                found = true;

                BigDecimal completed =
                        resultSet.getBigDecimal("completed_total");
                BigDecimal pending =
                        resultSet.getBigDecimal("pending_total");

                completedGrandTotal =
                        completedGrandTotal.add(completed);
                pendingGrandTotal = pendingGrandTotal.add(pending);

                System.out.printf("%-18s %-20s %-20s%n",
                        resultSet.getString("payment_method"),
                        completed,
                        pending);
            }

            if (!found) {
                System.out.println("No payments recorded yet.");
            }

            System.out.println("-----------------------------------------------");
            System.out.println("Total completed payments: "
                    + completedGrandTotal);
            System.out.println("Total pending payments: " + pendingGrandTotal);

        } catch (SQLException | IOException e) {
            System.out.println("Unable to generate payment summary: "
                    + e.getMessage());
        }
    }

}
