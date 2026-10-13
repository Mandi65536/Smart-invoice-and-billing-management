package org.example;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Main {

    private static final Scanner sc = new Scanner(System.in);

    private static final ProductDAO productDAO = new ProductDAO();
    private static final CustomerDAO customerDAO = new CustomerDAO();
    private static final InvoiceDAO invoiceDAO = new InvoiceDAO();
    private static final InvoiceItemDAO itemDAO = new InvoiceItemDAO();
    private static final PaymentDAO paymentDAO = new PaymentDAO();
    private static final BillingService billingService = new BillingService();
    private static final InvoicePDFGenerator pdfGenerator = new InvoicePDFGenerator();

    // =====================================================================
    // main
    // =====================================================================

    public static void main(String[] args) {
        while (true) {
            System.out.println("\n====== SMART INVOICE AND BILLING SYSTEM ======");
            System.out.println("1.  Products");
            System.out.println("2.  Customers");
            System.out.println("3.  Invoices");
            System.out.println("4.  New Bill / POS");
            System.out.println("5.  Payments");
            System.out.println("6.  Reports");
            System.out.println("7.  Low Stock");
            System.out.println("8.  Generate Invoice PDF");
            System.out.println("9.  Sales Dashboard");
            System.out.println("0.  Exit");

            switch (readInt("Enter choice: ")) {
                case 1 -> productMenu();
                case 2 -> customerMenu();
                case 3 -> invoiceMenu();
                case 4 -> posMenu();
                case 5 -> paymentMenu();
                case 6 -> reportsMenu();
                case 7 -> productDAO.displayLowStockProducts();
                case 8 -> generatePdf();
                case 9 -> new SalesDashboard().displayDashboard();
                case 0 -> {
                    System.out.println("Application closed.");
                    sc.close();
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    // =====================================================================
    // INPUT HELPERS
    // =====================================================================

    private static String readText(String prompt) {
        while (true) {
            System.out.print(prompt);
            String value = sc.nextLine().trim();
            if (!value.isEmpty()) return value;
            System.out.println("This field cannot be empty.");
        }
    }

    private static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                return Integer.parseInt(sc.nextLine().trim());
            } catch (NumberFormatException e) {
                System.out.println("Enter a valid whole number.");
            }
        }
    }

    private static double readDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            try {
                double value = Double.parseDouble(sc.nextLine().trim());
                if (Double.isFinite(value)) return value;
            } catch (NumberFormatException ignored) {
            }
            System.out.println("Enter a valid number.");
        }
    }

    private static BigDecimal money(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static Connection connection() throws SQLException, java.io.IOException {
        return DBConnection.getConnection();
    }

    // =====================================================================
    // PRODUCTS
    // =====================================================================

    private static void productMenu() {
        while (true) {
            System.out.println("\n--- PRODUCT MANAGEMENT ---");
            System.out.println("1. View products");
            System.out.println("2. Search by SKU");
            System.out.println("3. Add product");
            System.out.println("4. Update product");
            System.out.println("5. Delete product");
            System.out.println("6. Increase stock");
            System.out.println("7. Reduce stock");
            System.out.println("0. Back");

            switch (readInt("Choice: ")) {
                case 1 -> viewProducts();
                case 2 -> {
                    Product p = productDAO.getProductBySku(readText("SKU: "));
                    if (p == null) System.out.println("Product not found.");
                    else showProduct(p);
                }
                case 3 -> addProduct();
                case 4 -> updateProduct();
                case 5 -> deleteProduct();
                case 6 -> changeStock(true);
                case 7 -> changeStock(false);
                case 0 -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void viewProducts() {
        String sql = """
                SELECT product_id, sku, product_name, category,
                       purchase_price, selling_price, current_stock,
                       reorder_level, is_active
                FROM products
                ORDER BY product_id
                """;

        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.printf("%-5s %-12s %-25s %-15s %-10s %-10s %-8s %-8s %-6s%n",
                    "ID", "SKU", "Name", "Category", "Purchase", "Selling", "Stock", "Reorder", "Active");
            System.out.println("-----------------------------------------------------------------------------------------------");

            while (rs.next()) {
                System.out.printf("%-5d %-12s %-25s %-15s %-10s %-10s %-8d %-8d %-6d%n",
                        rs.getInt("product_id"),
                        rs.getString("sku"),
                        rs.getString("product_name"),
                        rs.getString("category"),
                        rs.getBigDecimal("purchase_price").toPlainString(),
                        rs.getBigDecimal("selling_price").toPlainString(),
                        rs.getInt("current_stock"),
                        rs.getInt("reorder_level"),
                        rs.getInt("is_active"));
            }
        } catch (Exception e) {
            System.out.println("Unable to retrieve products: " + e.getMessage());
        }
    }

    private static void showProduct(Product p) {
        System.out.println("\nProduct ID: " + p.getProductId());
        System.out.println("SKU: " + p.getSku());
        System.out.println("Name: " + p.getProductName());
        System.out.println("Category: " + p.getCategory());
        System.out.printf("Purchase price: %.2f%n", p.getPurchasePrice());
        System.out.printf("Selling price: %.2f%n", p.getSellingPrice());
        System.out.println("Stock: " + p.getCurrentStock());
        System.out.println("Reorder level: " + p.getReorderLevel());
    }

    private static void addProduct() {
        String sku = readText("SKU: ");
        String name = readText("Product name: ");
        String category = readText("Category: ");
        double purchase = readDouble("Purchase price: ");
        double selling = readDouble("Selling price: ");
        int stock = readInt("Initial stock: ");
        int reorder = readInt("Reorder level: ");

        if (purchase < 0 || selling < 0 || stock < 0 || reorder < 0) {
            System.out.println("Prices and stock values cannot be negative.");
            return;
        }

        String sql = """
                INSERT INTO products
                (sku, product_name, category, purchase_price,
                 selling_price, current_stock, reorder_level, is_active)
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
            System.out.println("Product added.");
        } catch (Exception e) {
            System.out.println("Could not add product: " + e.getMessage());
        }
    }

    private static void updateProduct() {
        String sku = readText("SKU: ");

        String name = readText("New product name: ");
        double purchase = readDouble("New purchase price: ");
        double selling = readDouble("New selling price: ");
        int reorder = readInt("New reorder level: ");

        if (purchase < 0 || selling < 0 || reorder < 0) {
            System.out.println("Values cannot be negative.");
            return;
        }

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
            System.out.println(ps.executeUpdate() == 1
                    ? "Product updated."
                    : "Product not found.");
        } catch (Exception e) {
            System.out.println("Update failed: " + e.getMessage());
        }
    }

    private static void deleteProduct() {
        String sku = readText("SKU to delete: ");
        if (readText("Type YES to confirm: ").equals("YES")) {
            System.out.println(productDAO.deleteProduct(sku)
                    ? "Product deleted."
                    : "Deletion failed. It may be referenced by an invoice.");
        } else {
            System.out.println("Cancelled.");
        }
    }

    private static void changeStock(boolean increase) {
        String sku = readText("SKU: ");
        int quantity = readInt("Quantity: ");

        if (quantity <= 0) {
            System.out.println("Quantity must be positive.");
            return;
        }

        boolean ok = increase
                ? productDAO.increaseStock(sku, quantity)
                : productDAO.reduceStock(sku, quantity);

        System.out.println(ok ? "Stock updated." : "Stock update failed.");
    }

    // =====================================================================
    // CUSTOMERS
    // =====================================================================

    private static void customerMenu() {
        while (true) {
            System.out.println("\n--- CUSTOMER MANAGEMENT ---");
            System.out.println("1. View customers");
            System.out.println("2. Search by ID");
            System.out.println("3. Search by phone");
            System.out.println("4. Add customer");
            System.out.println("5. Update customer");
            System.out.println("6. Delete customer");
            System.out.println("0. Back");

            switch (readInt("Choice: ")) {
                case 1 -> {
                    List<Customer> customers = customerDAO.getAllCustomers();
                    if (customers.isEmpty()) System.out.println("No customers found.");
                    else customers.forEach(Main::showCustomer);
                }
                case 2 -> {
                    Customer c = customerDAO.getCustomerById(readInt("Customer ID: "));
                    if (c == null) System.out.println("Customer not found.");
                    else showCustomer(c);
                }
                case 3 -> {
                    Customer c = customerDAO.getCustomerByPhone(readText("Phone: "));
                    if (c == null) System.out.println("Customer not found.");
                    else showCustomer(c);
                }
                case 4 -> addCustomer();
                case 5 -> updateCustomer();
                case 6 -> deleteCustomer();
                case 0 -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void showCustomer(Customer c) {
        System.out.println("\nCustomer ID: " + c.getCustomerId());
        System.out.println("Name: " + c.getCustomerName());
        System.out.println("Phone: " + c.getPhone());
        System.out.println("Email: " + c.getEmail());
        System.out.println("Address: " + c.getBillingAddress());
    }

    private static void addCustomer() {
        Customer c = new Customer(
                0,
                readText("Name: "),
                readText("Phone: "),
                readText("Email: "),
                readText("Billing address: ")
        );

        System.out.println(customerDAO.addCustomer(c)
                ? "Customer added."
                : "Could not add customer.");
    }

    private static void updateCustomer() {
        int id = readInt("Customer ID: ");
        Customer old = customerDAO.getCustomerById(id);
        if (old == null) {
            System.out.println("Customer not found.");
            return;
        }

        Customer updated = new Customer(
                id,
                readText("New name: "),
                readText("New phone: "),
                readText("New email: "),
                readText("New address: ")
        );

        System.out.println(customerDAO.updateCustomer(updated)
                ? "Customer updated."
                : "Customer update failed.");
    }

    private static void deleteCustomer() {
        int id = readInt("Customer ID to delete: ");
        if (readText("Type YES to confirm: ").equals("YES")) {
            System.out.println(customerDAO.deleteCustomer(id)
                    ? "Customer deleted."
                    : "Deletion failed. The customer may have invoices.");
        } else {
            System.out.println("Cancelled.");
        }
    }

    // =====================================================================
    // INVOICES (management)
    // =====================================================================

    private static void invoiceMenu() {
        while (true) {
            System.out.println("\n--- INVOICE MANAGEMENT ---");
            System.out.println("1. View all invoices");
            System.out.println("2. Search invoice by ID");
            System.out.println("3. Search invoice by number");
            System.out.println("4. Cancel draft invoice");
            System.out.println("0. Back");

            switch (readInt("Choice: ")) {
                case 1 -> {
                    List<Invoice> invoices = invoiceDAO.getAllInvoices();
                    if (invoices.isEmpty()) System.out.println("No invoices found.");
                    else invoices.forEach(Main::showInvoice);
                }
                case 2 -> {
                    Invoice i = invoiceDAO.getInvoiceById(readInt("Invoice ID: "));
                    if (i == null) System.out.println("Invoice not found.");
                    else showInvoice(i);
                }
                case 3 -> {
                    Invoice i = invoiceDAO.getInvoiceByNumber(readText("Invoice number: "));
                    if (i == null) System.out.println("Invoice not found.");
                    else showInvoice(i);
                }
                case 4 -> {
                    int id = readInt("Invoice ID: ");
                    boolean ok = invoiceDAO.updateInvoiceStatus(id, "CANCELLED");
                    System.out.println(ok ? "Invoice cancelled." : "Could not cancel invoice.");
                }
                case 0 -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void showInvoice(Invoice i) {
        System.out.println("\nInvoice ID: " + i.getInvoiceId());
        System.out.println("Invoice number: " + i.getInvoiceNumber());
        System.out.println("Customer ID: " + i.getCustomerId());
        System.out.println("Date: " + i.getInvoiceDate());
        System.out.printf("Subtotal: %.2f%n", i.getSubtotal());
        System.out.printf("Discount: %.2f%n", i.getDiscountAmount());
        System.out.printf("Tax: %.2f%n", i.getTaxAmount());
        System.out.printf("Grand total: %.2f%n", i.getGrandTotal());
        System.out.println("Status: " + i.getStatus());
    }

    // =====================================================================
    // NEW BILL / POS  (console mirror of the Swing POS)
    // =====================================================================

    private static void posMenu() {
        System.out.println("\n--- NEW BILL / POS ---");

        int invoiceId = selectDraftInvoice();
        if (invoiceId <= 0) return;

        BigDecimal taxPercent = BigDecimal.ZERO;
        boolean taxSet = false;

        while (true) {
            printPosHeader(invoiceId);
            printCart(invoiceId);

            BigDecimal grandTotal = getGrandTotal(invoiceId);
            BigDecimal paid = getCompletedPayments(invoiceId);
            BigDecimal balance = grandTotal.subtract(paid).setScale(2, RoundingMode.HALF_UP);

            System.out.println("Grand total: ₹" + grandTotal.toPlainString());
            System.out.println("Paid: ₹" + paid.toPlainString());
            System.out.println("Balance: ₹" + balance.toPlainString());

            System.out.println("\n1. Add product");
            System.out.println("2. Change quantity of a line");
            System.out.println("3. Remove a line");
            System.out.println("4. Set tax %");
            System.out.println("5. PAY & CHECKOUT");
            System.out.println("6. Generate PDF");
            System.out.println("0. Back");

            int choice = readInt("Choice: ");

            switch (choice) {
                case 1 -> {
                    addProductToCart(invoiceId, taxPercent, taxSet);
                    taxSet = true;
                }
                case 2 -> {
                    changeCartQty(invoiceId, taxPercent);
                    taxSet = true;
                }
                case 3 -> {
                    removeCartLine(invoiceId, taxPercent);
                    taxSet = true;
                }
                case 4 -> {
                    double pct = readDouble("Tax percent (e.g. 5 for 5%): ");
                    if (pct < 0) {
                        System.out.println("Tax percent cannot be negative.");
                    } else {
                        taxPercent = BigDecimal.valueOf(pct);
                        taxSet = true;
                        recalcInvoice(invoiceId, taxPercent);
                        System.out.println("Tax applied.");
                    }
                }
                case 5 -> {
                    if (payAndCheckout(invoiceId, taxPercent)) {
                        return;
                    }
                }
                case 6 -> {
                    recalcInvoice(invoiceId, taxPercent);
                    pdfGenerator.generateInvoicePDF(invoiceId);
                }
                case 0 -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static int selectDraftInvoice() {
        List<int[]> drafts = new ArrayList<>();
        List<String> labels = new ArrayList<>();

        String sql = """
                SELECT i.invoice_id, i.invoice_number, c.customer_name
                FROM invoices i
                JOIN customers c ON c.customer_id = i.customer_id
                WHERE i.status = 'DRAFT'
                ORDER BY i.invoice_id DESC
                """;

        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                drafts.add(new int[]{rs.getInt("invoice_id")});
                labels.add("#" + rs.getInt("invoice_id")
                        + " · " + rs.getString("invoice_number")
                        + " · " + rs.getString("customer_name"));
            }
        } catch (Exception e) {
            System.out.println("Unable to load drafts: " + e.getMessage());
            return -1;
        }

        System.out.println("\nDraft invoices:");
        for (int i = 0; i < labels.size(); i++) {
            System.out.println((i + 1) + ". " + labels.get(i));
        }
        System.out.println("N. Create new draft invoice");
        System.out.println("0. Cancel");

        int choice = readInt("Select: ");

        if (choice == 0) return -1;
        if (choice == labels.size() + 1 || choice == 'N' - '0') {
            return createNewDraftInvoiceConsole();
        }
        if (choice < 1 || choice > labels.size()) {
            System.out.println("Invalid selection.");
            return -1;
        }
        return drafts.get(choice - 1)[0];
    }

    private static int createNewDraftInvoiceConsole() {
        List<Customer> customers = customerDAO.getAllCustomers();

        if (customers.isEmpty()) {
            System.out.println("No customers exist yet.");
            if (readText("Add a new customer now? (YES/NO): ").equalsIgnoreCase("YES")) {
                addCustomer();
                customers = customerDAO.getAllCustomers();
            }
            if (customers.isEmpty()) return -1;
        }

        System.out.println("\nSelect customer:");
        for (int i = 0; i < customers.size(); i++) {
            Customer c = customers.get(i);
            System.out.println((i + 1) + ". #" + c.getCustomerId()
                    + " · " + c.getCustomerName()
                    + " · " + (c.getPhone() == null ? "" : c.getPhone()));
        }
        System.out.println((customers.size() + 1) + ". + New customer");

        int pick = readInt("Select: ");
        int customerId;

        if (pick == customers.size() + 1) {
            addCustomer();
            customers = customerDAO.getAllCustomers();
            customerId = customers.get(customers.size() - 1).getCustomerId();
        } else if (pick >= 1 && pick <= customers.size()) {
            customerId = customers.get(pick - 1).getCustomerId();
        } else {
            System.out.println("Invalid selection.");
            return -1;
        }

        System.out.println("\nDiscount type: 1. PERCENT   2. FIXED");
        int typeChoice = readInt("Choice: ");
        String discountType = (typeChoice == 2) ? "FIXED" : "PERCENT";

        double discountValue = readDouble("Discount value (0 if none): ");
        if (discountValue < 0) {
            System.out.println("Discount cannot be negative.");
            return -1;
        }
        if ("PERCENT".equals(discountType) && discountValue > 100) {
            System.out.println("Percentage discount cannot exceed 100.");
            return -1;
        }

        String invoiceNumber = "INV-" + System.currentTimeMillis();

        String sql = """
                INSERT INTO invoices
                (invoice_number, customer_id, invoice_date,
                 subtotal, discount_type, discount_value,
                 discount_amount, tax_amount, grand_total, status)
                VALUES (?, ?, CURRENT_TIMESTAMP, 0, ?, ?, 0, 0, 0, 'DRAFT')
                """;

        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, invoiceNumber);
            ps.setInt(2, customerId);
            ps.setString(3, discountType);
            ps.setBigDecimal(4, BigDecimal.valueOf(discountValue));
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int newId = keys.getInt(1);
                    System.out.println("Draft invoice created: " + invoiceNumber
                            + " (ID " + newId + ")");
                    return newId;
                }
            }
        } catch (Exception e) {
            System.out.println("Could not create draft: " + e.getMessage());
        }
        return -1;
    }

    private static void printPosHeader(int invoiceId) {
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement("""
                     SELECT i.invoice_number, c.customer_name
                     FROM invoices i
                     JOIN customers c ON c.customer_id = i.customer_id
                     WHERE i.invoice_id = ?
                     """)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("\n========== POS ==========");
                    System.out.println("Invoice: " + rs.getString("invoice_number"));
                    System.out.println("Customer: " + rs.getString("customer_name"));
                }
            }
        } catch (Exception e) {
            System.out.println("Unable to load invoice header: " + e.getMessage());
        }
    }

    private static void printCart(int invoiceId) {
        String sql = """
                SELECT ii.item_id, p.sku, p.product_name,
                       ii.quantity, ii.unit_price,
                       ii.discount_percent, ii.line_total
                FROM invoice_items ii
                JOIN products p ON p.product_id = ii.product_id
                WHERE ii.invoice_id = ?
                ORDER BY ii.item_id
                """;

        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);

            try (ResultSet rs = ps.executeQuery()) {
                System.out.println("\n--- CART ---");
                System.out.printf("%-5s %-12s %-25s %-6s %-10s %-8s %-10s%n",
                        "ID", "SKU", "Name", "Qty", "Price", "Disc%", "Total");
                System.out.println("---------------------------------------------------------------------------------");

                boolean any = false;
                while (rs.next()) {
                    any = true;
                    System.out.printf("%-5d %-12s %-25s %-6d %-10s %-8s %-10s%n",
                            rs.getInt("item_id"),
                            rs.getString("sku"),
                            rs.getString("product_name"),
                            rs.getInt("quantity"),
                            rs.getBigDecimal("unit_price").toPlainString(),
                            rs.getBigDecimal("discount_percent").toPlainString(),
                            rs.getBigDecimal("line_total").toPlainString());
                }
                if (!any) System.out.println("(empty)");
            }
        } catch (Exception e) {
            System.out.println("Unable to load cart: " + e.getMessage());
        }
    }

    private static void addProductToCart(int invoiceId, BigDecimal taxPercent, boolean taxSet) {
        String sku = readText("Product SKU: ");

        int productId;
        BigDecimal price;
        int stock;

        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement("""
                     SELECT product_id, selling_price, current_stock
                     FROM products WHERE sku = ? AND is_active = 1
                     """)) {
            ps.setString(1, sku);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Active product not found.");
                    return;
                }
                productId = rs.getInt("product_id");
                price = rs.getBigDecimal("selling_price");
                stock = rs.getInt("current_stock");
            }
        } catch (Exception e) {
            System.out.println("Lookup failed: " + e.getMessage());
            return;
        }

        int qty = readInt("Quantity: ");
        if (qty <= 0) {
            System.out.println("Quantity must be positive.");
            return;
        }
        if (qty > stock) {
            System.out.println("Insufficient stock. Available: " + stock);
            return;
        }

        double discPct = readDouble("Item discount percent (0-100): ");
        if (discPct < 0 || discPct > 100) {
            System.out.println("Discount must be between 0 and 100.");
            return;
        }

        // If this product is already on the invoice, increment that line
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT item_id, quantity FROM invoice_items WHERE invoice_id = ? AND product_id = ?")) {
            ps.setInt(1, invoiceId);
            ps.setInt(2, productId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int itemId = rs.getInt("item_id");
                    int newQty = rs.getInt("quantity") + qty;
                    if (newQty > stock) {
                        System.out.println("Insufficient stock. Available: " + stock);
                        return;
                    }
                    updateLine(itemId, newQty, price);
                    System.out.println("Line updated.");
                    recalcInvoice(invoiceId, taxPercent);
                    return;
                }
            }
        } catch (Exception e) {
            System.out.println("Cart update failed: " + e.getMessage());
            return;
        }

        // Otherwise insert a new line
        BigDecimal discPercentBd = BigDecimal.valueOf(discPct);
        BigDecimal gross = price.multiply(BigDecimal.valueOf(qty))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal itemDiscount = gross.multiply(discPercentBd)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal lineTotal = gross.subtract(itemDiscount)
                .setScale(2, RoundingMode.HALF_UP);

        String sql = """
                INSERT INTO invoice_items
                (invoice_id, product_id, quantity, unit_price,
                 discount_percent, discount_amount, line_total)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            ps.setInt(2, productId);
            ps.setInt(3, qty);
            ps.setBigDecimal(4, price);
            ps.setBigDecimal(5, discPercentBd);
            ps.setBigDecimal(6, itemDiscount);
            ps.setBigDecimal(7, lineTotal);
            ps.executeUpdate();
            System.out.println("Item added. Line total: ₹" + lineTotal.toPlainString());
        } catch (Exception e) {
            System.out.println("Insert failed: " + e.getMessage());
            return;
        }

        recalcInvoice(invoiceId, taxPercent);
    }

    private static void changeCartQty(int invoiceId, BigDecimal taxPercent) {
        int itemId = readInt("Item ID to change: ");
        int newQty = readInt("New quantity (0 removes the line): ");

        if (newQty < 0) {
            System.out.println("Quantity cannot be negative.");
            return;
        }

        if (newQty == 0) {
            try (Connection c = connection();
                 PreparedStatement ps = c.prepareStatement(
                         "DELETE FROM invoice_items WHERE item_id = ?")) {
                ps.setInt(1, itemId);
                ps.executeUpdate();
                System.out.println("Line removed.");
            } catch (Exception e) {
                System.out.println("Delete failed: " + e.getMessage());
            }
            recalcInvoice(invoiceId, taxPercent);
            return;
        }

        // Look up price and stock for the line's product
        BigDecimal price;
        int stock;
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement("""
                     SELECT ii.unit_price, p.current_stock
                     FROM invoice_items ii
                     JOIN products p ON p.product_id = ii.product_id
                     WHERE ii.item_id = ?
                     """)) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    System.out.println("Line not found.");
                    return;
                }
                price = rs.getBigDecimal("unit_price");
                stock = rs.getInt("current_stock");
            }
        } catch (Exception e) {
            System.out.println("Lookup failed: " + e.getMessage());
            return;
        }

        if (newQty > stock) {
            System.out.println("Insufficient stock. Available: " + stock);
            return;
        }

        updateLine(itemId, newQty, price);
        System.out.println("Line updated.");
        recalcInvoice(invoiceId, taxPercent);
    }

    private static void removeCartLine(int invoiceId, BigDecimal taxPercent) {
        int itemId = readInt("Item ID to remove: ");
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM invoice_items WHERE item_id = ?")) {
            ps.setInt(1, itemId);
            System.out.println(ps.executeUpdate() > 0
                    ? "Line removed."
                    : "Line not found.");
        } catch (Exception e) {
            System.out.println("Delete failed: " + e.getMessage());
        }
        recalcInvoice(invoiceId, taxPercent);
    }

    private static void updateLine(int itemId, int newQty, BigDecimal price) {
        BigDecimal discountPercent;
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT discount_percent FROM invoice_items WHERE item_id = ?")) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return;
                discountPercent = rs.getBigDecimal("discount_percent");
            }
        } catch (Exception e) {
            System.out.println("Failed to read line: " + e.getMessage());
            return;
        }

        BigDecimal gross = price.multiply(BigDecimal.valueOf(newQty))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal itemDiscount = gross.multiply(discountPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal lineTotal = gross.subtract(itemDiscount)
                .setScale(2, RoundingMode.HALF_UP);

        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement("""
                     UPDATE invoice_items
                     SET quantity = ?, line_total = ?, discount_amount = ?
                     WHERE item_id = ?
                     """)) {
            ps.setInt(1, newQty);
            ps.setBigDecimal(2, lineTotal);
            ps.setBigDecimal(3, itemDiscount);
            ps.setInt(4, itemId);
            ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("Update failed: " + e.getMessage());
        }
    }

    // Inline recalc: PERCENT + FIXED, tax as percent, discount_value untouched.
    private static void recalcInvoice(int invoiceId, BigDecimal taxPercent) {
        String readSql = """
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
             PreparedStatement ps = c.prepareStatement(readSql)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return;
                subtotal = rs.getBigDecimal("subtotal");
                discountValue = rs.getBigDecimal("discount_value");
                discountType = rs.getString("discount_type");
            }
        } catch (Exception e) {
            System.out.println("Recalc read failed: " + e.getMessage());
            return;
        }

        BigDecimal discountAmount;
        if ("FIXED".equals(discountType)) {
            discountAmount = discountValue.min(subtotal);
        } else {
            discountAmount = subtotal.multiply(discountValue)
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        BigDecimal taxable = subtotal.subtract(discountAmount);
        BigDecimal taxAmount = taxable.multiply(taxPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal grandTotal = taxable.add(taxAmount)
                .setScale(2, RoundingMode.HALF_UP);

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
        } catch (Exception e) {
            System.out.println("Recalc update failed: " + e.getMessage());
        }
    }

    // =====================================================================
    // PAY & CHECKOUT
    // =====================================================================

    private static boolean payAndCheckout(int invoiceId, BigDecimal taxPercent) {
        recalcInvoice(invoiceId, taxPercent);

        BigDecimal grandTotal = getGrandTotal(invoiceId);
        BigDecimal paid = getCompletedPayments(invoiceId);
        BigDecimal balance = grandTotal.subtract(paid).setScale(2, RoundingMode.HALF_UP);

        if (balance.signum() <= 0) {
            System.out.println("Already fully paid. Completing invoice…");
            return doCheckout(invoiceId);
        }

        System.out.println("Outstanding balance: ₹" + balance.toPlainString());
        double amount = readDouble("Payment amount: ");

        if (amount <= 0 || BigDecimal.valueOf(amount).compareTo(balance) > 0) {
            System.out.println("Payment must be positive and not exceed the balance.");
            return false;
        }

        System.out.println("Payment method: 1. CASH  2. CARD  3. UPI  4. BANK_TRANSFER");
        int methodChoice = readInt("Choice: ");
        String method = switch (methodChoice) {
            case 1 -> "CASH";
            case 2 -> "CARD";
            case 3 -> "UPI";
            case 4 -> "BANK_TRANSFER";
            default -> null;
        };

        if (method == null) {
            System.out.println("Invalid method.");
            return false;
        }

        String insertSql = """
                INSERT INTO payments
                (invoice_id, amount, payment_method,
                 payment_date, payment_status)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP, 'COMPLETED')
                """;

        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(insertSql)) {
            ps.setInt(1, invoiceId);
            ps.setBigDecimal(2, BigDecimal.valueOf(amount));
            ps.setString(3, method);
            ps.executeUpdate();
        } catch (Exception e) {
            System.out.println("Payment failed: " + e.getMessage());
            return false;
        }

        paid = getCompletedPayments(invoiceId);
        if (paid.compareTo(grandTotal) == 0) {
            System.out.println("Fully paid. Completing invoice…");
            return doCheckout(invoiceId);
        } else {
            BigDecimal remaining = grandTotal.subtract(paid)
                    .setScale(2, RoundingMode.HALF_UP);
            System.out.println("Payment recorded. Remaining balance: ₹"
                    + remaining.toPlainString());
            return false;
        }
    }

    private static boolean doCheckout(int invoiceId) {
        String confirm = readText("Type YES to confirm checkout: ");
        if (!confirm.equalsIgnoreCase("YES")) {
            System.out.println("Cancelled.");
            return false;
        }

        boolean ok = billingService.completeInvoice(invoiceId);
        if (ok) {
            System.out.println("Invoice completed. Stock updated.");
            return true;
        } else {
            System.out.println("Checkout failed. See the messages above.");
            return false;
        }
    }

    private static BigDecimal getGrandTotal(int invoiceId) {
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT grand_total FROM invoices WHERE invoice_id = ?")) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal("grand_total");
            }
        } catch (Exception e) {
            System.out.println("Grand total read failed: " + e.getMessage());
        }
        return BigDecimal.ZERO;
    }

    private static BigDecimal getCompletedPayments(int invoiceId) {
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement("""
                     SELECT COALESCE(SUM(amount), 0) AS paid
                     FROM payments
                     WHERE invoice_id = ? AND payment_status = 'COMPLETED'
                     """)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal("paid");
            }
        } catch (Exception e) {
            System.out.println("Payment read failed: " + e.getMessage());
            return BigDecimal.ZERO;
        }
    }

    // =====================================================================
    // PAYMENTS (management)
    // =====================================================================

    private static void paymentMenu() {
        while (true) {
            System.out.println("\n--- PAYMENT MANAGEMENT ---");
            System.out.println("1. View payments for invoice");
            System.out.println("2. Search payment by ID");
            System.out.println("3. Payment summary");
            System.out.println("0. Back");

            switch (readInt("Choice: ")) {
                case 1 -> {
                    int invoiceId = readInt("Invoice ID: ");
                    List<Payment> payments = paymentDAO.getPaymentsByInvoiceId(invoiceId);
                    if (payments.isEmpty()) System.out.println("No payments found.");
                    else payments.forEach(Main::showPayment);
                }
                case 2 -> {
                    Payment p = paymentDAO.getPaymentById(readInt("Payment ID: "));
                    if (p == null) System.out.println("Payment not found.");
                    else showPayment(p);
                }
                case 3 -> paymentDAO.displayPaymentSummary();
                case 0 -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void showPayment(Payment p) {
        System.out.println("\nPayment ID: " + p.getPaymentId());
        System.out.println("Invoice ID: " + p.getInvoiceId());
        System.out.printf("Amount: %.2f%n", p.getAmount());
        System.out.println("Method: " + p.getPaymentMethod());
        System.out.println("Date: " + p.getPaymentDate());
        System.out.println("Status: " + p.getStatus());
    }

    // =====================================================================
    // REPORTS
    // =====================================================================

    private static void reportsMenu() {
        while (true) {
            System.out.println("\n--- REPORTS ---");
            System.out.println("1. Sales summary");
            System.out.println("2. Payment summary");
            System.out.println("3. Stock movement history");
            System.out.println("4. Invoice summary");
            System.out.println("0. Back");

            switch (readInt("Choice: ")) {
                case 1 -> runReport("""
                        SELECT COUNT(*) AS completed_invoices,
                               COALESCE(SUM(grand_total), 0) AS total_sales
                        FROM invoices
                        WHERE status = 'COMPLETED'
                        """);
                case 2 -> runReport("""
                        SELECT payment_method, COUNT(*) AS payment_count,
                               SUM(amount) AS total_amount
                        FROM payments
                        WHERE payment_status = 'COMPLETED'
                        GROUP BY payment_method
                        ORDER BY total_amount DESC
                        """);
                case 3 -> runReport("""
                        SELECT sm.movement_id, p.sku, p.product_name,
                               sm.movement_type, sm.quantity,
                               sm.reference_type, sm.reference_id,
                               sm.notes, sm.movement_date
                        FROM stock_movements sm
                        JOIN products p ON p.product_id = sm.product_id
                        ORDER BY sm.movement_id DESC
                        """);
                case 4 -> runReport("""
                        SELECT i.invoice_id, i.invoice_number,
                               c.customer_name, i.invoice_date,
                               i.subtotal, i.discount_amount,
                               i.tax_amount, i.grand_total, i.status
                        FROM invoices i
                        JOIN customers c ON c.customer_id = i.customer_id
                        ORDER BY i.invoice_id DESC
                        """);
                case 0 -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void runReport(String sql) {
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            java.sql.ResultSetMetaData meta = rs.getMetaData();
            int cols = meta.getColumnCount();

            StringBuilder header = new StringBuilder();
            for (int i = 1; i <= cols; i++) {
                header.append(String.format("%-20s", meta.getColumnLabel(i)));
            }
            System.out.println();
            System.out.println(header);
            System.out.println("-".repeat(cols * 20));

            int count = 0;
            while (rs.next()) {
                StringBuilder row = new StringBuilder();
                for (int i = 1; i <= cols; i++) {
                    Object v = rs.getObject(i);
                    row.append(String.format("%-20s", v == null ? "" : v.toString()));
                }
                System.out.println(row);
                count++;
            }
            System.out.println("(" + count + " rows)");
        } catch (Exception e) {
            System.out.println("Report failed: " + e.getMessage());
        }
    }

    // =====================================================================
    // PDF
    // =====================================================================

    private static void generatePdf() {
        int invoiceId = readInt("Invoice ID: ");
        pdfGenerator.generateInvoicePDF(invoiceId);
    }
}