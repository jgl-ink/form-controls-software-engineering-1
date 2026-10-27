package crmpams.service;

import crmpams.dao.AuditDao;
import crmpams.dao.CustomerDao;
import crmpams.dao.OrderDao;
import crmpams.dao.ReferenceDataDao;
import crmpams.db.TransactionRunner;
import crmpams.model.Customer;
import crmpams.model.CurrentUser;
import crmpams.model.NewOrderRecord;
import crmpams.model.OrderRequest;
import crmpams.model.Role;
import crmpams.model.WorkflowStages;
import crmpams.security.AccessControl;
import crmpams.validation.FieldRules;
import crmpams.validation.Fields;
import crmpams.validation.ValidationResult;
import java.nio.file.Path;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Customer Order Intake (5.0): customer lookup (5.1), new customer registration (5.2),
 * order details validation (5.3), and order record creation (5.4).
 * Users: Operations Manager and Officer-in-Charge.
 */
public final class OrderIntakeService {

    private static final Role[] ALLOWED = {Role.OPERATIONS_MANAGER, Role.OFFICER_IN_CHARGE};

    private final TransactionRunner tx;
    private final ReferenceDataDao referenceDao;
    private final CustomerDao customerDao;
    private final OrderDao orderDao;
    private final AuditDao auditDao;
    private final Clock clock;

    public OrderIntakeService(TransactionRunner tx, ReferenceDataDao referenceDao, CustomerDao customerDao,
            OrderDao orderDao, AuditDao auditDao, Clock clock) {
        this.tx = tx;
        this.referenceDao = referenceDao;
        this.customerDao = customerDao;
        this.orderDao = orderDao;
        this.auditDao = auditDao;
        this.clock = clock;
    }

    public OrderFormData loadFormData(CurrentUser user) throws SQLException {
        AccessControl.require(user, ALLOWED);
        return tx.read(c -> new OrderFormData(referenceDao.listProjectTypes(c), referenceDao.listUrgencyLevels(c)));
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    /** IPO 5.1: finds a customer with the same name and contact number. */
    public Optional<Customer> lookupCustomer(CurrentUser user, String name, String contactNumber) throws SQLException {
        AccessControl.require(user, ALLOWED);
        String normalized = FieldRules.normalizeContactNumber(contactNumber);
        if (FieldRules.customerName(name) != null || normalized == null) {
            return Optional.empty();
        }
        return tx.read(c -> customerDao.findByNameAndContact(c, FieldRules.clean(name), normalized));
    }

    /** IPO 5.3: the rules shared by the form (before submitting) and this service (before saving). */
    public static ValidationResult validate(OrderRequest r, LocalDate today) {
        return new ValidationResult()
                .check(Fields.CUSTOMER_NAME, FieldRules.customerName(r.customerName()))
                .check(Fields.CONTACT_NUMBER, FieldRules.contactNumber(r.contactNumber(), true))
                .check(Fields.EMAIL, FieldRules.email(r.email()))
                .check(Fields.PROJECT_TYPE, FieldRules.requiredSelection(r.projectTypeId(), "Choose a project type."))
                .check(Fields.PROJECT_DESCRIPTION, FieldRules.requiredText(r.projectDescription(),
                        FieldRules.DESCRIPTION_MIN, FieldRules.DESCRIPTION_MAX, "Project description"))
                .check(Fields.REQUIRED_OUTPUT, FieldRules.optionalText(r.requiredOutput(),
                        FieldRules.REQUIRED_OUTPUT_MAX, "Required output"))
                .check(Fields.DEADLINE, FieldRules.deadline(r.deadline(), today))
                .check(Fields.URGENCY, FieldRules.requiredSelection(r.urgencyLevelId(), "Choose an urgency level."))
                .check(Fields.SPECIAL_INSTRUCTIONS, FieldRules.optionalText(r.specialInstructions(),
                        FieldRules.INSTRUCTIONS_MAX, "Special instructions"))
                .check(Fields.REFERENCE_FILE, FieldRules.referenceFile(r.referenceFilePath()));
    }

    /**
     * IPO 5.4: saves the order in one transaction (customer if new, order, reference file,
     * status history, activity log). When an open order of the same customer has the same
     * project description and confirmDuplicate is false, nothing is saved and the existing
     * order id is returned so the form can ask the user.
     */
    public SaveOrderResult save(CurrentUser user, OrderRequest r, boolean confirmDuplicate) throws SQLException {
        AccessControl.require(user, ALLOWED);
        ValidationResult v = validate(r, today());
        if (!v.isValid()) {
            return SaveOrderResult.invalid(v);
        }
        String name = FieldRules.clean(r.customerName());
        String contact = FieldRules.normalizeContactNumber(r.contactNumber());
        String email = FieldRules.blankToNull(r.email());
        String description = FieldRules.clean(r.projectDescription());
        String filePath = FieldRules.blankToNull(r.referenceFilePath());
        LocalDateTime now = LocalDateTime.now(clock);

        return tx.inTransaction(c -> {
            ValidationResult refErrors = new ValidationResult();
            if (!referenceDao.isActiveProjectType(c, r.projectTypeId())) {
                refErrors.check(Fields.PROJECT_TYPE, "This project type is no longer available. Choose another one.");
            }
            if (!referenceDao.isActiveUrgencyLevel(c, r.urgencyLevelId())) {
                refErrors.check(Fields.URGENCY, "This urgency level is no longer available. Choose another one.");
            }
            if (!refErrors.isValid()) {
                return SaveOrderResult.invalid(refErrors);
            }

            Integer customerId = resolveCustomer(c, r.customerId(), name, contact);
            if (customerId != null && !confirmDuplicate) {
                Optional<Integer> duplicate = orderDao.findOpenDuplicate(c, customerId, description,
                        WorkflowStages.CLOSED);
                if (duplicate.isPresent()) {
                    return SaveOrderResult.possibleDuplicate(duplicate.get());
                }
            }

            boolean newCustomer = customerId == null;
            if (newCustomer) {
                customerId = customerDao.insert(c, new Customer(null, name, contact, email));
                auditDao.logActivity(c, user.userId(), "CUSTOMER_CREATED",
                        "Customer " + customerId + " registered during order intake", now);
            }

            int stageId = referenceDao.findStageId(c, WorkflowStages.NEW)
                    .orElseThrow(() -> new SQLException("Workflow stage 'New' is missing from workflow_stages."));
            int orderId = orderDao.insert(c, new NewOrderRecord(
                    customerId,
                    r.projectTypeId(),
                    description,
                    FieldRules.blankToNull(r.requiredOutput()),
                    r.deadline(),
                    r.urgencyLevelId(),
                    FieldRules.blankToNull(r.specialInstructions()),
                    stageId,
                    now,
                    user.userId()));

            if (filePath != null) {
                Path p = Path.of(filePath);
                orderDao.insertReferenceFile(c, orderId, p.getFileName().toString(), filePath);
            }
            auditDao.addStatusHistory(c, orderId, stageId, user.userId(), now, "Order created", null,
                    WorkflowStages.NEW);
            auditDao.logActivity(c, user.userId(), "ORDER_CREATED", "Order " + orderId + " created", now);
            return SaveOrderResult.saved(orderId, newCustomer);
        });
    }

    /**
     * Uses the customer found by Look Up Customer when its name and contact number still match.
     * Otherwise looks up the customer again by name and contact number, so a second record
     * is never created for the same customer.
     */
    private Integer resolveCustomer(java.sql.Connection c, Integer hiddenId, String name, String contact)
            throws SQLException {
        if (hiddenId != null) {
            Optional<Customer> byId = customerDao.findById(c, hiddenId);
            if (byId.isPresent() && byId.get().name().equalsIgnoreCase(name)
                    && byId.get().contactNumber().equals(contact)) {
                return hiddenId;
            }
        }
        return customerDao.findByNameAndContact(c, name, contact).map(Customer::id).orElse(null);
    }
}
