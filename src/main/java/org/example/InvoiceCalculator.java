
package org.example;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class InvoiceCalculator {

    private InvoiceCalculator() {
        // Prevent creating objects of this utility class
    }

    // Calculate a discount using a percentage
    public static BigDecimal calculatePercentageDiscount(
            BigDecimal subtotal,
            BigDecimal discountPercent) {

        if (subtotal.signum() < 0
                || discountPercent.signum() < 0
                || discountPercent.compareTo(
                BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException(
                    "Invalid subtotal or discount percentage.");
        }

        return subtotal
                .multiply(discountPercent)
                .divide(BigDecimal.valueOf(100), 2,
                        RoundingMode.HALF_UP);
    }

    // Calculate the final total after discounts and tax
    public static BigDecimal calculateGrandTotal(
            BigDecimal subtotal,
            BigDecimal discountAmount,
            BigDecimal taxAmount) {

        if (subtotal.signum() < 0
                || discountAmount.signum() < 0
                || taxAmount.signum() < 0
                || discountAmount.compareTo(subtotal) > 0) {
            throw new IllegalArgumentException(
                    "Invalid invoice amounts.");
        }

        return subtotal
                .subtract(discountAmount)
                .add(taxAmount)
                .setScale(2, RoundingMode.HALF_UP);
    }
}
