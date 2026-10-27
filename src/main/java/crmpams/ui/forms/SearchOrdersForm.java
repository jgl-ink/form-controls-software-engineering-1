package crmpams.ui.forms;

import crmpams.model.CurrentUser;
import crmpams.model.LookupItem;
import crmpams.model.OrderLine;
import crmpams.model.OrderSearchCriteria;
import crmpams.model.OrderSummary;
import crmpams.model.UrgencyLevel;
import crmpams.service.SearchOptions;
import crmpams.service.SearchService;
import crmpams.ui.Dialogs;
import crmpams.ui.UiTasks;
import crmpams.ui.UiTheme;
import crmpams.ui.components.DateSpinner;
import crmpams.ui.components.FormPanel;
import crmpams.ui.components.HintTextField;
import crmpams.ui.components.InputFilters;
import crmpams.ui.components.RowTableModel;
import crmpams.ui.components.RowTableModel.Column;
import crmpams.validation.FieldRules;
import crmpams.validation.Fields;
import crmpams.validation.ValidationResult;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

/**
 * Supporting Form 1: Search Orders (Search 12.0, IPO 12.1 to 12.3).
 * Builds one parameterized query from the search text and filters, and opens a selected order.
 */
public final class SearchOrdersForm extends FormPanel {

    private static final long serialVersionUID = 1L;
    static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final transient SearchService service;
    private final transient CurrentUser user;
    private final transient List<LookupItem> stages;

    final HintTextField searchText = new HintTextField("Order ID, customer name, or description", 24);
    final JComboBox<LookupItem> projectType = new JComboBox<>();
    final JComboBox<LookupItem> status = new JComboBox<>();
    final JComboBox<LookupItem> employee = new JComboBox<>();
    final JComboBox<LookupItem> urgency = new JComboBox<>();
    final JCheckBox useDateRange = new JCheckBox("Filter by date received");
    final DateSpinner dateFrom;
    final DateSpinner dateTo;
    final JCheckBox includeClosed = new JCheckBox("Include released and completed orders");
    final JButton searchButton;
    final RowTableModel<OrderSummary> model;
    final JTable table;
    final JLabel resultCount = new JLabel(" ");
    final JButton viewButton = new JButton("View Selected Order");

    public SearchOrdersForm(SearchService service, CurrentUser user, SearchOptions options) {
        super("Search Orders", "Find an order by text, filters, or both. Leave everything blank to list open orders.",
                false);
        this.service = service;
        this.user = user;
        this.stages = options.stages();
        LocalDate today = service.today();
        dateFrom = new DateSpinner(today.minusDays(30));
        dateTo = new DateSpinner(today);

        fill(projectType, "All project types", options.projectTypes());
        fill(status, "All statuses", options.stages());
        fill(employee, "All employees", options.employees());
        fill(urgency, "All urgency levels", options.urgencyLevels().stream()
                .map(u -> new LookupItem(u.id(), u.name())).toList());
        InputFilters.install(searchText, FieldRules.SEARCH_TEXT_MAX, InputFilters.SINGLE_LINE);

        useTwoColumns(300);
        addSection("Search criteria");
        addField(Fields.SEARCH_TEXT, "Search text", KeyEvent.VK_T, false,
                "Partial match on order ID, customer name, or project description. Up to 100 characters.",
                searchText);
        addField(Fields.PROJECT_TYPE, "Project type", KeyEvent.VK_P, false, null, projectType);
        addField(Fields.STATUS, "Status", KeyEvent.VK_U, false, null, status);
        addField(Fields.ASSIGNED_EMPLOYEE, "Assigned employee", KeyEvent.VK_E, false, null, employee);
        addField(Fields.URGENCY, "Urgency level", KeyEvent.VK_G, false, null, urgency);
        includeClosed.setMnemonic(KeyEvent.VK_I);
        includeClosed.setOpaque(false);
        addRow(null, includeClosed);

        useDateRange.setMnemonic(KeyEvent.VK_D);
        useDateRange.setOpaque(false);
        useDateRange.addActionListener(e -> updateDateState());
        addRow(null, useDateRange);
        endRow();
        addField(Fields.DATE_FROM, "Received from", KeyEvent.VK_R, false,
                "YYYY-MM-DD, not later than today.", dateFrom, dateFrom.textField(), dateFrom);
        addField(Fields.DATE_TO, "Received to", KeyEvent.VK_O, false,
                "YYYY-MM-DD, not earlier than the start date.", dateTo, dateTo.textField(), dateTo);
        updateDateState();

        searchButton = addButton("Search Orders", KeyEvent.VK_S, "Run the search (Enter).", this::submit);
        addButton("Reset Filters", KeyEvent.VK_L, "Clear the search text and filters (Esc).", this::resetFilters);
        setDefaultButton(searchButton);
        setEscapeAction(this::resetFilters);

        model = new RowTableModel<>(List.<Column<OrderSummary>>of(
                new Column<>("Order ID", Integer.class, OrderSummary::orderId),
                new Column<>("Date Received", String.class, o -> o.dateReceived().format(DATE_TIME)),
                new Column<>("Customer", String.class, OrderSummary::customerName),
                new Column<>("Project Types", String.class, o -> Objects.requireNonNullElse(o.projectTypes(), "")),
                new Column<>("Deadline", String.class, o -> o.deadline().toString()),
                new Column<>("Urgency", String.class, OrderSummary::urgency),
                new Column<>("Status", String.class, OrderSummary::stage),
                new Column<>("Assigned To", String.class, o -> Objects.requireNonNullElse(o.assignedTo(), "Not assigned"))));
        table = new JTable(model);
        setUpTable(table);
        widths(table, 70, 130, 190, 170, 90, 75, 110, 150);
        table.getAccessibleContext().setAccessibleName("Search results");
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    viewSelected();
                }
            }
        });
        table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
                .put(KeyStroke.getKeyStroke("ENTER"), "view-order");
        table.getActionMap().put("view-order", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                viewSelected();
            }
        });

        viewButton.setMnemonic(KeyEvent.VK_V);
        viewButton.setToolTipText("Open the full record of the selected order (Enter on a row, or double-click).");
        viewButton.addActionListener(e -> viewSelected());
        JLabel resultsTitle = new JLabel("Results");
        resultsTitle.setFont(UiTheme.section());
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(resultsTitle, BorderLayout.WEST);
        top.add(resultCount, BorderLayout.EAST);
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bottom.setOpaque(false);
        bottom.add(viewButton);
        JPanel results = new JPanel(new BorderLayout(0, 8));
        results.setOpaque(false);
        results.setBorder(new EmptyBorder(6, 0, 0, 0));
        results.add(top, BorderLayout.NORTH);
        results.add(new JScrollPane(table), BorderLayout.CENTER);
        results.add(bottom, BorderLayout.SOUTH);
        setResultsPanel(results);
        registerResultsFocus(table, viewButton);
    }

    static void setUpTable(JTable t) {
        t.setAutoCreateRowSorter(true);
        t.setFillsViewportHeight(true);
        t.setRowHeight(26);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        // Tab leaves the table instead of moving between cells; arrow keys still move between rows.
        t.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        t.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
    }

    /** Sets the starting column widths; the table still fills the available width. */
    static void widths(JTable t, int... w) {
        for (int i = 0; i < w.length && i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setPreferredWidth(w[i]);
        }
    }

    private static void fill(JComboBox<LookupItem> combo, String allText, List<LookupItem> items) {
        combo.addItem(LookupItem.placeholder(allText));
        items.forEach(combo::addItem);
    }

    private static Integer idOf(JComboBox<LookupItem> combo) {
        LookupItem item = (LookupItem) combo.getSelectedItem();
        return item == null || item.isPlaceholder() ? null : item.id();
    }

    private void updateDateState() {
        boolean on = useDateRange.isSelected();
        dateFrom.setEnabled(on);
        dateTo.setEnabled(on);
    }

    OrderSearchCriteria collectCriteria() {
        boolean range = useDateRange.isSelected();
        return new OrderSearchCriteria(
                searchText.getText(),
                idOf(projectType),
                idOf(status),
                idOf(employee),
                idOf(urgency),
                range,
                range ? dateFrom.readDate() : null,
                range ? dateTo.readDate() : null,
                includeClosed.isSelected());
    }

    ValidationResult validateForm(OrderSearchCriteria k) {
        LookupItem st = (LookupItem) status.getSelectedItem();
        String stageName = st == null || st.isPlaceholder() ? null : st.name();
        ValidationResult v = SearchService.validate(k, stageName, service.today());
        if (k.useDateRange() && dateFrom.hasInvalidText()) {
            v.put(Fields.DATE_FROM, "Enter the start date as YYYY-MM-DD, for example " + service.today() + ".");
        }
        if (k.useDateRange() && dateTo.hasInvalidText()) {
            v.put(Fields.DATE_TO, "Enter the end date as YYYY-MM-DD, for example " + service.today() + ".");
        }
        return v;
    }

    public void submit() {
        OrderSearchCriteria k = collectCriteria();
        ValidationResult v = validateForm(k);
        if (!v.isValid()) {
            showErrors(v);
            return;
        }
        clearErrors();
        searchButton.setEnabled(false);
        UiTasks.run(this, () -> service.search(user, k), result -> {
            if (!result.errors().isValid()) {
                showErrors(result.errors());
                return;
            }
            model.setRows(result.rows());
            int n = result.rows().size();
            resultCount.setText(n == 0 ? "No orders match. Try fewer filters."
                    : result.limitReached() ? "Showing the first " + n + " orders. Add filters to narrow the list."
                    : n + (n == 1 ? " order found." : " orders found."));
            table.getAccessibleContext().setAccessibleDescription(resultCount.getText());
        }, () -> searchButton.setEnabled(true));
    }

    public void resetFilters() {
        searchText.setText("");
        projectType.setSelectedIndex(0);
        status.setSelectedIndex(0);
        employee.setSelectedIndex(0);
        urgency.setSelectedIndex(0);
        useDateRange.setSelected(false);
        dateFrom.setDate(service.today().minusDays(30));
        dateTo.setDate(service.today());
        includeClosed.setSelected(false);
        updateDateState();
        clearErrors();
        focusFirstField();
    }

    /** IPO 12.3: shows the full record of the selected order, including its order lines. */
    void viewSelected() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            Dialogs.info(this, "No order selected", "Select an order in the results first.");
            return;
        }
        OrderSummary o = model.rowAt(table.convertRowIndexToModel(viewRow));
        UiTasks.run(this, () -> service.loadOrderLines(user, o.orderId()), lines -> {
            StringBuilder text = new StringBuilder()
                    .append("J.O. #: ").append(o.orderId())
                    .append("\nStatus: ").append(o.stage())
                    .append("\nDate and time received: ").append(o.dateReceived().format(DATE_TIME))
                    .append("\nCustomer: ").append(o.customerName()).append(" (").append(o.contactNumber()).append(")")
                    .append("\nProject description: ").append(o.projectDescription())
                    .append("\nOrder lines:");
            for (int i = 0; i < lines.size(); i++) {
                text.append("\n   ").append(i + 1).append(". ").append(lines.get(i).describe());
            }
            int sheets = lines.stream().mapToInt(OrderLine::totalSheets).sum();
            text.append("\nTotal no. of sheets: ").append(sheets)
                    .append("\nDeadline: ").append(o.deadline())
                    .append("\nUrgency: ").append(o.urgency())
                    .append("\nDelivery address: ").append(Objects.requireNonNullElse(o.deliveryAddress(), "None (pick-up)"))
                    .append("\nCAD operator: ").append(Objects.requireNonNullElse(o.assignedTo(), "Not assigned"))
                    .append("\nSpecial instructions: ").append(Objects.requireNonNullElse(o.specialInstructions(), "None"));
            Dialogs.info(this, "J.O. # " + o.orderId(), text.toString());
        });
    }

    List<LookupItem> stages() {
        return stages;
    }
}
