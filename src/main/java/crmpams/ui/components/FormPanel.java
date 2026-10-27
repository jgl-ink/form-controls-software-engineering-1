package crmpams.ui.components;

import crmpams.ui.UiTheme;
import crmpams.validation.FieldError;
import crmpams.validation.ValidationResult;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.MatteBorder;
import javax.swing.text.JTextComponent;

/**
 * Base class of every form. It lays out each field as: label, input, helper text, and an error
 * line. It also provides the error summary at the top, the keyboard tab order, Enter for the
 * main button, and Escape for Cancel or Clear.
 *
 * Accessibility: each label is linked to its input with setLabelFor and has an Alt mnemonic;
 * each input gets an accessible name and description; errors are shown as text, a red border,
 * and an entry in the summary, so color is never the only signal.
 */
public abstract class FormPanel extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String SAVED_BORDER = "crmpams.savedBorder";

    private final JPanel fieldsPanel = new JPanel(new GridBagLayout());
    private final JPanel buttonBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    private final JPanel summaryPanel = new JPanel(new BorderLayout());
    private final JLabel summaryLabel = new JLabel();
    private final JPanel content = new JPanel(new BorderLayout());
    private final JPanel formArea = new JPanel(new BorderLayout());
    private final transient Map<String, FieldUi> fields = new LinkedHashMap<>();
    private final transient List<Component> fieldFocus = new ArrayList<>();
    private final transient List<Component> buttonFocus = new ArrayList<>();
    private final transient List<Component> resultFocus = new ArrayList<>();
    private int row;
    private int columns = 1;
    private int col;
    private int textWidth;
    private JButton defaultButton;
    private transient Runnable escapeAction;

    /** What the form remembers about one field. */
    private record FieldUi(String label, JComponent labelTarget, JComponent borderTarget, JLabel helper,
            JLabel error, JPanel cell, String helperText) {
    }

    protected FormPanel(String title, String intro, boolean showRequiredLegend) {
        super(new BorderLayout());
        setBorder(new EmptyBorder(16, 20, 16, 20));

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(UiTheme.heading());
        header.add(left(titleLabel));
        if (intro != null) {
            header.add(Box.createVerticalStrut(4));
            header.add(left(helperLabel(intro)));
        }
        if (showRequiredLegend) {
            header.add(Box.createVerticalStrut(2));
            header.add(left(helperLabel("Fields marked * are required.")));
        }
        summaryLabel.setForeground(UiTheme.ERROR);
        summaryLabel.setFont(UiTheme.base());
        summaryPanel.setBackground(UiTheme.ERROR_BACKGROUND);
        summaryPanel.setBorder(new CompoundBorder(new MatteBorder(0, 5, 0, 0, UiTheme.ERROR),
                new EmptyBorder(8, 12, 8, 12)));
        summaryPanel.add(summaryLabel, BorderLayout.CENTER);
        summaryPanel.setVisible(false);
        header.add(Box.createVerticalStrut(10));
        header.add(left(summaryPanel));
        add(header, BorderLayout.NORTH);

        fieldsPanel.setOpaque(false);
        fieldsPanel.setBorder(new EmptyBorder(6, 0, 6, 8));
        JPanel holder = new WidthTrackingPanel();
        holder.setOpaque(false);
        holder.add(fieldsPanel, BorderLayout.NORTH);
        JScrollPane scroll = new JScrollPane(holder);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        scroll.setViewportBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        formArea.setOpaque(false);
        formArea.add(scroll, BorderLayout.CENTER);
        buttonBar.setOpaque(false);
        buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
        formArea.add(buttonBar, BorderLayout.SOUTH);
        content.setOpaque(false);
        content.add(formArea, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        setFocusTraversalPolicyProvider(true);
        setFocusTraversalPolicy(new OrderedFocusPolicy(this::focusOrder));

        getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(KeyStroke.getKeyStroke("ESCAPE"), "form-escape");
        getActionMap().put("form-escape", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (escapeAction != null) {
                    escapeAction.run();
                }
            }
        });
    }

    private static JComponent left(JComponent c) {
        c.setAlignmentX(Component.LEFT_ALIGNMENT);
        return c;
    }

    public static JLabel helperLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(UiTheme.small());
        l.setForeground(UiTheme.HELPER);
        return l;
    }

    // ------------------------------------------------------------------ building the form

    /**
     * Lays the fields out in two columns (left then right, row by row) and wraps helper and error
     * text at the given width. Call before adding fields. Used by the filter forms so the results
     * table can use the full width below them.
     */
    protected final void useTwoColumns(int wrapWidthPx) {
        columns = 2;
        textWidth = wrapWidthPx;
    }

    /** Puts a results area (such as a table) below the fields in two-column mode, or to the right otherwise. */
    protected final void setResultsPanel(JComponent results) {
        content.remove(formArea);
        if (columns == 2) {
            formArea.setBorder(new EmptyBorder(0, 0, 12, 0));
            content.add(formArea, BorderLayout.NORTH);
        } else {
            formArea.setBorder(new EmptyBorder(0, 0, 0, 18));
            content.add(formArea, BorderLayout.WEST);
        }
        content.add(results, BorderLayout.CENTER);
    }

    private String wrap(String text) {
        if (textWidth <= 0 || text == null || text.isEmpty()) {
            return text;
        }
        return "<html><body style='width: " + textWidth + "px'>" + escape(text) + "</body></html>";
    }

    /** Places a label and a field cell in the next free slot of the grid. */
    private void place(JComponent label, JComponent cell) {
        int base = columns == 2 && col == 1 ? 2 : 0;
        GridBagConstraints gl = new GridBagConstraints();
        gl.gridx = base;
        gl.gridy = row;
        gl.anchor = GridBagConstraints.FIRST_LINE_START;
        gl.insets = new Insets(5, base == 0 ? 0 : 28, 12, 16);
        if (label != null) {
            fieldsPanel.add(label, gl);
        }
        GridBagConstraints gf = new GridBagConstraints();
        gf.gridx = base + 1;
        gf.gridy = row;
        gf.weightx = 1;
        gf.fill = GridBagConstraints.HORIZONTAL;
        gf.anchor = GridBagConstraints.FIRST_LINE_START;
        gf.insets = new Insets(0, 0, 12, 0);
        fieldsPanel.add(cell, gf);
        if (columns == 2 && col == 0) {
            col = 1;
        } else {
            col = 0;
            row++;
        }
    }

    /** In two-column mode, leaves the right-hand slot of the current row empty. */
    protected final void endRow() {
        if (col == 1) {
            col = 0;
            row++;
        }
    }

    protected final void addSection(String text) {
        if (col == 1) {
            col = 0;
            row++;
        }
        JLabel l = new JLabel(text);
        l.setFont(UiTheme.section());
        GridBagConstraints gc = new GridBagConstraints();
        gc.gridx = 0;
        gc.gridy = row++;
        gc.gridwidth = columns == 2 ? 4 : 2;
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(row == 1 ? 0 : 14, 0, 2, 0);
        fieldsPanel.add(l, gc);
        gc.gridy = row++;
        gc.insets = new Insets(0, 0, 8, 0);
        fieldsPanel.add(new JSeparator(), gc);
    }

    protected final void addField(String key, String label, int mnemonic, boolean required, String helper,
            JComponent input) {
        addField(key, label, mnemonic, required, helper, input, input, input);
    }

    /**
     * @param input        what is placed on the form
     * @param labelTarget  the control that receives focus from the label's Alt key
     * @param borderTarget the control that gets the red border when the field has an error
     */
    protected final void addField(String key, String label, int mnemonic, boolean required, String helper,
            JComponent input, JComponent labelTarget, JComponent borderTarget) {
        JLabel lbl = new JLabel(label + (required ? " *" : ""));
        if (mnemonic != 0) {
            lbl.setDisplayedMnemonic(mnemonic);
        }
        lbl.setLabelFor(labelTarget);

        JPanel cell = new JPanel(new GridBagLayout());
        cell.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.anchor = GridBagConstraints.WEST;
        c.fill = stretches(input) ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
        cell.add(input, c);
        JLabel help = helperLabel(helper == null ? "" : wrap(helper));
        help.setVisible(helper != null);
        c.gridy = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(3, 2, 0, 0);
        cell.add(help, c);
        JLabel err = new JLabel();
        err.setFont(UiTheme.smallBold());
        err.setForeground(UiTheme.ERROR);
        err.setVisible(false);
        c.gridy = 2;
        cell.add(err, c);
        place(lbl, cell);

        labelTarget.getAccessibleContext().setAccessibleName(label + (required ? ", required" : ", optional"));
        labelTarget.getAccessibleContext().setAccessibleDescription(describe(helper, labelTarget));
        if (helper != null && labelTarget.getToolTipText() == null) {
            labelTarget.setToolTipText(helper);
        }
        collectFocusables(input, fieldFocus);
        fields.put(key, new FieldUi(label, labelTarget, borderTarget, help, err, cell, helper));
    }

    /** Adds a row without its own validation, such as a status line or an inline button. */
    protected final void addRow(String label, JComponent comp) {
        place(label == null ? null : new JLabel(label), comp);
        collectFocusables(comp, fieldFocus);
    }

    protected final JButton addButton(String text, int mnemonic, String tooltip, Runnable action) {
        JButton b = new JButton(text);
        if (mnemonic != 0) {
            b.setMnemonic(mnemonic);
        }
        b.setToolTipText(tooltip);
        b.getAccessibleContext().setAccessibleDescription(tooltip);
        b.addActionListener(e -> action.run());
        if (buttonBar.getComponentCount() > 0) {
            buttonBar.add(Box.createHorizontalStrut(10));
        }
        buttonBar.add(b);
        buttonFocus.add(b);
        return b;
    }

    protected final void registerResultsFocus(Component... comps) {
        for (Component c : comps) {
            collectFocusables(c, resultFocus);
        }
    }

    protected final void setDefaultButton(JButton b) {
        defaultButton = b;
    }

    protected final void setEscapeAction(Runnable r) {
        escapeAction = r;
    }

    public final JButton defaultButton() {
        return defaultButton;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        JRootPane root = SwingUtilities.getRootPane(this);
        if (root != null) {
            root.setDefaultButton(defaultButton);
        }
    }

    /** Changes the helper line of a field, for example a live character count. */
    protected final void setHelper(String key, String text) {
        FieldUi f = fields.get(key);
        f.helper().setText(wrap(text));
        f.helper().setVisible(text != null && !text.isEmpty());
        if (!f.error().isVisible()) {
            f.labelTarget().getAccessibleContext().setAccessibleDescription(describe(text, f.labelTarget()));
        }
    }

    public final List<Component> focusOrder() {
        List<Component> all = new ArrayList<>(fieldFocus);
        all.addAll(buttonFocus);
        all.addAll(resultFocus);
        return all;
    }

    public final void focusFirstField() {
        SwingUtilities.invokeLater(() -> {
            Component first = getFocusTraversalPolicy().getFirstComponent(this);
            if (first != null) {
                first.requestFocusInWindow();
            }
        });
    }

    // ------------------------------------------------------------------ errors

    /** Shows each error under its field, lists all of them at the top, and focuses the first one. */
    public final void showErrors(ValidationResult result) {
        clearErrors();
        if (result == null || result.isValid()) {
            return;
        }
        List<String> lines = new ArrayList<>();
        FieldUi first = null;
        String general = result.messageFor(ValidationResult.GENERAL);
        if (general != null) {
            lines.add(escape(general));
        }
        for (FieldError e : result.errors()) {
            if (!fields.containsKey(e.field()) && !ValidationResult.GENERAL.equals(e.field())) {
                lines.add(escape(e.message()));
            }
        }
        for (Map.Entry<String, FieldUi> entry : fields.entrySet()) {
            String msg = result.messageFor(entry.getKey());
            if (msg == null) {
                continue;
            }
            FieldUi f = entry.getValue();
            f.error().setText(wrap("Error: " + msg));
            f.error().setVisible(true);
            JComponent target = f.borderTarget();
            target.putClientProperty(SAVED_BORDER, new SavedBorder(target.getBorder()));
            target.setBorder(new CompoundBorder(new LineBorder(UiTheme.ERROR, 2), target.getBorder()));
            f.labelTarget().getAccessibleContext().setAccessibleDescription(
                    "Error: " + msg + (f.helperText() == null ? "" : " " + f.helperText()));
            lines.add("<b>" + escape(f.label()) + ":</b> " + escape(msg));
            if (first == null) {
                first = f;
            }
        }
        int n = lines.size();
        StringBuilder html = new StringBuilder("<html><b>Please fix ")
                .append(n == 1 ? "this problem" : "these " + n + " problems")
                .append(" before continuing.</b>");
        StringBuilder plain = new StringBuilder("Please fix " + n + (n == 1 ? " problem." : " problems."));
        for (String line : lines) {
            html.append("<br>&bull; ").append(line);
            plain.append(' ').append(line.replaceAll("<[^>]+>", ""));
        }
        summaryLabel.setText(html.append("</html>").toString());
        summaryLabel.getAccessibleContext().setAccessibleName(plain.toString());
        summaryPanel.setVisible(true);
        revalidate();
        repaint();
        if (first != null) {
            FieldUi target = first;
            SwingUtilities.invokeLater(() -> {
                target.labelTarget().requestFocusInWindow();
                target.cell().scrollRectToVisible(new Rectangle(0, 0, target.cell().getWidth(),
                        target.cell().getHeight()));
            });
        }
    }

    public final void clearErrors() {
        for (FieldUi f : fields.values()) {
            f.error().setText("");
            f.error().setVisible(false);
            Object saved = f.borderTarget().getClientProperty(SAVED_BORDER);
            if (saved instanceof SavedBorder s) {
                f.borderTarget().setBorder(s.border());
                f.borderTarget().putClientProperty(SAVED_BORDER, null);
            }
            f.labelTarget().getAccessibleContext().setAccessibleDescription(
                    describe(f.helper().isVisible() ? f.helper().getText() : null, f.labelTarget()));
        }
        summaryPanel.setVisible(false);
        summaryLabel.setText("");
        revalidate();
        repaint();
    }

    public final boolean isSummaryShown() {
        return summaryPanel.isVisible();
    }

    /** The error text shown under a field, or null. Used by tests. */
    public final String shownError(String key) {
        FieldUi f = fields.get(key);
        return f != null && f.error().isVisible() ? f.error().getText() : null;
    }

    public final List<String> fieldKeys() {
        return new ArrayList<>(fields.keySet());
    }

    public final JComponent labelTargetOf(String key) {
        return fields.get(key).labelTarget();
    }

    // ------------------------------------------------------------------ helpers

    private record SavedBorder(Border border) {
    }

    /** Keeps the fields as wide as the visible area, so the form never scrolls sideways. */
    private static final class WidthTrackingPanel extends JPanel implements javax.swing.Scrollable {
        private static final long serialVersionUID = 1L;

        WidthTrackingPanel() {
            super(new BorderLayout());
        }

        @Override
        public java.awt.Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visible, int orientation, int direction) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visible, int orientation, int direction) {
            return Math.max(visible.height - 32, 16);
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return getParent() != null && getParent().getHeight() > getPreferredSize().height;
        }
    }

    private static boolean stretches(JComponent input) {
        return input instanceof JTextField || input instanceof JScrollPane || input instanceof JComboBox;
    }

    private static String describe(String helper, JComponent target) {
        String hint = null;
        if (target instanceof HintTextField h) {
            hint = h.getHint();
        } else if (target instanceof HintTextArea h) {
            hint = h.getHint();
        } else if (target instanceof HintPasswordField h) {
            hint = h.getHint();
        }
        if (helper == null || helper.isEmpty()) {
            return hint == null ? null : "Example: " + hint;
        }
        return hint == null ? helper : helper + " Example: " + hint;
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Finds the controls inside a component that can receive keyboard focus, in layout order. */
    static void collectFocusables(Component c, List<Component> out) {
        if (c instanceof JSpinner s) {
            if (s.getEditor() instanceof JSpinner.DefaultEditor de) {
                out.add(de.getTextField());
            }
            return;
        }
        if (c instanceof JScrollPane sp) {
            Component view = sp.getViewport().getView();
            if (view != null) {
                collectFocusables(view, out);
            }
            return;
        }
        if (c instanceof JTextComponent || c instanceof AbstractButton || c instanceof JComboBox
                || c instanceof JSlider || c instanceof JTable || c instanceof JList) {
            out.add(c);
            return;
        }
        if (c instanceof Container ct) {
            for (Component child : ct.getComponents()) {
                collectFocusables(child, out);
            }
        }
    }
}
