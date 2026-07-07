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
                try {
                    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
                } catch (Exception ignored) {
                }
                applyGlobalFont();
                new LoginFrame().setVisible(true);
            }
        });
    }

    private static void applyGlobalFont() {
        Font base = new Font("Segoe UI", Font.PLAIN, 13);
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
