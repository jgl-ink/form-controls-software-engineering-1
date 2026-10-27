package crmpams.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Black box cases for every rule: valid input, each invalid case, and boundary values. */
class FieldRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 27);

    @Test
    void customerNameAcceptsPersonAndCompanyNames() {
        assertNull(FieldRules.customerName("Juan Dela Cruz"));
        assertNull(FieldRules.customerName("ABC Builders & Co. (Main)"));
        assertNull(FieldRules.customerName("Peña-O'Neil"));
        assertNull(FieldRules.customerName("Jo"));                       // minimum length 2
        assertNull(FieldRules.customerName("A".repeat(100)));            // maximum length 100
    }

    @Test
    void customerNameRejectsBlankShortLongAndSymbols() {
        assertNotNull(FieldRules.customerName(""));
        assertNotNull(FieldRules.customerName("   "));
        assertNotNull(FieldRules.customerName("J"));
        assertNotNull(FieldRules.customerName("A".repeat(101)));
        assertNotNull(FieldRules.customerName("Juan <script>"));
        assertNotNull(FieldRules.customerName("12345"));                 // needs at least one letter
    }

    @Test
    void personNameRejectsDigits() {
        assertNull(FieldRules.personName("Maria Clara", "First name"));
        assertNotNull(FieldRules.personName("Maria2", "First name"));
        assertNotNull(FieldRules.personName("M", "First name"));
        assertNotNull(FieldRules.personName("M".repeat(51), "First name"));
    }

    @Test
    void contactNumberFormats() {
        assertNull(FieldRules.contactNumber("09171234567", true));
        assertNull(FieldRules.contactNumber("+639171234567", true));
        assertNull(FieldRules.contactNumber("0281234567", true));
        assertNotNull(FieldRules.contactNumber("", true));
        assertNull(FieldRules.contactNumber("", false));                 // optional when not required
        assertNotNull(FieldRules.contactNumber("0917123456", true));     // 10 digits
        assertNotNull(FieldRules.contactNumber("091712345678", true));   // 12 digits
        assertNotNull(FieldRules.contactNumber("08171234567", true));    // not 09
        assertNotNull(FieldRules.contactNumber("+63917123456", true));
    }

    @Test
    void contactNumberIsNormalized() {
        assertEquals("09171234567", FieldRules.normalizeContactNumber("+639171234567"));
        assertEquals("09171234567", FieldRules.normalizeContactNumber(" 09171234567 "));
        assertEquals("0281234567", FieldRules.normalizeContactNumber("0281234567"));
        assertNull(FieldRules.normalizeContactNumber("12345"));
    }

    @Test
    void emailIsOptionalButMustMatchPattern() {
        assertNull(FieldRules.email(""));
        assertNull(FieldRules.email("name@example.com"));
        assertNotNull(FieldRules.email("name@"));
        assertNotNull(FieldRules.email("name.example.com"));
        assertNotNull(FieldRules.email("a@b.c"));
        assertNotNull(FieldRules.email("a".repeat(95) + "@x.com"));      // 101 characters
    }

    @Test
    void requiredTextLengthBoundaries() {
        assertNotNull(FieldRules.requiredText("", 5, 500, "Project description"));
        assertNotNull(FieldRules.requiredText("abcd", 5, 500, "Project description"));
        assertNull(FieldRules.requiredText("abcde", 5, 500, "Project description"));
        assertNull(FieldRules.requiredText("a".repeat(500), 5, 500, "Project description"));
        assertNotNull(FieldRules.requiredText("a".repeat(501), 5, 500, "Project description"));
        assertNull(FieldRules.optionalText("", 150, "Required output"));
        assertNotNull(FieldRules.optionalText("a".repeat(151), 150, "Required output"));
    }

    @Test
    void deadlineBoundaries() {
        assertNotNull(FieldRules.deadline(null, TODAY));
        assertNotNull(FieldRules.deadline(TODAY.minusDays(1), TODAY));
        assertNull(FieldRules.deadline(TODAY, TODAY));                   // today is allowed
        assertNull(FieldRules.deadline(TODAY.plusDays(1), TODAY));
    }

    @Test
    void dateRangeBoundaries() {
        assertNull(FieldRules.dateFrom(TODAY, TODAY));
        assertNotNull(FieldRules.dateFrom(TODAY.plusDays(1), TODAY));
        assertNull(FieldRules.dateTo(TODAY, TODAY));                     // "to" equal to "from"
        assertNotNull(FieldRules.dateTo(TODAY, TODAY.minusDays(1)));
        assertNotNull(FieldRules.dateTo(TODAY, null));
    }

    @Test
    void referenceFileRules() throws Exception {
        Path dir = Files.createTempDirectory("crmpams-ref");
        Path pdf = Files.createFile(dir.resolve("plan.PDF"));
        Path exe = Files.createFile(dir.resolve("tool.exe"));
        assertNull(FieldRules.referenceFile(""));
        assertNull(FieldRules.referenceFile(pdf.toString()));           // extension check ignores case
        assertNotNull(FieldRules.referenceFile(exe.toString()));
        assertNotNull(FieldRules.referenceFile(dir.resolve("missing.dwg").toString()));
        assertNotNull(FieldRules.referenceFile("C:/" + "x".repeat(600) + ".pdf"));
    }

    @Test
    void usernameRules() {
        assertNull(FieldRules.username("msantos"));
        assertNull(FieldRules.username("m.santos_2"));
        assertNull(FieldRules.username("abcd"));                         // minimum 4
        assertNotNull(FieldRules.username("abc"));
        assertNotNull(FieldRules.username("a".repeat(31)));
        assertNotNull(FieldRules.username("2msantos"));                  // must start with a letter
        assertNotNull(FieldRules.username("m santos"));
    }

    @Test
    void passwordRules() {
        assertNull(FieldRules.password("abcdefg1".toCharArray(), "msantos"));       // exactly 8
        assertNotNull(FieldRules.password("abcdef1".toCharArray(), "msantos"));     // 7
        assertNotNull(FieldRules.password("abcdefgh".toCharArray(), "msantos"));    // no number
        assertNotNull(FieldRules.password("12345678".toCharArray(), "msantos"));    // no letter
        assertNotNull(FieldRules.password("Msantos1".toCharArray(), "msantos1"));   // same as username
        assertNotNull(FieldRules.password(("a1" + "b".repeat(63)).toCharArray(), "x")); // 65
        assertNotNull(FieldRules.password(new char[0], "x"));
    }

    @Test
    void passwordConfirmation() {
        assertNull(FieldRules.passwordConfirmation("abcdefg1".toCharArray(), "abcdefg1".toCharArray()));
        assertNotNull(FieldRules.passwordConfirmation("abcdefg1".toCharArray(), "abcdefg2".toCharArray()));
        assertNotNull(FieldRules.passwordConfirmation("abcdefg1".toCharArray(), new char[0]));
    }

    @Test
    void warningDaysLimits() {
        assertNull(FieldRules.warningDays(1));
        assertNull(FieldRules.warningDays(14));
        assertNotNull(FieldRules.warningDays(0));
        assertNotNull(FieldRules.warningDays(15));
    }

    @Test
    void searchTextLength() {
        assertNull(FieldRules.searchText("a".repeat(100)));
        assertNotNull(FieldRules.searchText("a".repeat(101)));
    }

    @Test
    void orderLineRules() {
        assertNull(FieldRules.lineMedia("Bond paper"));
        assertNotNull(FieldRules.lineMedia(" "));
        assertNotNull(FieldRules.lineMedia("m".repeat(41)));
        assertNull(FieldRules.lineSize("A1"));
        assertNotNull(FieldRules.lineSize(""));
        assertNotNull(FieldRules.lineSize("s".repeat(31)));
        assertNull(FieldRules.sheetCount(1));                            // boundaries 1 and 999
        assertNull(FieldRules.sheetCount(999));
        assertNotNull(FieldRules.sheetCount(0));
        assertNotNull(FieldRules.sheetCount(1000));
        assertNotNull(FieldRules.sheetCount(null));                      // typed text that is not a number
        assertNull(FieldRules.setCount(99));
        assertNotNull(FieldRules.setCount(100));
        assertNotNull(FieldRules.lineCount(0));
        assertNull(FieldRules.lineCount(20));
        assertNotNull(FieldRules.lineCount(21));
    }
}
