package crmpams.model;

/**
 * One line of a job order, following the company's order form: the project type, the media
 * (paper or output), the sheet size, the number of sheets, and the number of sets.
 * An order can have several lines, for example a floor plan and blueprint printing.
 * Prices are not recorded; the system does not compute prices (Chapter 1, Delimitations).
 */
public record OrderLine(Integer projectTypeId, String projectTypeName, String media, String size,
        int sheets, int sets) {

    /** Total number of sheets of the line: number of sheets times number of sets. */
    public int totalSheets() {
        return sheets * sets;
    }

    /** Short description, for example "Blueprint Printing, Blueprint paper, A1, 2 sheets x 3 sets = 6 sheets". */
    public String describe() {
        return projectTypeName + ", " + media + ", " + size + ", " + sheets + (sheets == 1 ? " sheet" : " sheets")
                + " x " + sets + (sets == 1 ? " set" : " sets") + " = " + totalSheets()
                + (totalSheets() == 1 ? " sheet" : " sheets");
    }
}
