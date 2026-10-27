package crmpams.service;

import crmpams.model.MonitoringRow;
import crmpams.validation.ValidationResult;
import java.util.List;
import java.util.Map;

/** Rows of the monitoring table plus the number of orders in each stage (Active Orders by Stage, 9.2). */
public record MonitoringResult(ValidationResult errors, List<MonitoringRow> rows, Map<String, Long> countByStage) {
}
