package crmpams.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PasswordHasherTest {

    private final PasswordHasher hasher = new PasswordHasher(1_000);

    @Test
    void verifiesTheRightPasswordOnly() {
        String stored = hasher.hash("Demo2026pass".toCharArray());
        assertTrue(hasher.verify("Demo2026pass".toCharArray(), stored));
        assertFalse(hasher.verify("demo2026pass".toCharArray(), stored));
        assertFalse(hasher.verify("".toCharArray(), stored));
    }

    @Test
    void usesARandomSaltAndNeverStoresThePlainPassword() {
        String a = hasher.hash("Demo2026pass".toCharArray());
        String b = hasher.hash("Demo2026pass".toCharArray());
        assertNotEquals(a, b);
        assertFalse(a.contains("Demo2026pass"));
        assertTrue(a.startsWith("pbkdf2_sha256$1000$"));
    }

    @Test
    void defaultIsSixHundredThousandIterations() {
        String stored = new PasswordHasher().hash("Demo2026pass".toCharArray());
        assertTrue(stored.startsWith("pbkdf2_sha256$600000$"));
    }

    @Test
    void rejectsMalformedHashes() {
        assertFalse(hasher.verify("x".toCharArray(), "plain-text"));
        assertFalse(hasher.verify("x".toCharArray(), "pbkdf2_sha256$abc$def$ghi"));
        assertFalse(hasher.verify("x".toCharArray(), null));
    }
}
