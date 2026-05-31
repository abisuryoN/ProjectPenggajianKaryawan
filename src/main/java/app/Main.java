package app;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
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
                new LoginFrame().setVisible(true);
            }
        });
    }
}
