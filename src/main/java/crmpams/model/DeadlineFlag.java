package crmpams.model;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Deadline indicator of the Production Status Monitoring module (IPO 9.4).
 * Days remaining = deadline minus the current date.
 * Below zero is Passed Deadline, within the warning period is Approaching Deadline,
 * and anything else is On Schedule.
 */
public enum DeadlineFlag {
    PASSED("Passed Deadline"),
    APPROACHING("Approaching Deadline"),
    ON_SCHEDULE("On Schedule");

    private final String label;

    DeadlineFlag(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public static long daysRemaining(LocalDate deadline, LocalDate today) {
        return ChronoUnit.DAYS.between(today, deadline);
    }

    public static DeadlineFlag classify(LocalDate deadline, LocalDate today, int warningDays) {
        long days = daysRemaining(deadline, today);
        if (days < 0) {
            return PASSED;
        }
        if (days <= warningDays) {
            return APPROACHING;
        }
        return ON_SCHEDULE;
    }

    @Override
    public String toString() {
        return label;
    }
}
