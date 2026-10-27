package crmpams.service;

import crmpams.dao.AccountDao;
import crmpams.dao.AuditDao;
import crmpams.db.TransactionRunner;
import crmpams.model.CurrentUser;
import crmpams.model.EmployeeRequest;
import crmpams.model.Role;
import crmpams.security.AccessControl;
import crmpams.security.PasswordHasher;
import crmpams.validation.FieldRules;
import crmpams.validation.Fields;
import crmpams.validation.ValidationResult;
import java.sql.SQLException;
import java.time.Clock;
import java.time.LocalDateTime;

/** Registration (11.0), Register New Employee (11.2). Only the Operations Manager may register accounts. */
public final class RegistrationService {

    private final TransactionRunner tx;
    private final AccountDao accountDao;
    private final AuditDao auditDao;
    private final PasswordHasher hasher;
    private final Clock clock;

    public RegistrationService(TransactionRunner tx, AccountDao accountDao, AuditDao auditDao,
            PasswordHasher hasher, Clock clock) {
        this.tx = tx;
        this.accountDao = accountDao;
        this.auditDao = auditDao;
        this.hasher = hasher;
        this.clock = clock;
    }

    /** Rules shared by the form and this service. */
    public static ValidationResult validate(EmployeeRequest r) {
        ValidationResult v = new ValidationResult()
                .check(Fields.FIRST_NAME, FieldRules.personName(r.firstName(), "First name"))
                .check(Fields.LAST_NAME, FieldRules.personName(r.lastName(), "Last name"))
                .check(Fields.ROLE, FieldRules.requiredSelection(r.role(), "Choose the role of this employee."));
        if (r.role() == Role.PRODUCTION_STAFF) {
            String p = r.position();
            v.check(Fields.POSITION, p == null || !FieldRules.PRODUCTION_POSITIONS.contains(p)
                    ? "Choose Designer, Drafter, or Modeler." : null);
        }
        return v
                .check(Fields.CONTACT_NUMBER, FieldRules.contactNumber(r.contactNumber(), false))
                .check(Fields.EMAIL, FieldRules.email(r.email()))
                .check(Fields.USERNAME, FieldRules.username(r.username()))
                .check(Fields.PASSWORD, FieldRules.password(r.password(), r.username()))
                .check(Fields.CONFIRM_PASSWORD, FieldRules.passwordConfirmation(r.password(), r.confirmPassword()));
    }

    /** The position stored for the employee. Managers and officers take the role name. */
    public static String positionFor(EmployeeRequest r) {
        return r.role() == Role.PRODUCTION_STAFF ? r.position() : r.role().label();
    }

    public SaveResult register(CurrentUser user, EmployeeRequest r) throws SQLException {
        AccessControl.require(user, Role.OPERATIONS_MANAGER);
        ValidationResult v = validate(r);
        if (!v.isValid()) {
            return SaveResult.invalid(v);
        }
        String username = FieldRules.clean(r.username());
        if (tx.read(c -> accountDao.usernameExists(c, username))) {
            return SaveResult.invalid(ValidationResult.of(Fields.USERNAME,
                    "This username is already taken. Choose another one."));
        }
        // Hash outside the transaction: 600,000 PBKDF2 rounds take a moment.
        String hash = hasher.hash(r.password());
        LocalDateTime now = LocalDateTime.now(clock);

        return tx.inTransaction(c -> {
            // Checked again inside the transaction in case another workstation just used the name.
            if (accountDao.usernameExists(c, username)) {
                return SaveResult.invalid(ValidationResult.of(Fields.USERNAME,
                        "This username is already taken. Choose another one."));
            }
            int employeeId = accountDao.insertEmployee(c,
                    FieldRules.clean(r.firstName()),
                    FieldRules.clean(r.lastName()),
                    positionFor(r),
                    FieldRules.normalizeContactNumber(r.contactNumber()),
                    FieldRules.blankToNull(r.email()));
            int userId = accountDao.insertUser(c, employeeId, username, hash, r.role(), r.active(), user.userId());
            auditDao.logActivity(c, user.userId(), "ACCOUNT_REGISTERED",
                    "Registered " + username + " as " + r.role().label() + (r.active() ? "" : " (inactive)"), now);
            return SaveResult.saved(userId);
        });
    }
}
