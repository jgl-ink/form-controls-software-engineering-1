package crmpams.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Collects field errors. The same object is produced by the form (client-side check)
 * and by the service (server-side check), so both layers report errors the same way.
 */
public final class ValidationResult {

    /** Key for errors that do not belong to one field. */
    public static final String GENERAL = "general";

    private final List<FieldError> errors = new ArrayList<>();

    /** Adds the message for the field when the message is not null. Keeps the first error of a field. */
    public ValidationResult check(String field, String message) {
        if (message != null && !has(field)) {
            errors.add(new FieldError(field, message));
        }
        return this;
    }

    /** Sets the message of a field, replacing an earlier one. */
    public ValidationResult put(String field, String message) {
        errors.removeIf(e -> e.field().equals(field));
        errors.add(new FieldError(field, message));
        return this;
    }

    public boolean isValid() {
        return errors.isEmpty();
    }

    public boolean has(String field) {
        return errors.stream().anyMatch(e -> e.field().equals(field));
    }

    public String messageFor(String field) {
        return errors.stream()
                .filter(e -> e.field().equals(field))
                .map(FieldError::message)
                .findFirst()
                .orElse(null);
    }

    public List<FieldError> errors() {
        return Collections.unmodifiableList(errors);
    }

    public static ValidationResult of(String field, String message) {
        return new ValidationResult().check(field, message);
    }

    @Override
    public String toString() {
        return "ValidationResult" + errors;
    }
}
