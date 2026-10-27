package crmpams.validation;

import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Every validation rule of the four forms in one place.
 * Each method returns null when the value is valid, or a message that tells the user how to fix it.
 * The forms call these rules before submitting, and the services call them again before saving.
 */
public final class FieldRules {

    // Lengths follow the column sizes in sql/schema.sql.
    public static final int CUSTOMER_NAME_MIN = 2;
    public static final int CUSTOMER_NAME_MAX = 100;
    public static final int PERSON_NAME_MIN = 2;
    public static final int PERSON_NAME_MAX = 50;
    public static final int CONTACT_NUMBER_MAX_CHARS = 13;
    public static final int EMAIL_MAX = 100;
    public static final int DESCRIPTION_MIN = 5;
    public static final int DESCRIPTION_MAX = 500;
    public static final int REQUIRED_OUTPUT_MAX = 150;
    public static final int INSTRUCTIONS_MAX = 500;
    public static final int FILE_PATH_MAX = 500;
    public static final int FILE_NAME_MAX = 255;
    public static final int USERNAME_MIN = 4;
    public static final int USERNAME_MAX = 30;
    public static final int PASSWORD_MIN = 8;
    public static final int PASSWORD_MAX = 64;
    public static final int SEARCH_TEXT_MAX = 100;
    public static final int MONITOR_TEXT_MAX = 60;
    public static final int WARNING_DAYS_MIN = 1;
    public static final int WARNING_DAYS_MAX = 14;

    /**
     * File types accepted as reference files. AutoCAD (dwg, dxf), PDF, and Photoshop (psd)
     * are the client file formats named in the interview; jpg, jpeg, and png cover scanned references.
     */
    public static final List<String> REFERENCE_FILE_EXTENSIONS =
            List.of("dwg", "dxf", "pdf", "psd", "jpg", "jpeg", "png");

    public static final List<String> PRODUCTION_POSITIONS = List.of("Designer", "Drafter", "Modeler");

    private static final Pattern CUSTOMER_NAME = Pattern.compile("[\\p{L}0-9 .,'&()\\-]+");
    private static final Pattern PERSON_NAME = Pattern.compile("[\\p{L} .'\\-]+");
    private static final Pattern HAS_LETTER = Pattern.compile(".*\\p{L}.*");
    private static final Pattern MOBILE_LOCAL = Pattern.compile("09\\d{9}");
    private static final Pattern MOBILE_INTL = Pattern.compile("\\+639\\d{9}");
    private static final Pattern LANDLINE_METRO_MANILA = Pattern.compile("02\\d{8}");
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+\\-]+@[A-Za-z0-9.\\-]+\\.[A-Za-z]{2,}");
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z][A-Za-z0-9._]*");

    private FieldRules() {
    }

    public static String clean(String value) {
        return value == null ? "" : value.strip();
    }

    public static String blankToNull(String value) {
        String v = clean(value);
        return v.isEmpty() ? null : v;
    }

    // ------------------------------------------------------------------ customer and names

    public static String customerName(String value) {
        String v = clean(value);
        if (v.isEmpty()) {
            return "Enter the customer name.";
        }
        if (v.length() < CUSTOMER_NAME_MIN || v.length() > CUSTOMER_NAME_MAX) {
            return "Customer name must be " + CUSTOMER_NAME_MIN + " to " + CUSTOMER_NAME_MAX + " characters long.";
        }
        if (!CUSTOMER_NAME.matcher(v).matches() || !HAS_LETTER.matcher(v).matches()) {
            return "Customer name may use letters, numbers, spaces, and . , ' & ( ) - only.";
        }
        return null;
    }

    public static String personName(String value, String label) {
        String v = clean(value);
        if (v.isEmpty()) {
            return "Enter the " + label.toLowerCase(Locale.ROOT) + ".";
        }
        if (v.length() < PERSON_NAME_MIN || v.length() > PERSON_NAME_MAX) {
            return label + " must be " + PERSON_NAME_MIN + " to " + PERSON_NAME_MAX + " characters long.";
        }
        if (!PERSON_NAME.matcher(v).matches() || !HAS_LETTER.matcher(v).matches()) {
            return label + " may use letters, spaces, periods, apostrophes, and hyphens only.";
        }
        return null;
    }

    // ------------------------------------------------------------------ contact details

    /**
     * Accepts a Philippine mobile number (09XXXXXXXXX or +639XXXXXXXXX)
     * or a Metro Manila landline (02 followed by 8 digits).
     */
    public static String contactNumber(String value, boolean required) {
        String v = clean(value);
        if (v.isEmpty()) {
            return required ? "Enter the contact number." : null;
        }
        if (normalizeContactNumber(v) == null) {
            return "Enter a mobile number like 09171234567 or +639171234567, or a landline like 0281234567.";
        }
        return null;
    }

    /** Returns the number in the stored form (09XXXXXXXXX or 02XXXXXXXX), or null when it is not valid. */
    public static String normalizeContactNumber(String value) {
        String v = clean(value);
        if (MOBILE_LOCAL.matcher(v).matches() || LANDLINE_METRO_MANILA.matcher(v).matches()) {
            return v;
        }
        if (MOBILE_INTL.matcher(v).matches()) {
            return "0" + v.substring(3);
        }
        return null;
    }

    /** Email is optional. When given it must look like name@example.com. */
    public static String email(String value) {
        String v = clean(value);
        if (v.isEmpty()) {
            return null;
        }
        if (v.length() > EMAIL_MAX) {
            return "Email must be " + EMAIL_MAX + " characters or fewer.";
        }
        if (!EMAIL.matcher(v).matches()) {
            return "Enter an email address like name@example.com.";
        }
        return null;
    }

    // ------------------------------------------------------------------ general text and selections

    public static String requiredText(String value, int min, int max, String label) {
        String v = clean(value);
        if (v.isEmpty()) {
            return "Enter the " + label.toLowerCase(Locale.ROOT) + ".";
        }
        if (v.length() < min) {
            return label + " must be at least " + min + " characters long.";
        }
        if (v.length() > max) {
            return label + " must be " + max + " characters or fewer.";
        }
        return null;
    }

    public static String optionalText(String value, int max, String label) {
        String v = clean(value);
        if (v.length() > max) {
            return label + " must be " + max + " characters or fewer.";
        }
        return null;
    }

    public static String requiredSelection(Object value, String message) {
        return value == null ? message : null;
    }

    // ------------------------------------------------------------------ dates

    public static String deadline(LocalDate deadline, LocalDate today) {
        if (deadline == null) {
            return "Choose the deadline date.";
        }
        if (deadline.isBefore(today)) {
            return "The deadline cannot be earlier than today (" + today + ").";
        }
        return null;
    }

    public static String dateFrom(LocalDate from, LocalDate today) {
        if (from == null) {
            return "Enter the start date as YYYY-MM-DD.";
        }
        if (from.isAfter(today)) {
            return "The start date cannot be later than today (" + today + ").";
        }
        return null;
    }

    public static String dateTo(LocalDate from, LocalDate to) {
        if (to == null) {
            return "Enter the end date as YYYY-MM-DD.";
        }
        if (from != null && to.isBefore(from)) {
            return "The end date cannot be earlier than the start date.";
        }
        return null;
    }

    // ------------------------------------------------------------------ reference file

    /** The reference file is optional. When given it must exist and have an accepted extension. */
    public static String referenceFile(String path) {
        String v = clean(path);
        if (v.isEmpty()) {
            return null;
        }
        if (v.length() > FILE_PATH_MAX) {
            return "The file location is too long. Move the file to a shorter folder path (up to "
                    + FILE_PATH_MAX + " characters).";
        }
        Path p;
        try {
            p = Path.of(v);
        } catch (InvalidPathException e) {
            return "The file location is not valid. Choose the file again with Browse Reference File.";
        }
        String name = p.getFileName() == null ? "" : p.getFileName().toString();
        if (name.length() > FILE_NAME_MAX) {
            return "The file name must be " + FILE_NAME_MAX + " characters or fewer.";
        }
        if (!REFERENCE_FILE_EXTENSIONS.contains(extensionOf(name))) {
            return "Choose a file of type " + String.join(", ", REFERENCE_FILE_EXTENSIONS) + ".";
        }
        if (!Files.isRegularFile(p)) {
            return "The file was not found. Check that the shared folder is reachable and choose the file again.";
        }
        return null;
    }

    public static String extensionOf(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? "" : fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    // ------------------------------------------------------------------ accounts

    public static String username(String value) {
        String v = clean(value);
        if (v.isEmpty()) {
            return "Enter a username.";
        }
        if (v.length() < USERNAME_MIN || v.length() > USERNAME_MAX) {
            return "Username must be " + USERNAME_MIN + " to " + USERNAME_MAX + " characters long.";
        }
        if (!USERNAME.matcher(v).matches()) {
            return "Username must start with a letter and use only letters, numbers, periods, and underscores.";
        }
        return null;
    }

    public static String password(char[] password, String username) {
        if (password == null || password.length == 0) {
            return "Enter a password.";
        }
        if (password.length < PASSWORD_MIN || password.length > PASSWORD_MAX) {
            return "Password must be " + PASSWORD_MIN + " to " + PASSWORD_MAX + " characters long.";
        }
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password) {
            if (Character.isLetter(c)) {
                hasLetter = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            }
        }
        if (!hasLetter || !hasDigit) {
            return "Password must contain at least one letter and one number.";
        }
        String u = clean(username);
        if (!u.isEmpty() && new String(password).equalsIgnoreCase(u)) {
            return "Password must not be the same as the username.";
        }
        return null;
    }

    public static String passwordConfirmation(char[] password, char[] confirmation) {
        if (confirmation == null || confirmation.length == 0) {
            return "Type the password again to confirm it.";
        }
        if (password == null || !java.util.Arrays.equals(password, confirmation)) {
            return "The two passwords do not match.";
        }
        return null;
    }

    // ------------------------------------------------------------------ search and monitoring

    public static String searchText(String value) {
        return optionalText(value, SEARCH_TEXT_MAX, "Search text");
    }

    public static String monitorText(String value) {
        return optionalText(value, MONITOR_TEXT_MAX, "Order ID or employee name");
    }

    public static String warningDays(int days) {
        if (days < WARNING_DAYS_MIN || days > WARNING_DAYS_MAX) {
            return "Warning period must be " + WARNING_DAYS_MIN + " to " + WARNING_DAYS_MAX + " days.";
        }
        return null;
    }
}
