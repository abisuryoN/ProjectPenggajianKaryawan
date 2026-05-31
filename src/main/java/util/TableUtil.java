package util;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

public class TableUtil {
    public static int getSelectedRowId(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) return -1;
        Object value = table.getValueAt(row, 0);
        return Integer.parseInt(value.toString());
    }

    public static String getValue(JTable table, int column) {
        int row = table.getSelectedRow();
        if (row < 0) return "";
        Object value = table.getValueAt(row, column);
        return value == null ? "" : value.toString();
    }

    public static void clearTable(JTable table) {
        DefaultTableModel model = (DefaultTableModel) table.getModel();
        model.setRowCount(0);
    }

    public static boolean hasSelectedRow(JTable table) {
        return table.getSelectedRow() >= 0;
    }
}
