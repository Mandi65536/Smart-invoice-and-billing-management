
package org.example;

import java.time.LocalDateTime;

public class StockMovement {

    private int movementId;
    private int productId;
    private String movementType;
    private int quantity;
    private String referenceType;
    private Integer referenceId;
    private String notes;
    private LocalDateTime movementDate;

    public StockMovement(
            int movementId,
            int productId,
            String movementType,
            int quantity,
            String referenceType,
            Integer referenceId,
            String notes,
            LocalDateTime movementDate) {

        this.movementId = movementId;
        this.productId = productId;
        this.movementType = movementType;
        this.quantity = quantity;
        this.referenceType = referenceType;
        this.referenceId = referenceId;
        this.notes = notes;
        this.movementDate = movementDate;
    }

    public int getMovementId() {
        return movementId;
    }

    public int getProductId() {
        return productId;
    }

    public String getMovementType() {
        return movementType;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public Integer getReferenceId() {
        return referenceId;
    }

    public String getNotes() {
        return notes;
    }

    public LocalDateTime getMovementDate() {
        return movementDate;
    }
}

