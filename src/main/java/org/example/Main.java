package org.example;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    public static void main(String[] args) {
        while (true) {
            System.out.println("\n====== SMART INVOICE AND BILLING SYSTEM ======");
            System.out.println("1. Manage Products");
            System.out.println("2. Manage Customers");
            System.out.println("3. Manage Invoices");
            System.out.println("4. Manage Invoice Items");
            System.out.println("5. Manage Payments");
            System.out.println("6. Billing Summary");
            System.out.println("7. Low-Stock Report");
            System.out.println("8. Payment Summary");
            System.out.println("9. Generate Invoice PDF");
            System.out.println("10. Sales Dashboard");
            System.out.println("0. Exit");

            switch (readInt("Enter choice: ")) {
                case 1 -> productMenu();
                case 2 -> customerMenu();
                case 3 -> invoiceMenu();
                case 4 -> itemMenu();
                case 5 -> paymentMenu();
                case 6 -> billingSummary();
                case 7-> {
                    productDAO.displayLowStockProducts();
                    break;
                }
                case 8->{
                    paymentDAO.displayPaymentSummary();
                    break;
                }
                case 9-> {
                    int invoiceId = readInt("Enter Invoice ID: ");

                    InvoicePDFGenerator pdfGenerator =
                            new InvoicePDFGenerator();

                    pdfGenerator.generateInvoicePDF(invoiceId);
                    break;
                }
                case 10-> {
                    SalesDashboard dashboard = new SalesDashboard();
                    dashboard.displayDashboard();
                    break;
                }
                case 0 -> {
                    System.out.println("Application closed.");
                    sc.close();
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    // ---------------- INPUT HELPERS ----------------

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
                // Show validation message below.
            }
            System.out.println("Enter a valid number.");
        }
    }

    private static BigDecimal money(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    // ---------------- PRODUCTS ----------------

    private static void productMenu() {
        while (true) {
            System.out.println("\n--- PRODUCT MANAGEMENT ---");
            System.out.println("1. View products");
            System.out.println("2. Search by SKU");
            System.out.println("3. Add product");
            System.out.println("4. Update selling price");
            System.out.println("5. Delete product");
            System.out.println("6. Increase stock");
            System.out.println("7. Reduce stock");
            System.out.println("0. Back");

            switch (readInt("Choice: ")) {
                case 1 -> productDAO.getAllProducts();
                case 2 -> {
                    Product p = productDAO.getProductBySku(readText("SKU: "));
                    if (p == null) System.out.println("Product not found.");
                    else showProduct(p);
                }
                case 3 -> addProduct();
                case 4 -> updateProductPrice();
                case 5 -> deleteProduct();
                case 6 -> changeStock(true);
                case 7 -> changeStock(false);
                case 0 -> { return; }
                default -> System.out.println("Invalid choice.");
            }
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

        Product p = new Product(
                0, sku, name, category, purchase, selling, stock, reorder
        );

        System.out.println(productDAO.addProduct(p)
                ? "Product added."
                : "Could not add product. Check the SKU.");
    }

    private static void updateProductPrice() {
        String sku = readText("SKU: ");
        Product p = productDAO.getProductBySku(sku);
        if (p == null) {
            System.out.println("Product not found.");
            return;
        }

        double price = readDouble("New selling price: ");
        if (price < 0) {
            System.out.println("Price cannot be negative.");
            return;
        }

        p.setSellingPrice(price);
        System.out.println(productDAO.updateProduct(p)
                ? "Price updated."
                : "Price update failed.");
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

    // ---------------- CUSTOMERS ----------------

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
                    if (customers == null || customers.isEmpty()) {
                        System.out.println("No customers found.");
                    } else {
                        for (Customer c : customers) showCustomer(c);
                    }
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

    // ---------------- INVOICES ----------------

    private static void invoiceMenu() {
        while (true) {
            System.out.println("\n--- INVOICE MANAGEMENT ---");
            System.out.println("1. Create invoice");
            System.out.println("2. View all invoices");
            System.out.println("3. Search invoice by ID");
            System.out.println("4. Search invoice by number");
            System.out.println("5. Complete invoice");
            System.out.println("6. Cancel draft invoice");
            System.out.println("0. Back");

            switch (readInt("Choice: ")) {
                case 1 -> createInvoice();
                case 2 -> {
                    List<Invoice> invoices = invoiceDAO.getAllInvoices();
                    if (invoices == null || invoices.isEmpty()) {
                        System.out.println("No invoices found.");
                    } else {
                        for (Invoice i : invoices) showInvoice(i);
                    }
                }
                case 3 -> {
                    Invoice i = invoiceDAO.getInvoiceById(readInt("Invoice ID: "));
                    if (i == null) System.out.println("Invoice not found.");
                    else showInvoice(i);
                }
                case 4 -> {
                    Invoice i = invoiceDAO.getInvoiceByNumber(readText("Invoice number: "));
                    if (i == null) System.out.println("Invoice not found.");
                    else showInvoice(i);
                }
                case 5 -> changeInvoiceStatus("COMPLETED");
                case 6 -> changeInvoiceStatus("CANCELLED");
                case 0 -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void createInvoice() {
        int customerId = readInt("Customer ID: ");
        Customer customer = customerDAO.getCustomerById(customerId);

        if (customer == null) {
            System.out.println("Customer not found. Add the customer first.");
            return;
        }

        String invoiceNumber = "INV-" +
                LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE) +
                "-" + System.currentTimeMillis() % 100000;

        Invoice invoice = new Invoice(
                0,
                invoiceNumber,
                customerId,
                LocalDate.now(),
                0.0,
                "PERCENT",
                0.0,
                0.0,
                0.0,
                0.0,
                "DRAFT"
        );

        if (invoiceDAO.addInvoice(invoice)) {
            Invoice saved = invoiceDAO.getInvoiceByNumber(invoiceNumber);
            if (saved != null) {
                System.out.println("Invoice created successfully.");
                showInvoice(saved);
                System.out.println("Add products using Manage Invoice Items.");
            } else {
                System.out.println("Invoice was added, but could not be retrieved.");
            }
        } else {
            System.out.println("Invoice creation failed.");
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


    private static void changeInvoiceStatus(String status) {
        int invoiceId = readInt("Enter Invoice ID: ");

        if ("COMPLETED".equalsIgnoreCase(status)) {
            billingService.completeInvoice(invoiceId);
        } else if ("CANCELLED".equalsIgnoreCase(status)) {
            boolean updated =
                    invoiceDAO.updateInvoiceStatus(invoiceId, "CANCELLED");

            System.out.println(updated
                    ? "Invoice cancelled."
                    : "Could not cancel invoice.");
        } else {
            System.out.println("Unsupported status.");
        }
    }


    // ---------------- INVOICE ITEMS ----------------

    private static void itemMenu() {
        while (true) {
            System.out.println("\n--- INVOICE ITEMS ---");
            System.out.println("1. Add product to draft invoice");
            System.out.println("2. View items for invoice");
            System.out.println("3. Recalculate invoice totals");
            System.out.println("4. Delete invoice item");
            System.out.println("0. Back");

            switch (readInt("Choice: ")) {
                case 1 -> addInvoiceItem();
                case 2 -> viewInvoiceItems();
                case 3 -> recalculateInvoice();
                case 4 -> deleteInvoiceItem();
                case 0 -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void addInvoiceItem() {
        int invoiceId = readInt("Invoice ID: ");
        Invoice invoice = invoiceDAO.getInvoiceById(invoiceId);

        if (invoice == null || !"DRAFT".equals(invoice.getStatus())) {
            System.out.println("A valid draft invoice is required.");
            return;
        }

        String sku = readText("Product SKU: ");
        Product product = productDAO.getProductBySku(sku);

        if (product == null) {
            System.out.println("Product not found.");
            return;
        }

        int quantity = readInt("Quantity: ");
        if (quantity <= 0) {
            System.out.println("Quantity must be positive.");
            return;
        }

        if (product.getCurrentStock() < quantity) {
            System.out.println("Insufficient stock. Available: " + product.getCurrentStock());
            return;
        }

        double discountPercent = readDouble("Item discount percent (0-100): ");
        if (discountPercent < 0 || discountPercent > 100) {
            System.out.println("Discount must be between 0 and 100.");
            return;
        }

        BigDecimal gross = money(product.getSellingPrice())
                .multiply(BigDecimal.valueOf(quantity))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal discount = gross
                .multiply(BigDecimal.valueOf(discountPercent))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);

        BigDecimal lineTotal = gross.subtract(discount);

        InvoiceItem item = new InvoiceItem(
                0,
                invoiceId,
                product.getProductId(),
                quantity,
                product.getSellingPrice(),
                discountPercent,
                discount.doubleValue(),
                lineTotal.doubleValue()
        );

        if (itemDAO.addInvoiceItem(item)) {
            System.out.println("Invoice item added.");
            System.out.println("Line total: " + lineTotal);
            System.out.println("Recalculate invoice totals from the invoice-items menu.");
        } else {
            System.out.println("Could not add invoice item.");
        }
    }

    private static void viewInvoiceItems() {
        int invoiceId = readInt("Invoice ID: ");
        List<InvoiceItem> items = itemDAO.getItemsByInvoiceId(invoiceId);

        if (items == null || items.isEmpty()) {
            System.out.println("No items found for this invoice.");
            return;
        }

        for (InvoiceItem item : items) {
            System.out.println("\nItem ID: " + item.getInvoiceItemId());
            System.out.println("Product ID: " + item.getProductId());
            System.out.println("Quantity: " + item.getQuantity());
            System.out.printf("Unit price: %.2f%n", item.getUnitPrice());
            System.out.printf("Discount: %.2f%n", item.getDiscountAmount());
            System.out.printf("Line total: %.2f%n", item.getLineTotal());
        }
    }

    private static void recalculateInvoice() {
        int invoiceId = readInt("Invoice ID: ");
        Invoice invoice = invoiceDAO.getInvoiceById(invoiceId);

        if (invoice == null || !"DRAFT".equals(invoice.getStatus())) {
            System.out.println("A valid draft invoice is required.");
            return;
        }

        double discount = readDouble("Invoice discount percent (0-100): ");
        double tax = readDouble("Tax amount: ");

        if (discount < 0 || discount > 100 || tax < 0) {
            System.out.println("Invalid discount or tax.");
            return;
        }

        boolean ok = billingService.updateDraftInvoiceTotals(
                invoiceId,
                BigDecimal.valueOf(discount),
                BigDecimal.valueOf(tax)
        );

        if (ok) {
            System.out.println("Invoice totals updated.");
            showInvoice(invoiceDAO.getInvoiceById(invoiceId));
        } else {
            System.out.println("Could not update invoice totals.");
        }
    }

    private static void deleteInvoiceItem() {
        int itemId = readInt("Invoice item ID to delete: ");
        System.out.println(itemDAO.deleteInvoiceItem(itemId)
                ? "Invoice item deleted. Recalculate the draft invoice totals afterward."
                : "Could not delete invoice item.");
    }

    // ---------------- PAYMENTS ----------------

    private static void paymentMenu() {
        while (true) {
            System.out.println("\n--- PAYMENT MANAGEMENT ---");
            System.out.println("1. Record payment");
            System.out.println("2. View payments for invoice");
            System.out.println("3. Search payment by ID");
            System.out.println("0. Back");

            switch (readInt("Choice: ")) {
                case 1 -> recordPayment();
                case 2 -> viewPayments();
                case 3 -> {
                    Payment p = paymentDAO.getPaymentById(readInt("Payment ID: "));
                    if (p == null) System.out.println("Payment not found.");
                    else showPayment(p);
                }
                case 0 -> { return; }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    private static void recordPayment() {
        int invoiceId = readInt("Invoice ID: ");
        Invoice invoice = invoiceDAO.getInvoiceById(invoiceId);

        if (invoice == null || !"DRAFT".equals(invoice.getStatus())) {
            System.out.println("A valid draft invoice is required to record a payment.");
            return;
        }

        double amount = readDouble("Payment amount: ");
        if (amount <= 0) {
            System.out.println("Payment amount must be positive.");
            return;
        }

        System.out.println("Methods: CASH, CARD, UPI, BANK_TRANSFER");
        String method = readText("Payment method: ").toUpperCase();

        System.out.println("Statuses: COMPLETED, PENDING");
        String status = readText("Payment status: ").toUpperCase();

        if (!method.matches("CASH|CARD|UPI|BANK_TRANSFER")
                || !status.matches("COMPLETED|PENDING")) {
            System.out.println("Invalid payment method or status.");
            return;
        }

        try {
            Payment payment = new Payment(
                    0,
                    invoiceId,
                    amount,
                    method,
                    LocalDateTime.now(),
                    status
            );

            System.out.println(paymentDAO.addPayment(payment)
                    ? "Payment recorded."
                    : "Payment could not be recorded. Check the invoice balance.");
        } catch (IllegalArgumentException e) {
            System.out.println("Invalid payment: " + e.getMessage());
        }
    }

    private static void viewPayments() {
        int invoiceId = readInt("Invoice ID: ");
        List<Payment> payments = paymentDAO.getPaymentsByInvoiceId(invoiceId);

        if (payments == null || payments.isEmpty()) {
            System.out.println("No payments found.");
            return;
        }

        for (Payment p : payments) showPayment(p);
    }

    private static void showPayment(Payment p) {
        System.out.println("\nPayment ID: " + p.getPaymentId());
        System.out.println("Invoice ID: " + p.getInvoiceId());
        System.out.printf("Amount: %.2f%n", p.getAmount());
        System.out.println("Method: " + p.getPaymentMethod());
        System.out.println("Date: " + p.getPaymentDate());
        System.out.println("Status: " + p.getStatus());
    }

    // ---------------- BILLING SUMMARY ----------------

    private static void billingSummary() {
        System.out.println("\n--- BILLING SUMMARY ---");

        List<Invoice> invoices = invoiceDAO.getAllInvoices();

        if (invoices == null || invoices.isEmpty()) {
            System.out.println("No invoices available.");
            return;
        }

        BigDecimal totalInvoiced = BigDecimal.ZERO;
        BigDecimal completedInvoices = BigDecimal.ZERO;
        BigDecimal draftInvoices = BigDecimal.ZERO;
        BigDecimal cancelledInvoices = BigDecimal.ZERO;

        for (Invoice invoice : invoices) {
            BigDecimal total = money(invoice.getGrandTotal());
            totalInvoiced = totalInvoiced.add(total);

            switch (invoice.getStatus()) {
                case "COMPLETED" -> completedInvoices = completedInvoices.add(total);
                case "DRAFT" -> draftInvoices = draftInvoices.add(total);
                case "CANCELLED" -> cancelledInvoices = cancelledInvoices.add(total);
            }
        }

        System.out.println("Number of in" +
                "voices: " + invoices.size());
        System.out.println("Total invoice value: " + totalInvoiced);
        System.out.println("Completed invoice value: " + completedInvoices);
        System.out.println("Draft invoice value: " + draftInvoices);
        System.out.println("Cancelled invoice value: " + cancelledInvoices);
        System.out.println("\nIndividual invoices:");

        for (Invoice invoice : invoices) {
            System.out.printf(
                    "%s | Customer ID: %d | Total: %.2f | Status: %s%n",
                    invoice.getInvoiceNumber(),
                    invoice.getCustomerId(),
                    invoice.getGrandTotal(),
                    invoice.getStatus()
            );
        }
    }
}