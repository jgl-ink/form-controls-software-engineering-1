package crmpams;

import crmpams.db.Database;
import crmpams.ui.Dialogs;
import crmpams.ui.MainWindow;
import crmpams.ui.UiTheme;
import crmpams.ui.forms.LoginForm;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

/** Starts the application: log in first, then open the main window for the user's role. */
public final class App {

    private static final Logger LOG = Logger.getLogger(App.class.getName());

    private App() {
    }

    public static void main(String[] args) {
        setUpLogging();
        SwingUtilities.invokeLater(() -> {
            UiTheme.install();
            Database db;
            try {
                db = Database.fromFile(Path.of("db.properties"));
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Cannot read database settings", e);
                Dialogs.error(null, "Database settings missing", e.getMessage());
                System.exit(1);
                return;
            }
            AppContext ctx = AppContext.create(db, Clock.systemDefaultZone());
            showLogin(ctx);
        });
    }

    static void showLogin(AppContext ctx) {
        JFrame frame = new JFrame("CRM-PAMS | Log In");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        LoginForm form = new LoginForm(ctx.auth(), user -> {
            frame.dispose();
            new MainWindow(ctx, user, () -> showLogin(ctx)).setVisible(true);
        }, () -> System.exit(0));
        frame.setContentPane(form);
        frame.setSize(520, 400);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        form.focusFirstField();
    }

    /** Technical errors go to logs/crm-pams.log; users only see plain messages. */
    private static void setUpLogging() {
        try {
            Files.createDirectories(Path.of("logs"));
            FileHandler handler = new FileHandler("logs/crm-pams.log", 1_000_000, 3, true);
            handler.setFormatter(new SimpleFormatter());
            Logger.getLogger("crmpams").addHandler(handler);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "File logging is not available", e);
        }
    }
}
