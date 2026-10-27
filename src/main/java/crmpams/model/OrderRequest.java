package crmpams.model;

import java.time.LocalDate;
import java.util.List;

/**
 * Everything the New Order form sends to the service.
 * customerId is the hidden value set by Look Up Customer; it may be null.
 * The order lines follow the columns of the company's order form. The J.O. number, the date and
 * time received, and the CAD operator are not entered here: the system sets the first two when the
 * order is saved, and the operator is assigned later in the Task Assignment module.
 */
public record OrderRequest(
        Integer customerId,
        String customerName,
        String contactNumber,
        String email,
        String projectDescription,
        List<OrderLine> lines,
        LocalDate deadline,
        Integer urgencyLevelId,
        String deliveryAddress,
        String specialInstructions,
        String referenceFilePath) {
}
