package panel.dashboard;

import config.Koneksi;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;

public class HomeKaryawanPanel extends JPanel {
    
    public HomeKaryawanPanel() {
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
        wrapper.add(createInfoSection());

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

        JLabel title = new JLabel("Dashboard Karyawan");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(new Color(22, 48, 92));

        JLabel welcome = new JLabel("Selamat datang, " + util.Session.getNamaKaryawan());
        welcome.setFont(new Font("Segoe UI", Font.PLAIN, 17));
        welcome.setForeground(new Color(84, 96, 110));

        JLabel desc = new JLabel("Pantau absensi, riwayat kehadiran, dan slip gaji Anda dari dashboard ini.");
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
        JPanel grid = new JPanel(new GridLayout(1, 3, 16, 0));
        grid.setOpaque(false);
        grid.add(createStatCard(getKehadiranBulanIni() + " Hari", "Kehadiran Bulan Ini"));
        grid.add(createStatCard(getIzinCutiBulanIni() + " Hari", "Izin / Cuti"));
        grid.add(createStatCard(getEstimasiGaji(), "Estimasi Gaji"));
        return grid;
    }

    private JPanel createStatCard(String value, String title) {
        JPanel card = new JPanel(new BorderLayout());
        card.setPreferredSize(new Dimension(250, 110));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(224, 229, 234)),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        lblTitle.setForeground(new Color(115, 123, 132));

        JLabel lblValue = new JLabel(value);
        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblValue.setForeground(new Color(35, 47, 62));

        card.add(lblTitle, BorderLayout.NORTH);
        card.add(lblValue, BorderLayout.CENTER);
        return card;
    }

    private JPanel createInfoSection() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(224, 229, 234)),
                BorderFactory.createEmptyBorder(18, 18, 18, 18)
        ));

        JLabel title = new JLabel("Informasi Saya");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(new Color(35, 47, 62));
        card.add(title, BorderLayout.NORTH);

        JTextArea textArea = new JTextArea();
        textArea.setEditable(false);
        textArea.setOpaque(false);
        textArea.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        textArea.setForeground(new Color(95, 104, 114));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.setText(
                "• Gunakan menu Absensi untuk melakukan absensi masuk dan pulang.\n\n"
                + "• Menu Riwayat Absensi digunakan untuk melihat kehadiran Anda sendiri.\n\n"
                + "• Menu Slip Gaji digunakan untuk melihat rincian gaji pribadi.\n\n"
                + "• Jika ada perubahan data, silakan hubungi HRD."
        );
        textArea.setBorder(BorderFactory.createEmptyBorder(18, 0, 0, 0));
        card.add(textArea, BorderLayout.CENTER);
        return card;
    }

    private int getKehadiranBulanIni() {
        try {
            int idKaryawan = util.Session.getIdKaryawan();
            Connection conn = Koneksi.getConnection();
            PreparedStatement pst = conn.prepareStatement(
                "SELECT COUNT(*) FROM absensi WHERE id_karyawan = ? "
                + "AND status = 'Hadir' "
                + "AND MONTH(tanggal) = MONTH(NOW()) AND YEAR(tanggal) = YEAR(NOW())"
            );
            pst.setInt(1, idKaryawan);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) {}
        return 0;
    }

    private int getIzinCutiBulanIni() {
        try {
            int idKaryawan = util.Session.getIdKaryawan();
            Connection conn = Koneksi.getConnection();
            PreparedStatement pst = conn.prepareStatement(
                "SELECT COUNT(*) FROM absensi WHERE id_karyawan = ? "
                + "AND status IN ('Izin', 'Cuti') "
                + "AND MONTH(tanggal) = MONTH(NOW()) AND YEAR(tanggal) = YEAR(NOW())"
            );
            pst.setInt(1, idKaryawan);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) return rs.getInt(1);
        } catch (Exception e) {}
        return 0;
    }

    private String getEstimasiGaji() {
        try {
            int idKaryawan = util.Session.getIdKaryawan();
            Connection conn = Koneksi.getConnection();
            PreparedStatement pst = conn.prepareStatement(
                "SELECT j.gaji_pokok FROM karyawan k "
                + "JOIN jabatan j ON k.id_jabatan = j.id_jabatan "
                + "WHERE k.id_karyawan = ?"
            );
            pst.setInt(1, idKaryawan);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                double gaji = rs.getDouble("gaji_pokok");
                return String.format("Rp %,.0f", gaji);
            }
        } catch (Exception e) {}
        return "Rp 0";
    }
}
