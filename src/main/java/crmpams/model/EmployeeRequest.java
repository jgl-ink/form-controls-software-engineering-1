package crmpams.model;

/**
 * Everything the Register New Employee form sends to the service (IPO 11.2).
 * Passwords are char arrays so the form can clear them after use.
 */
public record EmployeeRequest(
        String firstName,
        String lastName,
        Role role,
        String position,
        String contactNumber,
        String email,
        String username,
        char[] password,
        char[] confirmPassword,
        boolean active) {
}
