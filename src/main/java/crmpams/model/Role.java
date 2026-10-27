package crmpams.model;

/** The three user roles of the system (Chapter 1, Security module). */
public enum Role {
    OPERATIONS_MANAGER("Operations Manager"),
    OFFICER_IN_CHARGE("Officer-in-Charge"),
    PRODUCTION_STAFF("Production Staff");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
