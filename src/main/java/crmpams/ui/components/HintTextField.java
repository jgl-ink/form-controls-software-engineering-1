package crmpams.ui.components;

import java.awt.Graphics;
import javax.swing.JTextField;

/** A text field that shows placeholder text while it is empty. */
public class HintTextField extends JTextField {

    private static final long serialVersionUID = 1L;
    private final String hint;

    public HintTextField(String hint, int columns) {
        super(columns);
        this.hint = hint;
    }

    public String getHint() {
        return hint;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        HintPainter.paint(this, g, hint);
    }
}
