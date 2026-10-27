package crmpams.ui.components;

import crmpams.ui.UiTheme;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.JTextArea;
import javax.swing.text.JTextComponent;

/** Paints placeholder text inside an empty text component. Uses only the Java standard library. */
final class HintPainter {

    private HintPainter() {
    }

    static void paint(JTextComponent c, Graphics g, String hint) {
        if (hint == null || hint.isEmpty() || c.getDocument().getLength() > 0) {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setColor(UiTheme.HINT);
            g2.setFont(c.getFont().deriveFont(Font.ITALIC));
            FontMetrics fm = g2.getFontMetrics();
            Insets in = c.getInsets();
            int y;
            if (c instanceof JTextArea) {
                y = in.top + fm.getAscent();
            } else {
                int inner = c.getHeight() - in.top - in.bottom;
                y = in.top + (inner - fm.getHeight()) / 2 + fm.getAscent();
            }
            g2.drawString(hint, in.left + 1, y);
        } finally {
            g2.dispose();
        }
    }
}
