package util;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

public final class DesignUtil {
    private static final Color PAGE_BG = new Color(244, 247, 250);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color TEXT = new Color(17, 24, 39);
    private static final Color MUTED = new Color(75, 85, 99);
    private static final Color PRIMARY = new Color(22, 48, 92);
    private static final Color LINE = new Color(220, 226, 235);

    private DesignUtil() {
    }

    public static void applyPage(JPanel panel) {
        panel.setBackground(PAGE_BG);
        panel.setPreferredSize(new Dimension(1120, 660));
        panel.setMinimumSize(new Dimension(900, 560));
        applyComponent(panel);
    }

    private static void applyComponent(Component component) {
        if (component instanceof JPanel) {
            JPanel panel = (JPanel) component;
            panel.setBackground(component.getParent() == null ? PAGE_BG : CARD_BG);
            if (panel.getBorder() instanceof TitledBorder) {
                TitledBorder border = (TitledBorder) panel.getBorder();
                border.setTitleColor(PRIMARY);
                border.setTitleFont(new Font("Segoe UI", Font.BOLD, 13));
                panel.setBorder(BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(LINE),
                        border.getTitle(),
                        TitledBorder.DEFAULT_JUSTIFICATION,
                        TitledBorder.DEFAULT_POSITION,
                        border.getTitleFont(),
                        border.getTitleColor()
                ));
            }
        } else if (component instanceof JLabel) {
            styleLabel((JLabel) component);
        } else if (component instanceof JButton) {
            styleButton((JButton) component);
        } else if (component instanceof JTable) {
            styleTable((JTable) component);
        } else if (component instanceof JScrollPane) {
            ((JScrollPane) component).setBorder(BorderFactory.createLineBorder(LINE));
        } else if (component instanceof JTextField) {
            styleInput((JTextField) component);
        } else if (component instanceof JTextArea) {
            styleInput((JTextArea) component);
        } else if (component instanceof JComboBox) {
            component.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            component.setForeground(TEXT);
        }

        if (component instanceof java.awt.Container) {
            for (Component child : ((java.awt.Container) component).getComponents()) {
                applyComponent(child);
            }
        }
    }

    private static void styleLabel(JLabel label) {
        String text = label.getText() == null ? "" : label.getText();
        label.setFont(new Font("Segoe UI", isTitle(text) ? Font.BOLD : Font.PLAIN, isTitle(text) ? 24 : 13));
        label.setForeground(isTitle(text) ? PRIMARY : MUTED);
    }

    private static boolean isTitle(String text) {
        String normalized = text.trim();
        return normalized.startsWith("DATA ")
                || normalized.startsWith("Data ")
                || normalized.startsWith("LAPORAN ")
                || normalized.startsWith("PENGGAJIAN ")
                || normalized.startsWith("ABSENSI ");
    }

    private static void styleButton(JButton button) {
        button.setText(cleanButtonText(button.getText()));
        button.setUI(new BasicButtonUI());
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        button.setPreferredSize(new Dimension(Math.max(86, button.getPreferredSize().width), 36));
    }

    private static String cleanButtonText(String text) {
        if (text == null) return "";
        if (text.contains("HItung")) return "Hitung";
        if (text.contains("Filter")) return "Filter";
        if (text.contains("Cari")) return "Cari";
        if (text.contains("Cetak Slip")) return "Cetak Slip";
        if (text.contains("Cetak")) return "Cetak";
        if (text.contains("Export PDF")) return "Export PDF";
        if (text.contains("Refresh")) return "Refresh";
        if (text.contains("Reset")) return "Reset";
        return text;
    }

    private static void styleTable(JTable table) {
        applyTable(table);
    }

    public static void applyTable(JTable table) {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setForeground(TEXT);
        table.setRowHeight(28);
        table.setGridColor(LINE);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(true);
        table.setFillsViewportHeight(true);

        JTableHeader header = table.getTableHeader();
        if (header != null) {
            header.setFont(new Font("Segoe UI", Font.BOLD, 13));
            header.setForeground(Color.WHITE);
            header.setBackground(PRIMARY);
            header.setOpaque(true);
            header.setReorderingAllowed(false);
            header.setPreferredSize(new Dimension(header.getPreferredSize().width, 32));
            header.setDefaultRenderer(new HeaderRenderer());
        }
    }

    private static final class HeaderRenderer extends DefaultTableCellRenderer {
        HeaderRenderer() {
            setHorizontalAlignment(SwingConstants.LEFT);
            setOpaque(true);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(60, 86, 124)),
                    BorderFactory.createEmptyBorder(6, 8, 6, 8)
            ));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setText(value == null ? "" : value.toString());
            setFont(new Font("Segoe UI", Font.BOLD, 13));
            setForeground(Color.WHITE);
            setBackground(PRIMARY);
            return this;
        }
    }

    private static void styleInput(JComponent input) {
        input.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        input.setForeground(TEXT);
        input.setBackground(Color.WHITE);
        input.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(190, 198, 208)),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
    }
}
