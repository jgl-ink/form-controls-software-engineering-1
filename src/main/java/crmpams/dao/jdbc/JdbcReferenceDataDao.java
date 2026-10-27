package crmpams.dao.jdbc;

import crmpams.dao.ReferenceDataDao;
import crmpams.model.LookupItem;
import crmpams.model.UrgencyLevel;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcReferenceDataDao implements ReferenceDataDao {

    @Override
    public List<LookupItem> listProjectTypes(Connection c) throws SQLException {
        return lookup(c, "SELECT project_type_id, type_name FROM project_types WHERE is_active = 1 ORDER BY type_name");
    }

    @Override
    public List<UrgencyLevel> listUrgencyLevels(Connection c) throws SQLException {
        String sql = "SELECT urgency_level_id, level_name, urgency_rank FROM urgency_levels "
                + "WHERE is_active = 1 ORDER BY urgency_rank";
        List<UrgencyLevel> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new UrgencyLevel(rs.getInt(1), rs.getString(2), rs.getInt(3)));
            }
        }
        return list;
    }

    @Override
    public List<LookupItem> listWorkflowStages(Connection c) throws SQLException {
        return lookup(c, "SELECT stage_id, stage_name FROM workflow_stages WHERE is_active = 1 ORDER BY stage_order");
    }

    @Override
    public List<LookupItem> listProductionStaff(Connection c) throws SQLException {
        return lookup(c, "SELECT e.employee_id, CONCAT(e.first_name, ' ', e.last_name, ' (', e.position, ')') "
                + "FROM employees e JOIN users u ON u.employee_id = e.employee_id "
                + "WHERE u.role = 'PRODUCTION_STAFF' AND u.is_active = 1 "
                + "ORDER BY e.first_name, e.last_name");
    }

    @Override
    public Optional<Integer> findStageId(Connection c, String stageName) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT stage_id FROM workflow_stages WHERE stage_name = ?")) {
            ps.setString(1, stageName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getInt(1)) : Optional.empty();
            }
        }
    }

    @Override
    public boolean isActiveProjectType(Connection c, int projectTypeId) throws SQLException {
        return exists(c, "SELECT 1 FROM project_types WHERE project_type_id = ? AND is_active = 1", projectTypeId);
    }

    @Override
    public boolean isActiveUrgencyLevel(Connection c, int urgencyLevelId) throws SQLException {
        return exists(c, "SELECT 1 FROM urgency_levels WHERE urgency_level_id = ? AND is_active = 1", urgencyLevelId);
    }

    private static List<LookupItem> lookup(Connection c, String sql) throws SQLException {
        List<LookupItem> list = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(new LookupItem(rs.getInt(1), rs.getString(2)));
            }
        }
        return list;
    }

    private static boolean exists(Connection c, String sql, int id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
