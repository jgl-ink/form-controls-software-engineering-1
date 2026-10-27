package crmpams.tools;

import crmpams.AppContext;
import crmpams.model.Role;
import crmpams.testsupport.InMemoryStore;
import crmpams.testsupport.TestFixtures;
import crmpams.ui.Dialogs;
import crmpams.ui.MainWindow;
import crmpams.ui.UiTasks;
import crmpams.ui.UiTheme;
import crmpams.ui.components.FormPanel;
import crmpams.ui.forms.FormDriver;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

/**
 * Development tool, not part of the application (it lives in src/test, so it is never packaged).
 * Opens every form with the fictional sample data, captures it in its normal state, fills it with
 * invalid input, submits, and captures the error state. Also renders the directory tree.
 *
 * Run in NetBeans: right-click this file, Run File. Output: docs/form-controls/screenshots/.
 */
public final class FormScreenshotTool {

    private static Path out;
    private static String lastTitle;
    private static String lastMessage;
    private static int lastType;

    private FormScreenshotTool() {
    }

    public static void main(String[] args) throws Exception {
        out = Path.of(args.length > 0 ? args[0] : "docs/form-controls/screenshots");
        Path projectRoot = Path.of(args.length > 1 ? args[1] : ".");
        Files.createDirectories(out);
        UiTasks.setSynchronous(true);
        Dialogs.setHandler(new Dialogs.Handler() {
            @Override
            public void info(Component p, String title, String message) {
                remember(title, message, JOptionPane.INFORMATION_MESSAGE);
            }

            @Override
            public void error(Component p, String title, String message) {
                remember(title, message, JOptionPane.ERROR_MESSAGE);
            }

            @Override
            public boolean confirm(Component p, String title, String message) {
                remember(title, message, JOptionPane.WARNING_MESSAGE);
                return false;
            }
        });
        SwingUtilities.invokeAndWait(UiTheme::install);

        InMemoryStore store = TestFixtures.store();
        AppContext ctx = TestFixtures.context(store);
        Path sampleFile = Path.of(System.getProperty("user.home"), "CADD-Shared", "Orders",
                "navarro-floor-plan-rev3.dwg");
        Files.createDirectories(sampleFile.getParent());
        if (!Files.exists(sampleFile)) {
            Files.createFile(sampleFile);
        }
        FormDriver d = new FormDriver(ctx, sampleFile);

        // Login
        shoot("01_login_normal.png", d.login(), 520, 380, null);
        shoot("02_login_errors.png", d.login(), 520, 470, FormDriver::submitEmpty);

        // Main window of the Operations Manager
        mainWindow(ctx);

        // Major Form 1: New Order
        shoot("04_new_order_normal.png", d.newOrder(), 900, 1640, null);
        shoot("05_new_order_errors.png", d.newOrder(), 900, 1860, FormDriver::fillNewOrderInvalid);
        shoot("06_new_order_lookup_found.png", d.newOrder(), 900, 1640, FormDriver::lookUpExistingCustomer);
        shoot("07_new_order_valid_filled.png", d.newOrder(), 900, 1640, FormDriver::fillNewOrderValid);
        shoot("07b_new_order_line_errors.png", d.newOrder(), 900, 1720, FormDriver::addInvalidLine);
        int before = store.orders.size();
        FormPanel saved = d.newOrder();
        SwingUtilities.invokeAndWait(() -> {
            FormDriver.fillNewOrderValid(saved);
            FormDriver.submit(saved);
        });
        dialog("08_new_order_saved_message.png");
        check(store.orders.size() == before + 1, "valid New Order was not saved");
        FormPanel dup = d.newOrder();
        SwingUtilities.invokeAndWait(() -> {
            FormDriver.fillNewOrderDuplicate(dup);
            FormDriver.submit(dup);
        });
        dialog("09_new_order_duplicate_warning.png");

        // Major Form 2: Register New Employee
        shoot("10_register_normal.png", d.register(), 900, 1000, null);
        shoot("11_register_errors.png", d.register(), 900, 1240, FormDriver::fillRegisterInvalid);
        shoot("12_register_valid_filled.png", d.register(), 900, 1000, FormDriver::fillRegisterValid);
        int users = store.users.size();
        FormPanel reg = d.register();
        SwingUtilities.invokeAndWait(() -> {
            FormDriver.fillRegisterValid(reg);
            FormDriver.submit(reg);
        });
        dialog("13_register_saved_message.png");
        check(store.users.size() == users + 1, "valid registration was not saved");
        shoot("14_register_username_taken.png", d.register(), 900, 1060, FormDriver::fillRegisterTakenUsername);

        // Supporting Form 1: Search Orders
        shoot("15_search_results.png", d.search(), 1100, 780, FormDriver::submit);
        shoot("16_search_text_result.png", d.search(), 1100, 780, FormDriver::searchForPlan);
        shoot("17_search_errors.png", d.search(), 1100, 860, FormDriver::fillSearchInvalid);
        FormPanel view = d.search();
        SwingUtilities.invokeAndWait(() -> FormDriver.viewFirstOrder(view));
        dialog("18_search_view_order.png");

        // Supporting Form 2: Monitoring Filter
        shoot("19_monitoring_all_active.png", d.monitoring(), 1100, 800, FormDriver::submit);
        shoot("20_monitoring_approaching_7_days.png", d.monitoring(), 1100, 800,
                FormDriver::monitorApproachingSevenDays);
        shoot("21_monitoring_errors.png", d.monitoring(), 1100, 860, FormDriver::fillMonitoringInvalid);

        directoryTree(projectRoot, out.resolve("22_directory_structure.png"));
        System.out.println("Screenshots written to " + out.toAbsolutePath());
        System.exit(0);
    }

    private static void remember(String title, String message, int type) {
        lastTitle = title;
        lastMessage = message;
        lastType = type;
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }

    interface Step {
        void apply(FormPanel form);
    }

    private static void shoot(String file, FormPanel form, int width, int height, Step step) throws Exception {
        JFrame[] frame = new JFrame[1];
        SwingUtilities.invokeAndWait(() -> {
            frame[0] = new JFrame();
            frame[0].setContentPane(form);
            frame[0].setSize(width, height);
            frame[0].setVisible(true);
        });
        settle();
        if (step != null) {
            SwingUtilities.invokeAndWait(() -> step.apply(form));
            settle();
        }
        capture(frame[0].getContentPane(), out.resolve(file));
        SwingUtilities.invokeAndWait(frame[0]::dispose);
    }

    private static void mainWindow(AppContext ctx) throws Exception {
        MainWindow[] w = new MainWindow[1];
        SwingUtilities.invokeAndWait(() -> {
            w[0] = new MainWindow(ctx, TestFixtures.MANAGER, () -> { });
            w[0].setSize(1100, 560);
            w[0].setVisible(true);
        });
        settle();
        capture(w[0].getContentPane(), out.resolve("03_main_window_operations_manager.png"));
        SwingUtilities.invokeAndWait(w[0]::dispose);
        check(TestFixtures.MANAGER.role() == Role.OPERATIONS_MANAGER, "fixture role changed");
    }

    private static void dialog(String file) throws Exception {
        String title = lastTitle;
        String message = lastMessage;
        int type = lastType;
        check(title != null, "no dialog was shown for " + file);
        JDialog[] dlg = new JDialog[1];
        SwingUtilities.invokeAndWait(() -> {
            JOptionPane pane = new JOptionPane(wrap(message), type,
                    type == JOptionPane.WARNING_MESSAGE ? JOptionPane.YES_NO_OPTION : JOptionPane.DEFAULT_OPTION);
            dlg[0] = pane.createDialog(null, title);
            dlg[0].setModal(false);
            dlg[0].setVisible(true);
        });
        settle();
        capture(dlg[0].getContentPane(), out.resolve(file));
        SwingUtilities.invokeAndWait(dlg[0]::dispose);
        lastTitle = null;
    }

    private static String wrap(String message) {
        return "<html><body style='width: 380px'>" + message.replace("&", "&amp;").replace("<", "&lt;")
                .replace("\n", "<br>") + "</body></html>";
    }

    private static void settle() throws Exception {
        for (int i = 0; i < 3; i++) {
            SwingUtilities.invokeAndWait(() -> Toolkit.getDefaultToolkit().sync());
            Thread.sleep(250);
        }
    }

    private static void capture(Component c, Path file) throws Exception {
        BufferedImage[] img = new BufferedImage[1];
        SwingUtilities.invokeAndWait(() -> {
            img[0] = new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = img[0].createGraphics();
            c.paint(g);
            g.dispose();
        });
        ImageIO.write(img[0], "png", file.toFile());
        System.out.println("  " + file.getFileName());
    }

    /** Draws the project tree (source, SQL, docs) as an image for the documentation. */
    private static void directoryTree(Path root, Path file) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("crm-pams/");
        walk(root, root, "", lines);
        Font font = new Font(Font.MONOSPACED, Font.PLAIN, 14);
        int lineH = 19;
        int width = 860;
        BufferedImage img = new BufferedImage(width, lines.size() * lineH + 30, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, img.getWidth(), img.getHeight());
        g.setColor(new Color(0x202124));
        g.setFont(font);
        for (int i = 0; i < lines.size(); i++) {
            g.drawString(lines.get(i), 16, 24 + i * lineH);
        }
        g.dispose();
        ImageIO.write(img, "png", file.toFile());
        System.out.println("  " + file.getFileName());
    }

    private static void walk(Path root, Path dir, String indent, List<String> lines) throws IOException {
        List<Path> children;
        try (Stream<Path> s = Files.list(dir)) {
            children = s.filter(p -> {
                String n = p.getFileName().toString();
                return !n.startsWith(".") && !n.equals("target") && !n.equals("screenshots") && !n.equals("logs")
                        && !n.endsWith(".pdf") && !n.endsWith(".docx");
            }).sorted(Comparator.comparing((Path p) -> !Files.isDirectory(p)).thenComparing(Path::toString)).toList();
        }
        for (int i = 0; i < children.size(); i++) {
            Path p = children.get(i);
            boolean last = i == children.size() - 1;
            lines.add(indent + (last ? "\\-- " : "|-- ") + p.getFileName() + (Files.isDirectory(p) ? "/" : ""));
            if (Files.isDirectory(p)) {
                walk(root, p, indent + (last ? "    " : "|   "), lines);
            }
        }
    }
}
