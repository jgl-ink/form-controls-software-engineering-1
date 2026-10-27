package crmpams.model;

import java.util.List;
import java.util.Set;

/** Names of the workflow stages listed in Chapter 1 (Production Status Monitoring). */
public final class WorkflowStages {

    public static final String NEW = "New";
    public static final String ASSIGNED = "Assigned";
    public static final String IN_PROGRESS = "In Progress";
    public static final String FOR_REVIEW = "For Review";
    public static final String FOR_CUSTOMER_CONFIRMATION = "For Customer Confirmation";
    public static final String RELEASED = "Released";
    public static final String COMPLETED = "Completed";

    public static final List<String> ALL = List.of(
            NEW, ASSIGNED, IN_PROGRESS, FOR_REVIEW, FOR_CUSTOMER_CONFIRMATION, RELEASED, COMPLETED);

    /** Stages after which an order is no longer active. */
    public static final Set<String> CLOSED = Set.of(RELEASED, COMPLETED);

    private WorkflowStages() {
    }

    public static boolean isClosed(String stageName) {
        return stageName != null && CLOSED.contains(stageName);
    }
}
