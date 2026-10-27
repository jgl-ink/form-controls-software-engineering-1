package crmpams.ui.forms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import crmpams.AppContext;
import crmpams.model.Role;
import crmpams.testsupport.InMemoryStore;
import crmpams.testsupport.TestFixtures;
import crmpams.ui.Dialogs;
import crmpams.ui.UiTasks;
import crmpams.ui.components.FormPanel;
import crmpams.validation.Fields;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;
import javax.swing.JLabel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Checks the forms the way a user would: submit with bad input, then with good input.
 * Runs without a window, so it also works on a build server.
 */
class FormBehaviorTest {

    private InMemoryStore store;
    private AppContext ctx;
    private final List<String> messages = new ArrayList<>();

    @BeforeEach
    void setUp() {
        UiTasks.setSynchronous(true);
        store = TestFixtures.store();
        ctx = TestFixtures.context(store);
        messages.clear();
        Dialogs.setHandler(new Dialogs.Handler() {
            @Override
            public void info(Component p, String title, String message) {
                messages.add(title);
            }

            @Override
            public void error(Component p, String title, String message) {
                messages.add(title);
            }

            @Override
            public boolean confirm(Component p, String title, String message) {
                messages.add(title);
                return true;
            }
        });
    }

    private NewOrderForm newOrderForm() throws Exception {
        return new NewOrderForm(ctx.orderIntake(), TestFixtures.OFFICER,
                ctx.orderIntake().loadFormData(TestFixtures.OFFICER), () -> { });
    }

    @Test
    void emptyNewOrderIsBlockedAndNothingIsSaved() throws Exception {
        NewOrderForm f = newOrderForm();
        int before = store.orders.size();
        f.submit();
        assertTrue(f.isSummaryShown());
        assertNotNull(f.shownError(Fields.CUSTOMER_NAME));
        assertNotNull(f.shownError(Fields.CONTACT_NUMBER));
        assertNotNull(f.shownError(Fields.PROJECT_TYPE));
        assertNotNull(f.shownError(Fields.PROJECT_DESCRIPTION));
        assertNotNull(f.shownError(Fields.URGENCY));
        assertNull(f.shownError(Fields.EMAIL));                    // optional and empty is fine
        assertEquals(before, store.orders.size());
        assertEquals(0, store.transactions);
    }

    @Test
    void typedInvalidDeadlineGetsAFormatMessage() throws Exception {
        NewOrderForm f = newOrderForm();
        f.deadline.textField().setText("2026-13-45");
        f.submit();
        assertTrue(f.shownError(Fields.DEADLINE).contains("YYYY-MM-DD"));
    }

    @Test
    void validNewOrderIsSavedAndTheFormIsCleared() throws Exception {
        NewOrderForm f = newOrderForm();
        f.customerName.setText("Juan Dela Cruz");
        f.contactNumber.setText("+639171112222");
        f.projectType.setSelectedIndex(1);
        f.description.setText("Floor plan for a bungalow");
        f.deadline.setDate(TestFixtures.TODAY.plusDays(3));
        f.urgency.setSelected(store.urgencyLevels.get(1));
        f.submit();
        assertFalse(f.isSummaryShown());
        assertEquals(9, store.orders.size());
        assertTrue(messages.contains("Order saved"));
        assertEquals("", f.customerName.getText());
    }

    @Test
    void lookupFillsTheHiddenCustomerIdAndEditingClearsIt() throws Exception {
        NewOrderForm f = newOrderForm();
        f.customerName.setText("engr. dennis tan");
        f.contactNumber.setText("09181234503");
        f.lookUpCustomer();
        assertEquals(3, f.hiddenCustomerId());
        assertEquals("Engr. Dennis Tan", f.customerName.getText());
        f.customerName.setText("Someone Else");
        assertNull(f.hiddenCustomerId());
    }

    @Test
    void inputFiltersLimitLengthAndCharacters() throws Exception {
        NewOrderForm f = newOrderForm();
        f.contactNumber.setText("09ab17-123 456789999");
        assertEquals("091712345678", f.contactNumber.getText().substring(0, 12));
        assertTrue(f.contactNumber.getText().length() <= 13);
        f.description.setText("x".repeat(600));
        assertEquals(500, f.description.getText().length());
    }

    @Test
    void registrationShowsMismatchedPasswordsAndPositionRule() {
        RegisterEmployeeForm f = new RegisterEmployeeForm(ctx.registration(), TestFixtures.MANAGER, () -> { });
        f.firstName.setText("Maria");
        f.lastName.setText("Santos");
        f.role.setSelected(Role.PRODUCTION_STAFF);
        f.username.setText("msantos");
        f.password.setText("Design2026");
        f.confirmPassword.setText("Design2027");
        f.submit();
        assertNotNull(f.shownError(Fields.CONFIRM_PASSWORD));
        assertNotNull(f.shownError(Fields.POSITION));
        assertEquals(6, store.users.size());
    }

    @Test
    void validRegistrationCreatesTheAccount() {
        RegisterEmployeeForm f = new RegisterEmployeeForm(ctx.registration(), TestFixtures.MANAGER, () -> { });
        f.firstName.setText("Maria");
        f.lastName.setText("Santos");
        f.role.setSelected(Role.PRODUCTION_STAFF);
        f.position.setEnabled(true);
        f.position.setSelectedItem("Drafter");
        f.username.setText("msantos");
        f.password.setText("Design2026");
        f.confirmPassword.setText("Design2026");
        f.active.setSelected(false);
        f.submit();
        assertEquals(7, store.users.size());
        assertFalse(store.users.get(6).active());
        assertTrue(messages.contains("Employee registered"));
    }

    @Test
    void searchRejectsAnEndDateBeforeTheStartDate() throws Exception {
        SearchOrdersForm f = new SearchOrdersForm(ctx.search(), TestFixtures.OFFICER,
                ctx.search().loadOptions(TestFixtures.OFFICER));
        f.useDateRange.doClick();
        f.dateFrom.setDate(TestFixtures.TODAY);
        f.dateTo.setDate(TestFixtures.TODAY.minusDays(3));
        f.submit();
        assertNotNull(f.shownError(Fields.DATE_TO));
        assertEquals(0, f.model.getRowCount());
    }

    @Test
    void searchFillsTheResultsTable() throws Exception {
        SearchOrdersForm f = new SearchOrdersForm(ctx.search(), TestFixtures.OFFICER,
                ctx.search().loadOptions(TestFixtures.OFFICER));
        f.searchText.setText("plan");
        f.submit();
        assertTrue(f.model.getRowCount() > 0);
        assertTrue(f.resultCount.getText().contains("found"));
    }

    @Test
    void monitoringNeedsAStageAndShowsFlags() throws Exception {
        MonitoringFilterForm f = new MonitoringFilterForm(ctx.monitoring(), TestFixtures.MANAGER,
                ctx.monitoring().loadOptions(TestFixtures.MANAGER));
        f.stageBoxes.forEach(b -> b.setSelected(false));
        f.submit();
        assertNotNull(f.shownError(Fields.STAGES));
        f.resetFilters();
        f.submit();
        assertEquals(6, f.model.getRowCount());
        assertTrue(f.stageCounts.getText().startsWith("Orders per stage"));
    }

    @Test
    void everyFormHasEnoughControlsAndEveryInputHasAnAccessibleName() throws Exception {
        List<FormPanel> forms = List.of(
                newOrderForm(),
                new RegisterEmployeeForm(ctx.registration(), TestFixtures.MANAGER, () -> { }),
                new SearchOrdersForm(ctx.search(), TestFixtures.OFFICER, ctx.search().loadOptions(TestFixtures.OFFICER)),
                new MonitoringFilterForm(ctx.monitoring(), TestFixtures.MANAGER,
                        ctx.monitoring().loadOptions(TestFixtures.MANAGER)));
        for (FormPanel f : forms) {
            assertTrue(f.focusOrder().size() >= 10, f.getClass().getSimpleName() + " has "
                    + f.focusOrder().size() + " focusable controls");
            for (String key : f.fieldKeys()) {
                JComponent target = f.labelTargetOf(key);
                assertNotNull(target.getAccessibleContext().getAccessibleName(), key);
                assertTrue(hasLabelFor(f, target), key + " has no label");
            }
            assertNotNull(f.defaultButton());
        }
    }

    private static boolean hasLabelFor(java.awt.Container root, Component target) {
        for (Component c : root.getComponents()) {
            if (c instanceof JLabel l && l.getLabelFor() == target) {
                return true;
            }
            if (c instanceof java.awt.Container ct && hasLabelFor(ct, target)) {
                return true;
            }
        }
        return false;
    }
}
