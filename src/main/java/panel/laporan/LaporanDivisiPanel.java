package panel.laporan;

import config.Koneksi;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;
import report.CetakLaporanDivisi;

public class LaporanDivisiPanel extends javax.swing.JPanel {
    private final JTextField txtCari = new JTextField(24);
    private final JButton btnCari = new JButton("Cari");
    private final JButton btnCetak = new JButton("Cetak");
    private final JButton btnExport = new JButton("Export CSV");
    private final JButton btnRefresh = new JButton("Refresh");
    private final JButton btnReset = new JButton("Reset");
    private final JTable tblDivisi = new JTable();

    public LaporanDivisiPanel() {
        initComponents();
        buildUI();
        tampilData();
        util.DesignUtil.applyPage(this);
    }

    private void buildUI() {
        removeAll();
        setLayout(new BorderLayout(20, 18));
        setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        JLabel title = new JLabel("LAPORAN DIVISI KARYAWAN");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        add(title, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 14));
        content.setBorder(BorderFactory.createTitledBorder("DATA LAPORAN DIVISI"));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 12));
        toolbar.add(new JLabel("Cari Divisi / Kepala / Keterangan"));
        toolbar.add(txtCari);
        toolbar.add(btnCari);
        toolbar.add(btnCetak);
        toolbar.add(btnExport);
        toolbar.add(btnRefresh);
        toolbar.add(btnReset);
        content.add(toolbar, BorderLayout.NORTH);

        tblDivisi.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        content.add(new JScrollPane(tblDivisi), BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);

        btnCari.addActionListener(e -> cariData());
        btnCetak.addActionListener(e -> cetakData());
        btnExport.addActionListener(e -> exportCSV());
        btnRefresh.addActionListener(e -> tampilData());
        btnReset.addActionListener(e -> {
            txtCari.setText("");
            tampilData();
        });
    }

    private void tampilData() {
        DefaultTableModel model = buatModel();
        try {
            Connection conn = Koneksi.getConnection();
            PreparedStatement pst = conn.prepareStatement(
                    "SELECT id_divisi, nama_divisi, kepala_divisi, keterangan FROM divisi ORDER BY id_divisi ASC"
            );
            ResultSet rs = pst.executeQuery();
            int no = 1;
            while (rs.next()) {
                model.addRow(new Object[]{
                    no++,
                    rs.getString("id_divisi"),
                    rs.getString("nama_divisi"),
                    rs.getString("kepala_divisi"),
                    rs.getString("keterangan")
                });
            }
            tblDivisi.setModel(model);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal menampilkan laporan divisi: " + e.getMessage());
        }
    }

    private void cariData() {
        DefaultTableModel model = buatModel();
        String keyword = "%" + txtCari.getText().trim() + "%";
        try {
            Connection conn = Koneksi.getConnection();
            PreparedStatement pst = conn.prepareStatement(
                    "SELECT id_divisi, nama_divisi, kepala_divisi, keterangan FROM divisi "
                    + "WHERE nama_divisi LIKE ? OR kepala_divisi LIKE ? OR keterangan LIKE ? "
                    + "ORDER BY id_divisi ASC"
            );
            pst.setString(1, keyword);
            pst.setString(2, keyword);
            pst.setString(3, keyword);
            ResultSet rs = pst.executeQuery();
            int no = 1;
            while (rs.next()) {
                model.addRow(new Object[]{
                    no++,
                    rs.getString("id_divisi"),
                    rs.getString("nama_divisi"),
                    rs.getString("kepala_divisi"),
                    rs.getString("keterangan")
                });
            }
            tblDivisi.setModel(model);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal mencari laporan divisi: " + e.getMessage());
        }
    }

    private DefaultTableModel buatModel() {
        DefaultTableModel model = new DefaultTableModel();
        model.addColumn("NO");
        model.addColumn("ID Divisi");
        model.addColumn("Nama Divisi");
        model.addColumn("Kepala Divisi");
        model.addColumn("Keterangan");
        return model;
    }

    private void cetakData() {
        try {
            new CetakLaporanDivisi().cetak(tblDivisi);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal mencetak laporan divisi: " + e.getMessage());
        }
    }

    private void exportCSV() {
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("laporan_divisi.csv"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }

        try {
            new CetakLaporanDivisi().exportCSV(tblDivisi, chooser.getSelectedFile().getAbsolutePath());
            JOptionPane.showMessageDialog(this, "Laporan divisi berhasil diexport.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal export laporan divisi: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
    }
}
