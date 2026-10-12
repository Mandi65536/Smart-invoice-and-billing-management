package org.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SwingUI extends JFrame {

    private final ProductDAO productDAO = new ProductDAO();
    private final InvoicePDFGenerator pdfGenerator =
            new InvoicePDFGenerator();
    private final BillingService billingService =
            new BillingService();

    private final Color background = new Color(245, 247, 250);
    private final Color primary = new Color(35, 83, 145);
    private final Color textColor = new Color(40, 48, 60);

    @FunctionalInterface
    private interface DatabaseAction {
        void run() throws SQLException, IOException;
    }

    public SwingUI() {
        setTitle("Smart Invoice and Billing Management System");
        setSize(1050, 680);
        setMinimumSize(new Dimension(850, 560));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(20, 20));
        root.setBackground(background);
        root.setBorder(new EmptyBorder(25, 30, 25, 30));

        JLabel title = new JLabel("Smart Invoice & Billing");
        title.setFont(new Font("SansSerif", Font.BOLD, 28));
        title.setForeground(primary);

        JLabel subtitle = new JLabel(
                "Manage products, customers, invoices and payments");
        subtitle.setFont(new Font("SansSerif", Font.PLAIN, 14));
        subtitle.setForeground(Color.GRAY);

        JPanel header = new JPanel();
        header.setBackground(background);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(title);
        header.add(Box.createVerticalStrut(8));
        header.add(subtitle);

        root.add(header, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 3, 18, 18));
        grid.setBackground(background);

        addButton(grid, "Products", this::manageProducts);
        addButton(grid, "Customers", this::manageCustomers);
        addButton(grid, "Invoices", this::manageInvoices);
        addButton(grid, "Invoice Items", this::manageInvoiceItems);
        addButton(grid, "Payments", this::managePayments);
        addButton(grid, "Checkout Invoice", this::checkoutInvoice);
        addButton(grid, "Reports", this::showReports);
        addButton(grid, "Low Stock", this::showLowStock);
        addButton(grid, "Generate Invoice PDF", this::generatePDF);

        root.add(grid, BorderLayout.CENTER);

        JLabel footer = new JLabel(
                "Smart Billing System | Java + MySQL");
        footer.setForeground(Color.GRAY);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void addButton(
            JPanel panel, String label, Runnable action) {

        JButton button = new JButton(label);
        button.setFont(new Font("SansSerif", Font.BOLD, 15));
        button.setForeground(textColor);
        button.setBackground(Color.WHITE);
        button.setFocusPainted(false);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(
                        new Color(220, 225, 232)),
                new EmptyBorder(18, 12, 18, 12)));

        button.setCursor(Cursor.getPredefinedCursor(
                Cursor.HAND_CURSOR));

        button.addActionListener(e -> action.run());
        panel.add(button);
    }

    private String input(String message) {
        return JOptionPane.showInputDialog(this, message);
    }

    private String requiredInput(String message) {
        String value = input(message);

        if (value == null) {
            return null;
        }

        value = value.trim();

        if (value.isEmpty()) {
            throw new IllegalArgumentException(
                    "This field cannot be empty.");
        }

        return value;
    }

    private int readInt(String message) {
        String value = requiredInput(message);
        if (value == null) return -1;
        return Integer.parseInt(value);
    }

    private double readDouble(String message) {
        String value = requiredInput(message);
        if (value == null) return -1;

        double number = Double.parseDouble(value);

        if (!Double.isFinite(number)) {
            throw new IllegalArgumentException(
                    "Enter a finite number.");
        }

        return number;
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    private void showError(Exception e) {
        e.printStackTrace();
        showMessage("Operation failed: " + e.getMessage());
    }

    private void runSafely(DatabaseAction action) {
        try {
            action.run();
        } catch (Exception e) {
            showError(e);
        }
    }

    private Connection connection()
            throws SQLException, IOException {
        return DBConnection.getConnection();
    }

    // ==================== PRODUCTS ====================

    private void manageProducts() {
        String[] options = {
                "View Products", "Add Product",
                "Update Product", "Restock Product",
                "Delete Product"
        };

        int choice = JOptionPane.showOptionDialog(
                this, "Choose a product operation", "Products",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

        runSafely(() -> {
            switch (choice) {
                case 0 -> viewProducts();
                case 1 -> addProduct();
                case 2 -> updateProduct();
                case 3 -> restockProduct();
                case 4 -> deleteProduct();
                default -> {
                }
            }
        });
    }

    private void viewProducts()
            throws SQLException, IOException {
        showQuery("Products", """
                SELECT product_id, sku, product_name, category,
                       purchase_price, selling_price, current_stock,
                       reorder_level, is_active
                FROM products
                ORDER BY product_id
                """);
    }

    private void addProduct()
            throws SQLException, IOException {
        String sku = requiredInput("SKU:");
        if (sku == null) return;

        String name = requiredInput("Product name:");
        if (name == null) return;

        String category = input("Category:");
        double purchase = readDouble("Purchase price:");
        if (purchase < 0) return;

        double selling = readDouble("Selling price:");
        if (selling < 0) return;

        int stock = readInt("Opening stock:");
        if (stock < 0) return;

        int reorder = readInt("Reorder level:");
        if (reorder < 0) return;

        String sql = """
                INSERT INTO products
                (sku, product_name, category, purchase_price,
                 selling_price, current_stock, reorder_level,
                 is_active)
                VALUES (?, ?, ?, ?, ?, ?, ?, 1)
                """;

        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sku);
            ps.setString(2, name);
            ps.setString(3, category);
            ps.setBigDecimal(4, BigDecimal.valueOf(purchase));
            ps.setBigDecimal(5, BigDecimal.valueOf(selling));
            ps.setInt(6, stock);
            ps.setInt(7, reorder);
            ps.executeUpdate();
        }

        showMessage("Product added successfully.");
    }

    private void updateProduct()
            throws SQLException, IOException {
        String sku = requiredInput("SKU of product to update:");
        if (sku == null) return;

        String name = requiredInput("New product name:");
        if (name == null) return;

        double purchase = readDouble("New purchase price:");
        if (purchase < 0) return;

        double selling = readDouble("New selling price:");
        if (selling < 0) return;

        int reorder = readInt("New reorder level:");
        if (reorder < 0) return;

        String sql = """
                UPDATE products
                SET product_name = ?, purchase_price = ?,
                    selling_price = ?, reorder_level = ?
                WHERE sku = ?
                """;

        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setBigDecimal(2, BigDecimal.valueOf(purchase));
            ps.setBigDecimal(3, BigDecimal.valueOf(selling));
            ps.setInt(4, reorder);
            ps.setString(5, sku);

            showMessage(ps.executeUpdate() == 1
                    ? "Product updated successfully."
                    : "Product not found.");
        }
    }

    private void restockProduct() {
        String sku = requiredInput("SKU to restock:");
        if (sku == null) return;

        int quantity = readInt("Quantity to add:");
        if (quantity <= 0) {
            showMessage("Quantity must be positive.");
            return;
        }

        if (productDAO.increaseStock(sku, quantity)) {
            showMessage("Stock increased and movement recorded.");
        } else {
            showMessage("Restocking failed. Check the console.");
        }
    }

    private void deleteProduct()
            throws SQLException, IOException {
        String sku = requiredInput("SKU to delete:");
        if (sku == null) return;

        int confirm = JOptionPane.showConfirmDialog(
                this, "Delete product " + sku + "?",
                "Confirm deletion", JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM products WHERE sku = ?")) {
            ps.setString(1, sku);
            showMessage(ps.executeUpdate() == 1
                    ? "Product deleted."
                    : "Product not found.");
        }
    }

    // ==================== CUSTOMERS ====================

    private void manageCustomers() {
        String[] options = {"View Customers", "Add Customer"};

        int choice = JOptionPane.showOptionDialog(
                this, "Choose a customer operation", "Customers",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

        runSafely(() -> {
            if (choice == 0) {
                showQuery("Customers",
                        "SELECT * FROM customers ORDER BY customer_id");
            } else if (choice == 1) {
                addCustomer();
            }
        });
    }

    private void addCustomer()
            throws SQLException, IOException {
        String name = requiredInput("Customer name:");
        if (name == null) return;

        String phone = input("Phone:");
        String email = input("Email:");
        String address = input("Billing address:");

        String sql = """
                INSERT INTO customers
                (customer_name, phone, email, billing_address)
                VALUES (?, ?, ?, ?)
                """;

        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, name);
            ps.setString(2, phone);
            ps.setString(3, email);
            ps.setString(4, address);
            ps.executeUpdate();
        }

        showMessage("Customer added successfully.");
    }

    // ==================== INVOICES ====================

    private void manageInvoices() {
        String[] options = {
                "View Invoices", "Create Draft Invoice"
        };

        int choice = JOptionPane.showOptionDialog(
                this, "Choose an invoice operation", "Invoices",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

        runSafely(() -> {
            if (choice == 0) {
                showQuery("Invoices",
                        "SELECT * FROM invoices ORDER BY invoice_id DESC");
            } else if (choice == 1) {
                createInvoice();
            }
        });
    }

    private void createInvoice()
            throws SQLException, IOException {
        int customerId = readInt("Customer ID:");
        if (customerId <= 0) return;

        if (!exists(
                "SELECT customer_id FROM customers WHERE customer_id = ?",
                customerId)) {
            showMessage("Customer ID does not exist.");
            return;
        }

        String discountType = (String) JOptionPane.showInputDialog(
                this, "Select discount type", "Discount",
                JOptionPane.QUESTION_MESSAGE, null,
                new String[]{"PERCENT", "FIXED"}, "PERCENT");

        if (discountType == null) return;

        double discountValue = readDouble("Discount value:");
        if (discountValue < 0) return;

        if ("PERCENT".equals(discountType)
                && discountValue > 100) {
            showMessage("Percentage discount cannot exceed 100.");
            return;
        }

        String invoiceNumber = "INV-" + System.currentTimeMillis();

        String sql = """
                INSERT INTO invoices
                (invoice_number, customer_id, invoice_date,
                 subtotal, discount_type, discount_value,
                 discount_amount, tax_amount, grand_total, status)
                VALUES (?, ?, CURRENT_TIMESTAMP, 0, ?, ?, 0, 0, 0, 'DRAFT')
                """;

        int invoiceId;

        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(
                     sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, invoiceNumber);
            ps.setInt(2, customerId);
            ps.setString(3, discountType);
            ps.setBigDecimal(4, BigDecimal.valueOf(discountValue));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Could not retrieve invoice ID.");
                }
                invoiceId = keys.getInt(1);
            }
        }

        showMessage("Draft invoice created.\nInvoice ID: "
                + invoiceId + "\nInvoice number: " + invoiceNumber);
    }

    // ==================== INVOICE ITEMS ====================

    private void manageInvoiceItems() {
        String[] options = {
                "View Invoice Items", "Add Item to Draft Invoice"
        };

        int choice = JOptionPane.showOptionDialog(
                this, "Choose an invoice item operation",
                "Invoice Items", JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

        runSafely(() -> {
            if (choice == 0) {
                showQuery("Invoice Items", """
                        SELECT ii.item_id, ii.invoice_id, p.sku,
                               p.product_name, ii.quantity,
                               ii.unit_price, ii.discount_percent,
                               ii.discount_amount, ii.line_total
                        FROM invoice_items ii
                        JOIN products p ON p.product_id = ii.product_id
                        ORDER BY ii.item_id
                        """);
            } else if (choice == 1) {
                addInvoiceItem();
            }
        });
    }

    private void addInvoiceItem()
            throws SQLException, IOException {
        int invoiceId = readInt("Draft invoice ID:");
        if (invoiceId <= 0) return;

        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT status FROM invoices WHERE invoice_id = ?")) {
            ps.setInt(1, invoiceId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()
                        || !"DRAFT".equals(rs.getString("status"))) {
                    showMessage("Invoice not found or is not a draft.");
                    return;
                }
            }
        }

        String sku = requiredInput("Product SKU:");
        if (sku == null) return;

        int quantity = readInt("Quantity:");
        if (quantity <= 0) return;

        double discount = readDouble("Item discount percentage:");
        if (discount < 0 || discount > 100) {
            showMessage("Discount must be between 0 and 100.");
            return;
        }

        int productId;
        int stock;
        BigDecimal price;

        String productSql = """
                SELECT product_id, selling_price, current_stock
                FROM products
                WHERE sku = ? AND is_active = 1
                """;

        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(productSql)) {
            ps.setString(1, sku);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    showMessage("Active product not found.");
                    return;
                }
                productId = rs.getInt("product_id");
                price = rs.getBigDecimal("selling_price");
                stock = rs.getInt("current_stock");
            }
        }

        if (quantity > stock) {
            showMessage("Insufficient stock. Available: " + stock);
            return;
        }

        BigDecimal gross = price.multiply(BigDecimal.valueOf(quantity));
        BigDecimal itemDiscount = gross
                .multiply(BigDecimal.valueOf(discount))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal lineTotal = gross.subtract(itemDiscount);

        String insertSql = """
                INSERT INTO invoice_items
                (invoice_id, product_id, quantity, unit_price,
                 discount_percent, discount_amount, line_total)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(insertSql)) {
            ps.setInt(1, invoiceId);
            ps.setInt(2, productId);
            ps.setInt(3, quantity);
            ps.setBigDecimal(4, price);
            ps.setBigDecimal(5, BigDecimal.valueOf(discount));
            ps.setBigDecimal(6, itemDiscount);
            ps.setBigDecimal(7, lineTotal);
            ps.executeUpdate();
        }

        recalculateInvoice(invoiceId);
        showMessage("Item added.\nLine total: ₹" + lineTotal
                + "\nStock is deducted only during checkout.");
    }

    private void recalculateInvoice(int invoiceId)
            throws SQLException, IOException {
        String sql = """
                SELECT i.discount_type, i.discount_value,
                       COALESCE(SUM(ii.line_total), 0) AS subtotal
                FROM invoices i
                LEFT JOIN invoice_items ii ON ii.invoice_id = i.invoice_id
                WHERE i.invoice_id = ?
                GROUP BY i.invoice_id, i.discount_type, i.discount_value
                """;

        BigDecimal subtotal;
        BigDecimal discountValue;
        String discountType;

        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Invoice not found.");
                }
                subtotal = rs.getBigDecimal("subtotal");
                discountValue = rs.getBigDecimal("discount_value");
                discountType = rs.getString("discount_type");
            }
        }

        BigDecimal discountAmount;

        if ("PERCENT".equals(discountType)) {
            discountAmount = subtotal.multiply(discountValue)
                    .divide(BigDecimal.valueOf(100), 2,
                            RoundingMode.HALF_UP);
        } else {
            discountAmount = discountValue.min(subtotal);
        }

        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal grandTotal =
                subtotal.subtract(discountAmount).add(taxAmount);

        String updateSql = """
                UPDATE invoices
                SET subtotal = ?, discount_amount = ?,
                    tax_amount = ?, grand_total = ?
                WHERE invoice_id = ? AND status = 'DRAFT'
                """;

        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(updateSql)) {
            ps.setBigDecimal(1, subtotal);
            ps.setBigDecimal(2, discountAmount);
            ps.setBigDecimal(3, taxAmount);
            ps.setBigDecimal(4, grandTotal);
            ps.setInt(5, invoiceId);
            ps.executeUpdate();
        }
    }

    // ==================== PAYMENTS ====================

    private void managePayments() {
        String[] options = {"View Payments", "Record Payment"};

        int choice = JOptionPane.showOptionDialog(
                this, "Choose a payment operation", "Payments",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

        runSafely(() -> {
            if (choice == 0) {
                showQuery("Payments", """
                        SELECT payment_id, invoice_id, amount,
                               payment_method, payment_date, payment_status
                        FROM payments
                        ORDER BY payment_id DESC
                        """);
            } else if (choice == 1) {
                recordPayment();
            }
        });
    }

    private void recordPayment()
            throws SQLException, IOException {
        int invoiceId = readInt("Invoice ID:");
        if (invoiceId <= 0) return;

        BigDecimal grandTotal;
        String status;

        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT grand_total, status FROM invoices WHERE invoice_id = ?")) {
            ps.setInt(1, invoiceId);

            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    showMessage("Invoice not found.");
                    return;
                }
                grandTotal = rs.getBigDecimal("grand_total");
                status = rs.getString("status");
            }
        }

        if (!"DRAFT".equals(status)) {
            showMessage("Payments can only be added to draft invoices.");
            return;
        }

        String method = (String) JOptionPane.showInputDialog(
                this, "Payment method", "Payment",
                JOptionPane.QUESTION_MESSAGE, null,
                new String[]{"CASH", "CARD", "UPI", "BANK_TRANSFER"},
                "UPI");

        if (method == null) return;

        BigDecimal paid = getCompletedPayments(invoiceId);
        BigDecimal balance = grandTotal.subtract(paid);

        if (balance.signum() <= 0) {
            showMessage("This invoice has no outstanding balance.");
            return;
        }

        String amountText = requiredInput(
                "Outstanding balance: ₹" + balance + "\nPayment amount:");
        if (amountText == null) return;

        BigDecimal amount = new BigDecimal(amountText);

        if (amount.signum() <= 0 || amount.compareTo(balance) > 0) {
            showMessage("Payment must be positive and not exceed balance.");
            return;
        }

        String sql = """
                INSERT INTO payments
                (invoice_id, amount, payment_method,
                 payment_date, payment_status)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP, 'COMPLETED')
                """;

        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            ps.setBigDecimal(2, amount);
            ps.setString(3, method);
            ps.executeUpdate();
        }

        showMessage("Payment recorded.\nAmount: ₹" + amount
                + "\nRemaining balance: ₹" + balance.subtract(amount));
    }

    private BigDecimal getCompletedPayments(int invoiceId)
            throws SQLException, IOException {
        String sql = """
                SELECT COALESCE(SUM(amount), 0) AS paid
                FROM payments
                WHERE invoice_id = ? AND payment_status = 'COMPLETED'
                """;

        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal("paid");
            }
        }
    }

    // ==================== CHECKOUT ====================

    private void checkoutInvoice() {
        runSafely(() -> {
            int invoiceId = readInt("Invoice ID to checkout:");
            if (invoiceId <= 0) return;

            int confirm = JOptionPane.showConfirmDialog(
                    this, "Complete invoice " + invoiceId
                            + " and deduct stock?",
                    "Confirm checkout", JOptionPane.YES_NO_OPTION);

            if (confirm != JOptionPane.YES_OPTION) return;

            billingService.completeInvoice(invoiceId);

            showMessage("Checkout request processed. "
                    + "Check invoice status and stock movements "
                    + "to confirm the result.");
        });
    }

    // ==================== REPORTS ====================

    private void showReports() {
        String[] options = {
                "Sales Summary", "Payment Summary",
                "Stock Movement History", "Invoice Summary"
        };

        int choice = JOptionPane.showOptionDialog(
                this, "Select a report", "Reports",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

        runSafely(() -> {
            switch (choice) {
                case 0 -> showQuery("Sales Summary", """
                        SELECT COUNT(*) AS completed_invoices,
                               COALESCE(SUM(grand_total), 0) AS total_sales
                        FROM invoices
                        WHERE status = 'COMPLETED'
                        """);
                case 1 -> showQuery("Payment Summary", """
                        SELECT payment_method, COUNT(*) AS payment_count,
                               SUM(amount) AS total_amount
                        FROM payments
                        WHERE payment_status = 'COMPLETED'
                        GROUP BY payment_method
                        ORDER BY total_amount DESC
                        """);
                case 2 -> showQuery("Stock Movements", """
                        SELECT sm.movement_id, p.sku, p.product_name,
                               sm.movement_type, sm.quantity,
                               sm.reference_type, sm.reference_id,
                               sm.notes, sm.movement_date
                        FROM stock_movements sm
                        JOIN products p ON p.product_id = sm.product_id
                        ORDER BY sm.movement_id DESC
                        """);
                case 3 -> showQuery("Invoice Summary", """
                        SELECT i.invoice_id, i.invoice_number,
                               c.customer_name, i.invoice_date,
                               i.subtotal, i.discount_amount,
                               i.tax_amount, i.grand_total, i.status
                        FROM invoices i
                        JOIN customers c ON c.customer_id = i.customer_id
                        ORDER BY i.invoice_id DESC
                        """);
                default -> {
                }
            }
        });
    }

    private void showLowStock() {
        runSafely(() -> showQuery("Low Stock Products", """
                SELECT sku, product_name, current_stock, reorder_level
                FROM products
                WHERE is_active = 1
                  AND current_stock <= reorder_level
                ORDER BY current_stock
                """));
    }

    // ==================== PDF ====================

    private void generatePDF() {
        try {
            int invoiceId = readInt("Invoice ID for PDF:");
            if (invoiceId <= 0) return;

            Method selected = null;

            for (Method method : InvoicePDFGenerator.class.getMethods()) {
                String name = method.getName().toLowerCase();

                if ((name.contains("generate") || name.contains("create"))
                        && method.getParameterCount() == 1
                        && (method.getParameterTypes()[0] == int.class
                        || method.getParameterTypes()[0] == Integer.class)) {
                    selected = method;
                    break;
                }
            }

            if (selected == null) {
                showMessage("No public PDF method accepting an invoice ID "
                        + "was found. Check InvoicePDFGenerator.java.");
                return;
            }

            Object target = Modifier.isStatic(selected.getModifiers())
                    ? null : pdfGenerator;

            Object result = selected.invoke(target, invoiceId);

            showMessage("PDF generation method executed."
                    + (result instanceof String
                    ? "\nResult: " + result : ""));

        } catch (Exception e) {
            showError(e);
        }
    }

    // ==================== DATABASE HELPERS ====================

    private boolean exists(String sql, int id)
            throws SQLException, IOException {
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private void showQuery(String title, String sql)
            throws SQLException, IOException {
        List<String> columns = new ArrayList<>();
        List<Object[]> rows = new ArrayList<>();

        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            ResultSetMetaData meta = rs.getMetaData();
            int count = meta.getColumnCount();

            for (int i = 1; i <= count; i++) {
                columns.add(meta.getColumnLabel(i));
            }

            while (rs.next()) {
                Object[] row = new Object[count];

                for (int i = 1; i <= count; i++) {
                    row[i - 1] = rs.getObject(i);
                }

                rows.add(row);
            }
        }

        JTable table = new JTable(
                rows.toArray(new Object[0][]),
                columns.toArray(new String[0]));

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setFillsViewportHeight(true);
        table.setRowHeight(24);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(900, 400));

        JOptionPane.showMessageDialog(
                this, scrollPane,
                title + " (" + rows.size() + " rows)",
                JOptionPane.PLAIN_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(
                        UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
                // Use the default look and feel.
            }

            new SwingUI().setVisible(true);
        });
    }
}