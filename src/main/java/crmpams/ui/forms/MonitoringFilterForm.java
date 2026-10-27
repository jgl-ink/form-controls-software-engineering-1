package crmpams.ui.forms;

import crmpams.model.CurrentUser;
import crmpams.model.DeadlineFlag;
import crmpams.model.LookupItem;
import crmpams.model.MonitoringFilter;
import crmpams.model.MonitoringRow;
import crmpams.service.MonitoringOptions;
import crmpams.service.MonitoringService;
import crmpams.ui.UiTasks;
import crmpams.ui.UiTheme;
import crmpams.ui.components.FormPanel;
import crmpams.ui.components.HintTextField;
import crmpams.ui.components.InputFilters;
import crmpams.ui.components.RadioGroup;
import crmpams.ui.components.RowTableModel;
import crmpams.ui.components.RowTableModel.Column;
import crmpams.validation.FieldRules;
import crmpams.validation.Fields;
import crmpams.validation.ValidationResult;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Supporting Form 2: Monitoring Filter (Production Status Monitoring, Management Monitoring View,
 * IPO 9.1 to 9.4). Filters the active orders and flags each one with its deadline indicator.
 */
public final class MonitoringFilterForm extends FormPanel {

    private static final long serialVersionUID = 1L;
    public static final int DEFAULT_WARNING_DAYS = 3;

    /** One option of the deadline status radio group. flag null means all. */
    record DeadlineChoice(String label, DeadlineFlag flag) {
    }

    private final transient MonitoringService service;
    private final transient CurrentUser user;

    final HintTextField searchText = new HintTextField("Order ID or employee name", 20);
    final List<JCheckBox> stageBoxes = new ArrayList<>();
    private final transient List<LookupItem> stageItems;
    final JComboBox<LookupItem> employee = new JComboBox<>();
    final RadioGroup<DeadlineChoice> deadlineStatus;
    final JSlider warningDays = new JSlider(FieldRules.WARNING_DAYS_MIN, FieldRules.WARNING_DAYS_MAX,
            DEFAULT_WARNING_DAYS);
    final JLabel warningValue = new JLabel();
    final JButton applyButton;
    final RowTableModel<MonitoringRow> model;
    final JTable table;
    final JLabel stageCounts = FormPanel.helperLabel(" ");
    final JLabel resultCount = new JLabel(" ");

    public MonitoringFilterForm(MonitoringService service, CurrentUser user, MonitoringOptions options) {
        super("Production Monitoring", "See the stage and deadline of every active order without asking the "
                + "assigned employee. Click Apply Filters to refresh.", true);
        this.service = service;
        this.user = user;
        this.stageItems = options.activeStages();

        InputFilters.install(searchText, FieldRules.MONITOR_TEXT_MAX, InputFilters.SINGLE_LINE);
        employee.addItem(LookupItem.placeholder("All employees"));
        options.employees().forEach(employee::addItem);

        JPanel stagePanel = new JPanel(new GridLayout(0, 1, 0, 2));
        stagePanel.setOpaque(false);
        for (LookupItem s : stageItems) {
            JCheckBox box = new JCheckBox(s.name(), true);
            box.setOpaque(false);
            stageBoxes.add(box);
            stagePanel.add(box);
        }
        stagePanel.getAccessibleContext().setAccessibleName("Workflow stages");

        deadlineStatus = new RadioGroup<>(List.of(
                new DeadlineChoice("All", null),
                new DeadlineChoice(DeadlineFlag.PASSED.label(), DeadlineFlag.PASSED),
                new DeadlineChoice(DeadlineFlag.APPROACHING.label(), DeadlineFlag.APPROACHING),
                new DeadlineChoice(DeadlineFlag.ON_SCHEDULE.label(), DeadlineFlag.ON_SCHEDULE)),
                DeadlineChoice::label);
        deadlineStatus.setLayout(new GridLayout(0, 1, 0, 2));
        deadlineStatus.setSelected(allChoice());

        warningDays.setMajorTickSpacing(1);
        warningDays.setPaintTicks(true);
        warningDays.setSnapToTicks(true);
        Hashtable<Integer, JLabel> labels = new Hashtable<>();
        labels.put(FieldRules.WARNING_DAYS_MIN, new JLabel(String.valueOf(FieldRules.WARNING_DAYS_MIN)));
        labels.put(7, new JLabel("7"));
        labels.put(FieldRules.WARNING_DAYS_MAX, new JLabel(String.valueOf(FieldRules.WARNING_DAYS_MAX)));
        warningDays.setLabelTable(labels);
        warningDays.setPaintLabels(true);
        warningDays.setOpaque(false);
        warningDays.addChangeListener(e -> updateWarningLabel());
        java.awt.Dimension sliderSize = new java.awt.Dimension(260, warningDays.getPreferredSize().height);
        warningDays.setPreferredSize(sliderSize);
        warningDays.setMinimumSize(sliderSize);
        JPanel sliderRow = new JPanel(new BorderLayout(10, 0));
        sliderRow.setOpaque(false);
        sliderRow.add(warningDays, BorderLayout.CENTER);
        sliderRow.add(warningValue, BorderLayout.EAST);
        updateWarningLabel();

        useTwoColumns(280);
        addSection("Filters");
        addField(Fields.MONITOR_TEXT, "Order ID or employee", KeyEvent.VK_O, false,
                "Type an order ID for an exact match or part of an employee name. Leave blank for all.",
                searchText);
        addField(Fields.ASSIGNED_EMPLOYEE, "Assigned employee", KeyEvent.VK_E, false, null, employee);
        addField(Fields.STAGES, "Workflow stages", KeyEvent.VK_W, true,
                "Tick at least one. Released and Completed orders are not active.",
                stagePanel, stageBoxes.isEmpty() ? stagePanel : stageBoxes.get(0), stagePanel);
        deadlineStatus.getAccessibleContext().setAccessibleName("Deadline status");
        addField(Fields.DEADLINE_STATUS, "Deadline status", KeyEvent.VK_D, false,
                "Arrow keys change the choice.", deadlineStatus, deadlineStatus.firstButton(), deadlineStatus);
        addField(Fields.WARNING_DAYS, "Warning period", KeyEvent.VK_P, true,
                "Orders due within this many days are marked Approaching Deadline. Arrow keys change it.",
                sliderRow, warningDays, sliderRow);

        applyButton = addButton("Apply Filters", KeyEvent.VK_A, "Refresh the list with these filters (Enter).",
                this::submit);
        addButton("Reset Filters", KeyEvent.VK_R, "Go back to the default filters (Esc).", this::resetFilters);
        setDefaultButton(applyButton);
        setEscapeAction(this::resetFilters);

        model = new RowTableModel<>(List.<Column<MonitoringRow>>of(
                new Column<>("Order ID", Integer.class, MonitoringRow::orderId),
                new Column<>("Customer", String.class, MonitoringRow::customerName),
                new Column<>("Stage", String.class, MonitoringRow::stage),
                new Column<>("Assigned To", String.class, r -> Objects.requireNonNullElse(r.assignedTo(), "Not assigned")),
                new Column<>("Deadline", String.class, r -> r.deadline().toString()),
                new Column<>("Days Left", Long.class, MonitoringRow::daysLeft),
                new Column<>("Deadline Status", DeadlineFlag.class, MonitoringRow::flag),
                new Column<>("Last Update", String.class, r -> r.lastUpdate().format(SearchOrdersForm.DATE_TIME))));
        table = new JTable(model);
        SearchOrdersForm.setUpTable(table);
        SearchOrdersForm.widths(table, 70, 190, 170, 130, 90, 70, 150, 130);
        table.setDefaultRenderer(DeadlineFlag.class, new FlagRenderer());
        table.getAccessibleContext().setAccessibleName("Active orders");

        JLabel title = new JLabel("Active orders");
        title.setFont(UiTheme.section());
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(title, BorderLayout.WEST);
        top.add(resultCount, BorderLayout.EAST);
        JPanel counts = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        counts.setOpaque(false);
        counts.add(stageCounts);
        JPanel north = new JPanel(new BorderLayout(0, 4));
        north.setOpaque(false);
        north.add(top, BorderLayout.NORTH);
        north.add(counts, BorderLayout.SOUTH);
        JPanel results = new JPanel(new BorderLayout(0, 8));
        results.setOpaque(false);
        results.setBorder(new EmptyBorder(6, 0, 0, 0));
        results.add(north, BorderLayout.NORTH);
        results.add(new JScrollPane(table), BorderLayout.CENTER);
        setResultsPanel(results);
        registerResultsFocus(table);
    }

    private DeadlineChoice allChoice() {
        return new DeadlineChoice("All", null);
    }

    private void updateWarningLabel() {
        int d = warningDays.getValue();
        warningValue.setText(d + (d == 1 ? " day" : " days"));
        warningDays.getAccessibleContext().setAccessibleDescription(
                "Warning period " + warningValue.getText() + ". Orders due within this period are marked "
                + "Approaching Deadline.");
    }

    MonitoringFilter collectFilter() {
        Set<Integer> ids = new LinkedHashSet<>();
        for (int i = 0; i < stageBoxes.size(); i++) {
            if (stageBoxes.get(i).isSelected()) {
                ids.add(stageItems.get(i).id());
            }
        }
        LookupItem emp = (LookupItem) employee.getSelectedItem();
        DeadlineChoice choice = deadlineStatus.getSelected();
        return new MonitoringFilter(
                searchText.getText(),
                ids,
                emp == null || emp.isPlaceholder() ? null : emp.id(),
                choice == null ? null : choice.flag(),
                warningDays.getValue());
    }

    public void submit() {
        MonitoringFilter f = collectFilter();
        ValidationResult v = MonitoringService.validate(f);
        if (!v.isValid()) {
            showErrors(v);
            return;
        }
        clearErrors();
        applyButton.setEnabled(false);
        UiTasks.run(this, () -> service.load(user, f), result -> {
            if (!result.errors().isValid()) {
                showErrors(result.errors());
                return;
            }
            model.setRows(result.rows());
            int n = result.rows().size();
            resultCount.setText(n == 0 ? "No active orders match." : n + (n == 1 ? " order" : " orders"));
            stageCounts.setText(describeCounts(result.countByStage()));
            table.getAccessibleContext().setAccessibleDescription(resultCount.getText() + ". "
                    + stageCounts.getText());
        }, () -> applyButton.setEnabled(true));
    }

    private String describeCounts(Map<String, Long> counts) {
        if (counts.isEmpty()) {
            return "Orders per stage: none";
        }
        return "Orders per stage: " + stageItems.stream()
                .filter(s -> counts.containsKey(s.name()))
                .map(s -> s.name() + " " + counts.get(s.name()))
                .collect(Collectors.joining(", "));
    }

    public void resetFilters() {
        searchText.setText("");
        stageBoxes.forEach(b -> b.setSelected(true));
        employee.setSelectedIndex(0);
        deadlineStatus.setSelected(allChoice());
        warningDays.setValue(DEFAULT_WARNING_DAYS);
        clearErrors();
        focusFirstField();
    }

    /** Shows the deadline status as text on a light tinted background; the text carries the meaning. */
    private static final class FlagRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean selected, boolean focus,
                int row, int column) {
            super.getTableCellRendererComponent(t, value, selected, focus, row, column);
            if (!selected && value instanceof DeadlineFlag flag) {
                switch (flag) {
                    case PASSED -> colors(new Color(0xFDECEE), new Color(0x8A0012));
                    case APPROACHING -> colors(new Color(0xFFF4E5), new Color(0x6B3A00));
                    case ON_SCHEDULE -> colors(new Color(0xE8F5E9), new Color(0x1B5E20));
                    default -> colors(t.getBackground(), t.getForeground());
                }
            }
            return this;
        }

        private void colors(Color bg, Color fg) {
            setBackground(bg);
            setForeground(fg);
        }
    }
}
