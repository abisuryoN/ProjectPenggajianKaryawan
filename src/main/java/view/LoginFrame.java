package view;

import auth.LoginService;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

public class LoginFrame extends JFrame {
    private JTextField txtUsername;
    private JPasswordField txtPassword;
    private JButton btnLogin;

    public LoginFrame() {
        initUI();
    }

    private void initUI() {
        setTitle("Login - Sistem Penggajian Karyawan");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 560);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new BorderLayout());

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(createCardPanel(), BorderLayout.CENTER);
        add(mainPanel, BorderLayout.CENTER);
    }

    private JPanel createCardPanel() {
        JPanel container = new JPanel(new BorderLayout());
        container.setBackground(Color.WHITE);
        container.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(220, 226, 234)),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        container.add(createLeftInfoPanel(), BorderLayout.WEST);
        container.add(createRightLoginPanel(), BorderLayout.CENTER);
        return container;
    }

    private JPanel createLeftInfoPanel() {
        JPanel left = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(22, 46, 78), 0, getHeight(), new Color(18, 191, 168));
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        left.setPreferredSize(new Dimension(390, 0));
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBorder(BorderFactory.createEmptyBorder(50, 42, 50, 42));

        JLabel icon = new JLabel("▦", SwingConstants.LEFT);
        icon.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 64));
        icon.setForeground(Color.WHITE);
        icon.setAlignmentX(LEFT_ALIGNMENT);

        JLabel title = new JLabel("PENGGAJIAN");
        title.setFont(new Font("Segoe UI", Font.BOLD, 34));
        title.setForeground(Color.WHITE);
        title.setAlignmentX(LEFT_ALIGNMENT);

        JLabel subtitle = new JLabel("SISTEM MANAJEMEN KARYAWAN");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        subtitle.setForeground(new Color(222, 240, 245));
        subtitle.setAlignmentX(LEFT_ALIGNMENT);

        JPanel line = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(new Color(255, 255, 255, 140));
                g2.setStroke(new BasicStroke(1.3f));
                g2.drawLine(0, getHeight() / 2, getWidth(), getHeight() / 2);
                g2.dispose();
            }
        };
        line.setOpaque(false);
        line.setMaximumSize(new Dimension(230, 14));
        line.setPreferredSize(new Dimension(230, 14));
        line.setAlignmentX(LEFT_ALIGNMENT);

        left.add(icon);
        left.add(Box.createVerticalStrut(8));
        left.add(title);
        left.add(Box.createVerticalStrut(10));
        left.add(subtitle);
        left.add(Box.createVerticalStrut(26));
        left.add(line);
        left.add(Box.createVerticalStrut(28));
        left.add(createFeatureLabel("• Pengelolaan Data Karyawan & Divisi"));
        left.add(Box.createVerticalStrut(14));
        left.add(createFeatureLabel("• Absensi Manual dan Monitoring"));
        left.add(Box.createVerticalStrut(14));
        left.add(createFeatureLabel("• Perhitungan Gaji & Tunjangan"));
        left.add(Box.createVerticalStrut(14));
        left.add(createFeatureLabel("• Laporan Kehadiran dan Penggajian"));
        left.add(Box.createVerticalStrut(14));
        left.add(createFeatureLabel("• Akses HRD dan Karyawan"));
        left.add(Box.createVerticalGlue());

        return left;
    }

    private JLabel createFeatureLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        label.setForeground(new Color(236, 248, 250));
        label.setAlignmentX(LEFT_ALIGNMENT);
        return label;
    }

    private JPanel createRightLoginPanel() {
        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(new Color(245, 247, 251));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        formPanel.setPreferredSize(new Dimension(420, 360));

        JLabel title = new JLabel("Selamat Datang");
        title.setFont(new Font("Segoe UI", Font.BOLD, 28));
        title.setForeground(new Color(24, 50, 92));

        JLabel subtitle = new JLabel("Silakan login untuk melanjutkan");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        subtitle.setForeground(new Color(102, 110, 125));

        JLabel lblUsername = new JLabel("Username");
        lblUsername.setFont(new Font("Segoe UI", Font.BOLD, 13));

        JLabel lblPassword = new JLabel("Password");
        lblPassword.setFont(new Font("Segoe UI", Font.BOLD, 13));

        txtUsername = new JTextField();
        txtPassword = new JPasswordField();
        txtUsername.setPreferredSize(new Dimension(360, 44));
        txtPassword.setPreferredSize(new Dimension(360, 44));
        txtUsername.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(215, 221, 228)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        txtPassword.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(215, 221, 228)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        btnLogin = new JButton("MASUK");
        btnLogin.setPreferredSize(new Dimension(360, 48));
        btnLogin.setFont(new Font("Segoe UI", Font.BOLD, 20));
        btnLogin.setBackground(new Color(28, 49, 92));
        btnLogin.setForeground(Color.WHITE);
        btnLogin.setFocusPainted(false);
        btnLogin.setOpaque(true);
        btnLogin.setContentAreaFilled(true);
        btnLogin.setBorderPainted(false);
        btnLogin.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        btnLogin.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        JLabel info = new JLabel("Akun login dibuat oleh HRD. Tidak ada registrasi mandiri.");
        info.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        info.setForeground(new Color(0, 139, 122));

        JLabel demo = new JLabel("Demo: hrd / hrd123   |   karyawan / karyawan123");
        demo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        demo.setForeground(new Color(120, 126, 138));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 6, 0);
        formPanel.add(title, gbc);
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 24, 0);
        formPanel.add(subtitle, gbc);
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 8, 0);
        formPanel.add(lblUsername, gbc);
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 20, 0);
        formPanel.add(txtUsername, gbc);
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 8, 0);
        formPanel.add(lblPassword, gbc);
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 24, 0);
        formPanel.add(txtPassword, gbc);
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 14, 0);
        formPanel.add(btnLogin, gbc);
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 8, 0);
        formPanel.add(info, gbc);
        gbc.gridy++;
        gbc.insets = new Insets(0, 0, 0, 0);
        formPanel.add(demo, gbc);

        right.add(formPanel);

        btnLogin.addActionListener(e -> prosesLogin());
        txtPassword.addActionListener(e -> prosesLogin());
        return right;
    }

    private void prosesLogin() {
        String username = txtUsername.getText().trim();
        String password = new String(txtPassword.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username dan password wajib diisi.", "Peringatan", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (LoginService.login(username, password)) {
            dispose();
            new DashboardFrame().setVisible(true);
        } else {
            JOptionPane.showMessageDialog(this, "Username atau password salah.", "Login Gagal", JOptionPane.ERROR_MESSAGE);
        }
    }
}
