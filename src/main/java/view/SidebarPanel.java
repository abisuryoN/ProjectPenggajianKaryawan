package view;

import auth.Session;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import panel.laporan.LaporanAbsensiPanel;
import panel.laporan.LaporanDivisiPanel;
import panel.laporan.LaporanGajiPanel;
import panel.laporan.LaporanKaryawanPanel;
import panel.master.DataDivisiPanel;
import panel.master.DataJabatanPanel;
import panel.master.DataKaryawanPanel;
import panel.master.DataTunjanganPanel;
import panel.transaksi.AbsensiPanel;
import panel.transaksi.PenggajianPanel;

public class SidebarPanel extends JPanel {
    private final DashboardFrame dashboard;

    public SidebarPanel(DashboardFrame dashboard) {
        this.dashboard = dashboard;
        initUI();
    }

    private void initUI() {
        setBackground(new Color(28, 40, 58));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(24, 12, 24, 12));

        JLabel title = new JLabel("PenggajianApp");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(title);

        add(Box.createVerticalStrut(6));

        JLabel user = new JLabel(Session.getNama());
        user.setForeground(new Color(205, 213, 223));
        user.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        user.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(user);

        JLabel role = new JLabel(Session.isHRD() ? "Role: HRD" : "Role: Karyawan");
        role.setForeground(new Color(120, 200, 190));
        role.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        role.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(role);

        add(Box.createVerticalStrut(20));

        addMenuButton("Dashboard", () -> dashboard.tampilHome());

        if (Session.isHRD()) {
            addSection("MASTER DATA");
            addMenuButton("Data Karyawan", () -> dashboard.tampilPanel(new DataKaryawanPanel()));
            addMenuButton("Data Jabatan", () -> dashboard.tampilPanel(new DataJabatanPanel()));
            addMenuButton("Data Tunjangan", () -> dashboard.tampilPanel(new DataTunjanganPanel()));
            addMenuButton("Data Divisi", () -> dashboard.tampilPanel(new DataDivisiPanel()));

            addSection("TRANSAKSI");
            addMenuButton("Absensi", () -> dashboard.tampilPanel(new AbsensiPanel()));
            addMenuButton("Penggajian", () -> dashboard.tampilPanel(new PenggajianPanel()));

            addSection("LAPORAN");
            addMenuButton("Laporan Absensi", () -> dashboard.tampilPanel(new LaporanAbsensiPanel()));
            addMenuButton("Laporan Gaji", () -> dashboard.tampilPanel(new LaporanGajiPanel()));
            addMenuButton("Laporan Karyawan", () -> dashboard.tampilPanel(new LaporanKaryawanPanel()));
            addMenuButton("Laporan Divisi", () -> dashboard.tampilPanel(new LaporanDivisiPanel()));
        } else {
            addSection("MENU KARYAWAN");
            addMenuButton("Absensi", () -> dashboard.tampilPanel(new AbsensiPanel()));
            addMenuButton("Riwayat Absensi", () -> dashboard.tampilPanel(new LaporanAbsensiPanel()));
            addMenuButton("Slip Gaji", () -> dashboard.tampilPanel(new LaporanGajiPanel()));
        }

        add(Box.createVerticalStrut(20));
        addMenuButton("Logout", () -> dashboard.logout());
        add(Box.createVerticalStrut(20));
    }

    private void addSection(String text) {
        add(Box.createVerticalStrut(14));

        JLabel label = new JLabel(text);
        label.setForeground(new Color(160, 170, 182));
        label.setFont(new Font("Segoe UI", Font.BOLD, 11));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(label);

        add(Box.createVerticalStrut(8));
    }

    private void addMenuButton(String text, Runnable action) {
        JButton button = new JButton(text);
        button.setFocusPainted(false);
        button.setHorizontalAlignment(JButton.LEFT);
        button.setMaximumSize(new Dimension(210, 40));
        button.setPreferredSize(new Dimension(210, 40));
        button.setMinimumSize(new Dimension(210, 40));
        button.setBackground(new Color(245, 248, 252));
        button.setForeground(new Color(42, 52, 66));
        button.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 225, 230)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);

        button.addActionListener(e -> action.run());

        add(button);
        add(Box.createVerticalStrut(8));
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        return new Dimension(225, size.height);
    }
}
