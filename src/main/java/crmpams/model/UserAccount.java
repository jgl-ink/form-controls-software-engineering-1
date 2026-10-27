package crmpams.model;

/** A users row joined with the employee's name, used at login. */
public record UserAccount(
        int userId,
        int employeeId,
        String username,
        String passwordHash,
        Role role,
        boolean active,
        String fullName) {
}
