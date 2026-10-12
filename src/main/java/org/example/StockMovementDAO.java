
package org.example;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.io.IOException;
import java.sql.Timestamp;
import java.sql.Types;

public class StockMovementDAO {

    public boolean addMovement(StockMovement movement) {

        String query =
                "INSERT INTO stock_movements " +
                        "(product_id, movement_type, quantity, " +
                        "reference_type, reference_id, notes, movement_date) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (
                Connection connection = DBConnection.getConnection();
                PreparedStatement statement =
                        connection.prepareStatement(query)
        ) {

            statement.setInt(1, movement.getProductId());
            statement.setString(2, movement.getMovementType());
            statement.setInt(3, movement.getQuantity());
            statement.setString(4, movement.getReferenceType());

            if (movement.getReferenceId() != null) {
                statement.setInt(5, movement.getReferenceId());
            } else {
                statement.setNull(5, Types.INTEGER);
            }

            statement.setString(6, movement.getNotes());

            if (movement.getMovementDate() != null) {
                statement.setTimestamp(
                        7,
                        Timestamp.valueOf(movement.getMovementDate())
                );
            } else {
                statement.setTimestamp(
                        7,
                        new Timestamp(System.currentTimeMillis())
                );
            }

            int rowsInserted = statement.executeUpdate();

            return rowsInserted > 0;

        } catch (SQLException | IOException e) {
            System.out.println("Unable to record stock movement.");
            System.out.println("Reason: " + e.getMessage());

            return false;
        }
    }
}
