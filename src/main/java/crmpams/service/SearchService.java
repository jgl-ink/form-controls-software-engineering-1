package crmpams.service;

import crmpams.dao.OrderDao;
import crmpams.dao.ReferenceDataDao;
import crmpams.db.TransactionRunner;
import crmpams.model.CurrentUser;
import crmpams.model.LookupItem;
import crmpams.model.OrderLine;
import crmpams.model.OrderSearchCriteria;
import crmpams.model.OrderSummary;
import crmpams.model.Role;
import crmpams.model.WorkflowStages;
import crmpams.security.AccessControl;
import crmpams.validation.FieldRules;
import crmpams.validation.Fields;
import crmpams.validation.ValidationResult;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

/** Search (12.0): Enter Search Criteria (12.1) and Run Search Query (12.2). */
public final class SearchService {

    public static final int RESULT_LIMIT = 500;
    private static final Role[] ALLOWED = {Role.OPERATIONS_MANAGER, Role.OFFICER_IN_CHARGE};

    private final TransactionRunner tx;
    private final ReferenceDataDao referenceDao;
    private final OrderDao orderDao;
    private final Clock clock;

    public SearchService(TransactionRunner tx, ReferenceDataDao referenceDao, OrderDao orderDao, Clock clock) {
        this.tx = tx;
        this.referenceDao = referenceDao;
        this.orderDao = orderDao;
        this.clock = clock;
    }

    public LocalDate today() {
        return LocalDate.now(clock);
    }

    public SearchOptions loadOptions(CurrentUser user) throws SQLException {
        AccessControl.require(user, ALLOWED);
        return tx.read(c -> new SearchOptions(
                referenceDao.listProjectTypes(c),
                referenceDao.listWorkflowStages(c),
                referenceDao.listProductionStaff(c),
                referenceDao.listUrgencyLevels(c)));
    }

    /**
     * Rules shared by the form and the service. selectedStageName is the name of the chosen
     * status, or null for All.
     */
    public static ValidationResult validate(OrderSearchCriteria k, String selectedStageName, LocalDate today) {
        ValidationResult v = new ValidationResult().check(Fields.SEARCH_TEXT, FieldRules.searchText(k.text()));
        if (k.useDateRange()) {
            v.check(Fields.DATE_FROM, FieldRules.dateFrom(k.receivedFrom(), today));
            v.check(Fields.DATE_TO, FieldRules.dateTo(k.receivedFrom(), k.receivedTo()));
        }
        if (WorkflowStages.isClosed(selectedStageName) && !k.includeClosed()) {
            v.check(Fields.STATUS, "To search " + selectedStageName
                    + " orders, also tick Include released and completed orders.");
        }
        return v;
    }

    /** IPO 12.3: the order lines shown with the full record of a selected order. */
    public List<OrderLine> loadOrderLines(CurrentUser user, int orderId) throws SQLException {
        AccessControl.require(user, ALLOWED);
        return tx.read(c -> orderDao.findLines(c, orderId));
    }

    public SearchResult search(CurrentUser user, OrderSearchCriteria k) throws SQLException {
        AccessControl.require(user, ALLOWED);
        return tx.read(c -> {
            String stageName = null;
            if (k.stageId() != null) {
                stageName = referenceDao.listWorkflowStages(c).stream()
                        .filter(s -> k.stageId().equals(s.id()))
                        .map(LookupItem::name)
                        .findFirst()
                        .orElse(null);
            }
            ValidationResult v = validate(k, stageName, today());
            if (!v.isValid()) {
                return new SearchResult(v, List.of(), false);
            }
            List<OrderSummary> rows = orderDao.search(c, k, WorkflowStages.CLOSED, RESULT_LIMIT);
            return new SearchResult(v, rows, rows.size() >= RESULT_LIMIT);
        });
    }
}
