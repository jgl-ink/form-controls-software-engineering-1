package crmpams.ui.components;

import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.KeyStroke;
import javax.swing.border.EmptyBorder;

/**
 * A row of radio buttons for one choice. Arrow keys move between the options and select them,
 * the same way radio groups behave in most desktop applications.
 */
public final class RadioGroup<T> extends JPanel {

    private static final long serialVersionUID = 1L;
    private final transient ButtonGroup group = new ButtonGroup();
    private final List<JRadioButton> buttons = new ArrayList<>();
    private final transient List<T> values = new ArrayList<>();

    public RadioGroup(List<T> items, Function<T, String> labeler) {
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setOpaque(false);
        for (T item : items) {
            JRadioButton b = new JRadioButton(labeler.apply(item));
            b.setOpaque(false);
            b.setBorder(new EmptyBorder(2, 0, 2, 18));
            group.add(b);
            buttons.add(b);
            values.add(item);
            add(b);
        }
        for (int i = 0; i < buttons.size(); i++) {
            int index = i;
            JRadioButton b = buttons.get(i);
            bind(b, "LEFT", "radio-prev", () -> moveSelection(index, -1));
            bind(b, "UP", "radio-prev", () -> moveSelection(index, -1));
            bind(b, "RIGHT", "radio-next", () -> moveSelection(index, 1));
            bind(b, "DOWN", "radio-next", () -> moveSelection(index, 1));
        }
    }

    private static void bind(JComponent c, String key, String name, Runnable r) {
        c.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(key), name);
        c.getActionMap().put(name, new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                r.run();
            }
        });
    }

    private void moveSelection(int from, int delta) {
        int n = buttons.size();
        JRadioButton target = buttons.get((from + delta + n) % n);
        target.requestFocusInWindow();
        target.doClick(0);
    }

    public T getSelected() {
        for (int i = 0; i < buttons.size(); i++) {
            if (buttons.get(i).isSelected()) {
                return values.get(i);
            }
        }
        return null;
    }

    public void setSelected(T value) {
        int i = values.indexOf(value);
        if (i >= 0) {
            buttons.get(i).setSelected(true);
        }
    }

    public void clearSelection() {
        group.clearSelection();
    }

    public List<JRadioButton> buttons() {
        return Collections.unmodifiableList(buttons);
    }

    public JRadioButton firstButton() {
        return buttons.get(0);
    }

    public void addActionListener(ActionListener l) {
        buttons.forEach(b -> b.addActionListener(l));
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        buttons.forEach(b -> b.setEnabled(enabled));
    }
}
