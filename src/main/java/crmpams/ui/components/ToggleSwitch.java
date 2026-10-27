package crmpams.ui.components;

import crmpams.ui.UiTheme;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JToggleButton;

/**
 * An on or off switch. It is a JToggleButton underneath, so Space toggles it and screen readers
 * report it as a toggle button. The text beside the switch always states the current value,
 * so the state is not shown by color alone.
 */
public class ToggleSwitch extends JToggleButton {

    private static final long serialVersionUID = 1L;
    private static final int TRACK_W = 46;
    private static final int TRACK_H = 24;
    private final String onText;
    private final String offText;

    public ToggleSwitch(String onText, String offText, boolean selected) {
        super(selected ? onText : offText, selected);
        this.onText = onText;
        this.offText = offText;
        setOpaque(false);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        addItemListener(e -> {
            setText(isSelected() ? this.onText : this.offText);
            repaint();
        });
    }

    @Override
    public Dimension getPreferredSize() {
        FontMetrics fm = getFontMetrics(getFont());
        int textW = Math.max(fm.stringWidth(onText), fm.stringWidth(offText));
        return new Dimension(TRACK_W + 14 + textW + 6, Math.max(TRACK_H + 6, fm.getHeight() + 6));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            int x = 3;
            int y = (getHeight() - TRACK_H) / 2;
            Color track = isSelected() ? UiTheme.SWITCH_ON : UiTheme.SWITCH_OFF;
            if (!isEnabled()) {
                track = track.brighter();
            }
            g2.setColor(track);
            g2.fillRoundRect(x, y, TRACK_W, TRACK_H, TRACK_H, TRACK_H);
            int knob = TRACK_H - 6;
            int knobX = isSelected() ? x + TRACK_W - knob - 3 : x + 3;
            g2.setColor(Color.WHITE);
            g2.fillOval(knobX, y + 3, knob, knob);
            if (hasFocus()) {
                g2.setColor(UiTheme.FOCUS);
                g2.setStroke(new BasicStroke(2f));
                g2.drawRoundRect(x - 2, y - 2, TRACK_W + 4, TRACK_H + 4, TRACK_H + 4, TRACK_H + 4);
            }
            g2.setFont(getFont());
            g2.setColor(isEnabled() ? getForeground() : UiTheme.HINT);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(getText(), x + TRACK_W + 12, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
        } finally {
            g2.dispose();
        }
    }

    @Override
    protected void paintBorder(Graphics g) {
        // The switch draws its own focus ring.
    }
}
