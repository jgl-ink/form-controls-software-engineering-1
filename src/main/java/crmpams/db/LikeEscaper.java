package crmpams.db;

/**
 * Makes user text safe for a LIKE pattern. The percent sign and underscore typed by the user
 * are matched literally instead of acting as wildcards. Use with: LIKE ? ESCAPE '!'
 */
public final class LikeEscaper {

    public static final char ESCAPE_CHAR = '!';

    private LikeEscaper() {
    }

    public static String escape(String text) {
        StringBuilder sb = new StringBuilder(text.length() + 8);
        for (char c : text.toCharArray()) {
            if (c == ESCAPE_CHAR || c == '%' || c == '_') {
                sb.append(ESCAPE_CHAR);
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /** Pattern for a partial match: %text% with wildcards in the text escaped. */
    public static String contains(String text) {
        return "%" + escape(text) + "%";
    }
}
