package crmpams.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crmpams.model.Role;
import crmpams.testsupport.InMemoryStore;
import crmpams.testsupport.TestFixtures;
import crmpams.validation.Fields;
import crmpams.validation.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** White box cases for each login outcome: missing input, invalid credentials, inactive account, success. */
class AuthServiceTest {

    private InMemoryStore store;
    private AuthService auth;

    @BeforeEach
    void setUp() {
        store = TestFixtures.store();
        auth = TestFixtures.context(store).auth();
    }

    @Test
    void requiresBothFields() throws Exception {
        LoginResult r = auth.login(" ", new char[0]);
        assertNotNull(r.errors().messageFor(Fields.LOGIN_USERNAME));
        assertNotNull(r.errors().messageFor(Fields.LOGIN_PASSWORD));
    }

    @Test
    void wrongPasswordAndUnknownUserGiveTheSameMessage() throws Exception {
        LoginResult wrong = auth.login("preyes", "Wrong2026".toCharArray());
        LoginResult unknown = auth.login("nobody", "Wrong2026".toCharArray());
        assertFalse(wrong.success());
        assertEquals(wrong.errors().messageFor(ValidationResult.GENERAL),
                unknown.errors().messageFor(ValidationResult.GENERAL));
        assertTrue(store.activity.stream().anyMatch(a -> a.action().equals("LOGIN_FAILED")));
    }

    @Test
    void deactivatedAccountCannotLogIn() throws Exception {
        LoginResult r = auth.login("alim", TestFixtures.DEMO_PASSWORD.toCharArray());
        assertFalse(r.success());
        assertTrue(r.errors().messageFor(ValidationResult.GENERAL).contains("deactivated"));
    }

    @Test
    void successfulLoginIsLogged() throws Exception {
        LoginResult r = auth.login("rvillanueva", TestFixtures.DEMO_PASSWORD.toCharArray());
        assertTrue(r.success());
        assertEquals(Role.OPERATIONS_MANAGER, r.user().role());
        assertTrue(store.activity.stream().anyMatch(a -> a.action().equals("LOGIN")));
    }
}
