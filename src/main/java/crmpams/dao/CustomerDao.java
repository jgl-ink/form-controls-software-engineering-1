package crmpams.dao;

import crmpams.model.Customer;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;

/** Customer lookup and registration (IPO 5.1 and 5.2). */
public interface CustomerDao {

    Optional<Customer> findById(Connection c, int customerId) throws SQLException;

    /** Name comparison ignores letter case; the contact number must be in stored form. */
    Optional<Customer> findByNameAndContact(Connection c, String name, String contactNumber) throws SQLException;

    int insert(Connection c, Customer customer) throws SQLException;
}
