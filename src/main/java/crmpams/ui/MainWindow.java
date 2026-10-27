package crmpams.ui;

import crmpams.AppContext;
import crmpams.model.CurrentUser;
import crmpams.model.Role;
import crmpams.ui.components.FormPanel;
import crmpams.ui.forms.MonitoringFilterForm;
import crmpams.ui.forms.NewOrderForm;
import crmpams.ui.forms.RegisterEmployeeForm;
import crmpams.ui.forms.SearchOrdersForm;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDate;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;

/**
 * Main window: navigation on the left and the working area on the right (Chapter 1, Scope).
 * The menu shown depends on the role (Figures 2 to 4). Ctrl+1 to Ctrl+5 open the menu items.
 */
public final class MainWindow extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = Logger.getLogger(MainWindow.class.getName());

    private final transient AppContext ctx;
    private final transient CurrentUser user;
    private final transient Runnable onLogout;
    private final JPanel workArea = new JPanel(new BorderLayout());
    private final JPanel nav = new JPanel(new GridLayout(0, 1, 0, 8));
    private int shortcut = 1;

    public MainWindow(AppContext ctx, CurrentUser user, Runnable onLogout) {
        super("CRM-PAMS | CADDSolutions Cubao");
        this.ctx = ctx;
        this.user = user;
        this.onLogout = onLogout;
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 700));
        setSize(1320, 760);
        setLocationRelativeTo(null);

        JLabel who = new JLabel("Logged in as " + user.fullName() + " (" + user.role().label() + ")");
        who.setFont(UiTheme.smallBold());
        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(new CompoundEmpty(new MatteBorder(0, 0, 1, 0, UiTheme.HINT), 8, 16));
        JLabel appName = new JLabel("CRM-PAMS");
        appName.setFont(UiTheme.section());
        top.add(appName, BorderLayout.WEST);
        top.add(who, BorderLayout.EAST);

        nav.setOpaque(false);
        navButton("Home", this::showHome);
        if (user.role() == Role.OPERATIONS_MANAGER || user.role() == Role.OFFICER_IN_CHARGE) {
            navButton("New Order", this::openNewOrder);
            navButton("Search Orders", this::openSearch);
            navButton("Production Monitoring", this::openMonitoring);
        }
        if (user.role() == Role.OPERATIONS_MANAGER) {
            navButton("Register Employee", this::openRegistration);
        }
        JButton logout = new JButton("Log Out");
        logout.addActionListener(e -> logout());
        JPanel side = new JPanel();
        side.setLayout(new BoxLayout(side, BoxLayout.Y_AXIS));
        side.setBorder(new CompoundEmpty(new MatteBorder(0, 0, 0, 1, UiTheme.HINT), 16, 14));
        nav.setAlignmentX(LEFT_ALIGNMENT);
        nav.setMaximumSize(new Dimension(Integer.MAX_VALUE, nav.getPreferredSize().height));
        side.add(nav);
        side.add(Box.createVerticalGlue());
        logout.setAlignmentX(LEFT_ALIGNMENT);
        side.add(logout);
        JPanel sideHolder = new JPanel(new BorderLayout());
        sideHolder.add(side, BorderLayout.CENTER);
        sideHolder.setPreferredSize(new Dimension(220, 10));

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(sideHolder, BorderLayout.WEST);
        getContentPane().add(workArea, BorderLayout.CENTER);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                try {
                    ctx.auth().logout(user);
                } catch (Exception ex) {
                    LOG.log(Level.WARNING, "Could not record logout", ex);
                }
                System.exit(0);
            }
        });
        showHome();
    }

    private void navButton(String text, Runnable action) {
        JButton b = new JButton(text);
        b.setHorizontalAlignment(JButton.LEFT);
        b.addActionListener(e -> action.run());
        if (shortcut <= 9) {
            String key = "ctrl " + shortcut;
            b.setToolTipText(text + " (Ctrl+" + shortcut + ")");
            getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(key), key);
            getRootPane().getActionMap().put(key, new AbstractAction() {
                private static final long serialVersionUID = 1L;

                @Override
                public void actionPerformed(ActionEvent e) {
                    action.run();
                }
            });
            shortcut++;
        }
        nav.add(b);
    }

    private void show(JComponent c) {
        workArea.removeAll();
        workArea.add(c, BorderLayout.CENTER);
        workArea.revalidate();
        workArea.repaint();
        if (c instanceof FormPanel f) {
            getRootPane().setDefaultButton(f.defaultButton());
            f.focusFirstField();
        } else {
            getRootPane().setDefaultButton(null);
        }
    }

    public void showHome() {
        JPanel home = new JPanel();
        home.setLayout(new BoxLayout(home, BoxLayout.Y_AXIS));
        home.setBorder(new EmptyBorder(24, 24, 24, 24));
        JLabel title = new JLabel("Welcome, " + user.fullName());
        title.setFont(UiTheme.heading());
        home.add(title);
        home.add(Box.createVerticalStrut(8));
        home.add(FormPanel.helperLabel("Today is " + LocalDate.now() + ". Choose a menu item on the left, "
                + "or press Ctrl and the item number."));
        if (user.role() == Role.PRODUCTION_STAFF) {
            home.add(Box.createVerticalStrut(8));
            home.add(FormPanel.helperLabel("Your task list and status updates are built in a later sprint."));
        }
        show(home);
    }

    private void openNewOrder() {
        UiTasks.run(this, () -> ctx.orderIntake().loadFormData(user),
                data -> show(new NewOrderForm(ctx.orderIntake(), user, data, this::showHome)));
    }

    private void openSearch() {
        UiTasks.run(this, () -> ctx.search().loadOptions(user), options -> {
            SearchOrdersForm form = new SearchOrdersForm(ctx.search(), user, options);
            show(form);
            form.submit();
        });
    }

    private void openMonitoring() {
        UiTasks.run(this, () -> ctx.monitoring().loadOptions(user), options -> {
            MonitoringFilterForm form = new MonitoringFilterForm(ctx.monitoring(), user, options);
            show(form);
            form.submit();
        });
    }

    private void openRegistration() {
        show(new RegisterEmployeeForm(ctx.registration(), user, this::showHome));
    }

    private void logout() {
        UiTasks.run(this, () -> {
            ctx.auth().logout(user);
            return Boolean.TRUE;
        }, ok -> {
            dispose();
            SwingUtilities.invokeLater(onLogout);
        });
    }

    /** A border plus inner padding. */
    private static final class CompoundEmpty extends javax.swing.border.CompoundBorder {
        private static final long serialVersionUID = 1L;

        CompoundEmpty(javax.swing.border.Border outer, int vertical, int horizontal) {
            super(outer, BorderFactory.createEmptyBorder(vertical, horizontal, vertical, horizontal));
        }
    }
}
