package crmpams.testsupport;

import crmpams.AppContext;
import crmpams.model.Customer;
import crmpams.model.CurrentUser;
import crmpams.model.Role;
import crmpams.model.UrgencyLevel;
import crmpams.model.WorkflowStages;
import crmpams.security.PasswordHasher;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Fictional sample data that matches sql/seed.sql, a fixed clock, and ready-made users.
 * All names, numbers, and orders are invented for testing.
 */
public final class TestFixtures {

    public static final ZoneId MANILA = ZoneId.of("Asia/Manila");
    public static final LocalDate TODAY = LocalDate.of(2026, 10, 27);
    public static final Clock CLOCK = Clock.fixed(TODAY.atTime(9, 0).atZone(MANILA).toInstant(), MANILA);
    public static final String DEMO_PASSWORD = "Demo2026pass";

    public static final CurrentUser MANAGER =
            new CurrentUser(1, 1, "rvillanueva", "Ramon Villanueva", Role.OPERATIONS_MANAGER);
    public static final CurrentUser OFFICER =
            new CurrentUser(2, 2, "tbautista", "Teresa Bautista", Role.OFFICER_IN_CHARGE);
    public static final CurrentUser DESIGNER =
            new CurrentUser(3, 3, "preyes", "Paolo Reyes", Role.PRODUCTION_STAFF);

    /** A fast hasher for tests; the application uses 600,000 iterations. */
    public static final PasswordHasher FAST_HASHER = new PasswordHasher(1_000);

    private TestFixtures() {
    }

    public static InMemoryStore store() {
        InMemoryStore s = new InMemoryStore();
        String[] types = {"3D Model", "Architectural Floor Plan", "Blueprint Printing", "Large Format Scanning",
            "Site Development Plan", "Structural Layout", "Thesis Board Printing"};
        for (int i = 0; i < types.length; i++) {
            s.projectTypes.add(new InMemoryStore.ProjectType(i + 1, types[i], true));
        }
        s.urgencyLevels.add(new UrgencyLevel(1, "Low", 1));
        s.urgencyLevels.add(new UrgencyLevel(2, "Normal", 2));
        s.urgencyLevels.add(new UrgencyLevel(3, "High", 3));
        for (int i = 0; i < WorkflowStages.ALL.size(); i++) {
            s.stages.add(new InMemoryStore.Stage(i + 1, WorkflowStages.ALL.get(i), i + 1));
        }

        String hash = FAST_HASHER.hash(DEMO_PASSWORD.toCharArray());
        employee(s, "Ramon", "Villanueva", "Operations Manager", "rvillanueva", Role.OPERATIONS_MANAGER, true, hash);
        employee(s, "Teresa", "Bautista", "Officer-in-Charge", "tbautista", Role.OFFICER_IN_CHARGE, true, hash);
        employee(s, "Paolo", "Reyes", "Designer", "preyes", Role.PRODUCTION_STAFF, true, hash);
        employee(s, "Jasmine", "Cruz", "Drafter", "jcruz", Role.PRODUCTION_STAFF, true, hash);
        employee(s, "Mark", "Delos Santos", "Modeler", "mdelossantos", Role.PRODUCTION_STAFF, true, hash);
        employee(s, "Ana", "Lim", "Drafter", "alim", Role.PRODUCTION_STAFF, false, hash);

        s.customers.add(new Customer(1, "Arch. Lea Navarro", "09171234501", "lea.navarro@example.com"));
        s.customers.add(new Customer(2, "Solid Rock Builders Inc.", "0281234502", null));
        s.customers.add(new Customer(3, "Engr. Dennis Tan", "09181234503", null));
        s.customers.add(new Customer(4, "Bayview Construction Corp.", "09191234504", "projects@example.com"));
        s.customers.add(new Customer(5, "Kristine Ocampo", "09201234505", null));

        // id, customer, description, deadline offset, urgency, stage, assignee employee, received, address
        // Order lines match sql/seed.sql. Order 1001 has two project types.
        order(s, 1001, 1, "Two-storey residential floor plan revision", 5, 3, 3, 3, TODAY.minusDays(3).atTime(10, 5), null);
        line(s, 1001, 1, 2, "Bond paper", "A1", 2, 1);
        line(s, 1001, 2, 3, "Blueprint paper", "A1", 2, 3);
        order(s, 1002, 2, "Structural layout for three-storey commercial building", -1, 3, 2, 4,
                TODAY.minusDays(7).atTime(14, 20), "45 Sample Street, Barangay Example, Quezon City");
        line(s, 1002, 1, 6, "Bond paper", "20 x 30 in", 5, 2);
        order(s, 1003, 3, "Blueprint printing of approved site development plan", 2, 2, 1, 0, TODAY.atTime(8, 40), null);
        line(s, 1003, 1, 3, "Blueprint paper", "A1", 8, 3);
        order(s, 1004, 4, "3D model of warehouse extension", 12, 2, 4, 5, TODAY.minusDays(12).atTime(9, 0), null);
        line(s, 1004, 1, 1, "Digital file only", "A1", 1, 1);
        order(s, 1005, 5, "Thesis board printing, 4 plates", 1, 3, 5, 3, TODAY.minusDays(2).atTime(16, 45), null);
        line(s, 1005, 1, 7, "Photo paper", "A1", 4, 1);
        order(s, 1006, 1, "Kitchen renovation layout", -10, 2, 7, 4, TODAY.minusDays(22).atTime(11, 30), null);
        line(s, 1006, 1, 2, "Bond paper", "A2", 1, 2);
        order(s, 1007, 2, "As-built plans, 6 sheets", 3, 1, 6, 5, TODAY.minusDays(15).atTime(13, 10),
                "45 Sample Street, Barangay Example, Quezon City");
        line(s, 1007, 1, 3, "Blueprint paper", "A1", 6, 2);
        order(s, 1008, 3, "Site development plan revision 2", 8, 1, 3, 4, TODAY.minusDays(5).atTime(15, 0), null);
        line(s, 1008, 1, 5, "Vellum", "20 x 30 in", 3, 1);
        return s;
    }

    private static void employee(InMemoryStore s, String first, String last, String position, String username,
            Role role, boolean active, String hash) {
        int id = s.employees.size() + 1;
        s.employees.add(new InMemoryStore.Employee(id, first, last, position, null, null));
        s.users.add(new InMemoryStore.User(id, id, username, hash, role, active, id == 1 ? null : 1));
    }

    private static void order(InMemoryStore s, int id, int customer, String description, int deadlineOffset,
            int urgency, int stage, int assignee, LocalDateTime received, String address) {
        s.orders.add(new InMemoryStore.Order(id, customer, description, TODAY.plusDays(deadlineOffset), urgency,
                address, null, stage, received, 2));
        if (assignee > 0) {
            s.assignments.add(new InMemoryStore.Assignment(id, assignee, true));
        }
        s.history.add(new InMemoryStore.History(id, stage, 2, received.plusHours(1), "Status", null,
                WorkflowStages.ALL.get(stage - 1)));
    }

    private static void line(InMemoryStore s, int orderId, int lineNo, int type, String media, String size,
            int sheets, int sets) {
        s.items.add(new InMemoryStore.Item(orderId, lineNo, type, media, size, sheets, sets));
    }

    public static AppContext context(InMemoryStore s) {
        return AppContext.create(s, s.referenceDao(), s.customerDao(), s.orderDao(), s.accountDao(), s.auditDao(),
                FAST_HASHER, CLOCK);
    }
}
