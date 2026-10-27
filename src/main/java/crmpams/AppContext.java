package crmpams;

import crmpams.dao.AccountDao;
import crmpams.dao.AuditDao;
import crmpams.dao.CustomerDao;
import crmpams.dao.OrderDao;
import crmpams.dao.ReferenceDataDao;
import crmpams.dao.jdbc.JdbcAccountDao;
import crmpams.dao.jdbc.JdbcAuditDao;
import crmpams.dao.jdbc.JdbcCustomerDao;
import crmpams.dao.jdbc.JdbcOrderDao;
import crmpams.dao.jdbc.JdbcReferenceDataDao;
import crmpams.db.TransactionRunner;
import crmpams.security.PasswordHasher;
import crmpams.service.AuthService;
import crmpams.service.MonitoringService;
import crmpams.service.OrderIntakeService;
import crmpams.service.RegistrationService;
import crmpams.service.SearchService;
import java.time.Clock;

/** Creates the services once and hands them to the windows that need them. */
public record AppContext(
        AuthService auth,
        OrderIntakeService orderIntake,
        RegistrationService registration,
        SearchService search,
        MonitoringService monitoring) {

    public static AppContext create(TransactionRunner tx, Clock clock) {
        return create(tx, new JdbcReferenceDataDao(), new JdbcCustomerDao(), new JdbcOrderDao(),
                new JdbcAccountDao(), new JdbcAuditDao(), new PasswordHasher(), clock);
    }

    public static AppContext create(TransactionRunner tx, ReferenceDataDao ref, CustomerDao customers,
            OrderDao orders, AccountDao accounts, AuditDao audit, PasswordHasher hasher, Clock clock) {
        return new AppContext(
                new AuthService(tx, accounts, audit, hasher, clock),
                new OrderIntakeService(tx, ref, customers, orders, audit, clock),
                new RegistrationService(tx, accounts, audit, hasher, clock),
                new SearchService(tx, ref, orders, clock),
                new MonitoringService(tx, ref, orders, clock));
    }
}
