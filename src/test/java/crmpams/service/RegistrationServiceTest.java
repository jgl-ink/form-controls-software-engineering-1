package crmpams.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crmpams.model.EmployeeRequest;
import crmpams.model.Role;
import crmpams.security.AccessDeniedException;
import crmpams.testsupport.InMemoryStore;
import crmpams.testsupport.TestFixtures;
import crmpams.validation.Fields;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RegistrationServiceTest {

    private InMemoryStore store;
    private RegistrationService service;

    @BeforeEach
    void setUp() {
        store = TestFixtures.store();
        service = TestFixtures.context(store).registration();
    }

    private static EmployeeRequest staff(String username, String position, String pw, String confirm) {
        return new EmployeeRequest("Maria", "Santos", Role.PRODUCTION_STAFF, position, "09171234567", "",
                username, pw.toCharArray(), confirm.toCharArray(), true);
    }

    @Test
    void registersAnEmployeeWithAHashedPassword() throws Exception {
        SaveResult r = service.register(TestFixtures.MANAGER, staff("msantos", "Drafter", "Drafts2026", "Drafts2026"));
        assertTrue(r.saved());
        InMemoryStore.User u = store.users.get(store.users.size() - 1);
        assertEquals("msantos", u.username());
        assertFalse(u.passwordHash().contains("Drafts2026"));
        assertTrue(TestFixtures.FAST_HASHER.verify("Drafts2026".toCharArray(), u.passwordHash()));
        assertEquals(TestFixtures.MANAGER.userId(), u.createdBy());              // hidden value from session
        assertEquals("Drafter", store.employees.get(store.employees.size() - 1).position());
    }

    @Test
    void managersGetTheRoleAsTheirPosition() throws Exception {
        EmployeeRequest r = new EmployeeRequest("Liza", "Gomez", Role.OFFICER_IN_CHARGE, null, "", "", "lgomez",
                "Office2026".toCharArray(), "Office2026".toCharArray(), true);
        assertTrue(service.register(TestFixtures.MANAGER, r).saved());
        assertEquals("Officer-in-Charge", store.employees.get(store.employees.size() - 1).position());
    }

    @Test
    void usernameMustBeUniqueIgnoringCase() throws Exception {
        SaveResult r = service.register(TestFixtures.MANAGER, staff("PReyes", "Designer", "Design2026", "Design2026"));
        assertFalse(r.saved());
        assertNotNull(r.errors().messageFor(Fields.USERNAME));
    }

    @Test
    void productionStaffNeedsAPosition() throws Exception {
        SaveResult r = service.register(TestFixtures.MANAGER, staff("newstaff", null, "Design2026", "Design2026"));
        assertNotNull(r.errors().messageFor(Fields.POSITION));
    }

    @Test
    void passwordsMustMatch() throws Exception {
        SaveResult r = service.register(TestFixtures.MANAGER, staff("newstaff", "Modeler", "Design2026", "Design2027"));
        assertNotNull(r.errors().messageFor(Fields.CONFIRM_PASSWORD));
        assertEquals(0, store.transactions);
    }

    @Test
    void onlyTheOperationsManagerMayRegister() {
        assertThrows(AccessDeniedException.class,
                () -> service.register(TestFixtures.OFFICER, staff("x1234", "Drafter", "Drafts2026", "Drafts2026")));
    }
}
