package panel.transaksi;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalDate;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.DefaultTableModel;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import kamera.KameraService;
import service.AbsensiService;
import service.face.OpenCvBridge;
import service.face.WajahDAO;
import util.Session;

/**
 * Absensi HRD (UI kode, BAGIAN B & G):
 *  - tab Daftar Absensi: cari nama/NIK, filter periode, lihat jam & status verifikasi
 *  - tab Audit Verifikasi: riwayat kegagalan verifikasi (log_verifikasi_absensi)
 *  - tab Registrasi Wajah: daftar/daftar-ulang wajah karyawan (hanya HRD)
 *  - tab Pengaturan Lokasi: latitude, longitude, radius (maks 200 m)
 *
 * HRD tidak bisa mengubah hasil verifikasi wajah secara langsung: nilai
 * verifikasi hanya ditulis oleh alur absensi, bukan dari form ini.
 */
public class AbsensiHRDPanel extends JPanel {

    private static final Color BIRU_TUA = new Color(18, 40, 90);
    private static final Color ABU = new Color(90, 105, 125);
    private static final Color HIJAU = new Color(20, 184, 166);
    private static final Color MERAH = new Color(239, 68, 68);
    private static final Color ABU_MUDA = new Color(243, 244, 246);

    private final DefaultTableModel modelAbsensi = nonEditable(
            "ID", "Karyawan", "Tanggal", "Masuk", "Pulang", "Status",
            "Verifikasi Wajah", "Skor", "Jarak (m)", "Sumber");
    private final JTable tblAbsensi = new JTable(modelAbsensi);
    private final JTextField txtCari = new JTextField(18);
    private final JComboBox<String> cmbPeriode = new JComboBox<>(new String[]{
        "7 hari terakhir", "30 hari terakhir", "Bulan ini", "Semua"});
    private final JTextArea areaLog = new JTextArea(12, 60);

    private final DefaultTableModel modelWajah = nonEditable(
            "ID Karyawan", "Nama", "Status Wajah", "Didaftarkan Pada");
    private final JTable tblWajah = new JTable(modelWajah);
    private final JButton btnDaftarkanWajah = tombol("Daftarkan Wajah (kamera)", HIJAU, Color.WHITE);
    private final JLabel lblStatusDaftar = new JLabel("Pilih karyawan, lalu daftarkan wajahnya.");
    private final KameraService kameraDaftar = new KameraService();
    private final JLabel lblPreviewDaftar = new JLabel("Kamera untuk registrasi belum dinyalakan",
            JLabel.CENTER);
    private final WajahDAO wajahDAO = new WajahDAO();
    private final java.util.List<float[]> sampelWajah = new java.util.ArrayList<float[]>();
    private final List<BufferedImage> cadanganFrame = new java.util.ArrayList<BufferedImage>();

    private final LocationPanelForm pnlLokasi = new LocationPanelForm();

    public AbsensiHRDPanel() {
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        setBackground(Color.WHITE);

        JLabel judul = new JLabel("Absensi Karyawan (HRD)");
        judul.setFont(new Font(util.DesignUtil.UI_FONT, Font.BOLD, 24));
        judul.setForeground(BIRU_TUA);
        judul.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 0));
        add(judul, BorderLayout.NORTH);

        JTabbedPane tab = new JTabbedPane();
        tab.addTab("Daftar Absensi", bangunTabDaftar());
        tab.addTab("Audit Verifikasi", bangunTabAudit());
        tab.addTab("Registrasi Wajah", bangunTabRegistrasi());
        tab.addTab("Pengaturan Lokasi", pnlLokasi);
        add(tab, BorderLayout.CENTER);

        muatAbsensi();
        muatLog();
        muatStatusWajah();
        pnlLokasi.muat();
    }

    // ------------------------------------------------------- tab daftar

    private JPanel bangunTabDaftar() {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBorder(BorderFactory.createEmptyBorder(12, 4, 4, 4));

        JPanel filter = new JPanel();
        filter.setOpaque(false);
        filter.setLayout(new BoxLayout(filter, BoxLayout.X_AXIS));
        filter.add(new JLabel("Cari (nama / NIK):"));
        filter.add(Box.createHorizontalStrut(6));
        filter.add(txtCari);
        filter.add(Box.createHorizontalStrut(10));
        filter.add(new JLabel("Periode:"));
        filter.add(Box.createHorizontalStrut(6));
        filter.add(cmbPeriode);
        filter.add(Box.createHorizontalStrut(10));
        JButton btnCari = tombol("Cari", ABU_MUDA, Color.BLACK);
        JButton btnRefresh = tombol("Refresh", ABU_MUDA, Color.BLACK);
        filter.add(btnCari);
        filter.add(Box.createHorizontalStrut(6));
        filter.add(btnRefresh);
        filter.add(Box.createHorizontalGlue());
        p.add(filter, BorderLayout.NORTH);

        btnCari.addActionListener(e -> muatAbsensi());
        btnRefresh.addActionListener(e -> { txtCari.setText(""); cmbPeriode.setSelectedIndex(0); muatAbsensi(); });
        txtCari.addActionListener(e -> muatAbsensi());

        tblAbsensi.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        util.DesignUtil.applyTable(tblAbsensi);
        p.add(new JScrollPane(tblAbsensi), BorderLayout.CENTER);
        return p;
    }

    private void muatAbsensi() {
        modelAbsensi.setRowCount(0);
        String kondisiPeriode;
        int idx = cmbPeriode.getSelectedIndex();
        if (idx == 0) {
            kondisiPeriode = "AND a.tanggal >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)";
        } else if (idx == 1) {
            kondisiPeriode = "AND a.tanggal >= DATE_SUB(CURDATE(), INTERVAL 30 DAY)";
        } else if (idx == 2) {
            kondisiPeriode = "AND YEAR(a.tanggal)=YEAR(CURDATE()) AND MONTH(a.tanggal)=MONTH(CURDATE())";
        } else {
            kondisiPeriode = "";
        }
        String cari = txtCari.getText().trim();
        StringBuilder sql = new StringBuilder(
            "SELECT a.id_absensi, k.nama_karyawan, a.tanggal, a.jam_masuk, a.jam_pulang, "
          + "a.status, a.verifikasi_wajah, a.skor_wajah, a.jarak_meter, a.sumber "
          + "FROM absensi a JOIN karyawan k ON a.id_karyawan = k.id_karyawan "
          + "WHERE 1=1 " + kondisiPeriode + " ");
        boolean pakaiCari = !cari.isEmpty();
        if (pakaiCari) sql.append("AND (k.nama_karyawan LIKE ? OR k.nik LIKE ?) ");
        sql.append("ORDER BY a.tanggal DESC, a.jam_masuk DESC");
        try (java.sql.Connection c = config.Koneksi.getConnection();
             java.sql.PreparedStatement ps = c.prepareStatement(sql.toString())) {
            if (pakaiCari) { ps.setString(1, "%" + cari + "%"); ps.setString(2, "%" + cari + "%"); }
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object skor = rs.getObject("skor_wajah");
                    Object jarak = rs.getObject("jarak_meter");
                    modelAbsensi.addRow(new Object[]{
                        rs.getInt("id_absensi"),
                        rs.getString("nama_karyawan"),
                        rs.getString("tanggal"),
                        rs.getString("jam_masuk") == null ? "-" : rs.getString("jam_masuk"),
                        rs.getString("jam_pulang") == null ? "-" : rs.getString("jam_pulang"),
                        rs.getString("status") == null ? "-" : rs.getString("status"),
                        rs.getString("verifikasi_wajah") == null ? "-" : rs.getString("verifikasi_wajah"),
                        skor == null ? "-" : String.format("%.2f", rs.getDouble("skor_wajah")),
                        jarak == null ? "-" : String.format("%.0f", rs.getDouble("jarak_meter")),
                        rs.getString("sumber") == null ? "MANUAL" : rs.getString("sumber")});
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal memuat absensi: " + e.getMessage());
        }
    }

    // -------------------------------------------------------- tab audit

    private JPanel bangunTabAudit() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setBorder(BorderFactory.createEmptyBorder(12, 4, 4, 4));
        JLabel ket = new JLabel("<html>Riwayat <b>kegagalan verifikasi</b> — dicatat tanpa menyimpan "
                + "foto mentah berlebih (privasi). Hanya HRD yang dapat melihat.</html>");
        ket.setForeground(ABU);
        p.add(ket, BorderLayout.NORTH);

        areaLog.setEditable(false);
        areaLog.setFont(new Font(java.awt.Font.MONOSPACED, Font.PLAIN, 12));
        p.add(new JScrollPane(areaLog), BorderLayout.CENTER);

        JPanel bawah = new JPanel();
        bawah.setOpaque(false);
        JButton btnMuat = tombol("Muat Ulang", ABU_MUDA, Color.BLACK);
        btnMuat.addActionListener(e -> muatLog());
        bawah.add(btnMuat);
        p.add(bawah, BorderLayout.SOUTH);
        return p;
    }

    private void muatLog() {
        StringBuilder sb = new StringBuilder();
        String sql = "SELECT l.tanggal, l.waktu, k.nama_karyawan, l.alasan, l.detail "
                   + "FROM log_verifikasi_absensi l JOIN karyawan k ON l.id_karyawan = k.id_karyawan "
                   + "ORDER BY l.tanggal DESC, l.waktu DESC LIMIT 200";
        try (java.sql.Connection c = config.Koneksi.getConnection();
             java.sql.PreparedStatement ps = c.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                sb.append(String.format("%s %s  | %-20s | %-22s | %s%n",
                        rs.getString("tanggal"), rs.getString("waktu"),
                        rs.getString("nama_karyawan"), rs.getString("alasan"),
                        rs.getString("detail") == null ? "" : rs.getString("detail")));
            }
        } catch (Exception e) {
            sb.append("Gagal memuat log: ").append(e.getMessage());
        }
        if (sb.length() == 0) sb.append("Belum ada catatan kegagalan verifikasi.");
        areaLog.setText(sb.toString());
    }

    // --------------------------------------------------- tab registrasi

    private JPanel bangunTabRegistrasi() {
        JPanel p = new JPanel(new BorderLayout(12, 8));
        p.setBorder(BorderFactory.createEmptyBorder(12, 4, 4, 4));

        lblPreviewDaftar.setPreferredSize(new Dimension(480, 360));
        lblPreviewDaftar.setBackground(new Color(15, 27, 51));
        lblPreviewDaftar.setOpaque(true);
        lblPreviewDaftar.setForeground(new Color(205, 213, 223));
        JPanel kiri = new JPanel(new BorderLayout(0, 8));
        kiri.setBorder(BorderFactory.createTitledBorder("Kamera registrasi (tanpa mirror)"));
        kiri.add(lblPreviewDaftar, BorderLayout.CENTER);

        JPanel alur = new JPanel();
        alur.setOpaque(false);
        alur.setLayout(new BoxLayout(alur, BoxLayout.Y_AXIS));
        JLabel info = new JLabel("<html>Wajib <b>minimal 3 sampel</b> (depan, sedikit kiri, "
                + "sedikit kanan). Satu wajah per sampel, pencahayaan wajar.</html>");
        info.setForeground(ABU);
        info.setAlignmentX(LEFT_ALIGNMENT);

        JButton btnNyalakan = tombol("Nyalakan Kamera", BIRU_TUA, Color.WHITE);
        JButton btnSampel = tombol("Ambil Sampel", HIJAU, Color.WHITE);
        JButton btnResetSampel = tombol("Reset Sampel", ABU_MUDA, Color.BLACK);
        JButton btnSimpanWajah = tombol("Simpan Template", new Color(79, 70, 229), Color.WHITE);

        lblStatusDaftar.setAlignmentX(LEFT_ALIGNMENT);
        btnDaftarkanWajah.setAlignmentX(LEFT_ALIGNMENT); // dipakai sebagai penanda gaya

        alur.add(info);
        alur.add(Box.createVerticalStrut(10));
        alur.add(btnNyalakan);
        alur.add(Box.createVerticalStrut(6));
        alur.add(btnSampel);
        alur.add(Box.createVerticalStrut(6));
        alur.add(btnSimpanWajah);
        alur.add(Box.createVerticalStrut(6));
        alur.add(btnResetSampel);
        alur.add(Box.createVerticalStrut(14));
        alur.add(lblStatusDaftar);

        alur.add(Box.createVerticalStrut(14));
        tblWajah.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        util.DesignUtil.applyTable(tblWajah);
        JScrollPane sc = new JScrollPane(tblWajah);
        sc.setPreferredSize(new Dimension(420, 260));
        sc.setBorder(BorderFactory.createTitledBorder("Daftar karyawan & status wajah"));
        alur.add(sc);

        p.add(kiri, BorderLayout.CENTER);
        p.add(alur, BorderLayout.EAST);

        btnNyalakan.addActionListener(e -> {
            if (kameraDaftar.berjalan()) {
                kameraDaftar.berhenti();
                btnNyalakan.setText("Nyalakan Kamera");
            } else {
                kameraDaftar.tambahPendengar(new KameraService.PendengarFrame() {
                    @Override public void frameBaru(BufferedImage gambar) {
                        SwingUtilities.invokeLater(() -> {
                            lblPreviewDaftar.setIcon(new javax.swing.ImageIcon(
                                    gambar.getScaledInstance(480, 360, java.awt.Image.SCALE_FAST)));
                            lblPreviewDaftar.setText("");
                        });
                    }
                    @Override public void kameraGagal(String pesan) {
                        SwingUtilities.invokeLater(() -> lblStatusDaftar.setText("Kamera: " + pesan));
                    }
                });
                if (!kameraDaftar.mulai()) {
                    lblStatusDaftar.setText(kameraDaftar.status());
                } else {
                    btnNyalakan.setText("Matikan Kamera");
                }
            }
        });

        btnSampel.addActionListener(e -> {
            BufferedImage frame = kameraDaftar.frameTerakhir();
            if (frame == null) { lblStatusDaftar.setText("Belum ada gambar kamera."); return; }
            if (!OpenCvBridge.siap()) OpenCvBridge.siapkanModel();
            Mat mat = KameraService.gambarKeMat(frame);
            try {
                List<OpenCvBridge.WajahTerdeteksi> wajah = OpenCvBridge.deteksiWajah(mat);
                if (wajah.isEmpty()) {
                    lblStatusDaftar.setText("Gagal: tidak ada wajah. Registrasi ditolak.");
                    return;
                }
                if (wajah.size() > 1) {
                    lblStatusDaftar.setText("Gagal: " + wajah.size() + " wajah terdeteksi. "
                            + "Pastikan hanya satu orang di depan kamera.");
                    return;
                }
                Rect area = new Rect(wajah.get(0).x, wajah.get(0).y,
                        wajah.get(0).lebar, wajah.get(0).tinggi);
                if (!service.face.LivenessService.kualitasCukup(mat, area)) {
                    lblStatusDaftar.setText("Gagal: kualitas gambar terlalu buruk/gelap. Ulangi.");
                    return;
                }
                float[] emb = OpenCvBridge.hitungEmbedding(mat.submat(area));
                sampelWajah.add(emb);
                cadanganFrame.add(frame);
                lblStatusDaftar.setText("Sampel ke-" + sampelWajah.size()
                        + " tersimpan. Minimal 3 sampel sebelum bisa disimpan.");
            } catch (Throwable t) {
                lblStatusDaftar.setText("Gagal mengambil sampel: " + t.getMessage());
            } finally {
                mat.release();
            }
        });

        btnResetSampel.addActionListener(e -> {
            sampelWajah.clear();
            cadanganFrame.clear();
            lblStatusDaftar.setText("Sampel dikosongkan.");
        });

        btnSimpanWajah.addActionListener(e -> {
            int baris = tblWajah.getSelectedRow();
            if (baris < 0) { lblStatusDaftar.setText("Pilih karyawan di tabel dulu."); return; }
            int idKaryawan = Integer.parseInt(String.valueOf(modelWajah.getValueAt(baris, 0)));
            String nama = String.valueOf(modelWajah.getValueAt(baris, 1));
            try {
                // izin dicek di service (bukan hanya menyembunyikan tombol)
                wajahDAO.simpanTemplate(Session.isHRD(), Session.getIdUser(),
                        idKaryawan, sampelWajah, 100);
                lblStatusDaftar.setText("Wajah " + nama + " berhasil didaftarkan ("
                        + sampelWajah.size() + " sampel).");
                sampelWajah.clear();
                cadanganFrame.clear();
                muatStatusWajah();
            } catch (SecurityException se) {
                lblStatusDaftar.setText("Ditolak: " + se.getMessage());
            } catch (Exception ex) {
                lblStatusDaftar.setText("Gagal menyimpan: " + ex.getMessage());
            }
        });

        return p;
    }

    private void muatStatusWajah() {
        modelWajah.setRowCount(0);
        try {
            for (String[] b : wajahDAO.daftarStatusPendaftaran()) {
                modelWajah.addRow(b);
            }
        } catch (Exception e) {
            lblStatusDaftar.setText("Gagal memuat status wajah: " + e.getMessage());
        }
    }

    // -------------------------------------------------------------- util

    private static DefaultTableModel nonEditable(String... kolom) {
        return new DefaultTableModel(new Object[0][0], kolom) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
    }

    private static JButton tombol(String teks, Color bg, Color fg) {
        JButton b = new JButton(teks);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setFont(new Font(util.DesignUtil.UI_FONT, Font.BOLD, 13));
        return b;
    }

    @Override
    public void removeNotify() {
        kameraDaftar.berhenti();
        super.removeNotify();
    }
}
