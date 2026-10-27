package crmpams.db;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * JDBC connection to the MySQL 8.0 database on the server workstation.
 * Settings come from db.properties in the folder the application starts from
 * (copy db.properties.example and fill in the password).
 */
public final class Database implements TransactionRunner {

    private final String url;
    private final String user;
    private final String password;

    public Database(String url, String user, String password) {
        this.url = url;
        this.user = user;
        this.password = password;
    }

    public static Database fromFile(Path file) throws IOException {
        if (!Files.exists(file)) {
            throw new IOException("Missing " + file.toAbsolutePath()
                    + ". Copy db.properties.example to db.properties and set the database password.");
        }
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(file)) {
            p.load(in);
        }
        return new Database(
                p.getProperty("db.url", "jdbc:mysql://localhost:3306/crm_pams"),
                p.getProperty("db.user", "crm_app"),
                p.getProperty("db.password", ""));
    }

    public Connection open() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    @Override
    public <T> T read(SqlWork<T> work) throws SQLException {
        try (Connection c = open()) {
            c.setReadOnly(true);
            return work.run(c);
        }
    }

    @Override
    public <T> T inTransaction(SqlWork<T> work) throws SQLException {
        try (Connection c = open()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }
}
