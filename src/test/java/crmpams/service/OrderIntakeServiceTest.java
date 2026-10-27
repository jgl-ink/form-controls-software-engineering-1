package crmpams.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crmpams.model.OrderLine;
import crmpams.model.OrderRequest;
import crmpams.security.AccessDeniedException;
import crmpams.testsupport.InMemoryStore;
import crmpams.testsupport.TestFixtures;
import crmpams.validation.Fields;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrderIntakeServiceTest {

    private InMemoryStore store;
    private OrderIntakeService service;

    @BeforeEach
    void setUp() {
        store = TestFixtures.store();
        service = TestFixtures.context(store).orderIntake();
    }

    private static final OrderLine FLOOR_PLAN = new OrderLine(2, "Architectural Floor Plan", "Bond paper", "A1", 2, 1);
    private static final OrderLine BLUEPRINT = new OrderLine(3, "Blueprint Printing", "Blueprint paper", "A1", 2, 3);

    private static OrderRequest request(String name, String contact, String description, LocalDate deadline,
            String file) {
        return new OrderRequest(null, name, contact, "", description, List.of(FLOOR_PLAN), deadline, 2, "", "",
                file);
    }

    @Test
    void savesANewCustomerOrderHistoryAndActivityTogether() throws Exception {
        int customersBefore = store.customers.size();
        SaveOrderResult r = service.save(TestFixtures.OFFICER,
                request("Juan Dela Cruz", "+639171112222", "Floor plan for a bungalow",
                        TestFixtures.TODAY.plusDays(4), ""), false);

        assertEquals(SaveOrderResult.Status.SAVED, r.status());
        assertTrue(r.newCustomer());
        assertEquals(customersBefore + 1, store.customers.size());
        assertEquals("09171112222", store.customers.get(store.customers.size() - 1).contactNumber());
        InMemoryStore.Order saved = store.orders.get(store.orders.size() - 1);
        assertEquals(r.orderId(), saved.id());
        assertEquals(1, saved.stageId());                                        // New
        assertEquals(TestFixtures.OFFICER.userId(), saved.encodedBy());         // hidden value from session
        assertEquals(TestFixtures.TODAY, saved.dateReceived().toLocalDate());    // set by the system
        assertTrue(store.history.stream().anyMatch(h -> h.orderId() == r.orderId()));
        assertTrue(store.activity.stream().anyMatch(a -> a.action().equals("ORDER_CREATED")));
    }

    @Test
    void reusesAnExistingCustomerInsteadOfCreatingASecondRecord() throws Exception {
        int before = store.customers.size();
        SaveOrderResult r = service.save(TestFixtures.OFFICER,
                request("arch. lea navarro", "09171234501", "New elevation drawing",
                        TestFixtures.TODAY.plusDays(2), ""), false);
        assertEquals(SaveOrderResult.Status.SAVED, r.status());
        assertFalse(r.newCustomer());
        assertEquals(before, store.customers.size());
    }

    @Test
    void flagsAPossibleDuplicateAndSavesNothingUntilConfirmed() throws Exception {
        int ordersBefore = store.orders.size();
        OrderRequest dup = request("Arch. Lea Navarro", "09171234501",
                "  two-storey residential floor plan revision ", TestFixtures.TODAY.plusDays(3), "");

        SaveOrderResult first = service.save(TestFixtures.OFFICER, dup, false);
        assertEquals(SaveOrderResult.Status.POSSIBLE_DUPLICATE, first.status());
        assertEquals(1001, first.duplicateOrderId());
        assertEquals(ordersBefore, store.orders.size());

        SaveOrderResult confirmed = service.save(TestFixtures.OFFICER, dup, true);
        assertEquals(SaveOrderResult.Status.SAVED, confirmed.status());
        assertEquals(ordersBefore + 1, store.orders.size());
    }

    @Test
    void closedOrdersAreNotDuplicates() throws Exception {
        // Order 1006 has the same description but is Completed.
        SaveOrderResult r = service.save(TestFixtures.OFFICER,
                request("Arch. Lea Navarro", "09171234501", "Kitchen renovation layout",
                        TestFixtures.TODAY.plusDays(3), ""), false);
        assertEquals(SaveOrderResult.Status.SAVED, r.status());
    }

    @Test
    void invalidInputIsRejectedAndNothingIsWritten() throws Exception {
        int ordersBefore = store.orders.size();
        SaveOrderResult r = service.save(TestFixtures.OFFICER,
                new OrderRequest(null, "", "123", "bad@", "", List.of(), TestFixtures.TODAY.minusDays(1), null,
                        "", "", ""), false);
        assertEquals(SaveOrderResult.Status.INVALID, r.status());
        for (String f : new String[] {Fields.CUSTOMER_NAME, Fields.CONTACT_NUMBER, Fields.EMAIL,
            Fields.ORDER_LINES, Fields.PROJECT_DESCRIPTION, Fields.DEADLINE, Fields.URGENCY}) {
            assertNotNull(r.errors().messageFor(f), f);
        }
        assertEquals(ordersBefore, store.orders.size());
        assertEquals(0, store.transactions);
    }

    @Test
    void storesOnlyTheReferenceFileNameAndLocation() throws Exception {
        Path file = Files.createTempFile("site-plan", ".dwg");
        SaveOrderResult r = service.save(TestFixtures.MANAGER,
                request("Juan Dela Cruz", "09171112222", "Site plan update", TestFixtures.TODAY, file.toString()),
                false);
        assertEquals(SaveOrderResult.Status.SAVED, r.status());
        InMemoryStore.ReferenceFile f = store.referenceFiles.get(0);
        assertEquals(file.getFileName().toString(), f.name());
        assertEquals(file.toString(), f.location());
    }

    @Test
    void savesEveryOrderLineWithItsOwnProjectTypeAndTheAddress() throws Exception {
        OrderRequest r = new OrderRequest(null, "Juan Dela Cruz", "09171112222", "", "Floor plan and blueprints",
                List.of(FLOOR_PLAN, BLUEPRINT), TestFixtures.TODAY.plusDays(2), 3, "12 Sample St., Quezon City", "",
                "");
        SaveOrderResult saved = service.save(TestFixtures.OFFICER, r, false);
        assertEquals(SaveOrderResult.Status.SAVED, saved.status());
        List<InMemoryStore.Item> items = store.items.stream().filter(i -> i.orderId() == saved.orderId()).toList();
        assertEquals(2, items.size());
        assertEquals(2, items.get(0).projectTypeId());
        assertEquals(3, items.get(1).projectTypeId());
        assertEquals(2, items.get(1).lineNo());
        assertEquals(6, BLUEPRINT.totalSheets());                                // 2 sheets x 3 sets
        assertEquals("12 Sample St., Quezon City", store.orders.get(store.orders.size() - 1).deliveryAddress());
        assertEquals(TestFixtures.TODAY.atTime(9, 0), saved.dateReceived());     // date and time received
    }

    @Test
    void anInvalidLineIsReportedWithItsLineNumber() throws Exception {
        OrderLine bad = new OrderLine(3, "Blueprint Printing", "", "A1", 0, 1);
        OrderRequest r = new OrderRequest(null, "Juan Dela Cruz", "09171112222", "", "Floor plan and blueprints",
                List.of(FLOOR_PLAN, bad), TestFixtures.TODAY, 2, "", "", "");
        SaveOrderResult result = service.save(TestFixtures.OFFICER, r, false);
        assertTrue(result.errors().messageFor(Fields.ORDER_LINES).startsWith("Line 2:"));
        assertEquals(0, store.transactions);
    }

    @Test
    void productionStaffCannotCreateOrders() {
        assertThrows(AccessDeniedException.class, () -> service.save(TestFixtures.DESIGNER,
                request("Juan Dela Cruz", "09171112222", "Floor plan", TestFixtures.TODAY, ""), false));
    }

    @Test
    void lookupFindsTheExistingCustomer() throws Exception {
        assertTrue(service.lookupCustomer(TestFixtures.OFFICER, "Engr. Dennis Tan", "+639181234503").isPresent());
        assertFalse(service.lookupCustomer(TestFixtures.OFFICER, "Engr. Dennis Tan", "09180000000").isPresent());
    }
}
