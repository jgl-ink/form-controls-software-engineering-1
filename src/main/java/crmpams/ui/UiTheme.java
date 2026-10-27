package crmpams.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import javax.swing.UIManager;

/**
 * Colors and fonts shared by every screen. Text colors were chosen to keep a contrast ratio of
 * at least 4.5:1 against the light background. The base font is 14 points for readability.
 */
public final class UiTheme {

    public static final Color SURFACE = new Color(0xF4F5F7);
    public static final Color ERROR = new Color(0xB00020);
    public static final Color ERROR_BACKGROUND = new Color(0xFDECEE);
    public static final Color HELPER = new Color(0x4A4A4A);
    public static final Color HINT = new Color(0x6B6B6B);
    public static final Color SUCCESS = new Color(0x1B5E20);
    public static final Color FOCUS = new Color(0x1565C0);
    public static final Color SWITCH_ON = new Color(0x2E7D32);
    public static final Color SWITCH_OFF = new Color(0x8A8A8A);

    public static final int BASE_SIZE = 14;
    public static final int SMALL_SIZE = 13;

    private static String family = Font.SANS_SERIF;

    private UiTheme() {
    }

    /** Sets the Nimbus look and feel with the shared font. Call once before any window is created. */
    public static void install() {
        UIManager.put("control", SURFACE);
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ReflectiveOperationException | javax.swing.UnsupportedLookAndFeelException e) {
            // Keep the default look and feel.
        }
        if (!GraphicsEnvironment.isHeadless()) {
            String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
            if (Arrays.asList(names).contains("Segoe UI")) {
                family = "Segoe UI";
            }
        }
        UIManager.getLookAndFeelDefaults().put("defaultFont", base());
    }

    public static Font base() {
        return new Font(family, Font.PLAIN, BASE_SIZE);
    }

    public static Font small() {
        return new Font(family, Font.PLAIN, SMALL_SIZE);
    }

    public static Font smallBold() {
        return new Font(family, Font.BOLD, SMALL_SIZE);
    }

    public static Font section() {
        return new Font(family, Font.BOLD, 15);
    }

    public static Font heading() {
        return new Font(family, Font.BOLD, 20);
    }
}
