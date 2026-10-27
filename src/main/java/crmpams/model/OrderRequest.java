package crmpams.model;

import java.time.LocalDate;

/**
 * Everything the New Order form sends to the service.
 * customerId is the hidden value set by Look Up Customer; it may be null.
 */
public record OrderRequest(
        Integer customerId,
        String customerName,
        String contactNumber,
        String email,
        Integer projectTypeId,
        String projectDescription,
        String requiredOutput,
        LocalDate deadline,
        Integer urgencyLevelId,
        String specialInstructions,
        String referenceFilePath) {
}
