package crmpams.db;

import java.sql.Connection;
import java.sql.SQLException;

/** A unit of database work that runs on one connection. */
@FunctionalInterface
public interface SqlWork<T> {
    T run(Connection connection) throws SQLException;
}
