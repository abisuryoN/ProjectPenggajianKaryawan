package app;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Font;
import view.LoginFrame;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                // PENTING: jangan pakai getSystemLookAndFeelClassName() di Linux bertema gelap.
                // GTK Look&Feel akan mewarnai komponen Swing jadi gelap/abu-abu dan
                // bertabrakan dengan warna terang yang di-hardcode di kode ini.
                // Metal L&F memberi tampilan terang yang konsisten di semua OS.
                try {
                    UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
                } catch (Exception e) {
                    try {
                        UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                    } catch (Exception ignored) {
                    }
                }
                applyGlobalFont();
                new LoginFrame().setVisible(true);
            }
        });
    }

    private static void applyGlobalFont() {
        // "Segoe UI" hanya ada di Windows. Pilih font pertama yang tersedia di sistem.
        String[] kandidat = {"Segoe UI", "Inter", "Ubuntu", "Cantarell", "Noto Sans", "DejaVu Sans", "Dialog"};
        String namaFont = "Dialog";
        java.util.Set<String> tersedia = new java.util.HashSet<>(java.util.Arrays.asList(
                java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String k : kandidat) {
            if (tersedia.contains(k)) {
                namaFont = k;
                break;
            }
        }
        Font base = new Font(namaFont, Font.PLAIN, 13);
        String[] keys = {
            "Label.font", "Button.font", "TextField.font", "PasswordField.font",
            "TextArea.font", "ComboBox.font", "Table.font", "TableHeader.font",
            "TabbedPane.font", "TitledBorder.font", "OptionPane.messageFont",
            "OptionPane.buttonFont"
        };
        for (String key : keys) {
            UIManager.put(key, base);
        }
    }
}
