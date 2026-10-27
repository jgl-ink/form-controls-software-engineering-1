package crmpams.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crmpams.AppContext;
import crmpams.model.DeadlineFlag;
import crmpams.model.MonitoringFilter;
import crmpams.model.MonitoringRow;
import crmpams.model.OrderSearchCriteria;
import crmpams.model.OrderSummary;
import crmpams.security.AccessDeniedException;
import crmpams.testsupport.TestFixtures;
import crmpams.validation.Fields;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SearchAndMonitoringServiceTest {

    private static final Set<Integer> ACTIVE_STAGES = Set.of(1, 2, 3, 4, 5);
    private AppContext ctx;

    @BeforeEach
    void setUp() {
        ctx = TestFixtures.context(TestFixtures.store());
    }

    private static OrderSearchCriteria text(String t) {
        return new OrderSearchCriteria(t, null, null, null, null, false, null, null, false);
    }

    @Test
    void emptySearchListsOpenOrdersOnly() throws Exception {
        SearchResult r = ctx.search().search(TestFixtures.OFFICER, text(""));
        assertEquals(6, r.rows().size());                                      // 1006 and 1007 are closed
        assertTrue(r.rows().stream().noneMatch(o -> o.orderId() == 1006 || o.orderId() == 1007));
    }

    @Test
    void textMatchesCustomerNameAndDescription() throws Exception {
        // Navarro has orders 1001 (open) and 1006 (completed); only the open one is listed by default.
        assertEquals(1, ctx.search().search(TestFixtures.OFFICER, text("navarro")).rows().size());
        assertEquals(1, ctx.search().search(TestFixtures.OFFICER, text("warehouse")).rows().size());
    }

    @Test
    void closedStatusNeedsTheIncludeCheckbox() throws Exception {
        OrderSearchCriteria k = new OrderSearchCriteria("", null, 7, null, null, false, null, null, false);
        assertNotNull(ctx.search().search(TestFixtures.OFFICER, k).errors().messageFor(Fields.STATUS));
        OrderSearchCriteria withClosed = new OrderSearchCriteria("", null, 7, null, null, false, null, null, true);
        assertEquals(1, ctx.search().search(TestFixtures.OFFICER, withClosed).rows().size());
    }

    @Test
    void endDateBeforeStartDateIsRejected() throws Exception {
        OrderSearchCriteria k = new OrderSearchCriteria("", null, null, null, null, true,
                TestFixtures.TODAY, TestFixtures.TODAY.minusDays(1), false);
        assertNotNull(ctx.search().search(TestFixtures.OFFICER, k).errors().messageFor(Fields.DATE_TO));
    }

    @Test
    void dateRangeIncludesBothEnds() throws Exception {
        OrderSearchCriteria k = new OrderSearchCriteria("", null, null, null, null, true,
                TestFixtures.TODAY, TestFixtures.TODAY, false);
        for (OrderSummary o : ctx.search().search(TestFixtures.OFFICER, k).rows()) {
            assertEquals(TestFixtures.TODAY, o.dateReceived().toLocalDate());
        }
        assertEquals(1, ctx.search().search(TestFixtures.OFFICER, k).rows().size());
    }

    @Test
    void productionStaffCannotSearch() {
        assertThrows(AccessDeniedException.class, () -> ctx.search().search(TestFixtures.DESIGNER, text("")));
    }

    @Test
    void monitoringNeedsAtLeastOneStage() throws Exception {
        MonitoringResult r = ctx.monitoring().load(TestFixtures.MANAGER,
                new MonitoringFilter("", Set.of(), null, null, 3));
        assertNotNull(r.errors().messageFor(Fields.STAGES));
    }

    @Test
    void monitoringFlagsEveryActiveOrder() throws Exception {
        MonitoringResult r = ctx.monitoring().load(TestFixtures.MANAGER,
                new MonitoringFilter("", ACTIVE_STAGES, null, null, 3));
        assertEquals(6, r.rows().size());
        MonitoringRow passed = r.rows().stream().filter(x -> x.orderId() == 1002).findFirst().orElseThrow();
        assertEquals(DeadlineFlag.PASSED, passed.flag());
        assertEquals(-1, passed.daysLeft());
        MonitoringRow later = r.rows().stream().filter(x -> x.orderId() == 1004).findFirst().orElseThrow();
        assertEquals(DeadlineFlag.ON_SCHEDULE, later.flag());
    }

    @Test
    void monitoringFiltersByFlagAndEmployeeName() throws Exception {
        MonitoringResult approaching = ctx.monitoring().load(TestFixtures.MANAGER,
                new MonitoringFilter("", ACTIVE_STAGES, null, DeadlineFlag.APPROACHING, 3));
        assertTrue(approaching.rows().stream().allMatch(x -> x.flag() == DeadlineFlag.APPROACHING));
        assertEquals(2, approaching.rows().size());                            // 1003 (2 days), 1005 (1 day)

        MonitoringResult paolo = ctx.monitoring().load(TestFixtures.MANAGER,
                new MonitoringFilter("paolo", ACTIVE_STAGES, null, null, 3));
        assertTrue(paolo.rows().stream().allMatch(x -> "Paolo Reyes".equals(x.assignedTo())));
    }

    @Test
    void monitoringIgnoresClosedStagesEvenIfSent() throws Exception {
        MonitoringResult r = ctx.monitoring().load(TestFixtures.MANAGER,
                new MonitoringFilter("", Set.of(6, 7), null, null, 3));
        assertEquals(0, r.rows().size());
    }
}
