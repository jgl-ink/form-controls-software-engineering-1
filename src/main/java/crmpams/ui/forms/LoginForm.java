package crmpams.ui.forms;

import crmpams.model.CurrentUser;
import crmpams.service.AuthService;
import crmpams.ui.UiTasks;
import crmpams.ui.components.FormPanel;
import crmpams.ui.components.HintPasswordField;
import crmpams.ui.components.HintTextField;
import crmpams.ui.components.InputFilters;
import crmpams.validation.FieldRules;
import crmpams.validation.Fields;
import crmpams.validation.ValidationResult;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JCheckBox;

/** User Login (1.1). Not one of the four graded forms, but it uses the same validation and layout. */
public final class LoginForm extends FormPanel {

    private static final long serialVersionUID = 1L;

    private final transient AuthService auth;
    private final transient Consumer<CurrentUser> onSuccess;

    final HintTextField username = new HintTextField("Your username", 20);
    final HintPasswordField password = new HintPasswordField("Your password", 20);
    final JCheckBox showPassword = new JCheckBox("Show password");
    final JButton loginButton;

    public LoginForm(AuthService auth, Consumer<CurrentUser> onSuccess, Runnable onExit) {
        super("Log In", "CRM-PAMS, CADDSolutions Cubao branch", false);
        this.auth = auth;
        this.onSuccess = onSuccess;
        InputFilters.install(username, FieldRules.USERNAME_MAX, InputFilters.USERNAME);
        InputFilters.install(password, FieldRules.PASSWORD_MAX, InputFilters.SINGLE_LINE);

        addField(Fields.LOGIN_USERNAME, "Username", KeyEvent.VK_U, true, null, username);
        addField(Fields.LOGIN_PASSWORD, "Password", KeyEvent.VK_P, true, null, password);
        showPassword.setMnemonic(KeyEvent.VK_H);
        showPassword.setOpaque(false);
        showPassword.addActionListener(e -> password.setShowPassword(showPassword.isSelected()));
        addRow(null, showPassword);

        loginButton = addButton("Log In", KeyEvent.VK_L, "Log in with this username and password (Enter).",
                this::submit);
        addButton("Exit", KeyEvent.VK_X, "Close the application.", onExit);
        setDefaultButton(loginButton);
    }

    public void submit() {
        String name = username.getText();
        char[] pw = password.getPassword();
        ValidationResult v = AuthService.validate(name, pw);
        if (!v.isValid()) {
            Arrays.fill(pw, '\0');
            showErrors(v);
            return;
        }
        loginButton.setEnabled(false);
        UiTasks.run(this, () -> auth.login(name, pw), result -> {
            if (result.success()) {
                clearErrors();
                onSuccess.accept(result.user());
            } else {
                password.setText("");
                showErrors(result.errors());
                password.requestFocusInWindow();
            }
        }, () -> {
            Arrays.fill(pw, '\0');
            loginButton.setEnabled(true);
        });
    }
}
