package crmpams.validation;

/** Field keys shared by the forms and the services, so an error lands under the right input. */
public final class Fields {

    // New Order form (Customer Order Intake 5.1 to 5.4)
    public static final String CUSTOMER_NAME = "customerName";
    public static final String CONTACT_NUMBER = "contactNumber";
    public static final String EMAIL = "email";
    public static final String PROJECT_TYPE = "projectType";
    public static final String PROJECT_DESCRIPTION = "projectDescription";
    public static final String REQUIRED_OUTPUT = "requiredOutput";
    public static final String DEADLINE = "deadline";
    public static final String URGENCY = "urgency";
    public static final String SPECIAL_INSTRUCTIONS = "specialInstructions";
    public static final String REFERENCE_FILE = "referenceFile";

    // Register New Employee form (11.2)
    public static final String FIRST_NAME = "firstName";
    public static final String LAST_NAME = "lastName";
    public static final String ROLE = "role";
    public static final String POSITION = "position";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String CONFIRM_PASSWORD = "confirmPassword";
    public static final String ACCOUNT_ACTIVE = "accountActive";

    // Search Orders form (12.1)
    public static final String SEARCH_TEXT = "searchText";
    public static final String STATUS = "status";
    public static final String ASSIGNED_EMPLOYEE = "assignedEmployee";
    public static final String DATE_FROM = "dateFrom";
    public static final String DATE_TO = "dateTo";
    public static final String INCLUDE_CLOSED = "includeClosed";

    // Monitoring Filter form (9.2 to 9.4)
    public static final String MONITOR_TEXT = "monitorText";
    public static final String STAGES = "stages";
    public static final String DEADLINE_STATUS = "deadlineStatus";
    public static final String WARNING_DAYS = "warningDays";

    // Login (1.1)
    public static final String LOGIN_USERNAME = "loginUsername";
    public static final String LOGIN_PASSWORD = "loginPassword";

    private Fields() {
    }
}
