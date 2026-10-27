package crmpams.service;

import crmpams.validation.ValidationResult;

/** Outcome of saving a new order. */
public record SaveOrderResult(Status status, ValidationResult errors, Integer orderId,
        Integer duplicateOrderId, boolean newCustomer) {

    public enum Status { SAVED, INVALID, POSSIBLE_DUPLICATE }

    public static SaveOrderResult saved(int orderId, boolean newCustomer) {
        return new SaveOrderResult(Status.SAVED, new ValidationResult(), orderId, null, newCustomer);
    }

    public static SaveOrderResult invalid(ValidationResult errors) {
        return new SaveOrderResult(Status.INVALID, errors, null, null, false);
    }

    public static SaveOrderResult possibleDuplicate(int existingOrderId) {
        return new SaveOrderResult(Status.POSSIBLE_DUPLICATE, new ValidationResult(), null, existingOrderId, false);
    }
}
