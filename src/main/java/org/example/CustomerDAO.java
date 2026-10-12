package org.example;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.io.IOException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class CustomerDAO {

    public boolean addCustomer(Customer customer) {

        String query =
                "INSERT INTO customers " +
                        "(customer_name, phone, email, billing_address) " +
                        "VALUES (?, ?, ?, ?)";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(query)
        ) {

            statement.setString(1, customer.getCustomerName());
            statement.setString(2, customer.getPhone());
            statement.setString(3, customer.getEmail());
            statement.setString(4, customer.getBillingAddress());

            int rowsInserted = statement.executeUpdate();

            return rowsInserted > 0;

        } catch (SQLException | IOException e) {
            System.out.println("Unable to add customer.");
            System.out.println("Reason: " + e.getMessage());

            return false;
        }
    }

    public List<Customer> getAllCustomers() {

        List<Customer> customers = new ArrayList<>();

        String query = "SELECT * FROM customers";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(query);
                ResultSet resultSet = statement.executeQuery()
        ) {

            while (resultSet.next()) {

                Customer customer = new Customer(
                        resultSet.getInt("customer_id"),
                        resultSet.getString("customer_name"),
                        resultSet.getString("phone"),
                        resultSet.getString("email"),
                        resultSet.getString("billing_address")
                );

                customers.add(customer);
            }

        } catch (SQLException | IOException e) {
            System.out.println("Unable to retrieve customers.");
            System.out.println("Reason: " + e.getMessage());
        }

        return customers;
    }

    public Customer getCustomerById(int customerId) {

        String query =
                "SELECT * FROM customers WHERE customer_id = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(query)
        ) {

            statement.setInt(1, customerId);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {

                    return new Customer(
                            resultSet.getInt("customer_id"),
                            resultSet.getString("customer_name"),
                            resultSet.getString("phone"),
                            resultSet.getString("email"),
                            resultSet.getString("billing_address")
                    );
                }
            }

        } catch (SQLException | IOException e) {
            System.out.println("Unable to retrieve customer.");
            System.out.println("Reason: " + e.getMessage());
        }

        return null;
    }

    public boolean updateCustomer(Customer customer) {

        String query =
                "UPDATE customers SET " +
                        "customer_name = ?, " +
                        "phone = ?, " +
                        "email = ?, " +
                        "billing_address = ? " +
                        "WHERE customer_id = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(query)
        ) {

            statement.setString(1, customer.getCustomerName());
            statement.setString(2, customer.getPhone());
            statement.setString(3, customer.getEmail());
            statement.setString(4, customer.getBillingAddress());
            statement.setInt(5, customer.getCustomerId());

            int rowsUpdated = statement.executeUpdate();

            return rowsUpdated > 0;

        } catch (SQLException | IOException e) {
            System.out.println("Unable to update customer.");
            System.out.println("Reason: " + e.getMessage());

            return false;
        }
    }

    public Customer getCustomerByPhone(String phone) {

        String query =
                "SELECT * FROM customers WHERE phone = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(query)
        ) {

            statement.setString(1, phone);

            try (ResultSet resultSet = statement.executeQuery()) {

                if (resultSet.next()) {

                    return new Customer(
                            resultSet.getInt("customer_id"),
                            resultSet.getString("customer_name"),
                            resultSet.getString("phone"),
                            resultSet.getString("email"),
                            resultSet.getString("billing_address")
                    );
                }
            }

        } catch (SQLException | IOException e) {
            System.out.println("Unable to search customer by phone.");
            System.out.println("Reason: " + e.getMessage());
        }

        return null;
    }

    public boolean deleteCustomer(int customerId) {

        String query =
                "DELETE FROM customers WHERE customer_id = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(query)
        ) {

            statement.setInt(1, customerId);

            int rowsDeleted = statement.executeUpdate();

            return rowsDeleted > 0;

        } catch (SQLException | IOException e) {
            System.out.println("Unable to delete customer.");
            System.out.println("Reason: " + e.getMessage());

            return false;
        }
    }

}
