package crmpams.dao.jdbc;

import crmpams.dao.OrderDao;
import crmpams.db.LikeEscaper;
import crmpams.model.MonitoringRow;
import crmpams.model.NewOrderRecord;
import crmpams.model.OrderLine;
import crmpams.model.OrderSearchCriteria;
import crmpams.model.OrderSummary;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public final class JdbcOrderDao implements OrderDao {

    private static final String ESCAPE = " ESCAPE '" + LikeEscaper.ESCAPE_CHAR + "'";

    /** Joins every table needed to show an order with its current assignment. */
    private static final String ORDER_JOINS =
            " FROM orders o"
            + " JOIN customers c ON c.customer_id = o.customer_id"
            + " JOIN urgency_levels ul ON ul.urgency_level_id = o.urgency_level_id"
            + " JOIN workflow_stages ws ON ws.stage_id = o.stage_id"
            + " LEFT JOIN assignments a ON a.order_id = o.order_id AND a.is_current = 1"
            + " LEFT JOIN employees e ON e.employee_id = a.employee_id";

    @Override
    public Optional<Integer> findOpenDuplicate(Connection c, int customerId, String projectDescription,
            Collection<String> closedStages) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT o.order_id FROM orders o JOIN workflow_stages ws ON ws.stage_id = o.stage_id"
                + " WHERE o.customer_id = ? AND LOWER(TRIM(o.project_description)) = LOWER(TRIM(?))");
        if (!JdbcSupport.isEmpty(closedStages)) {
            sql.append(" AND ws.stage_name NOT IN (").append(JdbcSupport.placeholders(closedStages.size())).append(")");
        }
        sql.append(" ORDER BY o.order_id DESC LIMIT 1");
        try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
            int i = 1;
            ps.setInt(i++, customerId);
            ps.setString(i++, projectDescription);
            if (closedStages != null) {
                for (String s : closedStages) {
                    ps.setString(i++, s);
                }
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getInt(1)) : Optional.empty();
            }
        }
    }

    @Override
    public int insert(Connection c, NewOrderRecord o) throws SQLException {
        String sql = "INSERT INTO orders (customer_id, project_description, deadline, urgency_level_id,"
                + " delivery_address, special_instructions, stage_id, date_received, encoded_by)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, o.customerId());
            ps.setString(2, o.projectDescription());
            ps.setDate(3, Date.valueOf(o.deadline()));
            ps.setInt(4, o.urgencyLevelId());
            ps.setString(5, o.deliveryAddress());
            ps.setString(6, o.specialInstructions());
            ps.setInt(7, o.stageId());
            ps.setTimestamp(8, JdbcSupport.ts(o.dateReceived()));
            ps.setInt(9, o.encodedBy());
            ps.executeUpdate();
            return JdbcSupport.generatedKey(ps);
        }
    }

    @Override
    public void insertReferenceFile(Connection c, int orderId, String fileName, String fileLocation)
            throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO reference_files (order_id, file_name, file_location) VALUES (?, ?, ?)")) {
            ps.setInt(1, orderId);
            ps.setString(2, fileName);
            ps.setString(3, fileLocation);
            ps.executeUpdate();
        }
    }

    @Override
    public void insertLine(Connection c, int orderId, int lineNo, OrderLine line) throws SQLException {
        String sql = "INSERT INTO order_items (order_id, line_no, project_type_id, media, sheet_size, sheet_count,"
                + " set_count) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ps.setInt(2, lineNo);
            ps.setInt(3, line.projectTypeId());
            ps.setString(4, line.media());
            ps.setString(5, line.size());
            ps.setInt(6, line.sheets());
            ps.setInt(7, line.sets());
            ps.executeUpdate();
        }
    }

    @Override
    public List<OrderLine> findLines(Connection c, int orderId) throws SQLException {
        String sql = "SELECT oi.project_type_id, pt.type_name, oi.media, oi.sheet_size, oi.sheet_count, oi.set_count"
                + " FROM order_items oi JOIN project_types pt ON pt.project_type_id = oi.project_type_id"
                + " WHERE oi.order_id = ? ORDER BY oi.line_no";
        List<OrderLine> lines = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lines.add(new OrderLine(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4),
                            rs.getInt(5), rs.getInt(6)));
                }
            }
        }
        return lines;
    }

    @Override
    public List<OrderSummary> search(Connection c, OrderSearchCriteria k, Collection<String> closedStages,
            int limit) throws SQLException {
        StringBuilder sql = new StringBuilder(
                "SELECT o.order_id, c.customer_name, c.contact_number,"
                + " (SELECT GROUP_CONCAT(DISTINCT pt.type_name ORDER BY pt.type_name SEPARATOR ', ')"
                + " FROM order_items oi JOIN project_types pt ON pt.project_type_id = oi.project_type_id"
                + " WHERE oi.order_id = o.order_id) AS project_types,"
                + " o.project_description, o.deadline, ul.level_name, ws.stage_name,"
                + " CONCAT(e.first_name, ' ', e.last_name) AS assigned_to, o.date_received,"
                + " o.delivery_address, o.special_instructions")
                .append(ORDER_JOINS)
                .append(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        String text = k.text() == null ? "" : k.text().strip();
        if (!text.isEmpty()) {
            String pattern = LikeEscaper.contains(text);
            sql.append(" AND (CAST(o.order_id AS CHAR) LIKE ?").append(ESCAPE)
                    .append(" OR c.customer_name LIKE ?").append(ESCAPE)
                    .append(" OR o.project_description LIKE ?").append(ESCAPE).append(")");
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }
        if (k.projectTypeId() != null) {
            sql.append(" AND EXISTS (SELECT 1 FROM order_items oi2 WHERE oi2.order_id = o.order_id"
                    + " AND oi2.project_type_id = ?)");
            params.add(k.projectTypeId());
        }
        if (k.stageId() != null) {
            sql.append(" AND o.stage_id = ?");
            params.add(k.stageId());
        }
        if (k.employeeId() != null) {
            sql.append(" AND a.employee_id = ?");
            params.add(k.employeeId());
        }
        if (k.urgencyLevelId() != null) {
            sql.append(" AND o.urgency_level_id = ?");
            params.add(k.urgencyLevelId());
        }
        if (k.useDateRange()) {
            // Whole days: from the start of receivedFrom to before the day after receivedTo.
            sql.append(" AND o.date_received >= ? AND o.date_received < ?");
            params.add(Timestamp.valueOf(k.receivedFrom().atStartOfDay()));
            params.add(Timestamp.valueOf(k.receivedTo().plusDays(1).atStartOfDay()));
        }
        if (!k.includeClosed() && !JdbcSupport.isEmpty(closedStages)) {
            sql.append(" AND ws.stage_name NOT IN (").append(JdbcSupport.placeholders(closedStages.size())).append(")");
            params.addAll(closedStages);
        }
        sql.append(" ORDER BY o.date_received DESC, o.order_id DESC LIMIT ?");
        params.add(limit);

        List<OrderSummary> rows = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new OrderSummary(
                            rs.getInt("order_id"),
                            rs.getString("customer_name"),
                            rs.getString("contact_number"),
                            rs.getString("project_types"),
                            rs.getString("project_description"),
                            rs.getDate("deadline").toLocalDate(),
                            rs.getString("level_name"),
                            rs.getString("stage_name"),
                            rs.getString("assigned_to"),
                            JdbcSupport.time(rs.getTimestamp("date_received")),
                            rs.getString("delivery_address"),
                            rs.getString("special_instructions")));
                }
            }
        }
        return rows;
    }

    @Override
    public List<MonitoringRow> findForMonitoring(Connection c, Collection<Integer> stageIds, Integer employeeId,
            String text) throws SQLException {
        if (JdbcSupport.isEmpty(stageIds)) {
            return List.of();
        }
        StringBuilder sql = new StringBuilder(
                "SELECT o.order_id, c.customer_name, ws.stage_name,"
                + " CONCAT(e.first_name, ' ', e.last_name) AS assigned_to, o.deadline,"
                + " COALESCE((SELECT MAX(sh.changed_at) FROM status_history sh WHERE sh.order_id = o.order_id),"
                + " o.date_received) AS last_update")
                .append(ORDER_JOINS)
                .append(" WHERE o.stage_id IN (").append(JdbcSupport.placeholders(stageIds.size())).append(")");
        List<Object> params = new ArrayList<>(stageIds);

        if (employeeId != null) {
            sql.append(" AND a.employee_id = ?");
            params.add(employeeId);
        }
        String t = text == null ? "" : text.strip();
        if (!t.isEmpty()) {
            if (t.chars().allMatch(Character::isDigit)) {
                sql.append(" AND o.order_id = ?");
                params.add(Integer.parseInt(t.length() > 9 ? t.substring(0, 9) : t));
            } else {
                sql.append(" AND CONCAT(e.first_name, ' ', e.last_name) LIKE ?").append(ESCAPE);
                params.add(LikeEscaper.contains(t));
            }
        }
        sql.append(" ORDER BY o.deadline, o.order_id");

        List<MonitoringRow> rows = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
            bind(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new MonitoringRow(
                            rs.getInt("order_id"),
                            rs.getString("customer_name"),
                            rs.getString("stage_name"),
                            rs.getString("assigned_to"),
                            rs.getDate("deadline").toLocalDate(),
                            JdbcSupport.time(rs.getTimestamp("last_update")),
                            0,
                            null));
                }
            }
        }
        return rows;
    }

    private static void bind(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            Object p = params.get(i);
            if (p instanceof Integer n) {
                ps.setInt(i + 1, n);
            } else if (p instanceof Timestamp t) {
                ps.setTimestamp(i + 1, t);
            } else {
                ps.setString(i + 1, (String) p);
            }
        }
    }
}
