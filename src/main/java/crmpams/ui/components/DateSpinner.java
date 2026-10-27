package crmpams.ui.components;

import java.text.ParseException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import javax.swing.JFormattedTextField;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;

/**
 * A date input in YYYY-MM-DD form. The Up and Down arrow keys change the day.
 * Typed text that is not a real date is kept on screen so the form can report it.
 */
public final class DateSpinner extends JSpinner {

    private static final long serialVersionUID = 1L;
    public static final String PATTERN = "yyyy-MM-dd";
    private final JFormattedTextField field;

    public DateSpinner(LocalDate initial) {
        super(new SpinnerDateModel(toDate(initial), null, null, Calendar.DAY_OF_MONTH));
        JSpinner.DateEditor editor = new JSpinner.DateEditor(this, PATTERN);
        editor.getFormat().setLenient(false);
        setEditor(editor);
        field = editor.getTextField();
        field.setColumns(10);
        field.setFocusLostBehavior(JFormattedTextField.COMMIT);
    }

    public JFormattedTextField textField() {
        return field;
    }

    /** Returns the date, or null when the typed text is not a valid YYYY-MM-DD date. */
    public LocalDate readDate() {
        try {
            field.commitEdit();
        } catch (ParseException e) {
            return null;
        }
        return toLocalDate((Date) getValue());
    }

    public boolean hasInvalidText() {
        try {
            field.commitEdit();
            return false;
        } catch (ParseException e) {
            return true;
        }
    }

    public void setDate(LocalDate date) {
        setValue(toDate(date));
        field.setValue(getValue());
    }

    private static Date toDate(LocalDate d) {
        return Date.from(d.atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static LocalDate toLocalDate(Date d) {
        return d.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }
}
