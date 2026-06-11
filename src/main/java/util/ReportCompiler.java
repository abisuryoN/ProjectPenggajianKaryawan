package util;

import java.io.File;
import net.sf.jasperreports.engine.JasperCompileManager;

public class ReportCompiler {

    public static void main(String[] args) throws Exception {
        File reportDir = new File("src/main/resources/reports");
        File[] files = reportDir.listFiles((dir, name) -> name.endsWith(".jrxml"));
        if (files == null) {
            throw new IllegalStateException("Folder reports tidak ditemukan: " + reportDir.getAbsolutePath());
        }

        for (File jrxml : files) {
            String jasperPath = jrxml.getAbsolutePath().replace(".jrxml", ".jasper");
            JasperCompileManager.compileReportToFile(jrxml.getAbsolutePath(), jasperPath);
            System.out.println("Compiled " + jrxml.getName());
        }
    }
}
