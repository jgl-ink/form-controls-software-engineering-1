package crmpams.ui;

import java.awt.Component;
import javax.swing.JOptionPane;

/**
 * Message and confirmation dialogs. Tests and the screenshot tool replace the handler
 * so that no dialog blocks them.
 */
public final class Dialogs {

    public interface Handler {
        void info(Component parent, String title, String message);

        void error(Component parent, String title, String message);

        boolean confirm(Component parent, String title, String message);
    }

    private static Handler handler = new SwingHandler();

    private Dialogs() {
    }

    public static void setHandler(Handler h) {
        handler = h;
    }

    public static void info(Component parent, String title, String message) {
        handler.info(parent, title, message);
    }

    public static void error(Component parent, String title, String message) {
        handler.error(parent, title, message);
    }

    public static boolean confirm(Component parent, String title, String message) {
        return handler.confirm(parent, title, message);
    }

    /** Wraps long text so the dialog does not become one very wide line. */
    static String wrap(String message) {
        String safe = message.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\n", "<br>");
        return "<html><body style='width: 380px'>" + safe + "</body></html>";
    }

    private static final class SwingHandler implements Handler {
        @Override
        public void info(Component parent, String title, String message) {
            JOptionPane.showMessageDialog(parent, wrap(message), title, JOptionPane.INFORMATION_MESSAGE);
        }

        @Override
        public void error(Component parent, String title, String message) {
            JOptionPane.showMessageDialog(parent, wrap(message), title, JOptionPane.ERROR_MESSAGE);
        }

        @Override
        public boolean confirm(Component parent, String title, String message) {
            return JOptionPane.showConfirmDialog(parent, wrap(message), title, JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE) == JOptionPane.YES_OPTION;
        }
    }
}
