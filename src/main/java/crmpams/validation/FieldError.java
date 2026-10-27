package crmpams.validation;

/** One validation problem: the field key and a plain message that says how to fix it. */
public record FieldError(String field, String message) {
}
