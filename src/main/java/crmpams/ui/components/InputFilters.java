package crmpams.ui.components;

import java.util.function.IntPredicate;
import javax.swing.UIManager;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;

/**
 * Document filters that stop input beyond the column length and drop characters a field can
 * never accept. Format rules (such as the contact number pattern) are still checked on submit,
 * so a clear message can be shown.
 */
public final class InputFilters {

    /** Any printable character; new lines allowed (for text areas). */
    public static final IntPredicate TEXT = cp -> cp == '\n' || !Character.isISOControl(cp);
    /** Any printable character on one line. */
    public static final IntPredicate SINGLE_LINE = cp -> !Character.isISOControl(cp);
    /** Digits and the plus sign of +63. */
    public static final IntPredicate PHONE = cp -> (cp >= '0' && cp <= '9') || cp == '+';
    /** Characters allowed in a username. */
    public static final IntPredicate USERNAME = cp -> cp < 128 && (Character.isLetterOrDigit(cp) || cp == '.' || cp == '_');

    private InputFilters() {
    }

    public static void install(JTextComponent c, int maxLength, IntPredicate allowed) {
        ((AbstractDocument) c.getDocument()).setDocumentFilter(new Filter(c, maxLength, allowed));
    }

    private static final class Filter extends DocumentFilter {
        private final JTextComponent owner;
        private final int max;
        private final IntPredicate allowed;

        Filter(JTextComponent owner, int max, IntPredicate allowed) {
            this.owner = owner;
            this.max = max;
            this.allowed = allowed;
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr)
                throws BadLocationException {
            replace(fb, offset, 0, text, attr);
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs)
                throws BadLocationException {
            String in = text == null ? "" : text;
            StringBuilder kept = new StringBuilder(in.length());
            in.codePoints().filter(allowed).forEach(kept::appendCodePoint);
            int room = max - (fb.getDocument().getLength() - length);
            String out = kept.length() > Math.max(room, 0) ? kept.substring(0, Math.max(room, 0)) : kept.toString();
            if (out.length() < in.length()) {
                UIManager.getLookAndFeel().provideErrorFeedback(owner);
            }
            super.replace(fb, offset, length, out, attrs);
        }
    }
}
