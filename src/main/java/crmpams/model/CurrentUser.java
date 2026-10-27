package crmpams.model;

/** The user who is logged in. Services use it for role checks and audit entries. */
public record CurrentUser(int userId, int employeeId, String username, String fullName, Role role) {
}
