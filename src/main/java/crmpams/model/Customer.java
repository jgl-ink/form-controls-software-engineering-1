package crmpams.model;

/** A row of the customers table. The id is null before the record is saved. */
public record Customer(Integer id, String name, String contactNumber, String email) {
}
