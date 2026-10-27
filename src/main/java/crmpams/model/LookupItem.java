package crmpams.model;

/**
 * An id and display name pair used by drop-down lists (project types, stages, employees).
 * A placeholder item such as "All" or "Select a project type" has no id.
 */
public record LookupItem(Integer id, String name) {

    public static LookupItem placeholder(String text) {
        return new LookupItem(null, text);
    }

    public boolean isPlaceholder() {
        return id == null;
    }

    @Override
    public String toString() {
        return name;
    }
}
