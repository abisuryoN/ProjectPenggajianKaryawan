package panel.dashboard;

import auth.Session;
import config.Koneksi;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

public class HomeHRDPanel extends JPanel {
    public HomeHRDPanel() {
        initUI();
    }

    private void initUI() {
        setLayout(new BorderLayout());
        setBackground(new Color(244, 247, 250));
        setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        JPanel wrapper = new JPanel();
        wrapper.setOpaque(false);
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));

        wrapper.add(createHeaderCard());
        wrapper.add(Box.createVerticalStrut(18));
        wrapper.add(createSummaryCards());
        wrapper.add(Box.createVerticalStrut(18));
        wrapper.add(createBottomSection());

        add(wrapper, BorderLayout.NORTH);
    }

    private JPanel createHeaderCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(224, 229, 234)),
                BorderFactory.createEmptyBorder(22, 24, 22, 24)
        ));

        JPanel text = new JPanel();
        text.setOpaque(false);
        text.setLayout(new BoxLayout(text, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Dashboard HRD");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(new Color(22, 48, 92));

        JLabel welcome = new JLabel("Selamat datang, " + Session.getNama());
        welcome.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        welcome.setForeground(new Color(84, 96, 110));

        JLabel desc = new JLabel("Kelola master data, transaksi absensi, penggajian, dan laporan dari satu dashboard.");
        desc.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        desc.setForeground(new Color(110, 118, 128));

        text.add(title);
        text.add(Box.createVerticalStrut(10));
        text.add(welcome);
        text.add(Box.createVerticalStrut(6));
        text.add(desc);

        card.add(text, BorderLayout.CENTER);
        return card;
    }

   private JPanel createSummaryCards() {
    JPanel grid = new JPanel(new GridLayout(1, 4, 16, 0));
    grid.setOpaque(false);

    grid.add(createStatCard(String.valueOf(getTotalKaryawan()), "Total Karyawan", new Color(36, 107, 253)));
    grid.add(createStatCard(String.valueOf(getTotalDivisi()), "Total Divisi", new Color(14, 165, 164)));
    grid.add(createStatCard(String.valueOf(getTotalJabatan()), "Total Jabatan", new Color(249, 115, 22)));
    grid.add(createStatCard(getTotalPayroll(), "Payroll Bulan Ini", new Color(34, 197, 94)));

    return grid;
}

    private JPanel createStatCard(String value, String title, Color accent) {
        JPanel card = new JPanel(new BorderLayout());
        card.setPreferredSize(new Dimension(220, 110));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(224, 229, 234)),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));

        JPanel dot = new JPanel();
        dot.setBackground(accent);
        dot.setPreferredSize(new Dimension(14, 14));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblTitle.setForeground(new Color(115, 123, 132));

        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblValue.setForeground(new Color(35, 47, 62));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        top.setOpaque(false);
        top.add(dot);
        top.add(Box.createHorizontalStrut(8));
        top.add(lblTitle);

        card.add(top, BorderLayout.NORTH);
        card.add(lblValue, BorderLayout.CENTER);
        return card;
    }

    private JPanel createBottomSection() {
        JPanel bottom = new JPanel(new GridLayout(1, 2, 18, 0));
        bottom.setOpaque(false);

        bottom.add(createQuickAccessCard());
        bottom.add(createActivityCard());
        return bottom;
    }

    private JPanel createQuickAccessCard() {
    JPanel card = new JPanel(new BorderLayout());
    card.setBackground(Color.WHITE);
    card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(224, 229, 234)),
            BorderFactory.createEmptyBorder(18, 18, 18, 18)
    ));

    JLabel title = new JLabel("Akses Cepat");
    title.setFont(new Font("Segoe UI", Font.BOLD, 18));
    title.setForeground(new Color(35, 47, 62));
    card.add(title, BorderLayout.NORTH);

    JPanel btnWrap = new JPanel(new GridLayout(2, 2, 12, 12));
    btnWrap.setOpaque(false);
    btnWrap.setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));

    JButton btnInputKaryawan = createMiniButton("Input Karyawan");
    btnInputKaryawan.addActionListener(e -> {
        // Cari parent frame dan pindah ke Data Karyawan
        java.awt.Window window = javax.swing.SwingUtilities.getWindowAncestor(this);
        if (window instanceof javax.swing.JFrame) {
            javax.swing.JFrame frame = (javax.swing.JFrame) window;
            // Trigger klik menu Data Karyawan
            for (java.awt.Component comp : frame.getContentPane().getComponents()) {
                System.out.println(comp.getClass().getName());
            }
        }
    });

    btnWrap.add(btnInputKaryawan);
    btnWrap.add(createMiniButton("Input Absensi"));
    btnWrap.add(createMiniButton("Proses Gaji"));
    btnWrap.add(createMiniButton("Cetak Laporan"));

    card.add(btnWrap, BorderLayout.CENTER);
    return card;
}

    private JButton createMiniButton(String text) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setBackground(new Color(245, 247, 251));
        button.setForeground(new Color(32, 48, 76));
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(214, 221, 229)),
                BorderFactory.createEmptyBorder(14, 12, 14, 12)
        ));
        return button;
    }

    private JPanel createActivityCard() {
    JPanel card = new JPanel(new BorderLayout());
    card.setBackground(Color.WHITE);
    card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(224, 229, 234)),
            BorderFactory.createEmptyBorder(18, 18, 18, 18)
    ));

    JLabel title = new JLabel("Ringkasan Aktivitas");
    title.setFont(new Font("Segoe UI", Font.BOLD, 18));
    title.setForeground(new Color(35, 47, 62));
    card.add(title, BorderLayout.NORTH);

    int absensiHariIni = getAbsensiHariIni();
    int totalKaryawan = getTotalKaryawan();

    JTextArea textArea = new JTextArea();
    textArea.setEditable(false);
    textArea.setOpaque(false);
    textArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
    textArea.setForeground(new Color(95, 104, 114));
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    textArea.setText(
            "• " + absensiHariIni + " karyawan sudah melakukan absensi hari ini.\n\n"
            + "• Total " + totalKaryawan + " karyawan terdaftar.\n\n"
            + "• Penggajian bulan berjalan siap diproses.\n\n"
            + "• Laporan absensi dan gaji dapat dicetak dari menu laporan."
    );
    textArea.setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));
    card.add(textArea, BorderLayout.CENTER);

    return card;
}
private int getTotalKaryawan() {
    try {
        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement("SELECT COUNT(*) FROM karyawan");
        ResultSet rs = pst.executeQuery();
        if (rs.next()) return rs.getInt(1);
    } catch (Exception e) {}
    return 0;
}

private int getTotalDivisi() {
    try {
        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement("SELECT COUNT(*) FROM divisi");
        ResultSet rs = pst.executeQuery();
        if (rs.next()) return rs.getInt(1);
    } catch (Exception e) {}
    return 0;
}

private int getTotalJabatan() {
    try {
        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement("SELECT COUNT(*) FROM jabatan");
        ResultSet rs = pst.executeQuery();
        if (rs.next()) return rs.getInt(1);
    } catch (Exception e) {}
    return 0;
}

private String getTotalPayroll() {
    try {
        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement(
            "SELECT SUM(total_gaji) FROM penggajian WHERE periode = DATE_FORMAT(NOW(), '%Y-%m')"
        );
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            double total = rs.getDouble(1);
            if (total >= 1000000) {
                return String.format("Rp %.1f jt", total / 1000000);
            }
            return "Rp " + (long) total;
        }
    } catch (Exception e) {}
    return "Rp 0";
}

private int getAbsensiHariIni() {
    try {
        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement(
            "SELECT COUNT(*) FROM absensi WHERE tanggal = CURDATE()"
        );
        ResultSet rs = pst.executeQuery();
        if (rs.next()) return rs.getInt(1);
    } catch (Exception e) {}
    return 0;
}
}
