package crmpams.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** The cleaned values written to the orders table. */
public record NewOrderRecord(
        int customerId,
        int projectTypeId,
        String projectDescription,
        String requiredOutput,
        LocalDate deadline,
        int urgencyLevelId,
        String specialInstructions,
        int stageId,
        LocalDateTime dateReceived,
        int encodedBy) {
}
