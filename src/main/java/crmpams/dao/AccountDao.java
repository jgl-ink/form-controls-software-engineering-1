package crmpams.dao;

import crmpams.model.Role;
import crmpams.model.UserAccount;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

/** Employees and their user accounts (Registration 11.0, Login 1.1). */
public interface AccountDao {

    Optional<UserAccount> findByUsername(Connection c, String username) throws SQLException;

    /** Username comparison ignores letter case. */
    boolean usernameExists(Connection c, String username) throws SQLException;

    int insertEmployee(Connection c, String firstName, String lastName, String position,
            String contactNumber, String email) throws SQLException;

    int insertUser(Connection c, int employeeId, String username, String passwordHash, Role role,
            boolean active, int createdBy) throws SQLException;
}
