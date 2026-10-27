package crmpams.service;

import crmpams.dao.AccountDao;
import crmpams.dao.AuditDao;
import crmpams.db.TransactionRunner;
import crmpams.model.CurrentUser;
import crmpams.model.UserAccount;
import crmpams.security.PasswordHasher;
import crmpams.validation.FieldRules;
import crmpams.validation.Fields;
import crmpams.validation.ValidationResult;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

/** User Login (1.1) and User Logout (1.3). Every login and logout is written to the activity log. */
public final class AuthService {

    private final TransactionRunner tx;
    private final AccountDao accountDao;
    private final AuditDao auditDao;
    private final PasswordHasher hasher;
    private final Clock clock;
    private volatile String dummyHash;

    public AuthService(TransactionRunner tx, AccountDao accountDao, AuditDao auditDao, PasswordHasher hasher,
            Clock clock) {
        this.tx = tx;
        this.accountDao = accountDao;
        this.auditDao = auditDao;
        this.hasher = hasher;
        this.clock = clock;
    }

    public static ValidationResult validate(String username, char[] password) {
        return new ValidationResult()
                .check(Fields.LOGIN_USERNAME, FieldRules.clean(username).isEmpty() ? "Enter your username." : null)
                .check(Fields.LOGIN_PASSWORD, password == null || password.length == 0
                        ? "Enter your password." : null);
    }

    public LoginResult login(String username, char[] password) throws SQLException {
        ValidationResult v = validate(username, password);
        if (!v.isValid()) {
            return new LoginResult(null, v);
        }
        String name = FieldRules.clean(username);
        Optional<UserAccount> found = tx.read(c -> accountDao.findByUsername(c, name));
        LocalDateTime now = LocalDateTime.now(clock);

        // Hash even when the username does not exist, so the response time does not reveal valid usernames.
        boolean passwordOk = found.isPresent()
                ? hasher.verify(password, found.get().passwordHash())
                : hasher.verify(password, dummyHash());

        if (found.isEmpty() || !passwordOk) {
            Integer userId = found.map(UserAccount::userId).orElse(null);
            tx.inTransaction(c -> {
                auditDao.logActivity(c, userId, "LOGIN_FAILED", "Failed login for username " + name, now);
                return null;
            });
            return new LoginResult(null, ValidationResult.of(ValidationResult.GENERAL,
                    "Incorrect username or password."));
        }
        UserAccount a = found.get();
        if (!a.active()) {
            tx.inTransaction(c -> {
                auditDao.logActivity(c, a.userId(), "LOGIN_DENIED_INACTIVE", "Deactivated account", now);
                return null;
            });
            return new LoginResult(null, ValidationResult.of(ValidationResult.GENERAL,
                    "This account is deactivated. Ask the Operations Manager to reactivate it."));
        }
        tx.inTransaction(c -> {
            auditDao.logActivity(c, a.userId(), "LOGIN", "Logged in", now);
            return null;
        });
        return new LoginResult(new CurrentUser(a.userId(), a.employeeId(), a.username(), a.fullName(), a.role()),
                new ValidationResult());
    }

    public void logout(CurrentUser user) throws SQLException {
        LocalDateTime now = LocalDateTime.now(clock);
        tx.inTransaction(c -> {
            auditDao.logActivity(c, user.userId(), "LOGOUT", "Logged out", now);
            return null;
        });
    }

    private String dummyHash() {
        if (dummyHash == null) {
            dummyHash = hasher.hash("not-a-real-password-1".toCharArray());
        }
        return dummyHash;
    }
}
