package crmpams.dao.jdbc;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

/** Small helpers shared by the JDBC data access objects. */
final class JdbcSupport {

    private JdbcSupport() {
    }

    static int generatedKey(PreparedStatement ps) throws SQLException {
        try (ResultSet keys = ps.getGeneratedKeys()) {
            if (!keys.next()) {
                throw new SQLException("The database did not return a generated key.");
            }
            return keys.getInt(1);
        }
    }

    static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    static Timestamp ts(LocalDateTime time) {
        return time == null ? null : Timestamp.valueOf(time);
    }

    static LocalDateTime time(Timestamp ts) {
        return ts == null ? null : ts.toLocalDateTime();
    }

    /** "?, ?, ?" for an IN list of the given size. */
    static String placeholders(int count) {
        return String.join(", ", Collections.nCopies(count, "?"));
    }

    static boolean isEmpty(Collection<?> values) {
        return values == null || values.isEmpty();
    }
}
