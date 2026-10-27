package crmpams.service;

import crmpams.validation.ValidationResult;

/** Outcome of a save that can only succeed or fail validation. */
public record SaveResult(boolean saved, ValidationResult errors, Integer id) {

    public static SaveResult saved(int id) {
        return new SaveResult(true, new ValidationResult(), id);
    }

    public static SaveResult invalid(ValidationResult errors) {
        return new SaveResult(false, errors, null);
    }
}
