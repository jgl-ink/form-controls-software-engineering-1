package crmpams.ui.forms;

import crmpams.model.CurrentUser;
import crmpams.model.EmployeeRequest;
import crmpams.model.Role;
import crmpams.service.RegistrationService;
import crmpams.ui.Dialogs;
import crmpams.ui.UiTasks;
import crmpams.ui.components.FormPanel;
import crmpams.ui.components.HintPasswordField;
import crmpams.ui.components.HintTextField;
import crmpams.ui.components.InputFilters;
import crmpams.ui.components.RadioGroup;
import crmpams.ui.components.ToggleSwitch;
import crmpams.validation.FieldRules;
import crmpams.validation.Fields;
import crmpams.validation.ValidationResult;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JTextField;

/**
 * Major Form 2: Register New Employee (Registration 11.0, IPO 11.2).
 * Only the Operations Manager opens this form; the service checks the role again.
 */
public final class RegisterEmployeeForm extends FormPanel {

    private static final long serialVersionUID = 1L;
    static final String POSITION_PLACEHOLDER = "Select a position";

    private final transient RegistrationService service;
    private final transient CurrentUser user;
    private final transient Runnable onClose;

    final HintTextField firstName = new HintTextField("Maria", 22);
    final HintTextField lastName = new HintTextField("Santos", 22);
    final RadioGroup<Role> role = new RadioGroup<>(List.of(Role.values()), Role::label);
    final JComboBox<String> position = new JComboBox<>();
    final HintTextField contactNumber = new HintTextField("09171234567", 14);
    final HintTextField email = new HintTextField("name@example.com", 26);
    final HintTextField username = new HintTextField("msantos", 20);
    final HintPasswordField password = new HintPasswordField("At least 8 characters", 20);
    final HintPasswordField confirmPassword = new HintPasswordField("Type it again", 20);
    final JCheckBox showPasswords = new JCheckBox("Show passwords");
    final ToggleSwitch active = new ToggleSwitch("Active", "Inactive", true);
    final JButton registerButton;

    public RegisterEmployeeForm(RegistrationService service, CurrentUser user, Runnable onClose) {
        super("Register New Employee",
                "Create the login account of an employee. The password is stored only as a secure hash.", true);
        this.service = service;
        this.user = user;
        this.onClose = onClose;

        position.addItem(POSITION_PLACEHOLDER);
        FieldRules.PRODUCTION_POSITIONS.forEach(position::addItem);
        position.setEnabled(false);

        InputFilters.install(firstName, FieldRules.PERSON_NAME_MAX, InputFilters.SINGLE_LINE);
        InputFilters.install(lastName, FieldRules.PERSON_NAME_MAX, InputFilters.SINGLE_LINE);
        InputFilters.install(contactNumber, FieldRules.CONTACT_NUMBER_MAX_CHARS, InputFilters.PHONE);
        InputFilters.install(email, FieldRules.EMAIL_MAX, InputFilters.SINGLE_LINE);
        InputFilters.install(username, FieldRules.USERNAME_MAX, InputFilters.USERNAME);
        InputFilters.install(password, FieldRules.PASSWORD_MAX, InputFilters.SINGLE_LINE);
        InputFilters.install(confirmPassword, FieldRules.PASSWORD_MAX, InputFilters.SINGLE_LINE);

        addSection("Employee details");
        addField(Fields.FIRST_NAME, "First name", KeyEvent.VK_F, true,
                "Letters, spaces, periods, apostrophes, and hyphens. 2 to 50 characters.", firstName);
        addField(Fields.LAST_NAME, "Last name", KeyEvent.VK_L, true,
                "Letters, spaces, periods, apostrophes, and hyphens. 2 to 50 characters.", lastName);
        role.getAccessibleContext().setAccessibleName("Role");
        addField(Fields.ROLE, "Role", KeyEvent.VK_R, true,
                "Decides which menu the employee sees after logging in. Arrow keys change the choice.",
                role, role.firstButton(), role);
        addField(Fields.POSITION, "Position", KeyEvent.VK_P, false,
                "Required for Production Staff. Set automatically for the other roles.", position);
        addField(Fields.CONTACT_NUMBER, "Contact number", KeyEvent.VK_N, false,
                "Optional. Mobile 09XXXXXXXXX or +639XXXXXXXXX, or landline 02 plus 8 digits.", contactNumber);
        addField(Fields.EMAIL, "Email", KeyEvent.VK_E, false, "Optional.", email);

        addSection("Login account");
        addField(Fields.USERNAME, "Username", KeyEvent.VK_U, true,
                "4 to 30 characters. Start with a letter. Letters, numbers, periods, and underscores only.",
                username);
        addField(Fields.PASSWORD, "Password", KeyEvent.VK_W, true,
                "8 to 64 characters with at least one letter and one number. Must differ from the username.",
                password);
        addField(Fields.CONFIRM_PASSWORD, "Confirm password", KeyEvent.VK_C, true,
                "Type the same password again.", confirmPassword);
        showPasswords.setMnemonic(KeyEvent.VK_H);
        showPasswords.setOpaque(false);
        showPasswords.setToolTipText("Show the typed passwords so you can check them.");
        showPasswords.addActionListener(e -> {
            password.setShowPassword(showPasswords.isSelected());
            confirmPassword.setShowPassword(showPasswords.isSelected());
        });
        addRow(null, showPasswords);
        addField(Fields.ACCOUNT_ACTIVE, "Account status", KeyEvent.VK_A, false,
                "Inactive accounts cannot log in. Their records are kept.", active);

        registerButton = addButton("Register Employee", KeyEvent.VK_G,
                "Check the form and create the account (Enter).", this::submit);
        addButton("Clear Form", KeyEvent.VK_O, "Empty every field and start again.", this::clearForm);
        addButton("Cancel", 0, "Close the form without saving (Esc).", this::cancel);
        setDefaultButton(registerButton);
        setEscapeAction(this::cancel);

        role.addActionListener(e -> updatePositionState());
    }

    private void updatePositionState() {
        boolean staff = role.getSelected() == Role.PRODUCTION_STAFF;
        position.setEnabled(staff);
        if (!staff) {
            position.setSelectedIndex(0);
        }
        setHelper(Fields.POSITION, staff
                ? "Required. Choose Designer, Drafter, or Modeler."
                : "Required for Production Staff. Set automatically for the other roles.");
    }

    EmployeeRequest collectRequest() {
        String pos = position.getSelectedIndex() <= 0 ? null : (String) position.getSelectedItem();
        return new EmployeeRequest(
                firstName.getText(),
                lastName.getText(),
                role.getSelected(),
                pos,
                contactNumber.getText(),
                email.getText(),
                username.getText(),
                password.getPassword(),
                confirmPassword.getPassword(),
                active.isSelected());
    }

    public void submit() {
        EmployeeRequest request = collectRequest();
        ValidationResult v = RegistrationService.validate(request);
        if (!v.isValid()) {
            wipe(request);
            showErrors(v);
            return;
        }
        clearErrors();
        registerButton.setEnabled(false);
        UiTasks.run(this, () -> service.register(user, request), result -> {
            if (result.saved()) {
                Dialogs.info(this, "Employee registered", "The account of " + request.firstName().strip() + " "
                        + request.lastName().strip() + " was created with the username "
                        + request.username().strip() + ". Give the initial password to the employee in person.");
                clearForm();
            } else {
                showErrors(result.errors());
            }
        }, () -> {
            wipe(request);
            registerButton.setEnabled(true);
        });
    }

    private static void wipe(EmployeeRequest r) {
        Arrays.fill(r.password(), '\0');
        Arrays.fill(r.confirmPassword(), '\0');
    }

    public void clearForm() {
        for (JTextField f : new JTextField[] {firstName, lastName, contactNumber, email, username, password,
                confirmPassword}) {
            f.setText("");
        }
        role.clearSelection();
        updatePositionState();
        showPasswords.setSelected(false);
        password.setShowPassword(false);
        confirmPassword.setShowPassword(false);
        active.setSelected(true);
        clearErrors();
        focusFirstField();
    }

    boolean isDirty() {
        return !firstName.getText().isBlank() || !lastName.getText().isBlank() || role.getSelected() != null
                || !contactNumber.getText().isBlank() || !email.getText().isBlank()
                || !username.getText().isBlank() || password.getDocument().getLength() > 0;
    }

    private void cancel() {
        if (isDirty() && !Dialogs.confirm(this, "Discard this registration?",
                "The account has not been created. Close the form and discard what you entered?")) {
            return;
        }
        onClose.run();
    }
}
