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
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.JTableHeader;

public final class DesignUtil {
    private static final Color PAGE_BG = new Color(244, 247, 250);
    private static final Color CARD_BG = Color.WHITE;
    private static final Color TEXT = new Color(17, 24, 39);
    private static final Color MUTED = new Color(75, 85, 99);
    private static final Color PRIMARY = new Color(22, 48, 92);
    private static final Color LINE = new Color(220, 226, 235);

    // ---- Palet bersama panel laporan (dipakai semua panel agar warnanya sama) ----
    public static final Color WARNA_JUDUL = PRIMARY;                  // navy judul
    public static final Color TOMBOL_UTAMA = new Color(37, 99, 235);  // biru
    public static final Color TOMBOL_SEKUNDER = new Color(20, 184, 166); // teal
    public static final Color TOMBOL_BAHAYA = new Color(239, 68, 68); // merah
    public static final Color TOMBOL_NETRAL = new Color(243, 244, 246); // abu terang
    public static final Color TEKS_GELAP = new Color(17, 24, 39);

    /** Font UI yang benar-benar ada di sistem (Windows pakai Segoe UI, Linux pakai font sistem). */
    public static final String UI_FONT;

    static {
        String[] kandidat = {"Segoe UI", "Inter", "Ubuntu", "Cantarell", "Noto Sans", "DejaVu Sans", "Dialog"};
        java.util.Set<String> tersedia = new java.util.HashSet<>(java.util.Arrays.asList(
                java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        String pilihan = "Dialog";
        for (String k : kandidat) {
            if (tersedia.contains(k)) {
                pilihan = k;
                break;
            }
        }
        UI_FONT = pilihan;
    }

    private DesignUtil() {
    }

    /** Judul laporan: di tengah, HURUF BESAR, bold. Dipakai semua panel laporan. */
    public static void styleJudulLaporan(JLabel label, String judul) {
        label.setText(judul.toUpperCase(java.util.Locale.ROOT));
        label.setFont(new Font(UI_FONT, Font.BOLD, 24));
        label.setForeground(WARNA_JUDUL);
        label.setHorizontalAlignment(SwingConstants.CENTER);
    }

    /** Tombol laporan dengan warna seragam di seluruh panel. */
    public static void styleFilledButton(JButton button, Color bg, Color fg) {
        button.setUI(new BasicButtonUI());
        button.setBackground(bg);
        button.setForeground(fg);
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(false);
        button.setFocusPainted(false);
        button.setFont(new Font(UI_FONT, Font.BOLD, 12));
        button.setBorder(BorderFactory.createEmptyBorder(7, 16, 7, 16));
    }

    public static void applyPage(JPanel panel) {
        panel.setBackground(PAGE_BG);
        // JANGAN kunci ukuran page: biarkan layout manager yang mengatur supaya
        // panel ikut melar (full) saat window di-maximize.
        panel.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        applyComponent(panel);
        // bersihkan sisa preferred-size kaku pada anak-anak panel
        longgar(panel);
    }

    /**
     * Hapus preferred/minimum size kaku yang di-set oleh generated code Form NetBeans
     * (contoh nyata: pnlUtama 1000x650, pnlForm 300x430, bahkan setPreferredSize 5437x993)
     * supaya komponen ikut melebar penuh.
     */
    public static void longgar(java.awt.Container container) {
        for (Component child : container.getComponents()) {
            if (child instanceof JComponent) {
                JComponent jc = (JComponent) child;
                // jangan ganggu tombol (punya ukuran tombol yang memang pas)
                if (!(jc instanceof JButton)
                        && !(jc instanceof JTextField)
                        && !(jc instanceof JComboBox)
                        && !(jc instanceof JTextArea)) {
                    jc.setMinimumSize(null);
                    Dimension pref = jc.getPreferredSize();
                    if (pref != null && pref.width > 1400) {
                        // buang preferred size yang tidak wajar (mis. 5437 px)
                        jc.setPreferredSize(null);
                    }
                }
            }
            if (child instanceof java.awt.Container) {
                longgar((java.awt.Container) child);
            }
        }
    }

    private static void applyComponent(Component component) {
        if (component instanceof JPanel) {
            JPanel panel = (JPanel) component;
            panel.setBackground(component.getParent() == null ? PAGE_BG : CARD_BG);
            if (panel.getBorder() instanceof TitledBorder) {
                TitledBorder border = (TitledBorder) panel.getBorder();
                border.setTitleColor(PRIMARY);
                border.setTitleFont(new Font(util.DesignUtil.UI_FONT, Font.BOLD, 13));
                panel.setBorder(BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(LINE),
                        border.getTitle(),
                        TitledBorder.DEFAULT_JUSTIFICATION,
                        TitledBorder.DEFAULT_POSITION,
                        border.getTitleFont(),
                        border.getTitleColor()
                ));
            } else if (panel.getParent() != null) {
                panel.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
            }
        } else if (component instanceof JLabel) {
            styleLabel((JLabel) component);
        } else if (component instanceof JButton) {
            styleButton((JButton) component);
        } else if (component instanceof JTable) {
            styleTable((JTable) component);
        } else if (component instanceof JScrollPane) {
            JScrollPane scrollPane = (JScrollPane) component;
            scrollPane.setBorder(BorderFactory.createLineBorder(LINE));
            scrollPane.getViewport().setBackground(Color.WHITE);
            scrollPane.setBackground(Color.WHITE);
        } else if (component instanceof JTextField) {
            styleInput((JTextField) component);
        } else if (component instanceof JTextArea) {
            styleInput((JTextArea) component);
        } else if (component instanceof JComboBox) {
            styleCombo((JComboBox<?>) component);
        }

        if (component instanceof java.awt.Container) {
            for (Component child : ((java.awt.Container) component).getComponents()) {
                applyComponent(child);
            }
        }
    }

    private static void styleLabel(JLabel label) {
        String text = label.getText() == null ? "" : label.getText();
        label.setFont(new Font(util.DesignUtil.UI_FONT, isTitle(text) ? Font.BOLD : Font.PLAIN, isTitle(text) ? 24 : 13));
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
        button.setFont(new Font(util.DesignUtil.UI_FONT, Font.BOLD, 13));
        button.setBorder(BorderFactory.createEmptyBorder(8, 18, 8, 18));
        button.setPreferredSize(new Dimension(Math.max(92, button.getPreferredSize().width), 36));
        button.setMinimumSize(new Dimension(92, 34));
    }

    private static String cleanButtonText(String text) {
        if (text == null) return "";
        if (text.contains("HItung")) return "Hitung";
        if (text.contains("Filter")) return "Filter";
        if (text.contains("Cari")) return "Cari";
        if (text.contains("Cetak Slip")) return "Cetak Slip";
        if (text.contains("Cetak")) return "Cetak";
        if (text.contains("Export")) return "Export PDF";
        if (text.contains("Refresh")) return "Refresh";
        if (text.contains("Reset")) return "Reset";
        return text;
    }

    private static void styleTable(JTable table) {
        applyTable(table);
    }

    public static void applyTable(JTable table) {
        table.setFont(new Font(util.DesignUtil.UI_FONT, Font.PLAIN, 12));
        table.setForeground(TEXT);
        table.setRowHeight(23);
        table.setGridColor(LINE);
        table.setShowHorizontalLines(true);
        table.setShowVerticalLines(true);
        table.setFillsViewportHeight(true);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setIntercellSpacing(new Dimension(1, 1));
        table.setAutoCreateRowSorter(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        table.setBackground(Color.WHITE);
        table.setDefaultRenderer(Object.class, new CellRenderer());
        table.setDefaultEditor(Object.class, null);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
        table.setPreferredScrollableViewportSize(new Dimension(900, table.getRowHeight() * 10 + 28));
        if (!Boolean.TRUE.equals(table.getClientProperty("compactTableListenerInstalled"))) {
            table.addPropertyChangeListener("model", evt -> compactColumns(table));
            table.putClientProperty("compactTableListenerInstalled", Boolean.TRUE);
        }
        compactColumns(table);

        JTableHeader header = table.getTableHeader();
        if (header != null) {
            header.setFont(new Font(util.DesignUtil.UI_FONT, Font.BOLD, 12));
            header.setForeground(Color.WHITE);
            header.setBackground(PRIMARY);
            header.setOpaque(true);
            header.setReorderingAllowed(false);
            header.setPreferredSize(new Dimension(header.getPreferredSize().width, 27));
            header.setDefaultRenderer(new HeaderRenderer());
        }
    }

    public static void compactColumns(JTable table) {
        if (table.getColumnCount() == 0 || table.getColumnModel().getColumnCount() == 0) {
            return;
        }

        TableColumnModel columnModel = table.getColumnModel();
        int columnCount = Math.min(table.getColumnCount(), columnModel.getColumnCount());
        for (int column = 0; column < columnCount; column++) {
            int width = 44;
            Object headerValue = columnModel.getColumn(column).getHeaderValue();
            if (headerValue != null) {
                width = Math.max(width, headerValue.toString().length() * 8 + 24);
            }

            int rows = Math.min(table.getModel().getRowCount(), 200);
            for (int row = 0; row < rows; row++) {
                Object value = table.getModel().getValueAt(row, column);
                if (value != null) {
                    width = Math.max(width, value.toString().length() * 7 + 26);
                }
            }

            TableColumn tableColumn = columnModel.getColumn(column);
            tableColumn.setPreferredWidth(Math.min(Math.max(width, 64), 240));
        }
    }

    private static final class HeaderRenderer extends DefaultTableCellRenderer {
        HeaderRenderer() {
            setHorizontalAlignment(SwingConstants.CENTER);
            setOpaque(true);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(60, 86, 124)),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)
            ));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setText(value == null ? "" : value.toString());
            setFont(new Font(util.DesignUtil.UI_FONT, Font.BOLD, 12));
            setForeground(Color.WHITE);
            setBackground(PRIMARY);
            return this;
        }
    }

    private static final class CellRenderer extends DefaultTableCellRenderer {
        CellRenderer() {
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setFont(new Font(util.DesignUtil.UI_FONT, Font.PLAIN, 12));
            setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            if (isIdColumn(table, column)) {
                setHorizontalAlignment(SwingConstants.CENTER);
            } else if (isMoneyColumn(table, column)) {
                setHorizontalAlignment(SwingConstants.RIGHT);
            } else {
                setHorizontalAlignment(SwingConstants.LEFT);
            }
            return this;
        }

        private boolean isIdColumn(JTable table, int column) {
            String name = table.getColumnName(column);
            if (name == null) {
                return false;
            }
            String normalized = name.trim().toLowerCase();
            return normalized.equals("id")
                    || normalized.equals("no")
                    || normalized.equals("no.")
                    || normalized.startsWith("id ")
                    || normalized.startsWith("id_");
        }

        private boolean isMoneyColumn(JTable table, int column) {
            String name = table.getColumnName(column);
            if (name == null) {
                return false;
            }
            String normalized = name.trim().toLowerCase();
            return normalized.contains("gaji")
                    || normalized.contains("nominal")
                    || normalized.contains("tunjangan")
                    || normalized.contains("potongan")
                    || normalized.contains("total");
        }
    }

    private static void styleInput(JComponent input) {
        input.setFont(new Font(UI_FONT, Font.PLAIN, 13));
        input.setForeground(TEXT);
        input.setBackground(Color.WHITE);
        // caret hanya ada pada text component (JTextField/JTextArea/JTextPane)
        if (input instanceof javax.swing.text.JTextComponent) {
            ((javax.swing.text.JTextComponent) input).setCaretColor(TEXT);
        }
        input.setPreferredSize(new Dimension(Math.max(120, input.getPreferredSize().width), 34));
        input.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(190, 198, 208)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
    }

    /**
     * Dropdown/combobox yang rapi: tinggi lega, ada padding, warna teks & background
     * dipaksa konsisten (sebelumnya tampak "jelek" karena GTK L&F + ukuran 32 px sempit).
     */
    public static void styleCombo(JComboBox<?> combo) {
        combo.setFont(new Font(UI_FONT, Font.PLAIN, 13));
        combo.setForeground(TEXT);
        combo.setBackground(Color.WHITE);
        combo.setOpaque(true);
        combo.setPreferredSize(new Dimension(Math.max(150, combo.getPreferredSize().width), 34));
        combo.setMinimumSize(new Dimension(120, 34));
        combo.setBorder(BorderFactory.createLineBorder(new Color(190, 198, 208)));
        combo.setRenderer(new ComboRenderer());
    }

    private static final class ComboRenderer extends javax.swing.DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(javax.swing.JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            setFont(new Font(UI_FONT, Font.PLAIN, 13));
            setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            setText(value == null ? "" : value.toString());
            if (isSelected) {
                setBackground(PRIMARY);
                setForeground(Color.WHITE);
            } else {
                setBackground(Color.WHITE);
                setForeground(TEXT);
            }
            return this;
        }
    }
}
