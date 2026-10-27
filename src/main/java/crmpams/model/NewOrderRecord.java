package crmpams.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** The cleaned values written to the orders table. The order lines are written to order_items. */
public record NewOrderRecord(
        int customerId,
        String projectDescription,
        LocalDate deadline,
        int urgencyLevelId,
        String deliveryAddress,
        String specialInstructions,
        int stageId,
        LocalDateTime dateReceived,
        int encodedBy) {
}
