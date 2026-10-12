package org.example;
import java.util.List;
import java.util.ArrayList;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.io.IOException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;

public class ProductDAO {

    public List<Product> getAllProducts() {

        String query = "SELECT * FROM products";
        List<Product> products = new ArrayList<>();

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(query);
                ResultSet result = statement.executeQuery()
        ) {
            while (result.next()) {
                int id = result.getInt("product_id");
                String sku = result.getString("sku");
                String name = result.getString("product_name");
                String category = result.getString("category");
                double purchasePrice = result.getDouble("purchase_price");
                double sellingPrice = result.getDouble("selling_price");
                int stock = result.getInt("current_stock");
                int reorderLevel = result.getInt("reorder_level");

                Product product = new Product(
                        id,
                        sku,
                        name,
                        category,
                        purchasePrice,
                        sellingPrice,
                        stock,
                        reorderLevel
                );
                products.add(product);
                System.out.println("Product ID: " + product.getProductId());
                System.out.println("Product Name: " + product.getProductName());
                System.out.println("Selling Price: Rs. " + product.getSellingPrice());
                System.out.println("---------------------------");
            }
            return products;
        }
        catch (SQLException | IOException e) {
            System.out.println("Unable to retrieve products.");
            System.out.println("Reason: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    public static void main(String[] args) {

        ProductDAO productDAO = new ProductDAO();

        List<Product> products = productDAO.getAllProducts();

        for (Product product : products) {
            System.out.println("Product ID: " + product.getProductId());
            System.out.println("Product Name: " + product.getProductName());
            System.out.println("Selling Price: Rs. " + product.getSellingPrice());
            System.out.println("---------------------------");
        }
    }
    public boolean addProduct(Product product) {
        String query = "INSERT INTO products " +
                "(sku, product_name, category, purchase_price, " +
                "selling_price, current_stock, reorder_level) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(query)
        ) {
            statement.setString(1, product.getSku());
            statement.setString(2, product.getProductName());
            statement.setString(3, product.getCategory());
            statement.setBigDecimal(4, java.math.BigDecimal.valueOf(product.getPurchasePrice()));
            statement.setBigDecimal(5, java.math.BigDecimal.valueOf(product.getSellingPrice()));
            statement.setInt(6, product.getCurrentStock());
            statement.setInt(7, product.getReorderLevel());
            int rowsInserted = statement.executeUpdate();

            return rowsInserted > 0;
        }
        catch (SQLException | IOException e) {
            System.out.println("Unable to add product.");
            System.out.println("Reason: " + e.getMessage());

            return false;
        }
    }
    public boolean updateProduct(Product product) {

        String query = "UPDATE products " +
                "SET selling_price = ? " +
                "WHERE sku = ?";
        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(query)
        ) {
            statement.setBigDecimal(
                    1,
                    java.math.BigDecimal.valueOf(product.getSellingPrice())
            );
            statement.setString(2, product.getSku());
            int rowsUpdated = statement.executeUpdate();
            return rowsUpdated > 0;

        } catch (SQLException | IOException e) {
            System.out.println("Unable to update product.");
            System.out.println("Reason: " + e.getMessage());

            return false;
        }
    }

    public boolean deleteProduct(String sku) {

        String query = "DELETE FROM products WHERE sku = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(query)
        ) {

            statement.setString(1, sku);

            int rowsDeleted = statement.executeUpdate();

            return rowsDeleted > 0;

        } catch (SQLException | IOException e) {
            System.out.println("Unable to delete product.");
            System.out.println("Reason: " + e.getMessage());

            return false;
        }
    }

    public Product getProductBySku(String sku) {

        String query = "SELECT * FROM products WHERE sku = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(query)
        ) {

            statement.setString(1, sku);

            try (ResultSet result = statement.executeQuery()) {

                if (result.next()) {

                    int id = result.getInt("product_id");
                    String productSku = result.getString("sku");
                    String name = result.getString("product_name");
                    String category = result.getString("category");
                    double purchasePrice = result.getDouble("purchase_price");
                    double sellingPrice = result.getDouble("selling_price");
                    int stock = result.getInt("current_stock");
                    int reorderLevel = result.getInt("reorder_level");

                    return new Product(
                            id,
                            productSku,
                            name,
                            category,
                            purchasePrice,
                            sellingPrice,
                            stock,
                            reorderLevel
                    );
                }
            }

        } catch (SQLException | IOException e) {
            System.out.println("Unable to search for product.");
            System.out.println("Reason: " + e.getMessage());
        }

        return null;
    }

    public boolean increaseStock(String sku, int quantity) {

        if (quantity <= 0) {
            System.out.println("Quantity must be greater than zero.");
            return false;
        }

        String query =
                "UPDATE products " +
                        "SET current_stock = current_stock + ? " +
                        "WHERE sku = ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(query)
        ) {

            statement.setInt(1, quantity);
            statement.setString(2, sku);

            int rowsUpdated = statement.executeUpdate();

            return rowsUpdated > 0;

        } catch (SQLException | IOException e) {
            System.out.println("Unable to increase stock.");
            System.out.println("Reason: " + e.getMessage());

            return false;
        }
    }

    public boolean reduceStock(String sku, int quantity) {

        if (quantity <= 0) {
            System.out.println("Quantity must be greater than zero.");
            return false;
        }

        String query =
                "UPDATE products " +
                        "SET current_stock = current_stock - ? " +
                        "WHERE sku = ? AND current_stock >= ?";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(query)
        ) {

            statement.setInt(1, quantity);
            statement.setString(2, sku);
            statement.setInt(3, quantity);

            int rowsUpdated = statement.executeUpdate();

            return rowsUpdated > 0;

        } catch (SQLException | IOException e) {
            System.out.println("Unable to reduce stock.");
            System.out.println("Reason: " + e.getMessage());

            return false;
        }
    }

    public boolean reduceStockAndRecordMovement(
            String sku, int quantity, int invoiceId) {

        if (quantity <= 0) {
            System.out.println("Quantity must be greater than zero.");
            return false;
        }

        String updateQuery =
                "UPDATE products " +
                        "SET current_stock = current_stock - ? " +
                        "WHERE sku = ? AND current_stock >= ?";

        String movementQuery =
                "INSERT INTO stock_movements " +
                        "(product_id, movement_type, quantity, " +
                        "reference_type, reference_id, notes, movement_date) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DBConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {
                int productId;

                String findQuery =
                        "SELECT product_id FROM products WHERE sku = ?";

                try (PreparedStatement findStatement =
                             connection.prepareStatement(findQuery)) {

                    findStatement.setString(1, sku);

                    try (ResultSet result = findStatement.executeQuery()) {
                        if (!result.next()) {
                            connection.rollback();
                            System.out.println("Product not found.");
                            return false;
                        }

                        productId = result.getInt("product_id");
                    }
                }

                try (PreparedStatement updateStatement =
                             connection.prepareStatement(updateQuery)) {

                    updateStatement.setInt(1, quantity);
                    updateStatement.setString(2, sku);
                    updateStatement.setInt(3, quantity);

                    if (updateStatement.executeUpdate() == 0) {
                        connection.rollback();
                        System.out.println("Insufficient stock.");
                        return false;
                    }
                }

                try (PreparedStatement movementStatement =
                             connection.prepareStatement(movementQuery)) {

                    movementStatement.setInt(1, productId);
                    movementStatement.setString(2, "OUT");
                    movementStatement.setInt(3, quantity);
                    movementStatement.setString(4, "SALE");
                    movementStatement.setInt(5, invoiceId);
                    movementStatement.setString(6, "Stock reduced for sale");
                    movementStatement.setTimestamp(
                            7, Timestamp.valueOf(LocalDateTime.now()));

                    movementStatement.executeUpdate();
                }

                connection.commit();
                return true;

            } catch (SQLException e) {
                connection.rollback();
                throw e;

            } finally {
                connection.setAutoCommit(true);
            }

        } catch (SQLException | IOException e) {
            System.out.println("Stock transaction failed.");
            System.out.println("Reason: " + e.getMessage());
            return false;
        }
    }
    public void displayLowStockProducts() {
        String query =
                "SELECT sku, product_name, current_stock, reorder_level " +
                        "FROM products " +
                        "WHERE current_stock <= reorder_level " +
                        "ORDER BY current_stock ASC";

        try (Connection connection = DBConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(query);
             ResultSet resultSet = statement.executeQuery()) {

            System.out.println("\n--- LOW-STOCK REPORT ---");
            boolean found = false;

            System.out.printf("%-12s %-25s %-15s %-15s%n",
                    "SKU", "Product", "Current Stock", "Reorder Level");

            while (resultSet.next()) {
                found = true;

                System.out.printf("%-12s %-25s %-15d %-15d%n",
                        resultSet.getString("sku"),
                        resultSet.getString("product_name"),
                        resultSet.getInt("current_stock"),
                        resultSet.getInt("reorder_level"));
            }

            if (!found) {
                System.out.println("No low-stock products found.");
            }

        } catch (SQLException | IOException e) {
            System.out.println("Unable to generate low-stock report: "
                    + e.getMessage());
        }
    }

}