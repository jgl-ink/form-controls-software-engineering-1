package crmpams.ui.forms;

import crmpams.model.Customer;
import crmpams.model.CurrentUser;
import crmpams.model.LookupItem;
import crmpams.model.OrderLine;
import crmpams.model.OrderRequest;
import crmpams.model.UrgencyLevel;
import crmpams.service.OrderFormData;
import crmpams.service.OrderIntakeService;
import crmpams.service.SaveOrderResult;
import crmpams.ui.Dialogs;
import crmpams.ui.UiTasks;
import crmpams.ui.components.DateSpinner;
import crmpams.ui.components.FormPanel;
import crmpams.ui.components.HintTextArea;
import crmpams.ui.components.HintTextField;
import crmpams.ui.components.InputFilters;
import crmpams.ui.components.RadioGroup;
import crmpams.validation.FieldRules;
import crmpams.validation.Fields;
import crmpams.validation.ValidationResult;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.text.ParseException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.text.JTextComponent;

/**
 * Major Form 1: New Order (Customer Order Intake 5.0, IPO 5.1 to 5.4).
 * Used by the Operations Manager and the Officer-in-Charge to encode an order once.
 *
 * The fields follow the company's order form (System Revised Order Form): customer name, the order
 * lines (project type, media, size, number of sheets, number of sets, total number of sheets),
 * and the delivery address. The J.O. number and the date and time received are set by the system
 * when the order is saved. The CAD operator is assigned later in the Task Assignment module, and
 * prices are not recorded because the system does not compute prices.
 */
public final class NewOrderForm extends FormPanel {

    private static final long serialVersionUID = 1L;
    static final String LOOKUP_NOT_CHECKED =
            "Not checked yet. Use Look Up Customer to find an existing customer record.";
    private static final DateTimeFormatter SAVED_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter SAVED_TIME = DateTimeFormatter.ofPattern("h:mm a");

    private final transient OrderIntakeService service;
    private final transient CurrentUser user;
    private final transient Runnable onClose;

    // Customer
    final HintTextField customerName = new HintTextField("Juan Dela Cruz or ABC Builders", 28);
    final HintTextField contactNumber = new HintTextField("09171234567", 14);
    final HintTextField email = new HintTextField("name@example.com", 28);
    final JButton lookupButton = new JButton("Look Up Customer");
    final JLabel lookupStatus = FormPanel.helperLabel(LOOKUP_NOT_CHECKED);

    // Order details
    final HintTextArea description = new HintTextArea("What the customer needs drafted or printed", 3, 28);
    final DateSpinner deadline;
    final RadioGroup<UrgencyLevel> urgency;
    final HintTextField deliveryAddress = new HintTextField("Leave blank if the customer will pick up", 28);
    final HintTextArea instructions = new HintTextArea("Paper type, sheet size, or other notes", 3, 28);

    // Order line entry
    final JComboBox<LookupItem> lineProjectType = new JComboBox<>();
    final JComboBox<String> lineMedia = new JComboBox<>(FieldRules.MEDIA_SUGGESTIONS.toArray(String[]::new));
    final JComboBox<String> lineSize = new JComboBox<>(FieldRules.SIZE_SUGGESTIONS.toArray(String[]::new));
    final JSpinner lineSheets = new JSpinner(new SpinnerNumberModel(1, FieldRules.SHEETS_MIN, FieldRules.SHEETS_MAX, 1));
    final JSpinner lineSets = new JSpinner(new SpinnerNumberModel(1, FieldRules.SETS_MIN, FieldRules.SETS_MAX, 1));
    final JLabel lineTotal = FormPanel.helperLabel(" ");
    final JButton addLineButton = new JButton("Add Order Line");
    final JButton removeLineButton = new JButton("Remove Selected Line");
    final LinesModel linesModel = new LinesModel();
    final JTable linesTable = new JTable(linesModel);

    // Reference file
    final HintTextField referenceFile = new HintTextField("No file selected", 20);
    final JButton browseButton = new JButton("Browse Reference File...");
    final JButton removeFileButton = new JButton("Remove File");
    final JButton saveButton;

    /** Hidden value: the id of the customer found by Look Up Customer. */
    private Integer customerId;
    private boolean fillingFromLookup;

    public NewOrderForm(OrderIntakeService service, CurrentUser user, OrderFormData data, Runnable onClose) {
        super("New Order", "Record a customer order once. The J.O. number and the date and time received are "
                + "set when the order is saved.", true);
        this.service = service;
        this.user = user;
        this.onClose = onClose;
        this.deadline = new DateSpinner(service.today());
        this.urgency = new RadioGroup<>(data.urgencyLevels(), UrgencyLevel::name);

        lineProjectType.addItem(LookupItem.placeholder("Select a project type"));
        data.projectTypes().forEach(lineProjectType::addItem);
        setUpEditableCombo(lineMedia, FieldRules.MEDIA_MAX);
        setUpEditableCombo(lineSize, FieldRules.SIZE_MAX);
        setUpNumberSpinner(lineSheets);
        setUpNumberSpinner(lineSets);

        InputFilters.install(customerName, FieldRules.CUSTOMER_NAME_MAX, InputFilters.SINGLE_LINE);
        InputFilters.install(contactNumber, FieldRules.CONTACT_NUMBER_MAX_CHARS, InputFilters.PHONE);
        InputFilters.install(email, FieldRules.EMAIL_MAX, InputFilters.SINGLE_LINE);
        InputFilters.install(description, FieldRules.DESCRIPTION_MAX, InputFilters.TEXT);
        InputFilters.install(deliveryAddress, FieldRules.ADDRESS_MAX, InputFilters.SINGLE_LINE);
        InputFilters.install(instructions, FieldRules.INSTRUCTIONS_MAX, InputFilters.TEXT);
        referenceFile.setEditable(false);

        // ---- Customer (IPO 5.1, 5.2)
        addSection("Customer");
        addField(Fields.CUSTOMER_NAME, "Customer name", KeyEvent.VK_C, true,
                "Person or company name, 2 to 100 characters.", customerName);
        addField(Fields.CONTACT_NUMBER, "Contact number", KeyEvent.VK_N, true,
                "Mobile 09XXXXXXXXX or +639XXXXXXXXX, or landline 02 plus 8 digits.", contactNumber);
        addField(Fields.EMAIL, "Email", KeyEvent.VK_E, false,
                "Optional. Saved with a new customer record.", email);
        lookupButton.setMnemonic(KeyEvent.VK_L);
        lookupButton.setToolTipText("Find an existing customer with this name and contact number.");
        lookupButton.addActionListener(e -> lookUpCustomer());
        JPanel lookupRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        lookupRow.setOpaque(false);
        lookupRow.add(lookupButton);
        lookupRow.add(Box.createHorizontalStrut(12));
        lookupRow.add(lookupStatus);
        addRow(null, lookupRow);

        // ---- Order lines (columns of the company's order form)
        addSection("Order lines");
        addField(Fields.LINE_PROJECT_TYPE, "Project type", KeyEvent.VK_P, true,
                "Each line has its own project type, for example a floor plan and blueprint printing.",
                lineProjectType);
        addField(Fields.LINE_MEDIA, "Media", KeyEvent.VK_M, true,
                "Choose from the list or type, for example Bond paper. Up to 40 characters.",
                lineMedia, editorOf(lineMedia), lineMedia);
        addField(Fields.LINE_SIZE, "Size", KeyEvent.VK_Z, true,
                "Choose from the list or type, for example A1. Up to 30 characters.",
                lineSize, editorOf(lineSize), lineSize);
        addField(Fields.LINE_SHEETS, "No. of sheets", KeyEvent.VK_H, true,
                "Whole number from 1 to 999. Up and Down arrow keys change it.",
                lineSheets, textOf(lineSheets), lineSheets);
        addField(Fields.LINE_SETS, "No. of sets", KeyEvent.VK_T, true,
                "Whole number from 1 to 99. Up and Down arrow keys change it.",
                lineSets, textOf(lineSets), lineSets);
        addLineButton.setMnemonic(KeyEvent.VK_O);
        addLineButton.setToolTipText("Check this line and add it to the order lines below.");
        addLineButton.addActionListener(e -> addLine());
        JPanel lineButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        lineButtons.setOpaque(false);
        lineButtons.add(addLineButton);
        lineButtons.add(Box.createHorizontalStrut(12));
        lineButtons.add(lineTotal);
        addRow(null, lineButtons);

        setUpLinesTable();
        JScrollPane linesScroll = new JScrollPane(linesTable);
        linesScroll.setPreferredSize(new Dimension(560, 150));
        addField(Fields.ORDER_LINES, "Order lines", 0, true, linesSummary(), linesScroll, linesTable, linesScroll);
        removeLineButton.setToolTipText("Remove the selected line (Delete key in the table).");
        removeLineButton.addActionListener(e -> removeSelectedLine());
        JPanel removeRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        removeRow.setOpaque(false);
        removeRow.add(removeLineButton);
        addRow(null, removeRow);

        // ---- Order details (IPO 5.3)
        addSection("Order details");
        JScrollPane descriptionScroll = new JScrollPane(description);
        addField(Fields.PROJECT_DESCRIPTION, "Project description", KeyEvent.VK_D, true,
                countText(), descriptionScroll, description, descriptionScroll);
        addField(Fields.DEADLINE, "Deadline", KeyEvent.VK_A, true,
                "YYYY-MM-DD, today or later. Up and Down arrow keys change the day.",
                deadline, deadline.textField(), deadline);
        urgency.getAccessibleContext().setAccessibleName("Urgency level");
        addField(Fields.URGENCY, "Urgency level", KeyEvent.VK_U, true,
                "Used by the production queue when two deadlines are the same. Arrow keys change the choice.",
                urgency, urgency.firstButton(), urgency);
        addField(Fields.DELIVERY_ADDRESS, "Delivery address", KeyEvent.VK_Y, false,
                "Optional. Where the rider delivers the output. Up to 255 characters.", deliveryAddress);
        JScrollPane instructionsScroll = new JScrollPane(instructions);
        addField(Fields.SPECIAL_INSTRUCTIONS, "Special instructions", KeyEvent.VK_I, false,
                "Optional. Up to 500 characters.", instructionsScroll, instructions, instructionsScroll);

        // ---- Reference file
        addSection("Reference file");
        browseButton.setMnemonic(KeyEvent.VK_B);
        browseButton.addActionListener(e -> chooseFile());
        removeFileButton.setMnemonic(KeyEvent.VK_V);
        removeFileButton.addActionListener(e -> referenceFile.setText(""));
        removeFileButton.setToolTipText("Remove the selected file from this order.");
        JPanel fileRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        fileRow.setOpaque(false);
        fileRow.add(referenceFile);
        fileRow.add(Box.createHorizontalStrut(8));
        fileRow.add(browseButton);
        fileRow.add(Box.createHorizontalStrut(8));
        fileRow.add(removeFileButton);
        referenceFile.getAccessibleContext().setAccessibleName("Selected reference file");
        addField(Fields.REFERENCE_FILE, "Reference file", KeyEvent.VK_R, false,
                "Optional. " + String.join(", ", FieldRules.REFERENCE_FILE_EXTENSIONS)
                        + ". Only the file name and location are saved.",
                fileRow, browseButton, referenceFile);

        saveButton = addButton("Save Order", KeyEvent.VK_S, "Check the form and save the order (Enter).",
                this::submit);
        addButton("Clear Form", KeyEvent.VK_F, "Empty every field and start again.", this::clearForm);
        addButton("Cancel", 0, "Close the form without saving (Esc).", this::cancel);
        setDefaultButton(saveButton);
        setEscapeAction(this::cancel);

        DocumentListener customerChanged = new SimpleDocListener(() -> {
            if (!fillingFromLookup) {
                customerId = null;
                lookupStatus.setText(LOOKUP_NOT_CHECKED);
            }
        });
        customerName.getDocument().addDocumentListener(customerChanged);
        contactNumber.getDocument().addDocumentListener(customerChanged);
        description.getDocument().addDocumentListener(
                new SimpleDocListener(() -> setHelper(Fields.PROJECT_DESCRIPTION, countText())));
        lineSheets.addChangeListener(e -> updateLineTotal());
        lineSets.addChangeListener(e -> updateLineTotal());
        updateLineTotal();
    }

    // ------------------------------------------------------------------ set-up helpers

    private static void setUpEditableCombo(JComboBox<String> combo, int maxLength) {
        combo.setEditable(true);
        combo.setSelectedItem("");
        InputFilters.install(editorOf(combo), maxLength, InputFilters.SINGLE_LINE);
    }

    private static void setUpNumberSpinner(JSpinner spinner) {
        JSpinner.NumberEditor editor = new JSpinner.NumberEditor(spinner, "#");
        spinner.setEditor(editor);
        editor.getTextField().setColumns(4);
        editor.getTextField().setFocusLostBehavior(JFormattedTextField.COMMIT);
    }

    static JTextComponent editorOf(JComboBox<String> combo) {
        return (JTextComponent) combo.getEditor().getEditorComponent();
    }

    static JFormattedTextField textOf(JSpinner spinner) {
        return ((JSpinner.DefaultEditor) spinner.getEditor()).getTextField();
    }

    /** The whole number in a spinner, or null when the typed text is not a number in range. */
    static Integer readInt(JSpinner spinner) {
        try {
            textOf(spinner).commitEdit();
        } catch (ParseException e) {
            return null;
        }
        return ((Number) spinner.getValue()).intValue();
    }

    private void setUpLinesTable() {
        linesTable.setRowHeight(24);
        linesTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        linesTable.setFillsViewportHeight(true);
        linesTable.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        linesTable.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
        int[] widths = {34, 150, 105, 55, 100, 85, 135};
        for (int i = 0; i < widths.length; i++) {
            linesTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        linesTable.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("DELETE"), "remove-line");
        linesTable.getActionMap().put("remove-line", new AbstractAction() {
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed(ActionEvent e) {
                removeSelectedLine();
            }
        });
    }

    private String countText() {
        return description.getDocument().getLength() + " of " + FieldRules.DESCRIPTION_MAX
                + " characters used. At least " + FieldRules.DESCRIPTION_MIN + ".";
    }

    private void updateLineTotal() {
        Integer sheets = readIntQuietly(lineSheets);
        Integer sets = readIntQuietly(lineSets);
        lineTotal.setText(sheets == null || sets == null ? "Total no. of sheets: enter whole numbers first."
                : "Total no. of sheets for this line: " + sheets + " x " + sets + " = " + (sheets * sets));
    }

    private static Integer readIntQuietly(JSpinner spinner) {
        Object v = spinner.getValue();
        return v instanceof Number n ? n.intValue() : null;
    }

    private String linesSummary() {
        int count = linesModel.lines.size();
        if (count == 0) {
            return "No lines yet. Fill in the line above and click Add Order Line.";
        }
        int sheets = linesModel.lines.stream().mapToInt(OrderLine::totalSheets).sum();
        return count + (count == 1 ? " line, " : " lines, ") + sheets + (sheets == 1 ? " sheet" : " sheets")
                + " in all. Select a line and press Delete to remove it.";
    }

    // ------------------------------------------------------------------ order lines

    /** Checks the line entry and adds it to the order lines. */
    void addLine() {
        LookupItem type = (LookupItem) lineProjectType.getSelectedItem();
        Integer typeId = type == null || type.isPlaceholder() ? null : type.id();
        String media = editorOf(lineMedia).getText();
        String size = editorOf(lineSize).getText();
        Integer sheets = readInt(lineSheets);
        Integer sets = readInt(lineSets);
        ValidationResult v = OrderIntakeService.validateLine(typeId, media, size, sheets, sets);
        if (linesModel.lines.size() >= FieldRules.LINES_MAX) {
            v.check(Fields.ORDER_LINES, FieldRules.lineCount(FieldRules.LINES_MAX + 1));
        }
        if (!v.isValid()) {
            showErrors(v);
            return;
        }
        clearErrors();
        linesModel.add(new OrderLine(typeId, type.name(), FieldRules.clean(media), FieldRules.clean(size),
                sheets, sets));
        setHelper(Fields.ORDER_LINES, linesSummary());
        resetLineEntry();
        lineProjectType.requestFocusInWindow();
    }

    void removeSelectedLine() {
        int row = linesTable.getSelectedRow();
        if (row < 0) {
            Dialogs.info(this, "No line selected", "Select a line in the order lines table first.");
            return;
        }
        linesModel.remove(row);
        setHelper(Fields.ORDER_LINES, linesSummary());
        if (linesModel.getRowCount() > 0) {
            int next = Math.min(row, linesModel.getRowCount() - 1);
            linesTable.setRowSelectionInterval(next, next);
        }
    }

    private void resetLineEntry() {
        lineProjectType.setSelectedIndex(0);
        lineMedia.setSelectedItem("");
        lineSize.setSelectedItem("");
        lineSheets.setValue(1);
        lineSets.setValue(1);
        textOf(lineSheets).setValue(1);
        textOf(lineSets).setValue(1);
        updateLineTotal();
    }

    private boolean lineEntryStarted() {
        return lineProjectType.getSelectedIndex() > 0 || !editorOf(lineMedia).getText().isBlank()
                || !editorOf(lineSize).getText().isBlank();
    }

    // ------------------------------------------------------------------ actions

    /** Builds the request from the form, including the hidden customer id. */
    OrderRequest collectRequest() {
        UrgencyLevel u = urgency.getSelected();
        return new OrderRequest(
                customerId,
                customerName.getText(),
                contactNumber.getText(),
                email.getText(),
                description.getText(),
                List.copyOf(linesModel.lines),
                deadline.readDate(),
                u == null ? null : u.id(),
                deliveryAddress.getText(),
                instructions.getText(),
                referenceFile.getText());
    }

    /** Client-side check: the same rules the service uses, plus checks that only the screen can make. */
    ValidationResult validateForm(OrderRequest request) {
        ValidationResult v = OrderIntakeService.validate(request, service.today());
        if (deadline.hasInvalidText()) {
            v.put(Fields.DEADLINE, "Enter the deadline as YYYY-MM-DD, for example "
                    + service.today().plusDays(7) + ".");
        }
        if (request.lines().isEmpty() && lineEntryStarted()) {
            v.put(Fields.ORDER_LINES, "Click Add Order Line to add the line you filled in.");
        }
        return v;
    }

    public void submit() {
        OrderRequest request = collectRequest();
        ValidationResult v = validateForm(request);
        if (!v.isValid()) {
            showErrors(v);
            return;
        }
        clearErrors();
        save(request, false);
    }

    private void save(OrderRequest request, boolean confirmDuplicate) {
        saveButton.setEnabled(false);
        UiTasks.run(this, () -> service.save(user, request, confirmDuplicate), result -> {
            switch (result.status()) {
                case INVALID -> showErrors(result.errors());
                case POSSIBLE_DUPLICATE -> {
                    boolean saveAnyway = Dialogs.confirm(this, "Possible duplicate order",
                            "J.O. # " + result.duplicateOrderId() + " of this customer has the same project "
                            + "description and is still open.\n\nSave this as a separate new order anyway?");
                    if (saveAnyway) {
                        save(request, true);
                    }
                }
                case SAVED -> {
                    Dialogs.info(this, "Order saved", savedMessage(result, request.lines()));
                    clearForm();
                }
                default -> throw new IllegalStateException("Unknown status " + result.status());
            }
        }, () -> saveButton.setEnabled(true));
    }

    private static String savedMessage(SaveOrderResult r, List<OrderLine> lines) {
        int sheets = lines.stream().mapToInt(OrderLine::totalSheets).sum();
        return "J.O. # " + r.orderId() + " was saved with the status New.\n"
                + "Date received: " + r.dateReceived().format(SAVED_DATE) + "\n"
                + "Time received: " + r.dateReceived().format(SAVED_TIME) + "\n"
                + "Order lines: " + lines.size() + ", " + sheets + (sheets == 1 ? " sheet" : " sheets") + " in all.\n"
                + (r.newCustomer() ? "A new customer record was also created."
                        : "It is linked to the existing customer record.");
    }

    void lookUpCustomer() {
        ValidationResult v = new ValidationResult()
                .check(Fields.CUSTOMER_NAME, FieldRules.customerName(customerName.getText()))
                .check(Fields.CONTACT_NUMBER, FieldRules.contactNumber(contactNumber.getText(), true));
        if (!v.isValid()) {
            showErrors(v);
            return;
        }
        clearErrors();
        String name = customerName.getText();
        String contact = contactNumber.getText();
        UiTasks.run(this, () -> service.lookupCustomer(user, name, contact), this::applyLookup);
    }

    private void applyLookup(Optional<Customer> found) {
        if (found.isPresent()) {
            Customer c = found.get();
            fillingFromLookup = true;
            try {
                customerName.setText(c.name());
                contactNumber.setText(c.contactNumber());
                if (c.email() != null && email.getText().isBlank()) {
                    email.setText(c.email());
                }
            } finally {
                fillingFromLookup = false;
            }
            customerId = c.id();
            lookupStatus.setText("Existing customer found (customer ID " + c.id()
                    + "). The order will be linked to this record.");
        } else {
            customerId = null;
            lookupStatus.setText("No match. A new customer record will be created when you save.");
        }
        lookupButton.getAccessibleContext().setAccessibleDescription(lookupStatus.getText());
    }

    private void chooseFile() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Choose Reference File");
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setFileFilter(new FileNameExtensionFilter(
                "Design and reference files (" + String.join(", ", FieldRules.REFERENCE_FILE_EXTENSIONS) + ")",
                FieldRules.REFERENCE_FILE_EXTENSIONS.toArray(String[]::new)));
        String current = referenceFile.getText();
        if (!current.isBlank()) {
            chooser.setSelectedFile(new File(current));
        }
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            referenceFile.setText(chooser.getSelectedFile().getAbsolutePath());
            referenceFile.setCaretPosition(0);
        }
    }

    public void clearForm() {
        for (JTextField f : new JTextField[] {customerName, contactNumber, email, deliveryAddress, referenceFile}) {
            f.setText("");
        }
        description.setText("");
        instructions.setText("");
        deadline.setDate(service.today());
        urgency.clearSelection();
        linesModel.clear();
        resetLineEntry();
        setHelper(Fields.ORDER_LINES, linesSummary());
        customerId = null;
        lookupStatus.setText(LOOKUP_NOT_CHECKED);
        clearErrors();
        focusFirstField();
    }

    boolean isDirty() {
        return !customerName.getText().isBlank() || !contactNumber.getText().isBlank()
                || !email.getText().isBlank() || !description.getText().isBlank()
                || !deliveryAddress.getText().isBlank() || !instructions.getText().isBlank()
                || !referenceFile.getText().isBlank() || urgency.getSelected() != null
                || !linesModel.lines.isEmpty() || lineEntryStarted();
    }

    private void cancel() {
        if (isDirty() && !Dialogs.confirm(this, "Discard this order?",
                "This order has not been saved. Close the form and discard what you entered?")) {
            return;
        }
        onClose.run();
    }

    Integer hiddenCustomerId() {
        return customerId;
    }

    /** Table model of the order lines, with the columns of the company's order form. */
    static final class LinesModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private static final String[] COLUMNS = {"No.", "Project Type", "Media", "Size", "No. of Sheets",
            "No. of Sets", "Total No. of Sheets"};
        final transient List<OrderLine> lines = new ArrayList<>();

        void add(OrderLine line) {
            lines.add(line);
            fireTableRowsInserted(lines.size() - 1, lines.size() - 1);
        }

        void remove(int row) {
            lines.remove(row);
            fireTableDataChanged();
        }

        void clear() {
            lines.clear();
            fireTableDataChanged();
        }

        @Override
        public int getRowCount() {
            return lines.size();
        }

        @Override
        public int getColumnCount() {
            return COLUMNS.length;
        }

        @Override
        public String getColumnName(int column) {
            return COLUMNS[column];
        }

        @Override
        public Class<?> getColumnClass(int column) {
            return column == 0 || column >= 4 ? Integer.class : String.class;
        }

        @Override
        public Object getValueAt(int row, int column) {
            OrderLine l = lines.get(row);
            return switch (column) {
                case 0 -> row + 1;
                case 1 -> l.projectTypeName();
                case 2 -> l.media();
                case 3 -> l.size();
                case 4 -> l.sheets();
                case 5 -> l.sets();
                default -> l.totalSheets();
            };
        }
    }

    /** Calls a Runnable whenever the document changes. */
    static final class SimpleDocListener implements DocumentListener {
        private final Runnable r;

        SimpleDocListener(Runnable r) {
            this.r = r;
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            r.run();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            r.run();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            r.run();
        }
    }
}
