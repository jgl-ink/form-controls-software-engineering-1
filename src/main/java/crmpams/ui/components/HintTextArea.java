package crmpams.ui.components;

import java.awt.Graphics;
import java.awt.KeyboardFocusManager;
import javax.swing.JTextArea;

/**
 * A wrapping text area with placeholder text. Tab and Shift+Tab move focus to the next or
 * previous control instead of typing a tab character, so keyboard users are not trapped.
 */
public class HintTextArea extends JTextArea {

    private static final long serialVersionUID = 1L;
    private final String hint;

    public HintTextArea(String hint, int rows, int columns) {
        super(rows, columns);
        this.hint = hint;
        setLineWrap(true);
        setWrapStyleWord(true);
        setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
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
