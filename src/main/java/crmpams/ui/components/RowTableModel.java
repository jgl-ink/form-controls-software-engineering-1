package crmpams.ui.components;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.swing.table.AbstractTableModel;

/** A read-only table model built from a list of records and column definitions. */
public final class RowTableModel<T> extends AbstractTableModel {

    private static final long serialVersionUID = 1L;

    public record Column<T>(String name, Class<?> type, Function<T, Object> value) {
    }

    private final transient List<Column<T>> columns;
    private final transient List<T> rows = new ArrayList<>();

    public RowTableModel(List<Column<T>> columns) {
        this.columns = columns;
    }

    public void setRows(List<T> newRows) {
        rows.clear();
        rows.addAll(newRows);
        fireTableDataChanged();
    }

    public T rowAt(int modelIndex) {
        return rows.get(modelIndex);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public String getColumnName(int column) {
        return columns.get(column).name();
    }

    @Override
    public Class<?> getColumnClass(int column) {
        return columns.get(column).type();
    }

    @Override
    public Object getValueAt(int row, int column) {
        return columns.get(column).value().apply(rows.get(row));
    }
}
