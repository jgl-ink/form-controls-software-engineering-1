package crmpams.dao.jdbc;

import crmpams.dao.AccountDao;
import crmpams.model.Role;
import crmpams.model.UserAccount;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public final class JdbcAccountDao implements AccountDao {

    @Override
    public Optional<UserAccount> findByUsername(Connection c, String username) throws SQLException {
        String sql = "SELECT u.user_id, u.employee_id, u.username, u.password_hash, u.role, u.is_active,"
                + " CONCAT(e.first_name, ' ', e.last_name) AS full_name"
                + " FROM users u JOIN employees e ON e.employee_id = u.employee_id WHERE u.username = ?";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                return Optional.of(new UserAccount(
                        rs.getInt("user_id"),
                        rs.getInt("employee_id"),
                        rs.getString("username"),
                        rs.getString("password_hash"),
                        Role.valueOf(rs.getString("role")),
                        rs.getBoolean("is_active"),
                        rs.getString("full_name")));
            }
        }
    }

    @Override
    public boolean usernameExists(Connection c, String username) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM users WHERE username = ?")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public int insertEmployee(Connection c, String firstName, String lastName, String position,
            String contactNumber, String email) throws SQLException {
        String sql = "INSERT INTO employees (first_name, last_name, position, contact_number, email)"
                + " VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, position);
            ps.setString(4, contactNumber);
            ps.setString(5, email);
            ps.executeUpdate();
            return JdbcSupport.generatedKey(ps);
        }
    }

    @Override
    public int insertUser(Connection c, int employeeId, String username, String passwordHash, Role role,
            boolean active, int createdBy) throws SQLException {
        String sql = "INSERT INTO users (employee_id, username, password_hash, role, is_active, created_by)"
                + " VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, employeeId);
            ps.setString(2, username);
            ps.setString(3, passwordHash);
            ps.setString(4, role.name());
            ps.setBoolean(5, active);
            ps.setInt(6, createdBy);
            ps.executeUpdate();
            return JdbcSupport.generatedKey(ps);
        }
    }
}
