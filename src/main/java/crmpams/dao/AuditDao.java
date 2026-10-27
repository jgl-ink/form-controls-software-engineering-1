package crmpams.dao;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

/** Status history (6.2) and the activity log (Security module). */
public interface AuditDao {

    void addStatusHistory(Connection c, int orderId, Integer stageId, int changedBy, LocalDateTime changedAt,
            String fieldChanged, String oldValue, String newValue) throws SQLException;

    void logActivity(Connection c, Integer userId, String action, String details, LocalDateTime loggedAt)
            throws SQLException;
}
