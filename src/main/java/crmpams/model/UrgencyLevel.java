package crmpams.model;

/** A row of the urgency_levels table. A higher rank means more urgent. */
public record UrgencyLevel(int id, String name, int rank) {

    @Override
    public String toString() {
        return name;
    }
}
