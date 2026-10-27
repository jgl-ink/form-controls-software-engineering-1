package crmpams.dao.jdbc;

import crmpams.dao.AuditDao;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;

public final class JdbcAuditDao implements AuditDao {

    private static final int VALUE_MAX = 500;
    private static final int DETAILS_MAX = 255;

    @Override
    public void addStatusHistory(Connection c, int orderId, Integer stageId, int changedBy,
            LocalDateTime changedAt, String fieldChanged, String oldValue, String newValue) throws SQLException {
        String sql = "INSERT INTO status_history (order_id, stage_id, changed_by, changed_at, field_changed,"
                + " old_value, new_value) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            JdbcSupport.setNullableInt(ps, 2, stageId);
            ps.setInt(3, changedBy);
            ps.setTimestamp(4, JdbcSupport.ts(changedAt));
            ps.setString(5, fieldChanged);
            ps.setString(6, cut(oldValue, VALUE_MAX));
            ps.setString(7, cut(newValue, VALUE_MAX));
            ps.executeUpdate();
        }
    }

    @Override
    public void logActivity(Connection c, Integer userId, String action, String details, LocalDateTime loggedAt)
            throws SQLException {
        String sql = "INSERT INTO activity_log (user_id, action, details, logged_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            JdbcSupport.setNullableInt(ps, 1, userId);
            ps.setString(2, action);
            ps.setString(3, cut(details, DETAILS_MAX));
            ps.setTimestamp(4, JdbcSupport.ts(loggedAt));
            ps.executeUpdate();
        }
    }

    private static String cut(String s, int max) {
        return s == null || s.length() <= max ? s : s.substring(0, max);
    }
}
