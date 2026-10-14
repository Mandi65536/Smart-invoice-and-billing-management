package org.example;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SwingUI extends JFrame {

    // ---------- DAOs / services ----------
    private final ProductDAO productDAO = new ProductDAO();
    private final InvoicePDFGenerator pdfGenerator = new InvoicePDFGenerator();
    private final BillingService billingService = new BillingService();

    // ---------- Colors ----------
    private final Color background = new Color(245, 247, 250);
    private final Color primary = new Color(35, 83, 145);
    private final Color panelBorderColor = new Color(210, 217, 228);

    // ---------- POS: product side ----------
    private final DefaultTableModel productModel = new DefaultTableModel(
            new Object[]{"SKU", "Name", "Category", "Price", "Stock"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable productTable = new JTable(productModel);
    private final JTextField productSearchField = new JTextField();

    // ---------- POS: cart side ----------
    private final DefaultTableModel cartModel = new DefaultTableModel(
            new Object[]{"Item ID", "SKU", "Name", "Qty", "Unit Price", "Disc %", "Line Total"}, 0) {
        @Override
        public boolean isCellEditable(int r, int c) {
            return false;
        }
    };
    private final JTable cartTable = new JTable(cartModel);

    // ---------- POS: context ----------
    private final JComboBox<InvoiceChoice> invoiceBox = new JComboBox<>();
    private final JLabel customerLabel = new JLabel("Customer: —");

    // ---------- POS: totals ----------
    private final JLabel subtotalLabel = new JLabel("Subtotal: ₹0.00");
    private final JLabel discountLabel = new JLabel("Discount: ₹0.00");
    private final JLabel taxLabel = new JLabel("Tax: ₹0.00");
    private final JLabel grandLabel = new JLabel("TOTAL: ₹0.00");
    private final JTextField taxPercentField = new JTextField("0", 4);

    // ---------- In-memory product cache ----------
    private final List<Object[]> allActiveProducts = new ArrayList<>();

    // ---------- Selected invoice ----------
    private int currentInvoiceId = -1;

    // =====================================================================
    // CONSTRUCTOR
    // =====================================================================

    public SwingUI() {
        setTitle("Smart Invoice and Billing Management System");
        setSize(1200, 760);
        setMinimumSize(new Dimension(1000, 650));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(background);
        setContentPane(root);

        root.add(buildToolbar(), BorderLayout.NORTH);
        root.add(buildTill(), BorderLayout.CENTER);

        loadActiveProducts();
        loadDraftInvoices();
    }

    // Adds a thin light-gray border and inner padding to a panel.
    private void addPanelBorder(JPanel panel) {
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(panelBorderColor),
                new EmptyBorder(6, 6, 6, 6)));
    }

    // =====================================================================
    // TOOLBAR
    // =====================================================================

    private JPanel buildToolbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(primary);
        bar.setBorder(new EmptyBorder(10, 14, 10, 14));

        JLabel title = new JLabel("SMART BILLING");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("SansSerif", Font.BOLD, 20));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);

        JButton productsBtn = toolbarButton("Products", this::openProductsDialog);
        JButton customersBtn = toolbarButton("Customers", this::openCustomersDialog);
        JButton reportsBtn = toolbarButton("Reports", this::openReportsDialog);
        JButton lowStockBtn = toolbarButton("Low Stock", this::openLowStockDialog);
        JButton exitBtn = toolbarButton("Exit", () -> dispose());

        right.add(productsBtn);
        right.add(customersBtn);
        right.add(reportsBtn);
        right.add(lowStockBtn);
        right.add(exitBtn);

        bar.add(title, BorderLayout.WEST);
        bar.add(right, BorderLayout.EAST);

        return bar;
    }

    private JButton toolbarButton(String label, Runnable action) {
        JButton b = new JButton(label);
        b.setFocusPainted(false);
        b.setFont(new Font("SansSerif", Font.BOLD, 13));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(e -> action.run());
        return b;
    }

    // =====================================================================
    // TILL LAYOUT
    // =====================================================================

    private JPanel buildTill() {
        JPanel wrapper = new JPanel(new BorderLayout(10, 10));
        wrapper.setBackground(background);
        wrapper.setBorder(new EmptyBorder(12, 14, 12, 14));

        wrapper.add(buildContextBar(), BorderLayout.NORTH);

        JPanel center = new JPanel(new GridLayout(1, 2, 12, 12));
        center.setBackground(background);
        center.add(buildProductPanel());
        center.add(buildCartPanel());

        wrapper.add(center, BorderLayout.CENTER);
        return wrapper;
    }

    private JPanel buildContextBar() {
        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
        bar.setBackground(background);

        bar.add(new JLabel("Draft invoice:"));
        invoiceBox.setPreferredSize(new Dimension(340, 28));
        bar.add(invoiceBox);

        JButton newDraft = new JButton("+ New draft…");
        newDraft.addActionListener(e -> createNewDraftInvoice());
        bar.add(newDraft);

        customerLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        bar.add(Box.createHorizontalStrut(20));
        bar.add(customerLabel);

        addPanelBorder(bar);
        return bar;
    }

    private JPanel buildProductPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBackground(background);

        JPanel searchRow = new JPanel(new BorderLayout(6, 0));
        searchRow.setBackground(background);
        searchRow.add(new JLabel("Search:"), BorderLayout.WEST);
        searchRow.add(productSearchField, BorderLayout.CENTER);
        panel.add(searchRow, BorderLayout.NORTH);

        productTable.setRowHeight(24);
        productTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        productTable.getTableHeader().setReorderingAllowed(false);
        panel.add(new JScrollPane(productTable), BorderLayout.CENTER);

        JLabel hint = new JLabel("Double-click a product to add it to the cart.");
        hint.setForeground(Color.GRAY);
        panel.add(hint, BorderLayout.SOUTH);

        addPanelBorder(panel);
        return panel;
    }

    private JPanel buildCartPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBackground(background);

        JLabel title = new JLabel("CART");
        title.setFont(new Font("SansSerif", Font.BOLD, 14));
        title.setForeground(primary);
        panel.add(title, BorderLayout.NORTH);

        cartTable.setRowHeight(24);
        cartTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        cartTable.getTableHeader().setReorderingAllowed(false);
        panel.add(new JScrollPane(cartTable), BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout(0, 8));
        bottom.setBackground(background);

        JPanel qtyRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        qtyRow.setBackground(background);

        JButton minusBtn = new JButton("− Qty");
        JButton plusBtn = new JButton("+ Qty");
        JButton changeQtyBtn = new JButton("Change qty…");
        JButton removeBtn = new JButton("Remove line");
        JButton discountBtn = new JButton("Set discount…");

        minusBtn.addActionListener(e -> adjustSelectedCartQty(-1));
        plusBtn.addActionListener(e -> adjustSelectedCartQty(+1));
        changeQtyBtn.addActionListener(e -> setSelectedLineQuantity());
        removeBtn.addActionListener(e -> removeSelectedCartLine());
        discountBtn.addActionListener(e -> setSelectedLineDiscount());

        qtyRow.add(minusBtn);
        qtyRow.add(plusBtn);
        qtyRow.add(changeQtyBtn);
        qtyRow.add(removeBtn);
        qtyRow.add(discountBtn);

        bottom.add(qtyRow, BorderLayout.NORTH);
        bottom.add(buildTotalsPanel(), BorderLayout.CENTER);

        panel.add(bottom, BorderLayout.SOUTH);

        addPanelBorder(panel);
        return panel;
    }

    private JPanel buildTotalsPanel() {
        JPanel outer = new JPanel(new BorderLayout(0, 8));
        outer.setBackground(background);

        JPanel grid = new JPanel(new GridLayout(0, 2, 8, 6));
        grid.setBackground(background);

        subtotalLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        discountLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        taxLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        grandLabel.setFont(new Font("SansSerif", Font.BOLD, 18));
        grandLabel.setForeground(primary);

        grid.add(subtotalLabel);
        grid.add(new JLabel(""));
        grid.add(discountLabel);
        grid.add(new JLabel(""));

        JPanel taxRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        taxRow.setBackground(background);
        taxRow.add(new JLabel("Tax %:"));
        taxPercentField.setPreferredSize(new Dimension(60, 26));
        taxRow.add(taxPercentField);
        grid.add(taxRow);
        grid.add(taxLabel);

        grid.add(new JLabel(""));
        grid.add(new JLabel(""));

        grid.add(new JLabel(""));
        grid.add(grandLabel);

        outer.add(grid, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 4));
        actions.setBackground(background);

        JButton draftPdfBtn = new JButton("Print Draft PDF");
        draftPdfBtn.addActionListener(e -> generateCurrentInvoicePDF());

        JButton payPrintBtn = new JButton("PAY & PRINT");
        payPrintBtn.setFont(new Font("SansSerif", Font.BOLD, 14));
        payPrintBtn.setPreferredSize(new Dimension(180, 40));
        payPrintBtn.addActionListener(e -> payAndPrint());

        actions.add(draftPdfBtn);
        actions.add(payPrintBtn);

        outer.add(actions, BorderLayout.SOUTH);

        addPanelBorder(outer);
        return outer;
    }

    // =====================================================================
    // SMALL HELPERS
    // =====================================================================

    private Connection connection() throws SQLException, IOException {
        return DBConnection.getConnection();
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    private void showError(Exception e) {
        e.printStackTrace();
        showMessage("Operation failed: " + e.getMessage());
    }

    private String input(String message) {
        return JOptionPane.showInputDialog(this, message);
    }

    private String requiredInput(String message) {
        String value = input(message);
        if (value == null) return null;
        value = value.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("This field cannot be empty.");
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
            throw new IllegalArgumentException("Enter a finite number.");
        }
        return number;
    }

    // =====================================================================
    // DROPDOWN ENTRY HOLDERS
    // =====================================================================

    private static final class InvoiceChoice {
        final int invoiceId;
        final String invoiceNumber;
        final String customerName;

        InvoiceChoice(int invoiceId, String invoiceNumber, String customerName) {
            this.invoiceId = invoiceId;
            this.invoiceNumber = invoiceNumber;
            this.customerName = customerName;
        }

        @Override
        public String toString() {
            return "#" + invoiceId + " · " + invoiceNumber + " · " + customerName;
        }
    }

    private static final class CustomerChoice {
        final int customerId;
        final String customerName;
        final String phone;
        final String email;
        final String billingAddress;

        CustomerChoice(int customerId, String customerName, String phone,
                       String email, String billingAddress) {
            this.customerId = customerId;
            this.customerName = customerName;
            this.phone = phone;
            this.email = email;
            this.billingAddress = billingAddress;
        }

        CustomerChoice(int customerId, String customerName, String phone) {
            this(customerId, customerName, phone, null, null);
        }

        @Override
        public String toString() {
            String phoneText = (phone == null || phone.isBlank())
                    ? "" : " · " + phone;
            return "#" + customerId + " · " + customerName + phoneText;
        }
    }

    // =====================================================================
    // MAIN
    // =====================================================================

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new SwingUI().setVisible(true);
        });
    }
    // =====================================================================
    // PRODUCT LOADING + FILTERING
    // =====================================================================

    private void loadActiveProducts() {
        allActiveProducts.clear();

        String sql = """
                SELECT product_id, sku, product_name, category,
                       selling_price, current_stock, is_active
                FROM products
                WHERE is_active = 1
                ORDER BY product_name
                """;

        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                allActiveProducts.add(new Object[]{
                        rs.getInt("product_id"),
                        rs.getString("sku"),
                        rs.getString("product_name"),
                        rs.getString("category"),
                        rs.getBigDecimal("selling_price"),
                        rs.getInt("current_stock"),
                        rs.getInt("is_active")
                });
            }
        } catch (Exception e) {
            showError(e);
        }

        filterProductTable();

        productSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { filterProductTable(); }
            @Override public void removeUpdate(DocumentEvent e) { filterProductTable(); }
            @Override public void changedUpdate(DocumentEvent e) { filterProductTable(); }
        });

        // Remove any previously-added mouse listeners so repeated calls
        // to loadActiveProducts() don't stack duplicate add-handlers.
        for (java.awt.event.MouseListener ml : productTable.getMouseListeners()) {
            productTable.removeMouseListener(ml);
        }

        productTable.addMouseListener(new MouseAdapter() {
            private long lastAdd = 0L;
            private int lastRow = -1;

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() != 2) return;

                // Use the row under the cursor — not getSelectedRow(),
                // which may still point to the previously selected row.
                int row = productTable.rowAtPoint(e.getPoint());
                if (row < 0) return;

                long now = System.currentTimeMillis();
                if (row == lastRow && now - lastAdd < 400) return;  // debounce
                lastAdd = now;
                lastRow = row;

                addSelectedProductToCart(row);
            }
        });
    }

    private void filterProductTable() {
        productModel.setRowCount(0);
        String needle = productSearchField.getText().trim().toLowerCase();

        for (Object[] p : allActiveProducts) {
            String sku = ((String) p[1]).toLowerCase();
            String name = ((String) p[2]).toLowerCase();

            if (needle.isEmpty() || sku.contains(needle) || name.contains(needle)) {
                productModel.addRow(new Object[]{
                        p[1],
                        p[2],
                        p[3],
                        ((BigDecimal) p[4]).setScale(2, RoundingMode.HALF_UP).toPlainString(),
                        p[5]
                });
            }
        }
    }

    // =====================================================================
    // INVOICE DROPDOWN
    // =====================================================================

    private void loadDraftInvoices() {
        invoiceBox.removeAllItems();

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
                invoiceBox.addItem(new InvoiceChoice(
                        rs.getInt("invoice_id"),
                        rs.getString("invoice_number"),
                        rs.getString("customer_name")
                ));
            }
        } catch (Exception e) {
            showError(e);
        }

        for (java.awt.event.ActionListener al : invoiceBox.getActionListeners()) {
            invoiceBox.removeActionListener(al);
        }
        invoiceBox.addActionListener(e -> onInvoiceSelected());

        onInvoiceSelected();
    }

    private void onInvoiceSelected() {
        Object sel = invoiceBox.getSelectedItem();

        if (!(sel instanceof InvoiceChoice choice)) {
            currentInvoiceId = -1;
            customerLabel.setText("Customer: —");
            cartModel.setRowCount(0);
            subtotalLabel.setText("Subtotal: ₹0.00");
            discountLabel.setText("Discount: ₹0.00");
            taxLabel.setText("Tax: ₹0.00");
            grandLabel.setText("TOTAL: ₹0.00");
            return;
        }

        currentInvoiceId = choice.invoiceId;
        customerLabel.setText("Customer: " + choice.customerName);

        refreshCart();
        recomputeTotals();
    }

    // =====================================================================
    // CART
    // =====================================================================

    private void refreshCart() {
        cartModel.setRowCount(0);

        if (currentInvoiceId <= 0) return;

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
            ps.setInt(1, currentInvoiceId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    cartModel.addRow(new Object[]{
                            rs.getInt("item_id"),
                            rs.getString("sku"),
                            rs.getString("product_name"),
                            rs.getInt("quantity"),
                            rs.getBigDecimal("unit_price").toPlainString(),
                            rs.getBigDecimal("discount_percent").toPlainString(),
                            rs.getBigDecimal("line_total").toPlainString()
                    });
                }
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    private void addSelectedProductToCart(int row) {
        if (row < 0 || row >= productTable.getRowCount()) {
            showMessage("Click on a product row.");
            return;
        }

        if (currentInvoiceId <= 0) {
            showMessage("Select or create a draft invoice first.");
            return;
        }

        String sku = (String) productModel.getValueAt(row, 0);
        Object[] found = null;
        for (Object[] p : allActiveProducts) {
            if (p[1].equals(sku)) { found = p; break; }
        }
        if (found == null) return;

        int productId = (int) found[0];
        BigDecimal price = (BigDecimal) found[4];
        int stock = (int) found[5];

        String checkSql = """
                SELECT item_id, quantity, discount_percent FROM invoice_items
                WHERE invoice_id = ? AND product_id = ?
                """;

        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(checkSql)) {
            ps.setInt(1, currentInvoiceId);
            ps.setInt(2, productId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int itemId = rs.getInt("item_id");
                    int currentQty = rs.getInt("quantity");
                    BigDecimal existingDisc = rs.getBigDecimal("discount_percent");
                    int newQty = currentQty + 1;

                    if (newQty > stock) {
                        showMessage("Only " + stock + " in stock.");
                        return;
                    }

                    recalcLine(itemId, newQty, price, existingDisc);
                } else {
                    if (stock < 1) {
                        showMessage("Out of stock.");
                        return;
                    }
                    insertCartLine(productId, 1, price, BigDecimal.ZERO);
                }
            }
        } catch (Exception e) {
            showError(e);
            return;
        }

        refreshCart();
        recomputeTotals();
    }

    private void insertCartLine(int productId, int qty, BigDecimal price,
                                BigDecimal discountPercent) {
        BigDecimal gross = price.multiply(BigDecimal.valueOf(qty))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal itemDiscount = gross.multiply(discountPercent)
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
            ps.setInt(1, currentInvoiceId);
            ps.setInt(2, productId);
            ps.setInt(3, qty);
            ps.setBigDecimal(4, price);
            ps.setBigDecimal(5, discountPercent);
            ps.setBigDecimal(6, itemDiscount);
            ps.setBigDecimal(7, lineTotal);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void recalcLine(int itemId, int newQty, BigDecimal price,
                            BigDecimal discountPercent) {
        BigDecimal gross = price.multiply(BigDecimal.valueOf(newQty))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal itemDiscount = gross.multiply(discountPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal lineTotal = gross.subtract(itemDiscount)
                .setScale(2, RoundingMode.HALF_UP);

        String sql = """
                UPDATE invoice_items
                SET quantity = ?, discount_percent = ?,
                    discount_amount = ?, line_total = ?
                WHERE item_id = ?
                """;

        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, newQty);
            ps.setBigDecimal(2, discountPercent);
            ps.setBigDecimal(3, itemDiscount);
            ps.setBigDecimal(4, lineTotal);
            ps.setInt(5, itemId);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private BigDecimal readLineDiscount(int itemId) {
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT discount_percent FROM invoice_items WHERE item_id = ?")) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getBigDecimal("discount_percent");
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return BigDecimal.ZERO;
    }

    private void adjustSelectedCartQty(int delta) {
        int row = cartTable.getSelectedRow();
        if (row < 0) {
            showMessage("Select a cart line first.");
            return;
        }

        int itemId = (int) cartModel.getValueAt(row, 0);
        int currentQty = (int) cartModel.getValueAt(row, 3);
        int newQty = currentQty + delta;

        String sku = (String) cartModel.getValueAt(row, 1);
        int stock = 0;
        BigDecimal price = null;
        for (Object[] p : allActiveProducts) {
            if (p[1].equals(sku)) {
                stock = (int) p[5];
                price = (BigDecimal) p[4];
                break;
            }
        }

        if (newQty <= 0) {
            removeCartLineById(itemId);
        } else {
            if (price != null && newQty > stock) {
                showMessage("Only " + stock + " in stock.");
                return;
            }
            if (price != null) {
                BigDecimal disc = readLineDiscount(itemId);
                recalcLine(itemId, newQty, price, disc);
            }
        }

        refreshCart();
        recomputeTotals();
    }

    // Asks for an exact quantity on the selected cart line.
    private void setSelectedLineQuantity() {
        int row = cartTable.getSelectedRow();
        if (row < 0) {
            showMessage("Select a cart line first.");
            return;
        }

        int itemId = (int) cartModel.getValueAt(row, 0);
        String sku = (String) cartModel.getValueAt(row, 1);
        int currentQty = (int) cartModel.getValueAt(row, 3);

        BigDecimal price = null;
        int stock = 0;
        for (Object[] p : allActiveProducts) {
            if (p[1].equals(sku)) {
                price = (BigDecimal) p[4];
                stock = (int) p[5];
                break;
            }
        }
        if (price == null) {
            showMessage("Product not found in cache. Reload the product list.");
            return;
        }

        String entered = input("Quantity for line " + sku
                + " (current: " + currentQty + ", stock: " + stock + "):");
        if (entered == null) return;

        int newQty;
        try {
            newQty = Integer.parseInt(entered.trim());
        } catch (NumberFormatException ex) {
            showMessage("Enter a valid whole number.");
            return;
        }

        if (newQty <= 0) {
            removeCartLineById(itemId);
        } else if (newQty > stock) {
            showMessage("Only " + stock + " in stock.");
            return;
        } else {
            BigDecimal disc = readLineDiscount(itemId);
            recalcLine(itemId, newQty, price, disc);
        }

        refreshCart();
        recomputeTotals();
    }

    private void removeSelectedCartLine() {
        int row = cartTable.getSelectedRow();
        if (row < 0) {
            showMessage("Select a cart line first.");
            return;
        }

        int itemId = (int) cartModel.getValueAt(row, 0);
        removeCartLineById(itemId);
        refreshCart();
        recomputeTotals();
    }

    private void removeCartLineById(int itemId) {
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM invoice_items WHERE item_id = ?")) {
            ps.setInt(1, itemId);
            ps.executeUpdate();
        } catch (Exception e) {
            showError(e);
        }
    }

    private void setSelectedLineDiscount() {
        int row = cartTable.getSelectedRow();
        if (row < 0) {
            showMessage("Select a cart line first.");
            return;
        }

        int itemId = (int) cartModel.getValueAt(row, 0);
        String sku = (String) cartModel.getValueAt(row, 1);
        BigDecimal price = null;
        for (Object[] p : allActiveProducts) {
            if (p[1].equals(sku)) {
                price = (BigDecimal) p[4];
                break;
            }
        }
        if (price == null) {
            showMessage("Product not found in cache. Reload the product list.");
            return;
        }

        BigDecimal currentDisc = readLineDiscount(itemId);

        String entered = input(
                "Discount % for line " + sku
                        + " (current: " + currentDisc.toPlainString() + "):");
        if (entered == null) return;

        BigDecimal newDisc;
        try {
            newDisc = new BigDecimal(entered.trim());
        } catch (NumberFormatException ex) {
            showMessage("Enter a valid number.");
            return;
        }

        if (newDisc.signum() < 0
                || newDisc.compareTo(BigDecimal.valueOf(100)) > 0) {
            showMessage("Discount must be between 0 and 100.");
            return;
        }

        int qty = (int) cartModel.getValueAt(row, 3);
        recalcLine(itemId, qty, price, newDisc);
        refreshCart();
        recomputeTotals();
    }

    // =====================================================================
    // TOTALS
    // =====================================================================

    private void recomputeTotals() {
        if (currentInvoiceId <= 0) return;

        BigDecimal taxPercent;
        try {
            String text = taxPercentField.getText().trim();
            if (text.isEmpty()) text = "0";
            taxPercent = new BigDecimal(text);
            if (taxPercent.signum() < 0) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            showMessage("Tax percent must be a non-negative number.");
            return;
        }

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
            ps.setInt(1, currentInvoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return;
                subtotal = rs.getBigDecimal("subtotal");
                discountValue = rs.getBigDecimal("discount_value");
                discountType = rs.getString("discount_type");
            }
        } catch (Exception e) {
            showError(e);
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
            ps.setInt(5, currentInvoiceId);
            ps.executeUpdate();
        } catch (Exception e) {
            showError(e);
            return;
        }

        subtotalLabel.setText("Subtotal: ₹" + subtotal.toPlainString());
        discountLabel.setText("Discount: ₹" + discountAmount.toPlainString());
        taxLabel.setText("Tax: ₹" + taxAmount.toPlainString());
        grandLabel.setText("TOTAL: ₹" + grandTotal.toPlainString());
    }

    // =====================================================================
    // CUSTOMER SEARCH (by name or phone)
    // =====================================================================

    private List<CustomerChoice> loadCustomers() throws SQLException, IOException {
        List<CustomerChoice> customers = new ArrayList<>();

        String sql = """
                SELECT customer_id, customer_name, phone, email, billing_address
                FROM customers
                WHERE is_active = 1
                ORDER BY customer_name
                """;

        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                customers.add(new CustomerChoice(
                        rs.getInt("customer_id"),
                        rs.getString("customer_name"),
                        rs.getString("phone"),
                        rs.getString("email"),
                        rs.getString("billing_address")
                ));
            }
        }

        return customers;
    }

    private CustomerChoice pickCustomer(String title) {
        try {
            List<CustomerChoice> all = loadCustomers();

            if (all.isEmpty()) {
                int addNow = JOptionPane.showConfirmDialog(
                        this,
                        "No customers exist yet. Add a new customer now?",
                        title,
                        JOptionPane.YES_NO_OPTION);
                if (addNow == JOptionPane.YES_OPTION) {
                    showCustomerForm("New customer", null);
                    all = loadCustomers();
                }
                if (all.isEmpty()) return null;
            }

            JTextField searchField = new JTextField();
            JComboBox<CustomerChoice> box = new JComboBox<>();
            box.setPreferredSize(new Dimension(360, 28));

            Runnable refresh = () -> {
                String q = searchField.getText().trim().toLowerCase();
                box.removeAllItems();
                try {
                    for (CustomerChoice cc : loadCustomers()) {
                        String name = cc.customerName == null ? "" : cc.customerName.toLowerCase();
                        String phone = cc.phone == null ? "" : cc.phone.toLowerCase();
                        if (q.isEmpty() || name.contains(q) || phone.contains(q)) {
                            box.addItem(cc);
                        }
                    }
                } catch (Exception ex) {
                    showError(ex);
                }
            };

            searchField.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { refresh.run(); }
                @Override public void removeUpdate(DocumentEvent e) { refresh.run(); }
                @Override public void changedUpdate(DocumentEvent e) { refresh.run(); }
            });

            refresh.run();

            JButton addNewBtn = new JButton("+ New customer…");
            addNewBtn.addActionListener(ev -> {
                showCustomerForm("New customer", null);
                refresh.run();
            });

            JPanel panel = new JPanel(new GridLayout(0, 1, 6, 6));
            JPanel searchRow = new JPanel(new BorderLayout(6, 0));
            searchRow.add(new JLabel("Search (name or phone):"), BorderLayout.WEST);
            searchRow.add(searchField, BorderLayout.CENTER);
            panel.add(searchRow);

            JPanel pickRow = new JPanel(new BorderLayout(6, 0));
            pickRow.add(box, BorderLayout.CENTER);
            pickRow.add(addNewBtn, BorderLayout.EAST);
            panel.add(pickRow);

            int result = JOptionPane.showConfirmDialog(
                    this, panel, title,
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);

            if (result != JOptionPane.OK_OPTION) return null;

            CustomerChoice selected = (CustomerChoice) box.getSelectedItem();
            if (selected == null) {
                showMessage("No customer selected.");
                return null;
            }
            return selected;
        } catch (Exception e) {
            showError(e);
            return null;
        }
    }

    private void showCustomerForm(String title, CustomerChoice existing) {
        try {
            JTextField nameField = new JTextField(
                    existing == null || existing.customerName == null
                            ? "" : existing.customerName);
            JTextField phoneField = new JTextField(
                    existing == null || existing.phone == null
                            ? "" : existing.phone);
            JTextField emailField = new JTextField(
                    existing == null || existing.email == null
                            ? "" : existing.email);
            JTextField addressField = new JTextField(
                    existing == null || existing.billingAddress == null
                            ? "" : existing.billingAddress);

            JPanel form = new JPanel(new GridLayout(0, 2, 8, 8));
            form.add(new JLabel("Name:*"));
            form.add(nameField);
            form.add(new JLabel("Phone:"));
            form.add(phoneField);
            form.add(new JLabel("Email:"));
            form.add(emailField);
            form.add(new JLabel("Billing address:"));
            form.add(addressField);

            int result = JOptionPane.showConfirmDialog(
                    this, form, title,
                    JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.PLAIN_MESSAGE);

            if (result != JOptionPane.OK_OPTION) return;

            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                showMessage("Customer name is required.");
                return;
            }

            String phone = phoneField.getText().trim();
            String email = emailField.getText().trim();
            String address = addressField.getText().trim();

            String sql;
            if (existing == null) {
                sql = """
                        INSERT INTO customers
                        (customer_name, phone, email, billing_address)
                        VALUES (?, ?, ?, ?)
                        """;
            } else {
                sql = """
                        UPDATE customers
                        SET customer_name = ?, phone = ?, email = ?, billing_address = ?
                        WHERE customer_id = ?
                        """;
            }

            try (Connection c = connection();
                 PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setString(1, name);
                ps.setString(2, phone);
                ps.setString(3, email);
                ps.setString(4, address);
                if (existing != null) {
                    ps.setInt(5, existing.customerId);
                }
                ps.executeUpdate();
            }

            if (existing == null) {
                showMessage("Customer added: " + name);
            } else {
                showMessage("Customer updated: " + name);
                loadDraftInvoices();
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    // =====================================================================
    // NEW DRAFT INVOICE
    // =====================================================================

    private void createNewDraftInvoice() {
        CustomerChoice selected = pickCustomer("Select customer for new draft");
        if (selected == null) return;

        String discountType = (String) JOptionPane.showInputDialog(
                this, "Discount type", "New draft invoice",
                JOptionPane.QUESTION_MESSAGE, null,
                new String[]{"PERCENT", "FIXED"}, "PERCENT");
        if (discountType == null) return;

        String discText = requiredInput("Discount value (0 if none):");
        if (discText == null) return;

        BigDecimal discountValue;
        try {
            discountValue = new BigDecimal(discText.trim());
        } catch (NumberFormatException ex) {
            showMessage("Enter a valid number.");
            return;
        }

        if (discountValue.signum() < 0) {
            showMessage("Discount value cannot be negative.");
            return;
        }
        if ("PERCENT".equals(discountType)
                && discountValue.compareTo(BigDecimal.valueOf(100)) > 0) {
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

        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, invoiceNumber);
            ps.setInt(2, selected.customerId);
            ps.setString(3, discountType);
            ps.setBigDecimal(4, discountValue);
            ps.executeUpdate();
        } catch (Exception e) {
            showError(e);
            return;
        }

        loadDraftInvoices();
        showMessage("Draft invoice created for "
                + selected.customerName
                + " (" + invoiceNumber + ")");
    }

    // =====================================================================
    // PAY & PRINT
    // =====================================================================

    private void payAndPrint() {
        if (currentInvoiceId <= 0) {
            showMessage("Select or create a draft invoice first.");
            return;
        }

        recomputeTotals();

        int cleared = JOptionPane.showConfirmDialog(
                this,
                "Has the payment been cleared?",
                "Settle invoice " + currentInvoiceId,
                JOptionPane.YES_NO_CANCEL_OPTION);

        if (cleared == JOptionPane.CANCEL_OPTION) return;
        if (cleared == JOptionPane.NO_OPTION) {
            showMessage("Invoice remains open. Record payment when ready.");
            return;
        }

        String method = (String) JOptionPane.showInputDialog(
                this, "Payment method", "Payment",
                JOptionPane.QUESTION_MESSAGE, null,
                new String[]{"CASH", "CARD", "UPI", "BANK_TRANSFER"}, "CASH");
        if (method == null) return;

        BigDecimal grandTotal;
        BigDecimal paid;
        try {
            grandTotal = getInvoiceGrandTotal(currentInvoiceId);
            paid = getCompletedPayments(currentInvoiceId);
        } catch (Exception e) {
            showError(e);
            return;
        }

        BigDecimal balance = grandTotal.subtract(paid)
                .setScale(2, RoundingMode.HALF_UP);

        if (balance.signum() > 0) {
            String insertSql = """
                    INSERT INTO payments
                    (invoice_id, amount, payment_method,
                     payment_date, payment_status)
                    VALUES (?, ?, ?, CURRENT_TIMESTAMP, 'COMPLETED')
                    """;

            try (Connection c = connection();
                 PreparedStatement ps = c.prepareStatement(insertSql)) {
                ps.setInt(1, currentInvoiceId);
                ps.setBigDecimal(2, balance);
                ps.setString(3, method);
                ps.executeUpdate();
            } catch (Exception e) {
                showError(e);
                return;
            }
        }

        boolean completed = billingService.completeInvoice(currentInvoiceId);

        if (!completed) {
            showMessage("Checkout failed. See the console for details.\n"
                    + "Invoice remains open — fix the issue and try again.");
            return;
        }

        pdfGenerator.generateInvoicePDF(currentInvoiceId);

        resetPos();

        showMessage("Payment recorded, invoice completed, stock updated.\n"
                + "PDF generated. Ready for the next customer.");
    }

    private void resetPos() {
        currentInvoiceId = -1;
        cartModel.setRowCount(0);
        customerLabel.setText("Customer: —");
        subtotalLabel.setText("Subtotal: ₹0.00");
        discountLabel.setText("Discount: ₹0.00");
        taxLabel.setText("Tax: ₹0.00");
        grandLabel.setText("TOTAL: ₹0.00");
        taxPercentField.setText("0");

        allActiveProducts.clear();
        loadActiveProducts();
        loadDraftInvoices();
    }

    private BigDecimal getInvoiceGrandTotal(int invoiceId)
            throws SQLException, IOException {
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT grand_total FROM invoices WHERE invoice_id = ?")) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("grand_total");
                }
            }
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal getCompletedPayments(int invoiceId)
            throws SQLException, IOException {
        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT COALESCE(SUM(amount), 0) AS paid " +
                             "FROM payments WHERE invoice_id = ? " +
                             "AND payment_status = 'COMPLETED'")) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getBigDecimal("paid");
            }
        }
    }

    // =====================================================================
    // PDF (draft preview)
    // =====================================================================

    private void generateCurrentInvoicePDF() {
        if (currentInvoiceId <= 0) {
            showMessage("Select or create a draft invoice first.");
            return;
        }

        recomputeTotals();
        pdfGenerator.generateInvoicePDF(currentInvoiceId);
        showMessage("Draft PDF generated (check the project working directory).");
    }
    // =====================================================================
    // MANAGEMENT DIALOGS — PRODUCTS
    // =====================================================================

    private void openProductsDialog() {
        String[] options = {
                "View Products", "Add Product",
                "Update Product", "Restock Product",
                "Delete Product"
        };

        int choice = JOptionPane.showOptionDialog(
                this, "Choose a product operation", "Products",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

        try {
            switch (choice) {
                case 0 -> showQuery("Products", """
                        SELECT product_id, sku, product_name, category,
                               purchase_price, selling_price, current_stock,
                               reorder_level, is_active
                        FROM products
                        ORDER BY product_id
                        """);
                case 1 -> dialogAddProduct();
                case 2 -> dialogUpdateProduct();
                case 3 -> dialogRestockProduct();
                case 4 -> dialogDeleteProduct();
                default -> { /* cancelled */ }
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    private void dialogAddProduct() throws SQLException, IOException {
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
        }

        loadActiveProducts();
        filterProductTable();

        showMessage("Product added successfully.");
    }

    private void dialogUpdateProduct() throws SQLException, IOException {
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

        loadActiveProducts();
        filterProductTable();
    }

    private void dialogRestockProduct() {
        String sku = requiredInput("SKU to restock:");
        if (sku == null) return;

        int quantity = readInt("Quantity to add:");
        if (quantity <= 0) {
            showMessage("Quantity must be positive.");
            return;
        }

        if (productDAO.increaseStock(sku, quantity)) {
            showMessage("Stock increased and movement recorded.");
            loadActiveProducts();
            filterProductTable();
        } else {
            showMessage("Restocking failed. Check the console.");
        }
    }

    private void dialogDeleteProduct() throws SQLException, IOException {
        String sku = requiredInput("SKU to delete:");
        if (sku == null) return;

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Mark product " + sku + " as inactive?\n"
                        + "(It will be hidden from the POS but kept in history.)",
                "Confirm deactivation",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = "UPDATE products SET is_active = 0 WHERE sku = ?";

        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, sku);
            int rows = ps.executeUpdate();
            showMessage(rows == 1
                    ? "Product marked inactive. It will no longer appear in the POS."
                    : "Product not found.");
        }

        loadActiveProducts();
        filterProductTable();
    }

    // =====================================================================
    // MANAGEMENT DIALOGS — CUSTOMERS
    // =====================================================================

    private void openCustomersDialog() {
        String[] options = {
                "View Customers", "Add Customer",
                "Update Customer", "Delete Customer"
        };

        int choice = JOptionPane.showOptionDialog(
                this, "Choose a customer operation", "Customers",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

        try {
            switch (choice) {
                case 0 -> showQuery("Customers",
                        "SELECT * FROM customers ORDER BY customer_id");
                case 1 -> showCustomerForm("Add customer", null);
                case 2 -> dialogUpdateCustomer();
                case 3 -> dialogDeleteCustomer();
                default -> { /* cancelled */ }
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    private void dialogUpdateCustomer() {
        CustomerChoice selected = pickCustomer("Update which customer?");
        if (selected == null) return;
        showCustomerForm(
                "Update customer #" + selected.customerId + " · " + selected.customerName,
                selected);
    }

    private void dialogDeleteCustomer() {
        CustomerChoice selected = pickCustomer("Deactivate which customer?");
        if (selected == null) return;

        int confirm = JOptionPane.showConfirmDialog(
                this,
                "Mark customer #" + selected.customerId + " · "
                        + selected.customerName + " as inactive?\n"
                        + "(They will be hidden from new drafts but kept in history.)",
                "Confirm deactivation",
                JOptionPane.YES_NO_OPTION);

        if (confirm != JOptionPane.YES_OPTION) return;

        String sql = "UPDATE customers SET is_active = 0 WHERE customer_id = ?";

        try (Connection c = connection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, selected.customerId);
            int rows = ps.executeUpdate();
            showMessage(rows == 1
                    ? "Customer marked inactive."
                    : "Customer not found.");
        } catch (Exception e) {
            showError(e);
        }

        loadDraftInvoices();
    }

    // =====================================================================
    // MANAGEMENT DIALOGS — REPORTS
    // =====================================================================

    private void openReportsDialog() {
        String[] options = {
                "Sales Summary", "Payment Summary",
                "Stock Movement History", "Invoice Summary",
                "All Products", "All Customers"
        };

        int choice = JOptionPane.showOptionDialog(
                this, "Select a report", "Reports",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

        try {
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
                case 4 -> showQuery("All Products", """
                        SELECT product_id, sku, product_name, category,
                               purchase_price, selling_price,
                               current_stock, reorder_level, is_active
                        FROM products
                        ORDER BY product_id
                        """);
                case 5 -> showQuery("All Customers",
                        "SELECT * FROM customers ORDER BY customer_id");
                default -> { /* cancelled */ }
            }
        } catch (Exception e) {
            showError(e);
        }
    }

    // =====================================================================
    // MANAGEMENT DIALOGS — LOW STOCK
    // =====================================================================

    private void openLowStockDialog() {
        try {
            showQuery("Low Stock Products", """
                    SELECT sku, product_name, current_stock, reorder_level
                    FROM products
                    WHERE is_active = 1
                      AND current_stock <= reorder_level
                    ORDER BY current_stock
                    """);
        } catch (Exception e) {
            showError(e);
        }
    }

    // =====================================================================
    // SHARED QUERY DISPLAY
    // =====================================================================

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
        scrollPane.setPreferredSize(new Dimension(950, 450));

        JOptionPane.showMessageDialog(
                this, scrollPane,
                title + " (" + rows.size() + " rows)",
                JOptionPane.PLAIN_MESSAGE);
    }
}

