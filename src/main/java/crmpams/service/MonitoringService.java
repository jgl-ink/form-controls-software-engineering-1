package crmpams.service;

import crmpams.dao.OrderDao;
import crmpams.dao.ReferenceDataDao;
import crmpams.db.TransactionRunner;
import crmpams.model.CurrentUser;
import crmpams.model.DeadlineFlag;
import crmpams.model.LookupItem;
import crmpams.model.MonitoringFilter;
import crmpams.model.MonitoringRow;
import crmpams.model.Role;
import crmpams.model.WorkflowStages;
import crmpams.security.AccessControl;
import crmpams.validation.FieldRules;
import crmpams.validation.Fields;
import crmpams.validation.ValidationResult;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Management Monitoring View (9.1): Active Orders by Stage (9.2), Status Dashboard (9.3),
 * and Deadline Indicators (9.4). Users: Operations Manager and Officer-in-Charge.
 */
public final class MonitoringService {

    private static final Role[] ALLOWED = {Role.OPERATIONS_MANAGER, Role.OFFICER_IN_CHARGE};

    private final TransactionRunner tx;
    private final ReferenceDataDao referenceDao;
    private final OrderDao orderDao;
    private final Clock clock;

    public MonitoringService(TransactionRunner tx, ReferenceDataDao referenceDao, OrderDao orderDao, Clock clock) {
        this.tx = tx;
        this.referenceDao = referenceDao;
        this.orderDao = orderDao;
        this.clock = clock;
    }

    public MonitoringOptions loadOptions(CurrentUser user) throws SQLException {
        AccessControl.require(user, ALLOWED);
        return tx.read(c -> new MonitoringOptions(
                referenceDao.listWorkflowStages(c).stream()
                        .filter(s -> !WorkflowStages.isClosed(s.name()))
                        .toList(),
                referenceDao.listProductionStaff(c)));
    }

    public static ValidationResult validate(MonitoringFilter f) {
        return new ValidationResult()
                .check(Fields.MONITOR_TEXT, FieldRules.monitorText(f.text()))
                .check(Fields.STAGES, f.stageIds() == null || f.stageIds().isEmpty()
                        ? "Tick at least one workflow stage." : null)
                .check(Fields.WARNING_DAYS, FieldRules.warningDays(f.warningDays()));
    }

    public MonitoringResult load(CurrentUser user, MonitoringFilter f) throws SQLException {
        AccessControl.require(user, ALLOWED);
        ValidationResult v = validate(f);
        if (!v.isValid()) {
            return new MonitoringResult(v, List.of(), Map.of());
        }
        LocalDate today = LocalDate.now(clock);
        return tx.read(c -> {
            // Only active stages may be monitored; closed stages are ignored even if sent.
            List<Integer> activeIds = referenceDao.listWorkflowStages(c).stream()
                    .filter(s -> !WorkflowStages.isClosed(s.name()))
                    .map(LookupItem::id)
                    .filter(f.stageIds()::contains)
                    .toList();
            List<MonitoringRow> rows = orderDao.findForMonitoring(c, activeIds, f.employeeId(), f.text()).stream()
                    .map(r -> r.withDeadlineInfo(DeadlineFlag.daysRemaining(r.deadline(), today),
                            DeadlineFlag.classify(r.deadline(), today, f.warningDays())))
                    .filter(r -> f.deadlineFlag() == null || r.flag() == f.deadlineFlag())
                    .toList();
            Map<String, Long> counts = new LinkedHashMap<>();
            for (MonitoringRow r : rows) {
                counts.merge(r.stage(), 1L, Long::sum);
            }
            return new MonitoringResult(v, rows, counts);
        });
    }
}
