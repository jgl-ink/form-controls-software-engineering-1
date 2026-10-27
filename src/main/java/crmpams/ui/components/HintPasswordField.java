package crmpams.ui.components;

import java.awt.Graphics;
import javax.swing.JPasswordField;

/** A password field with placeholder text and a show or hide option. */
public class HintPasswordField extends JPasswordField {

    private static final long serialVersionUID = 1L;
    private final String hint;
    private final char hiddenEchoChar;

    public HintPasswordField(String hint, int columns) {
        super(columns);
        this.hint = hint;
        this.hiddenEchoChar = super.getEchoChar();
    }

    public String getHint() {
        return hint;
    }

    public void setShowPassword(boolean show) {
        setEchoChar(show ? (char) 0 : hiddenEchoChar);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        HintPainter.paint(this, g, hint);
    }
}
