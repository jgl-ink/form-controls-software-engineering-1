package crmpams.db;

import java.sql.SQLException;

/**
 * Runs database work. Services depend on this interface so unit tests can supply
 * an in-memory version without a MySQL server.
 */
public interface TransactionRunner {

    /** Runs read-only work. */
    <T> T read(SqlWork<T> work) throws SQLException;

    /** Runs work in one transaction. Everything is committed together or rolled back together. */
    <T> T inTransaction(SqlWork<T> work) throws SQLException;
}
