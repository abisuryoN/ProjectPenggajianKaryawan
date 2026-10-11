package view;

import util.Session;
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
import panel.absensi.AbsensiHRDPanel;
import panel.absensi.AbsensiKaryawanPanel;
import panel.transaksi.PenggajianPanel;

public class SidebarPanel extends JPanel {
    private final DashboardFrame dashboard;
    private final java.util.List<JButton> menuButtons = new java.util.ArrayList<>();
    private JButton activeMenu;

    public SidebarPanel(DashboardFrame dashboard) {
        this.dashboard = dashboard;
        initUI();
    }

    private void initUI() {
        setBackground(new Color(15, 27, 51)); // #0F1B33
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(24, 12, 24, 12));

        JLabel title = new JLabel("PenggajianApp");
        title.setForeground(Color.WHITE);
        title.setFont(new Font(util.DesignUtil.UI_FONT, Font.BOLD, 20));
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(title);

        add(Box.createVerticalStrut(6));

        JLabel user = new JLabel(Session.getNama());
        user.setForeground(new Color(205, 213, 223));
        user.setFont(new Font(util.DesignUtil.UI_FONT, Font.PLAIN, 14));
        user.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(user);

        JLabel role = new JLabel(Session.isHRD() ? "Role: HRD" : "Role: Karyawan");
        role.setForeground(new Color(120, 200, 190));
        role.setFont(new Font(util.DesignUtil.UI_FONT, Font.PLAIN, 12));
        role.setAlignmentX(Component.LEFT_ALIGNMENT);
        add(role);

        add(Box.createVerticalStrut(20));

        addMenuButton("Dashboard", () -> dashboard.tampilHome());
        // menu aktif default = Dashboard
        if (!menuButtons.isEmpty()) setActiveMenu(menuButtons.get(0));

        if (Session.isHRD()) {
            addSection("MASTER DATA");
            addMenuButton("Data Karyawan", () -> dashboard.tampilPanel(new DataKaryawanPanel()));
            addMenuButton("Data Jabatan", () -> dashboard.tampilPanel(new DataJabatanPanel()));
            addMenuButton("Data Tunjangan", () -> dashboard.tampilPanel(new DataTunjanganPanel()));
            addMenuButton("Data Divisi", () -> dashboard.tampilPanel(new DataDivisiPanel()));

            addSection("TRANSAKSI");
            addMenuButton("Absensi Karyawan (HRD)", () -> dashboard.tampilPanel(new AbsensiHRDPanel()));
            addMenuButton("Absensi Saya (HRD)", () -> dashboard.tampilPanel(new AbsensiKaryawanPanel()));
            addMenuButton("Penggajian", () -> dashboard.tampilPanel(new PenggajianPanel()));

            addSection("LAPORAN");
            addMenuButton("Laporan Absensi", () -> dashboard.tampilPanel(new LaporanAbsensiPanel()));
            addMenuButton("Laporan Gaji", () -> dashboard.tampilPanel(new LaporanGajiPanel()));
            addMenuButton("Laporan Karyawan", () -> dashboard.tampilPanel(new LaporanKaryawanPanel()));
            addMenuButton("Laporan Divisi", () -> dashboard.tampilPanel(new LaporanDivisiPanel()));
        } else {
            addSection("MENU KARYAWAN");
            addMenuButton("Absensi", () -> dashboard.tampilPanel(new AbsensiKaryawanPanel()));
            addMenuButton("Riwayat Absensi", () -> dashboard.tampilPanel(new LaporanAbsensiPanel()));
            addMenuButton("Slip Gaji", () -> dashboard.tampilPanel(new LaporanGajiPanel()));
        }

        add(Box.createVerticalStrut(20));
        addMenuButton("Logout", () -> dashboard.logout());
        add(Box.createVerticalStrut(20));
    }

    private void addSection(String text) {
        add(Box.createVerticalStrut(16));

        JLabel label = new JLabel(text);
        label.setForeground(new Color(140, 155, 175));
        label.setFont(new Font(util.DesignUtil.UI_FONT, Font.BOLD, 11));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        label.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 0));
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
        button.setFont(new Font(util.DesignUtil.UI_FONT, Font.PLAIN, 13));
        button.setAlignmentX(Component.LEFT_ALIGNMENT);
        applyMenuStyle(button, false);

        button.addActionListener(e -> {
            setActiveMenu(button);
            action.run();
        });

        menuButtons.add(button);
        add(button);
        add(Box.createVerticalStrut(8));
    }

    /** Gaya tombol menu: normal = transparan, aktif = aksen teal. */
    private void applyMenuStyle(JButton b, boolean aktif) {
        if (aktif) {
            b.setBackground(new Color(20, 184, 166));      // #14B8A6
            b.setForeground(Color.WHITE);
            b.setOpaque(true);
        } else {
            b.setBackground(new Color(15, 27, 51));         // #0F1B33
            b.setForeground(new Color(205, 213, 223));
            b.setOpaque(true);
        }
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(0, 4, 0, 4),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
    }

    /** Tandai tombol menu aktif (halaman yang sedang dibuka). */
    public void setActiveMenu(JButton aktif) {
        if (activeMenu != null && activeMenu != aktif) {
            applyMenuStyle(activeMenu, false);
        }
        activeMenu = aktif;
        applyMenuStyle(aktif, true);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension size = super.getPreferredSize();
        return new Dimension(225, size.height);
    }
}
