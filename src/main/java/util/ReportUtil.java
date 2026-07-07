package util;

import config.Koneksi;
import java.io.InputStream;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JOptionPane;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.view.JasperViewer;

public class ReportUtil {

    public static void showReport(String reportName, Map<String, Object> params) {
        if (params == null) {
            params = new HashMap<>();
        }

        try (Connection conn = Koneksi.getConnection()) {
            JasperReport report = loadReport(reportName);
            JasperPrint print = JasperFillManager.fillReport(report, params, conn);

            if (print.getPages() == null || print.getPages().isEmpty()) {
                JOptionPane.showMessageDialog(null, "Data laporan tidak ditemukan.");
                return;
            }

            JasperViewer viewer = new JasperViewer(print, false);
            viewer.setTitle("Preview Laporan - " + reportName);
            viewer.setVisible(true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Gagal membuka laporan Jasper: " + e.getMessage());
        }
    }

    public static void showReport(String reportName) {
        showReport(reportName, new HashMap<>());
    }

    private static JasperReport loadReport(String reportName) throws Exception {
        String jrxmlPath = "/panel/laporan/" + reportName + ".jrxml";
        InputStream jrxmlStream = ReportUtil.class.getResourceAsStream(jrxmlPath);
        if (jrxmlStream != null) {
            return JasperCompileManager.compileReport(jrxmlStream);
        }

        jrxmlPath = "/laporan/" + reportName + ".jrxml";
        jrxmlStream = ReportUtil.class.getResourceAsStream(jrxmlPath);
        if (jrxmlStream != null) {
            return JasperCompileManager.compileReport(jrxmlStream);
        }

        jrxmlPath = "/reports/" + reportName + ".jrxml";
        jrxmlStream = ReportUtil.class.getResourceAsStream(jrxmlPath);
        if (jrxmlStream != null) {
            return JasperCompileManager.compileReport(jrxmlStream);
        }

        throw new IllegalArgumentException("File JRXML report tidak ditemukan: " + reportName);
    }
}
