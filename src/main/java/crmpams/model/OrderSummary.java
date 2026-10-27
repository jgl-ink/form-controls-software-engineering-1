package crmpams.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** One row of the search results and the data shown in View Selected Order Record (IPO 12.3). */
public record OrderSummary(
        int orderId,
        String customerName,
        String contactNumber,
        String projectType,
        String projectDescription,
        String requiredOutput,
        LocalDate deadline,
        String urgency,
        String stage,
        String assignedTo,
        LocalDateTime dateReceived,
        String specialInstructions) {
}
