package crmpams.dao.jdbc;

import crmpams.dao.CustomerDao;
import crmpams.model.Customer;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public final class JdbcCustomerDao implements CustomerDao {

    private static final String SELECT = "SELECT customer_id, customer_name, contact_number, email FROM customers ";

    @Override
    public Optional<Customer> findById(Connection c, int customerId) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(SELECT + "WHERE customer_id = ?")) {
            ps.setInt(1, customerId);
            return one(ps);
        }
    }

    @Override
    public Optional<Customer> findByNameAndContact(Connection c, String name, String contactNumber)
            throws SQLException {
        // The database collation (utf8mb4_0900_as_ci) makes the name comparison ignore letter case.
        try (PreparedStatement ps = c.prepareStatement(SELECT + "WHERE customer_name = ? AND contact_number = ?")) {
            ps.setString(1, name);
            ps.setString(2, contactNumber);
            return one(ps);
        }
    }

    @Override
    public int insert(Connection c, Customer customer) throws SQLException {
        String sql = "INSERT INTO customers (customer_name, contact_number, email) VALUES (?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, customer.name());
            ps.setString(2, customer.contactNumber());
            ps.setString(3, customer.email());
            ps.executeUpdate();
            return JdbcSupport.generatedKey(ps);
        }
    }

    private static Optional<Customer> one(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) {
                return Optional.empty();
            }
            return Optional.of(new Customer(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getString(4)));
        }
    }
}
