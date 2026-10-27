package crmpams.service;

import crmpams.validation.ValidationResult;
import java.time.LocalDateTime;

/**
 * Outcome of saving a new order. When saved, orderId is the J.O. number and dateReceived is the
 * date and time received that the system recorded.
 */
public record SaveOrderResult(Status status, ValidationResult errors, Integer orderId,
        Integer duplicateOrderId, boolean newCustomer, LocalDateTime dateReceived) {

    public enum Status { SAVED, INVALID, POSSIBLE_DUPLICATE }

    public static SaveOrderResult saved(int orderId, boolean newCustomer, LocalDateTime dateReceived) {
        return new SaveOrderResult(Status.SAVED, new ValidationResult(), orderId, null, newCustomer, dateReceived);
    }

    public static SaveOrderResult invalid(ValidationResult errors) {
        return new SaveOrderResult(Status.INVALID, errors, null, null, false, null);
    }

    public static SaveOrderResult possibleDuplicate(int existingOrderId) {
        return new SaveOrderResult(Status.POSSIBLE_DUPLICATE, new ValidationResult(), null, existingOrderId, false,
                null);
    }
}
