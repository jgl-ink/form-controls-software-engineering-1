package crmpams.ui.forms;

import crmpams.model.Customer;
import crmpams.model.CurrentUser;
import crmpams.model.LookupItem;
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
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Optional;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Major Form 1: New Order (Customer Order Intake 5.0, IPO 5.1 to 5.4).
 * Used by the Operations Manager and the Officer-in-Charge to encode an order once.
 */
public final class NewOrderForm extends FormPanel {

    private static final long serialVersionUID = 1L;
    static final String LOOKUP_NOT_CHECKED =
            "Not checked yet. Use Look Up Customer to find an existing customer record.";

    private final transient OrderIntakeService service;
    private final transient CurrentUser user;
    private final transient Runnable onClose;

    final HintTextField customerName = new HintTextField("Juan Dela Cruz or ABC Builders", 28);
    final HintTextField contactNumber = new HintTextField("09171234567", 14);
    final HintTextField email = new HintTextField("name@example.com", 28);
    final JButton lookupButton = new JButton("Look Up Customer");
    final JLabel lookupStatus = FormPanel.helperLabel(LOOKUP_NOT_CHECKED);
    final JComboBox<LookupItem> projectType = new JComboBox<>();
    final HintTextArea description = new HintTextArea("What the customer needs drafted or printed", 4, 28);
    final HintTextField requiredOutput = new HintTextField("3 sets, A1 size, blueprint", 28);
    final DateSpinner deadline;
    final RadioGroup<UrgencyLevel> urgency;
    final HintTextArea instructions = new HintTextArea("Paper type, sheet size, or other notes", 3, 28);
    final HintTextField referenceFile = new HintTextField("No file selected", 20);
    final JButton browseButton = new JButton("Browse Reference File...");
    final JButton removeFileButton = new JButton("Remove File");
    final JButton saveButton;

    /** Hidden value: the id of the customer found by Look Up Customer. */
    private Integer customerId;
    private boolean fillingFromLookup;

    public NewOrderForm(OrderIntakeService service, CurrentUser user, OrderFormData data, Runnable onClose) {
        super("New Order", "Record a customer order once. The order is saved with the status New.", true);
        this.service = service;
        this.user = user;
        this.onClose = onClose;
        this.deadline = new DateSpinner(service.today());
        this.urgency = new RadioGroup<>(data.urgencyLevels(), UrgencyLevel::name);

        projectType.addItem(LookupItem.placeholder("Select a project type"));
        data.projectTypes().forEach(projectType::addItem);

        InputFilters.install(customerName, FieldRules.CUSTOMER_NAME_MAX, InputFilters.SINGLE_LINE);
        InputFilters.install(contactNumber, FieldRules.CONTACT_NUMBER_MAX_CHARS, InputFilters.PHONE);
        InputFilters.install(email, FieldRules.EMAIL_MAX, InputFilters.SINGLE_LINE);
        InputFilters.install(description, FieldRules.DESCRIPTION_MAX, InputFilters.TEXT);
        InputFilters.install(requiredOutput, FieldRules.REQUIRED_OUTPUT_MAX, InputFilters.SINGLE_LINE);
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

        // ---- Order details (IPO 5.3)
        addSection("Order details");
        addField(Fields.PROJECT_TYPE, "Project type", KeyEvent.VK_P, true,
                "Choose from the list kept in Maintenance.", projectType);
        JScrollPane descriptionScroll = new JScrollPane(description);
        addField(Fields.PROJECT_DESCRIPTION, "Project description", KeyEvent.VK_D, true,
                countText(), descriptionScroll, description, descriptionScroll);
        addField(Fields.REQUIRED_OUTPUT, "Required output", KeyEvent.VK_O, false,
                "Optional. Up to 150 characters.", requiredOutput);
        addField(Fields.DEADLINE, "Deadline", KeyEvent.VK_A, true,
                "YYYY-MM-DD, today or later. Up and Down arrow keys change the day.",
                deadline, deadline.textField(), deadline);
        urgency.getAccessibleContext().setAccessibleName("Urgency level");
        addField(Fields.URGENCY, "Urgency level", KeyEvent.VK_U, true,
                "Used by the production queue when two deadlines are the same. Arrow keys change the choice.",
                urgency, urgency.firstButton(), urgency);
        JScrollPane instructionsScroll = new JScrollPane(instructions);
        addField(Fields.SPECIAL_INSTRUCTIONS, "Special instructions", KeyEvent.VK_I, false,
                "Optional. Up to 500 characters.", instructionsScroll, instructions, instructionsScroll);

        // ---- Reference file
        addSection("Reference file");
        browseButton.setMnemonic(KeyEvent.VK_B);
        browseButton.addActionListener(e -> chooseFile());
        removeFileButton.setMnemonic(KeyEvent.VK_M);
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
    }

    private String countText() {
        return description.getDocument().getLength() + " of " + FieldRules.DESCRIPTION_MAX
                + " characters used. At least " + FieldRules.DESCRIPTION_MIN + ".";
    }

    // ------------------------------------------------------------------ actions

    /** Builds the request from the form, including the hidden customer id. */
    OrderRequest collectRequest() {
        LookupItem pt = (LookupItem) projectType.getSelectedItem();
        UrgencyLevel u = urgency.getSelected();
        return new OrderRequest(
                customerId,
                customerName.getText(),
                contactNumber.getText(),
                email.getText(),
                pt == null || pt.isPlaceholder() ? null : pt.id(),
                description.getText(),
                requiredOutput.getText(),
                deadline.readDate(),
                u == null ? null : u.id(),
                instructions.getText(),
                referenceFile.getText());
    }

    /** Client-side check: the same rules the service uses, plus the date format of the spinner. */
    ValidationResult validateForm(OrderRequest request) {
        ValidationResult v = OrderIntakeService.validate(request, service.today());
        if (deadline.hasInvalidText()) {
            v.put(Fields.DEADLINE, "Enter the deadline as YYYY-MM-DD, for example "
                    + service.today().plusDays(7) + ".");
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
                            "Order " + result.duplicateOrderId() + " of this customer has the same project "
                            + "description and is still open.\n\nSave this as a separate new order anyway?");
                    if (saveAnyway) {
                        save(request, true);
                    }
                }
                case SAVED -> {
                    Dialogs.info(this, "Order saved", savedMessage(result));
                    clearForm();
                }
                default -> throw new IllegalStateException("Unknown status " + result.status());
            }
        }, () -> saveButton.setEnabled(true));
    }

    private static String savedMessage(SaveOrderResult r) {
        return "Order " + r.orderId() + " was saved with the status New."
                + (r.newCustomer() ? " A new customer record was also created." : " It is linked to the existing customer record.");
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
        for (JTextField f : new JTextField[] {customerName, contactNumber, email, requiredOutput, referenceFile}) {
            f.setText("");
        }
        description.setText("");
        instructions.setText("");
        projectType.setSelectedIndex(0);
        deadline.setDate(service.today());
        urgency.clearSelection();
        customerId = null;
        lookupStatus.setText(LOOKUP_NOT_CHECKED);
        clearErrors();
        focusFirstField();
    }

    boolean isDirty() {
        return !customerName.getText().isBlank() || !contactNumber.getText().isBlank()
                || !email.getText().isBlank() || !description.getText().isBlank()
                || !requiredOutput.getText().isBlank() || !instructions.getText().isBlank()
                || !referenceFile.getText().isBlank() || projectType.getSelectedIndex() > 0
                || urgency.getSelected() != null;
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
