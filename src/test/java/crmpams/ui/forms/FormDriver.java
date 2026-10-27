package crmpams.ui.forms;

import crmpams.AppContext;
import crmpams.model.LookupItem;
import crmpams.model.Role;
import crmpams.testsupport.TestFixtures;
import crmpams.ui.components.FormPanel;
import java.nio.file.Path;
import javax.swing.JComboBox;
import javax.swing.JRadioButton;

/**
 * Fills the forms the way a user would, for the screenshot tool and the demo.
 * Lives in the forms package so it can reach the form controls directly.
 */
public final class FormDriver {

    private final AppContext ctx;
    private final Path sampleFile;

    public FormDriver(AppContext ctx, Path sampleFile) {
        this.ctx = ctx;
        this.sampleFile = sampleFile;
        FormDriver.file = sampleFile;
    }

    private static Path file;

    // ------------------------------------------------------------------ creating forms

    public FormPanel login() {
        return new LoginForm(ctx.auth(), u -> { }, () -> { });
    }

    public FormPanel newOrder() throws Exception {
        return new NewOrderForm(ctx.orderIntake(), TestFixtures.OFFICER,
                ctx.orderIntake().loadFormData(TestFixtures.OFFICER), () -> { });
    }

    public FormPanel register() {
        return new RegisterEmployeeForm(ctx.registration(), TestFixtures.MANAGER, () -> { });
    }

    public FormPanel search() throws Exception {
        return new SearchOrdersForm(ctx.search(), TestFixtures.OFFICER, ctx.search().loadOptions(TestFixtures.OFFICER));
    }

    public FormPanel monitoring() throws Exception {
        return new MonitoringFilterForm(ctx.monitoring(), TestFixtures.MANAGER,
                ctx.monitoring().loadOptions(TestFixtures.MANAGER));
    }

    public Path sampleFile() {
        return sampleFile;
    }

    // ------------------------------------------------------------------ steps

    public static void submit(FormPanel f) {
        if (f instanceof NewOrderForm n) {
            n.submit();
        } else if (f instanceof RegisterEmployeeForm r) {
            r.submit();
        } else if (f instanceof SearchOrdersForm s) {
            s.submit();
        } else if (f instanceof MonitoringFilterForm m) {
            m.submit();
        } else if (f instanceof LoginForm l) {
            l.submit();
        }
    }

    public static void submitEmpty(FormPanel f) {
        submit(f);
    }

    private static void select(JComboBox<?> combo, String name) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            Object item = combo.getItemAt(i);
            String text = item instanceof LookupItem li ? li.name() : String.valueOf(item);
            if (text.equals(name)) {
                combo.setSelectedIndex(i);
                return;
            }
        }
        throw new IllegalArgumentException("No item " + name);
    }

    private static void clickRadio(java.util.List<JRadioButton> buttons, String text) {
        buttons.stream().filter(b -> b.getText().equals(text)).findFirst().orElseThrow().doClick(0);
    }

    public static void fillNewOrderInvalid(FormPanel form) {
        NewOrderForm f = (NewOrderForm) form;
        f.customerName.setText("J");
        f.contactNumber.setText("0917123");
        f.email.setText("juan@mail");
        f.description.setText("Plan");
        f.deadline.setDate(TestFixtures.TODAY.minusDays(1));
        f.submit();
    }

    public static void lookUpExistingCustomer(FormPanel form) {
        NewOrderForm f = (NewOrderForm) form;
        f.customerName.setText("arch. lea navarro");
        f.contactNumber.setText("+639171234501");
        f.lookUpCustomer();
    }

    public static void fillNewOrderValid(FormPanel form) {
        NewOrderForm f = (NewOrderForm) form;
        f.customerName.setText("Juan Dela Cruz");
        f.contactNumber.setText("09171112222");
        f.email.setText("juan.delacruz@example.com");
        select(f.projectType, "Architectural Floor Plan");
        f.description.setText("Floor plan of a two-bedroom bungalow with roof plan");
        f.requiredOutput.setText("2 sets, A1 size, blueprint");
        f.deadline.setDate(TestFixtures.TODAY.plusDays(4));
        clickRadio(f.urgency.buttons(), "High");
        f.instructions.setText("Customer will pick up the prints at the Cubao branch.");
        f.referenceFile.setText(file.toString());
    }

    public static void fillNewOrderDuplicate(FormPanel form) {
        NewOrderForm f = (NewOrderForm) form;
        f.customerName.setText("Arch. Lea Navarro");
        f.contactNumber.setText("09171234501");
        select(f.projectType, "Architectural Floor Plan");
        f.description.setText("Two-storey residential floor plan revision");
        f.deadline.setDate(TestFixtures.TODAY.plusDays(5));
        clickRadio(f.urgency.buttons(), "Normal");
    }

    public static void fillRegisterInvalid(FormPanel form) {
        RegisterEmployeeForm f = (RegisterEmployeeForm) form;
        f.firstName.setText("Ma1");
        f.contactNumber.setText("12345");
        f.email.setText("maria@");
        f.username.setText("ms");
        f.password.setText("short");
        f.confirmPassword.setText("shor");
        f.submit();
    }

    public static void fillRegisterValid(FormPanel form) {
        RegisterEmployeeForm f = (RegisterEmployeeForm) form;
        f.firstName.setText("Maria");
        f.lastName.setText("Santos");
        clickRadio(f.role.buttons(), Role.PRODUCTION_STAFF.label());
        select(f.position, "Drafter");
        f.contactNumber.setText("09175550123");
        f.email.setText("maria.santos@example.com");
        f.username.setText("msantos");
        f.password.setText("Drafts2026");
        f.confirmPassword.setText("Drafts2026");
    }

    public static void fillRegisterTakenUsername(FormPanel form) {
        fillRegisterValid(form);
        RegisterEmployeeForm f = (RegisterEmployeeForm) form;
        f.username.setText("preyes");
        f.submit();
    }

    public static void searchForPlan(FormPanel form) {
        SearchOrdersForm f = (SearchOrdersForm) form;
        f.searchText.setText("plan");
        f.submit();
    }

    public static void fillSearchInvalid(FormPanel form) {
        SearchOrdersForm f = (SearchOrdersForm) form;
        select(f.status, "Completed");
        f.useDateRange.doClick(0);
        f.dateFrom.setDate(TestFixtures.TODAY.plusDays(2));
        f.dateTo.setDate(TestFixtures.TODAY.minusDays(3));
        f.submit();
    }

    public static void viewFirstOrder(FormPanel form) {
        SearchOrdersForm f = (SearchOrdersForm) form;
        f.submit();
        f.table.setRowSelectionInterval(0, 0);
        f.viewSelected();
    }

    public static void monitorApproachingSevenDays(FormPanel form) {
        MonitoringFilterForm f = (MonitoringFilterForm) form;
        f.warningDays.setValue(7);
        clickRadio(f.deadlineStatus.buttons(), "Approaching Deadline");
        f.submit();
    }

    public static void fillMonitoringInvalid(FormPanel form) {
        MonitoringFilterForm f = (MonitoringFilterForm) form;
        f.stageBoxes.forEach(b -> b.setSelected(false));
        f.submit();
    }
}
