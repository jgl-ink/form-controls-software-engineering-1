package crmpams.testsupport;

import crmpams.dao.AccountDao;
import crmpams.dao.AuditDao;
import crmpams.dao.CustomerDao;
import crmpams.dao.OrderDao;
import crmpams.dao.ReferenceDataDao;
import crmpams.db.SqlWork;
import crmpams.db.TransactionRunner;
import crmpams.model.Customer;
import crmpams.model.LookupItem;
import crmpams.model.MonitoringRow;
import crmpams.model.NewOrderRecord;
import crmpams.model.OrderSearchCriteria;
import crmpams.model.OrderSummary;
import crmpams.model.Role;
import crmpams.model.UrgencyLevel;
import crmpams.model.UserAccount;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * An in-memory copy of the eleven tables with DAO implementations that behave like the JDBC ones.
 * Used by unit tests and by the screenshot tool so they run without a MySQL server.
 */
public final class InMemoryStore implements TransactionRunner {

    public record ProjectType(int id, String name, boolean active) { }

    public record Stage(int id, String name, int order) { }

    public record Employee(int id, String firstName, String lastName, String position, String contact,
            String email) { }

    public record User(int id, int employeeId, String username, String passwordHash, Role role, boolean active,
            Integer createdBy) { }

    public record Order(int id, int customerId, int projectTypeId, String description, String requiredOutput,
            java.time.LocalDate deadline, int urgencyLevelId, String instructions, int stageId,
            LocalDateTime dateReceived, int encodedBy) { }

    public record ReferenceFile(int orderId, String name, String location) { }

    public record Assignment(int orderId, int employeeId, boolean current) { }

    public record History(int orderId, Integer stageId, int changedBy, LocalDateTime at, String field,
            String oldValue, String newValue) { }

    public record Activity(Integer userId, String action, String details, LocalDateTime at) { }

    public final List<ProjectType> projectTypes = new ArrayList<>();
    public final List<UrgencyLevel> urgencyLevels = new ArrayList<>();
    public final List<Stage> stages = new ArrayList<>();
    public final List<Employee> employees = new ArrayList<>();
    public final List<User> users = new ArrayList<>();
    public final List<Customer> customers = new ArrayList<>();
    public final List<Order> orders = new ArrayList<>();
    public final List<ReferenceFile> referenceFiles = new ArrayList<>();
    public final List<Assignment> assignments = new ArrayList<>();
    public final List<History> history = new ArrayList<>();
    public final List<Activity> activity = new ArrayList<>();

    /** Number of transactions run, so tests can check that invalid input never reached the database. */
    public int transactions;

    // ------------------------------------------------------------------ TransactionRunner

    @Override
    public <T> T read(SqlWork<T> work) throws SQLException {
        return work.run(null);
    }

    @Override
    public <T> T inTransaction(SqlWork<T> work) throws SQLException {
        transactions++;
        return work.run(null);
    }

    // ------------------------------------------------------------------ helpers

    private String stageName(int stageId) {
        return stages.stream().filter(s -> s.id() == stageId).map(Stage::name).findFirst().orElse("?");
    }

    private Optional<Employee> currentAssignee(int orderId) {
        return assignments.stream().filter(a -> a.orderId() == orderId && a.current()).findFirst()
                .flatMap(a -> employees.stream().filter(e -> e.id() == a.employeeId()).findFirst());
    }

    private static String fullName(Employee e) {
        return e.firstName() + " " + e.lastName();
    }

    private static boolean containsIgnoreCase(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needle.toLowerCase(Locale.ROOT));
    }

    // ------------------------------------------------------------------ DAOs

    public ReferenceDataDao referenceDao() {
        return new ReferenceDataDao() {
            @Override
            public List<LookupItem> listProjectTypes(Connection c) {
                return projectTypes.stream().filter(ProjectType::active)
                        .sorted(Comparator.comparing(ProjectType::name))
                        .map(p -> new LookupItem(p.id(), p.name())).toList();
            }

            @Override
            public List<UrgencyLevel> listUrgencyLevels(Connection c) {
                return urgencyLevels.stream().sorted(Comparator.comparingInt(UrgencyLevel::rank)).toList();
            }

            @Override
            public List<LookupItem> listWorkflowStages(Connection c) {
                return stages.stream().sorted(Comparator.comparingInt(Stage::order))
                        .map(s -> new LookupItem(s.id(), s.name())).toList();
            }

            @Override
            public List<LookupItem> listProductionStaff(Connection c) {
                return users.stream().filter(u -> u.role() == Role.PRODUCTION_STAFF && u.active())
                        .map(u -> employees.stream().filter(e -> e.id() == u.employeeId()).findFirst().orElseThrow())
                        .sorted(Comparator.comparing(Employee::firstName).thenComparing(Employee::lastName))
                        .map(e -> new LookupItem(e.id(), fullName(e) + " (" + e.position() + ")"))
                        .toList();
            }

            @Override
            public Optional<Integer> findStageId(Connection c, String stageName) {
                return stages.stream().filter(s -> s.name().equals(stageName)).map(Stage::id).findFirst();
            }

            @Override
            public boolean isActiveProjectType(Connection c, int id) {
                return projectTypes.stream().anyMatch(p -> p.id() == id && p.active());
            }

            @Override
            public boolean isActiveUrgencyLevel(Connection c, int id) {
                return urgencyLevels.stream().anyMatch(u -> u.id() == id);
            }
        };
    }

    public CustomerDao customerDao() {
        return new CustomerDao() {
            @Override
            public Optional<Customer> findById(Connection c, int id) {
                return customers.stream().filter(x -> x.id() == id).findFirst();
            }

            @Override
            public Optional<Customer> findByNameAndContact(Connection c, String name, String contact) {
                return customers.stream()
                        .filter(x -> x.name().equalsIgnoreCase(name) && x.contactNumber().equals(contact))
                        .findFirst();
            }

            @Override
            public int insert(Connection c, Customer customer) {
                int id = customers.size() + 1;
                customers.add(new Customer(id, customer.name(), customer.contactNumber(), customer.email()));
                return id;
            }
        };
    }

    public OrderDao orderDao() {
        return new OrderDao() {
            @Override
            public Optional<Integer> findOpenDuplicate(Connection c, int customerId, String description,
                    Collection<String> closedStages) {
                return orders.stream()
                        .filter(o -> o.customerId() == customerId
                                && o.description().strip().equalsIgnoreCase(description.strip())
                                && !closedStages.contains(stageName(o.stageId())))
                        .map(Order::id)
                        .max(Integer::compare);
            }

            @Override
            public int insert(Connection c, NewOrderRecord o) {
                int id = 1000 + orders.size() + 1;
                orders.add(new Order(id, o.customerId(), o.projectTypeId(), o.projectDescription(),
                        o.requiredOutput(), o.deadline(), o.urgencyLevelId(), o.specialInstructions(),
                        o.stageId(), o.dateReceived(), o.encodedBy()));
                return id;
            }

            @Override
            public void insertReferenceFile(Connection c, int orderId, String name, String location) {
                referenceFiles.add(new ReferenceFile(orderId, name, location));
            }

            @Override
            public List<OrderSummary> search(Connection c, OrderSearchCriteria k, Collection<String> closed,
                    int limit) {
                String text = k.text() == null ? "" : k.text().strip();
                return orders.stream()
                        .filter(o -> text.isEmpty() || String.valueOf(o.id()).contains(text)
                                || containsIgnoreCase(customer(o).name(), text)
                                || containsIgnoreCase(o.description(), text))
                        .filter(o -> k.projectTypeId() == null || o.projectTypeId() == k.projectTypeId())
                        .filter(o -> k.stageId() == null || o.stageId() == k.stageId())
                        .filter(o -> k.employeeId() == null
                                || currentAssignee(o.id()).map(e -> e.id() == k.employeeId()).orElse(false))
                        .filter(o -> k.urgencyLevelId() == null || o.urgencyLevelId() == k.urgencyLevelId())
                        .filter(o -> !k.useDateRange()
                                || (!o.dateReceived().toLocalDate().isBefore(k.receivedFrom())
                                && !o.dateReceived().toLocalDate().isAfter(k.receivedTo())))
                        .filter(o -> k.includeClosed() || !closed.contains(stageName(o.stageId())))
                        .sorted(Comparator.comparing(Order::dateReceived).reversed())
                        .limit(limit)
                        .map(this::summary)
                        .toList();
            }

            private Customer customer(Order o) {
                return customers.stream().filter(x -> x.id() == o.customerId()).findFirst().orElseThrow();
            }

            private OrderSummary summary(Order o) {
                Customer cu = customer(o);
                String type = projectTypes.stream().filter(p -> p.id() == o.projectTypeId())
                        .map(ProjectType::name).findFirst().orElse("?");
                String urg = urgencyLevels.stream().filter(u -> u.id() == o.urgencyLevelId())
                        .map(UrgencyLevel::name).findFirst().orElse("?");
                return new OrderSummary(o.id(), cu.name(), cu.contactNumber(), type, o.description(),
                        o.requiredOutput(), o.deadline(), urg, stageName(o.stageId()),
                        currentAssignee(o.id()).map(InMemoryStore::fullName).orElse(null), o.dateReceived(),
                        o.instructions());
            }

            @Override
            public List<MonitoringRow> findForMonitoring(Connection c, Collection<Integer> stageIds,
                    Integer employeeId, String text) {
                String t = text == null ? "" : text.strip();
                boolean numeric = !t.isEmpty() && t.chars().allMatch(Character::isDigit);
                return orders.stream()
                        .filter(o -> stageIds.contains(o.stageId()))
                        .filter(o -> employeeId == null
                                || currentAssignee(o.id()).map(e -> e.id() == employeeId).orElse(false))
                        .filter(o -> t.isEmpty()
                                || (numeric ? String.valueOf(o.id()).equals(t)
                                : currentAssignee(o.id()).map(e -> containsIgnoreCase(fullName(e), t)).orElse(false)))
                        .sorted(Comparator.comparing(Order::deadline).thenComparing(Order::id))
                        .map(o -> new MonitoringRow(o.id(), customer(o).name(), stageName(o.stageId()),
                                currentAssignee(o.id()).map(InMemoryStore::fullName).orElse(null), o.deadline(),
                                history.stream().filter(h -> h.orderId() == o.id()).map(History::at)
                                        .max(Comparator.naturalOrder()).orElse(o.dateReceived()),
                                0, null))
                        .toList();
            }
        };
    }

    public AccountDao accountDao() {
        return new AccountDao() {
            @Override
            public Optional<UserAccount> findByUsername(Connection c, String username) {
                return users.stream().filter(u -> u.username().equalsIgnoreCase(username)).findFirst()
                        .map(u -> {
                            Employee e = employees.stream().filter(x -> x.id() == u.employeeId()).findFirst()
                                    .orElseThrow();
                            return new UserAccount(u.id(), u.employeeId(), u.username(), u.passwordHash(), u.role(),
                                    u.active(), fullName(e));
                        });
            }

            @Override
            public boolean usernameExists(Connection c, String username) {
                return users.stream().anyMatch(u -> u.username().equalsIgnoreCase(username));
            }

            @Override
            public int insertEmployee(Connection c, String first, String last, String position, String contact,
                    String email) {
                int id = employees.size() + 1;
                employees.add(new Employee(id, first, last, position, contact, email));
                return id;
            }

            @Override
            public int insertUser(Connection c, int employeeId, String username, String hash, Role role,
                    boolean active, int createdBy) {
                int id = users.size() + 1;
                users.add(new User(id, employeeId, username, hash, role, active, createdBy));
                return id;
            }
        };
    }

    public AuditDao auditDao() {
        return new AuditDao() {
            @Override
            public void addStatusHistory(Connection c, int orderId, Integer stageId, int changedBy,
                    LocalDateTime at, String field, String oldValue, String newValue) {
                history.add(new History(orderId, stageId, changedBy, at, field, oldValue, newValue));
            }

            @Override
            public void logActivity(Connection c, Integer userId, String action, String details,
                    LocalDateTime at) {
                activity.add(new Activity(userId, action, details, at));
            }
        };
    }
}
