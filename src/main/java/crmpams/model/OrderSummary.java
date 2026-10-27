package crmpams.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One row of the search results and the data shown in View Selected Order Record (IPO 12.3).
 * projectTypes lists the project types of all order lines, for example "Architectural Floor Plan, Blueprint Printing".
 */
public record OrderSummary(
        int orderId,
        String customerName,
        String contactNumber,
        String projectTypes,
        String projectDescription,
        LocalDate deadline,
        String urgency,
        String stage,
        String assignedTo,
        LocalDateTime dateReceived,
        String deliveryAddress,
        String specialInstructions) {
}
