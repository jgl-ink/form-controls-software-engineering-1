package crmpams.db;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LikeEscaperTest {

    @Test
    void escapesWildcardsSoTheyMatchLiterally() {
        assertEquals("50!% off", LikeEscaper.escape("50% off"));
        assertEquals("file!_name", LikeEscaper.escape("file_name"));
        assertEquals("wow!!", LikeEscaper.escape("wow!"));
        assertEquals("%Navarro%", LikeEscaper.contains("Navarro"));
    }
}
