package crmpams.ui.components;

import java.awt.Component;
import java.awt.Container;
import java.awt.FocusTraversalPolicy;
import java.util.List;
import java.util.function.Supplier;
import javax.swing.SwingUtilities;

/**
 * Tab order that follows the order in which the form added its controls: top to bottom,
 * then the buttons, then the results table. Disabled or hidden controls are skipped.
 */
public final class OrderedFocusPolicy extends FocusTraversalPolicy {

    private final Supplier<List<Component>> order;

    public OrderedFocusPolicy(Supplier<List<Component>> order) {
        this.order = order;
    }

    private static boolean accept(Component c) {
        return c.isShowing() && c.isVisible() && c.isEnabled() && c.isFocusable();
    }

    private static int indexOf(List<Component> list, Component c) {
        int i = list.indexOf(c);
        if (i >= 0) {
            return i;
        }
        for (int j = 0; j < list.size(); j++) {
            if (list.get(j) instanceof Container ct && SwingUtilities.isDescendingFrom(c, ct)) {
                return j;
            }
        }
        return -1;
    }

    @Override
    public Component getComponentAfter(Container root, Component current) {
        List<Component> list = order.get();
        int n = list.size();
        int start = indexOf(list, current);
        for (int k = 1; k <= n; k++) {
            Component c = list.get(Math.floorMod(start + k, n));
            if (accept(c)) {
                return c;
            }
        }
        return null;
    }

    @Override
    public Component getComponentBefore(Container root, Component current) {
        List<Component> list = order.get();
        int n = list.size();
        int start = indexOf(list, current);
        if (start < 0) {
            start = 0;
        }
        for (int k = 1; k <= n; k++) {
            Component c = list.get(Math.floorMod(start - k, n));
            if (accept(c)) {
                return c;
            }
        }
        return null;
    }

    @Override
    public Component getFirstComponent(Container root) {
        return order.get().stream().filter(OrderedFocusPolicy::accept).findFirst().orElse(null);
    }

    @Override
    public Component getLastComponent(Container root) {
        List<Component> list = order.get();
        for (int i = list.size() - 1; i >= 0; i--) {
            if (accept(list.get(i))) {
                return list.get(i);
            }
        }
        return null;
    }

    @Override
    public Component getDefaultComponent(Container root) {
        return getFirstComponent(root);
    }
}
