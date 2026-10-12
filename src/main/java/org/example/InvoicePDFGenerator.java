package org.example;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.FileOutputStream;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

public class InvoicePDFGenerator {

    public boolean generateInvoicePDF(int invoiceId) {
        String invoiceQuery =
                "SELECT i.invoice_id, i.invoice_number, i.invoice_date, " +
                        "i.subtotal, i.discount_amount, i.tax_amount, " +
                        "i.grand_total, i.status, c.customer_name " +
                        "FROM invoices i " +
                        "JOIN customers c ON i.customer_id = c.customer_id " +
                        "WHERE i.invoice_id = ?";

        String itemsQuery =
                "SELECT p.sku, p.product_name, ii.quantity, " +
                        "ii.unit_price, ii.line_total " +
                        "FROM invoice_items ii " +
                        "JOIN products p ON ii.product_id = p.product_id " +
                        "WHERE ii.invoice_id = ? ORDER BY ii.item_id";

        String paymentsQuery =
                "SELECT payment_method, amount, payment_status " +
                        "FROM payments WHERE invoice_id = ? " +
                        "ORDER BY payment_id";

        String invoiceNumber;
        String invoiceDate;
        String customerName;
        String status;
        BigDecimal subtotal;
        BigDecimal discount;
        BigDecimal tax;
        BigDecimal grandTotal;

        try (Connection connection = DBConnection.getConnection()) {

            try (PreparedStatement statement =
                         connection.prepareStatement(invoiceQuery)) {
                statement.setInt(1, invoiceId);

                try (ResultSet rs = statement.executeQuery()) {
                    if (!rs.next()) {
                        System.out.println("Invoice not found.");
                        return false;
                    }

                    invoiceNumber = rs.getString("invoice_number");
                    invoiceDate = rs.getString("invoice_date");
                    customerName = rs.getString("customer_name");
                    status = rs.getString("status");
                    subtotal = rs.getBigDecimal("subtotal");
                    discount = rs.getBigDecimal("discount_amount");
                    tax = rs.getBigDecimal("tax_amount");
                    grandTotal = rs.getBigDecimal("grand_total");
                }
            }

            Document document = new Document();

            String fileName = "Invoice_" + invoiceNumber + ".pdf";

            PdfWriter.getInstance(
                    document, new FileOutputStream(fileName));

            document.open();

            Font titleFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD, 20);
            Font headingFont = FontFactory.getFont(
                    FontFactory.HELVETICA_BOLD, 12);

            Paragraph title = new Paragraph(
                    "SMART INVOICE AND BILLING SYSTEM", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph(" "));
            document.add(new Paragraph("INVOICE", headingFont));
            document.add(new Paragraph(
                    "Invoice Number: " + invoiceNumber));
            document.add(new Paragraph("Invoice ID: " + invoiceId));
            document.add(new Paragraph("Date: " + invoiceDate));
            document.add(new Paragraph("Customer: " + customerName));
            document.add(new Paragraph("Status: " + status));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{2, 4, 1.5f, 2, 2});

            addHeader(table, "SKU");
            addHeader(table, "Product");
            addHeader(table, "Qty");
            addHeader(table, "Unit Price");
            addHeader(table, "Line Total");

            try (PreparedStatement statement =
                         connection.prepareStatement(itemsQuery)) {
                statement.setInt(1, invoiceId);

                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        table.addCell(rs.getString("sku"));
                        table.addCell(rs.getString("product_name"));
                        table.addCell(String.valueOf(
                                rs.getInt("quantity")));
                        table.addCell(rs.getBigDecimal(
                                "unit_price").toPlainString());
                        table.addCell(rs.getBigDecimal(
                                "line_total").toPlainString());
                    }
                }
            }

            document.add(table);
            document.add(new Paragraph(" "));

            document.add(new Paragraph(
                    "Subtotal: " + subtotal.toPlainString()));
            document.add(new Paragraph(
                    "Discount: " + discount.toPlainString()));
            document.add(new Paragraph(
                    "Tax: " + tax.toPlainString()));

            Paragraph total = new Paragraph(
                    "Grand Total: " + grandTotal.toPlainString(),
                    headingFont);
            total.setAlignment(Element.ALIGN_RIGHT);
            document.add(total);

            document.add(new Paragraph(" "));
            document.add(new Paragraph("PAYMENT DETAILS", headingFont));

            PdfPTable paymentTable = new PdfPTable(3);
            paymentTable.setWidthPercentage(100);

            addHeader(paymentTable, "Method");
            addHeader(paymentTable, "Amount");
            addHeader(paymentTable, "Status");

            try (PreparedStatement statement =
                         connection.prepareStatement(paymentsQuery)) {
                statement.setInt(1, invoiceId);

                try (ResultSet rs = statement.executeQuery()) {
                    while (rs.next()) {
                        paymentTable.addCell(
                                rs.getString("payment_method"));
                        paymentTable.addCell(
                                rs.getBigDecimal("amount").toPlainString());
                        paymentTable.addCell(
                                rs.getString("payment_status"));
                    }
                }
            }

            document.add(paymentTable);
            document.add(new Paragraph(" "));
            document.add(new Paragraph(
                    "Thank you for your business!"));

            document.close();

            System.out.println("Invoice PDF generated successfully.");
            System.out.println("File: " + fileName);
            return true;

        } catch (Exception e) {
            System.out.println(
                    "Failed to generate invoice PDF: " + e.getMessage());
            return false;
        }
    }

    private void addHeader(PdfPTable table, String text) {
        Font font = FontFactory.getFont(
                FontFactory.HELVETICA_BOLD, 10);

        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
    }
    public static void main(String[] args) {
        InvoicePDFGenerator generator = new InvoicePDFGenerator();

        boolean success = generator.generateInvoicePDF(1);

        if (success) {
            System.out.println("PDF test completed successfully.");
        } else {
            System.out.println("PDF generation failed.");
        }
    }
}