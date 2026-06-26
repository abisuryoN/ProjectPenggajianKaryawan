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
import net.sf.jasperreports.engine.util.JRLoader;
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

            JasperViewer.viewReport(print, false);
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

        String jasperPath = "/panel/laporan/" + reportName + ".jasper";
        InputStream jasperStream = ReportUtil.class.getResourceAsStream(jasperPath);
        if (jasperStream != null) {
            return (JasperReport) JRLoader.loadObject(jasperStream);
        }

        jasperPath = "/laporan/" + reportName + ".jasper";
        jasperStream = ReportUtil.class.getResourceAsStream(jasperPath);
        if (jasperStream != null) {
            return (JasperReport) JRLoader.loadObject(jasperStream);
        }

        jasperPath = "/reports/" + reportName + ".jasper";
        jasperStream = ReportUtil.class.getResourceAsStream(jasperPath);
        if (jasperStream != null) {
            return (JasperReport) JRLoader.loadObject(jasperStream);
        }

        throw new IllegalArgumentException("File report tidak ditemukan: " + reportName);
    }
}
