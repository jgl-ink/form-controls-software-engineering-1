package crmpams.model;

import java.time.LocalDate;

/**
 * Search criteria of the Search module (IPO 12.1). Null ids mean "All".
 * The date range is used only when useDateRange is true.
 */
public record OrderSearchCriteria(
        String text,
        Integer projectTypeId,
        Integer stageId,
        Integer employeeId,
        Integer urgencyLevelId,
        boolean useDateRange,
        LocalDate receivedFrom,
        LocalDate receivedTo,
        boolean includeClosed) {
}
