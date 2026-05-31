package report;

import java.awt.print.PrinterException;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.JTable;

public class CetakLaporanKaryawan {
    public void cetak(JTable table) throws PrinterException {
        table.print(JTable.PrintMode.FIT_WIDTH);
    }

    public void exportCSV(JTable table, String pathFile) throws IOException {
        try (FileWriter writer = new FileWriter(pathFile)) {
            for (int c = 0; c < table.getColumnCount(); c++) {
                writer.append(table.getColumnName(c));
                if (c < table.getColumnCount() - 1) writer.append(",");
            }
            writer.append("\n");

            for (int r = 0; r < table.getRowCount(); r++) {
                for (int c = 0; c < table.getColumnCount(); c++) {
                    Object value = table.getValueAt(r, c);
                    writer.append(value == null ? "" : value.toString().replace(",", " "));
                    if (c < table.getColumnCount() - 1) writer.append(",");
                }
                writer.append("\n");
            }
        }
    }
}
