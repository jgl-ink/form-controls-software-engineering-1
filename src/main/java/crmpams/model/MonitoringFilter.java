package crmpams.model;

import java.util.Set;

/**
 * Filter of the Management Monitoring View (IPO 9.2 to 9.4).
 * text is an order ID or an employee name (Status Dashboard, 9.3).
 * deadlineFlag null means all deadline indicators.
 */
public record MonitoringFilter(
        String text,
        Set<Integer> stageIds,
        Integer employeeId,
        DeadlineFlag deadlineFlag,
        int warningDays) {
}
