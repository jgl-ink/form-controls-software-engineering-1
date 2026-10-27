package crmpams.service;

import crmpams.model.OrderSummary;
import crmpams.validation.ValidationResult;
import java.util.List;

/** Search outcome: either validation errors or the matching orders. */
public record SearchResult(ValidationResult errors, List<OrderSummary> rows, boolean limitReached) {
}
