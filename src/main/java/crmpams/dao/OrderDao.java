package crmpams.dao;

import crmpams.model.MonitoringRow;
import crmpams.model.NewOrderRecord;
import crmpams.model.OrderSearchCriteria;
import crmpams.model.OrderSummary;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/** Orders, their reference files, searching, and monitoring queries. */
public interface OrderDao {

    /**
     * Finds an open order (stage not in closedStages) of the same customer with the same
     * project description, ignoring letter case and outer spaces. Returns its order id.
     */
    Optional<Integer> findOpenDuplicate(Connection c, int customerId, String projectDescription,
            Collection<String> closedStages) throws SQLException;

    /** Inserts the order and returns the generated order id (Order Register, 6.1). */
    int insert(Connection c, NewOrderRecord order) throws SQLException;

    void insertReferenceFile(Connection c, int orderId, String fileName, String fileLocation) throws SQLException;

    /** One parameterized query built from the selected criteria (IPO 12.2). */
    List<OrderSummary> search(Connection c, OrderSearchCriteria criteria, Collection<String> closedStages,
            int limit) throws SQLException;

    /**
     * Active orders for the monitoring view. text is an order ID (exact) or part of an employee name.
     * daysLeft and flag are left empty; the service fills them in.
     */
    List<MonitoringRow> findForMonitoring(Connection c, Collection<Integer> stageIds, Integer employeeId,
            String text) throws SQLException;
}
