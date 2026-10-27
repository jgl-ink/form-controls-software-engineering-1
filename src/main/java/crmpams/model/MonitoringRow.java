package crmpams.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** One active order in the monitoring table. daysLeft and flag are filled in by the service. */
public record MonitoringRow(
        int orderId,
        String customerName,
        String stage,
        String assignedTo,
        LocalDate deadline,
        LocalDateTime lastUpdate,
        long daysLeft,
        DeadlineFlag flag) {

    public MonitoringRow withDeadlineInfo(long days, DeadlineFlag newFlag) {
        return new MonitoringRow(orderId, customerName, stage, assignedTo, deadline, lastUpdate, days, newFlag);
    }
}
