package crmpams.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** White box cases: each branch of the deadline indicator rule (IPO 9.4). */
class DeadlineFlagTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 27);

    @Test
    void passedWhenDaysRemainingIsBelowZero() {
        assertEquals(DeadlineFlag.PASSED, DeadlineFlag.classify(TODAY.minusDays(1), TODAY, 3));
    }

    @Test
    void approachingFromTodayUpToTheWarningPeriod() {
        assertEquals(DeadlineFlag.APPROACHING, DeadlineFlag.classify(TODAY, TODAY, 3));
        assertEquals(DeadlineFlag.APPROACHING, DeadlineFlag.classify(TODAY.plusDays(3), TODAY, 3));
    }

    @Test
    void onScheduleAfterTheWarningPeriod() {
        assertEquals(DeadlineFlag.ON_SCHEDULE, DeadlineFlag.classify(TODAY.plusDays(4), TODAY, 3));
        assertEquals(DeadlineFlag.ON_SCHEDULE, DeadlineFlag.classify(TODAY.plusDays(2), TODAY, 1));
    }

    @Test
    void daysRemainingIsDeadlineMinusToday() {
        assertEquals(-2, DeadlineFlag.daysRemaining(TODAY.minusDays(2), TODAY));
        assertEquals(5, DeadlineFlag.daysRemaining(TODAY.plusDays(5), TODAY));
    }
}
