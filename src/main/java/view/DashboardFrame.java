package view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import panel.dashboard.HomeHRDPanel;
import panel.dashboard.HomeKaryawanPanel;

public class DashboardFrame extends JFrame {
    private JPanel contentPanel;

    public DashboardFrame() {
        initUI();
    }

    private void initUI() {
        // ✅ Ganti auth.Session -> util.Session dan getNama() -> getNamaKaryawan()
        setTitle("Dashboard - " + util.Session.getNamaKaryawan());
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1366, 768);
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        contentPanel = new JPanel(new BorderLayout());
        contentPanel.setBackground(new Color(244, 247, 250));

        SidebarPanel sidebarPanel = new SidebarPanel(this);
        JScrollPane sidebarScroll = new JScrollPane(sidebarPanel);
        sidebarScroll.setPreferredSize(new Dimension(240, 0));
        sidebarScroll.setBorder(BorderFactory.createEmptyBorder());
        sidebarScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        sidebarScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        sidebarScroll.getVerticalScrollBar().setUnitIncrement(16);

        JScrollPane contentScroll = new JScrollPane(contentPanel);
        contentScroll.setBorder(BorderFactory.createEmptyBorder());
        contentScroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        contentScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        contentScroll.getVerticalScrollBar().setUnitIncrement(16);
        contentScroll.getHorizontalScrollBar().setUnitIncrement(16);

        add(sidebarScroll, BorderLayout.WEST);
        add(contentScroll, BorderLayout.CENTER);

        tampilHome();
    }

    public void tampilPanel(JPanel panel) {
        contentPanel.removeAll();
        contentPanel.add(panel, BorderLayout.CENTER);
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    public void tampilHome() {
        // ✅ Ganti Session.isHRD() -> cek role dari util.Session
        if ("HRD".equalsIgnoreCase(util.Session.getRole())) {
            tampilPanel(new HomeHRDPanel());
        } else {
            tampilPanel(new HomeKaryawanPanel());
        }
    }

    public void logout() {
        // ✅ Ganti Session.clear() -> util.Session.logout()
        util.Session.logout();
        dispose();
        new LoginFrame().setVisible(true);
    }
}