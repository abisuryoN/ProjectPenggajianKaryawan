package panel.absensi;

import kamera.KameraService;
import model.LokasiKantor;
import service.AbsensiService;
import service.LocationService;
import service.face.LivenessService;
import service.face.OpenCvBridge;
import service.face.WajahDAO;
import util.Session;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
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
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Halaman ABSENSI HRD (kode, bukan .form lama) — administrasi:
 * tabel riwayat + pencarian + filter tanggal, status verifikasi/lokasi,
 * izin bukti foto, pengaturan koordinat & radius kantor, serta log
 * verifikasi gagal untuk audit.
 *
 * <p>Registrasi wajah karyawan TIDAK dicampur di sini (itu halaman
 * karyawan di {@link RegistrasiWajahPanel}); di sini HRD hanya melihat
 * STATUS registrasi dan mendaftarkan ulang bila perlu.
 */
public class AbsensiHRDPanel extends JPanel {

    private final AbsensiService absensiService = new AbsensiService();
    private final LocationService lokasiService = new LocationService();
    private final WajahDAO wajahDAO = new WajahDAO();

    // ---- tab riwayat
    private final JTextField txtCari = AbsensiUI.input(18);
    private final JTextField txtTanggal = AbsensiUI.input(12);
    private final JTextField txtTanggalAkhir = AbsensiUI.input(12);
    private final DefaultTableModel modelRiwayat = new DefaultTableModel(new Object[0][0],
            new String[]{"ID Absen", "Tanggal", "NIK", "Nama", "Masuk", "Pulang",
                    "Status", "Verifikasi", "Jarak (m)", "Bukti"}) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblRiwayat = new JTable(modelRiwayat);
    private final JLabel lblRingkas = AbsensiUI.subJudul("Memuat...");

    // ---- tab lokasi
    private final JTextField txtNamaLokasi = AbsensiUI.input(24);
    private final JTextField txtLat = AbsensiUI.input(14);
    private final JTextField txtLon = AbsensiUI.input(14);
    private final JTextField txtRadius = AbsensiUI.input(8);
    private final JLabel lblStatusLokasi = AbsensiUI.subJudul("Belum ada konfigurasi lokasi.");
    private final JLabel lblPetaPreview = new JLabel("Koordinat belum diisi", JLabel.CENTER);

    // ---- tab wajah
    private final DefaultTableModel modelWajah = new DefaultTableModel(new Object[0][0],
            new String[]{"ID Karyawan", "NIK", "Nama", "Status", "Kualitas", "Didaftarkan"}) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblWajah = new JTable(modelWajah);

    // ---- tab audit
    private final JTextArea areaLog = new JTextArea();

    // ---- kanvas bukti
    private final JLabel lblBukti = new JLabel("Pilih baris untuk melihat bukti foto", JLabel.CENTER);
    private final KameraService kamera = new KameraService();

    public AbsensiHRDPanel() {
        setLayout(new BorderLayout());
        setBackground(AbsensiUI.PAGE);

        JPanel atas = new JPanel(new BorderLayout());
        atas.setOpaque(false);
        atas.setBorder(BorderFactory.createEmptyBorder(16, 18, 8, 18));
        AbsensiUI.RoundedPanel h = AbsensiUI.header();
        JPanel kiri = new JPanel();
        kiri.setOpaque(false);
        kiri.setLayout(new BoxLayout(kiri, BoxLayout.Y_AXIS));
        JLabel j = AbsensiUI.judul("Absensi HRD");
        JLabel s = AbsensiUI.subJudul("Administrasi kehadiran: riwayat, verifikasi wajah, "
                + "bukti foto, radius kantor, dan audit");
        j.setAlignmentX(LEFT_ALIGNMENT);
        s.setAlignmentX(LEFT_ALIGNMENT);
        kiri.add(j);
        kiri.add(Box.createVerticalStrut(4));
        kiri.add(s);
        h.add(kiri, BorderLayout.CENTER);
        atas.add(h, BorderLayout.CENTER);

        JTabbedPane tab = new JTabbedPane();
        tab.setFont(AbsensiUI.font(Font.PLAIN, 13));
        tab.addTab("Riwayat & Bukti", bangunTabRiwayat());
        tab.addTab("Wajah Karyawan", bangunTabWajah());
        tab.addTab("Lokasi & Radius", bangunTabLokasi());
        tab.addTab("Audit Verifikasi", bangunTabAudit());

        JPanel tengah = new JPanel(new BorderLayout());
        tengah.setOpaque(false);
        tengah.setBorder(BorderFactory.createEmptyBorder(0, 18, 16, 18));
        tengah.add(tab, BorderLayout.CENTER);

        add(atas, BorderLayout.NORTH);
        add(tengah, BorderLayout.CENTER);

        muatRiwayat();
        muatStatusWajah();
        muatKonfigurasiLokasi();
        muatLog();
    }

    // ---------------------------------------------------------------- tab 1

    private JPanel bangunTabRiwayat() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(12, 4, 4, 4));

        JPanel kartuFilter = AbsensiUI.kartu();
        JPanel baris = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        baris.setOpaque(false);
        baris.add(AbsensiUI.label("Cari (NIK/Nama):"));
        baris.add(txtCari);
        baris.add(AbsensiUI.label("Dari tanggal:"));
        txtTanggal.setToolTipText("yyyy-MM-dd");
        txtTanggalAkhir.setToolTipText("yyyy-MM-dd");
        baris.add(txtTanggal);
        baris.add(AbsensiUI.label("s/d"));
        baris.add(txtTanggalAkhir);
        JButton btnCari = AbsensiUI.tombol("Cari", AbsensiUI.AKSEN_BIRU);
        JButton btnReset = AbsensiUI.tombolNetral("Reset");
        JButton btnHariIni = AbsensiUI.tombolNetral("Hari Ini");
        JButton btnEkspor = AbsensiUI.tombolNetral("Ekspor CSV");
        baris.add(btnCari);
        baris.add(btnReset);
        baris.add(btnHariIni);
        baris.add(btnEkspor);
        kartuFilter.add(baris, BorderLayout.CENTER);
        kartuFilter.add(lblRingkas, BorderLayout.SOUTH);

        btnCari.addActionListener(e -> muatRiwayat());
        btnReset.addActionListener(e -> {
            txtCari.setText("");
            txtTanggal.setText("");
            txtTanggalAkhir.setText("");
            muatRiwayat();
        });
        btnHariIni.addActionListener(e -> {
            String t = LocalDate.now().toString();
            txtTanggal.setText(t);
            txtTanggalAkhir.setText(t);
            muatRiwayat();
        });
        btnEkspor.addActionListener(e -> eksporCsv());

        tblRiwayat.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        util.DesignUtil.applyTable(tblRiwayat);
        tblRiwayat.setRowHeight(26);
        JScrollPane scTabel = new JScrollPane(tblRiwayat);
        scTabel.setBorder(BorderFactory.createLineBorder(AbsensiUI.GARIS));
        scTabel.setPreferredSize(new Dimension(880, 300));

        JPanel kartuTabel = AbsensiUI.kartu();
        kartuTabel.add(AbsensiUI.judulBagian("Riwayat absensi"), BorderLayout.NORTH);
        kartuTabel.add(scTabel, BorderLayout.CENTER);

        lblBukti.setPreferredSize(new Dimension(320, 260));
        lblBukti.setOpaque(true);
        lblBukti.setBackground(new Color(15, 27, 51));
        lblBukti.setForeground(new Color(203, 213, 225));
        JPanel kartuBukti = AbsensiUI.kartu();
        kartuBukti.add(AbsensiUI.judulBagian("Bukti foto absensi"), BorderLayout.NORTH);
        kartuBukti.add(lblBukti, BorderLayout.CENTER);

        tblRiwayat.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) tampilkanBukti();
        });

        GridBagConstraints g = AbsensiUI.gbc(0, 0);
        g.gridwidth = 2;
        g.weightx = 1; g.weighty = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        p.add(kartuFilter, g);

        g = AbsensiUI.gbc(0, 1);
        g.weightx = 0.7; g.weighty = 1;
        g.fill = GridBagConstraints.BOTH;
        p.add(kartuTabel, g);

        g = AbsensiUI.gbc(1, 1);
        g.weightx = 0.3; g.weighty = 1;
        g.fill = GridBagConstraints.BOTH;
        p.add(kartuBukti, g);
        return p;
    }

    private void muatRiwayat() {
        modelRiwayat.setRowCount(0);
        StringBuilder sql = new StringBuilder(
                "SELECT a.id_absensi, a.tanggal, k.nik, k.nama_karyawan AS nama, "
              + "a.jam_masuk, a.jam_pulang, a.status, a.verifikasi_wajah, a.jarak_meter, a.foto_bukti "
              + "FROM absensi a LEFT JOIN karyawan k ON k.id_karyawan = a.id_karyawan WHERE 1=1 ");
        java.util.List<Object> param = new java.util.ArrayList<>();
        String cari = txtCari.getText().trim();
        if (!cari.isEmpty()) {
            sql.append("AND (k.nama_karyawan LIKE ? OR k.nik LIKE ?) ");
            param.add("%" + cari + "%");
            param.add("%" + cari + "%");
        }
        String t1 = txtTanggal.getText().trim();
        String t2 = txtTanggalAkhir.getText().trim();
        if (!t1.isEmpty()) { sql.append("AND a.tanggal >= ? "); param.add(t1); }
        if (!t2.isEmpty()) { sql.append("AND a.tanggal <= ? "); param.add(t2); }
        sql.append("ORDER BY a.tanggal DESC, a.jam_masuk DESC LIMIT 500");

        try (java.sql.Connection c = config.Koneksi.getConnection();
             java.sql.PreparedStatement ps = c.prepareStatement(sql.toString())) {
            for (int i = 0; i < param.size(); i++) ps.setObject(i + 1, param.get(i));
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                int cocok = 0, gagalWajah = 0;
                while (rs.next()) {
                    String verif = rs.getString("verifikasi_wajah");
                    if (verif != null && !"COCOK".equals(verif)) gagalWajah++;
                    Object jarak = rs.getObject("jarak_meter");
                    String bukti = rs.getString("foto_bukti");
                    modelRiwayat.addRow(new Object[]{
                            rs.getInt("id_absensi"),
                            rs.getString("tanggal"),
                            rs.getString("nik") == null ? "-" : rs.getString("nik"),
                            rs.getString("nama") == null ? "-" : rs.getString("nama"),
                            rs.getString("jam_masuk") == null ? "-" : rs.getString("jam_masuk"),
                            rs.getString("jam_pulang") == null ? "-" : rs.getString("jam_pulang"),
                            rs.getString("status") == null ? "-" : rs.getString("status"),
                            verif == null ? "-" : verif,
                            jarak == null ? "-" : String.format("%.0f", rs.getDouble("jarak_meter")),
                            (bukti == null || bukti.isEmpty()) ? "tidak ada" : "ada"});
                    cocok++;
                }
                lblRingkas.setText(cocok + " baris ditampilkan  •  " + gagalWajah
                        + " dengan verifikasi wajah tidak cocok");
                lblRingkas.setForeground(AbsensiUI.TEKS_LEMBUT);
            }
        } catch (Exception e) {
            lblRingkas.setText("Gagal memuat riwayat: " + e.getMessage());
            lblRingkas.setForeground(AbsensiUI.MERAH);
        }
    }

    private void tampilkanBukti() {
        int baris = tblRiwayat.getSelectedRow();
        if (baris < 0) { lblBukti.setIcon(null); return; }
        // Hak akses: hanya HRD yang boleh membuka bukti foto (dicek di sini DAN
        // dilaporkan ke log agar tidak sekadar menyembunyikan komponen).
        if (!Session.isHRD()) {
            // id_karyawan boleh 0 (HRD tanpa tautan) — log tanpa FK karyawan.
            int idUntukLog = Session.getIdKaryawan() > 0 ? Session.getIdKaryawan() : -1;
            if (idUntukLog > 0) {
                AbsensiService.catatLog(idUntukLog, LocalDate.now(),
                        AbsensiService.ALASAN_WAJAH_TAK_DIKENAL,
                        "Percobaan membuka bukti foto tanpa hak HRD");
            } else {
                System.err.println("Percobaan membuka bukti foto tanpa hak HRD.");
            }
            lblBukti.setIcon(null);
            lblBukti.setText("Akses bukti foto hanya untuk HRD.");
            return;
        }
        String idAbsen = String.valueOf(modelRiwayat.getValueAt(baris, 0));
        String path = null;
        try (java.sql.Connection c = config.Koneksi.getConnection();
             java.sql.PreparedStatement ps = c.prepareStatement(
                     "SELECT foto_bukti FROM absensi WHERE id_absensi = ?")) {
            ps.setInt(1, Integer.parseInt(idAbsen));
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                if (rs.next()) path = rs.getString(1);
            }
        } catch (Exception ignored) { }
        if (path == null || path.trim().isEmpty()) {
            lblBukti.setText("Absensi ini tidak memiliki bukti foto.");
            lblBukti.setIcon(null);
            return;
        }
        try {
            java.io.File f = new java.io.File(path);
            if (!f.exists()) {
                lblBukti.setText("File bukti tidak ditemukan di disk.");
                lblBukti.setIcon(null);
                return;
            }
            BufferedImage img = javax.imageio.ImageIO.read(f);
            Image skala = img.getScaledInstance(300, 240, Image.SCALE_SMOOTH);
            lblBukti.setIcon(new javax.swing.ImageIcon(skala));
            lblBukti.setText("");
        } catch (Exception e) {
            lblBukti.setText("Bukti tidak dapat dibuka: " + e.getMessage());
            lblBukti.setIcon(null);
        }
    }

    private void eksporCsv() {
        if (modelRiwayat.getRowCount() == 0) {
            lblRingkas.setText("Tidak ada data untuk diekspor.");
            return;
        }
        javax.swing.JFileChooser fc = new javax.swing.JFileChooser();
        fc.setSelectedFile(new java.io.File("riwayat_absensi_" + LocalDate.now() + ".csv"));
        if (fc.showSaveDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) return;
        try (java.io.PrintWriter pw = new java.io.PrintWriter(fc.getSelectedFile(), "UTF-8")) {
            for (int c = 0; c < modelRiwayat.getColumnCount(); c++) {
                if (c > 0) pw.print(';');
                pw.print(modelRiwayat.getColumnName(c));
            }
            pw.println();
            for (int r = 0; r < modelRiwayat.getRowCount(); r++) {
                for (int c = 0; c < modelRiwayat.getColumnCount(); c++) {
                    if (c > 0) pw.print(';');
                    Object v = modelRiwayat.getValueAt(r, c);
                    pw.print(v == null ? "" : String.valueOf(v).replace(';', ','));
                }
                pw.println();
            }
            lblRingkas.setText("Riwayat diekspor ke " + fc.getSelectedFile().getAbsolutePath());
            lblRingkas.setForeground(AbsensiUI.HIJAU);
        } catch (Exception e) {
            lblRingkas.setText("Ekspor gagal: " + e.getMessage());
            lblRingkas.setForeground(AbsensiUI.MERAH);
        }
    }

    // ---------------------------------------------------------------- tab 2

    private JPanel bangunTabWajah() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(12, 4, 4, 4));

        JLabel info = AbsensiUI.subJudul("Status registrasi wajah per karyawan. "
                + "Registrasi 3 foto dilakukan karyawan sendiri di halaman absensi karyawan "
                + "(memakai sesi login masing-masing).");
        p.add(info, BorderLayout.NORTH);

        tblWajah.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        util.DesignUtil.applyTable(tblWajah);
        tblWajah.setRowHeight(26);
        JScrollPane sc = new JScrollPane(tblWajah);
        sc.setBorder(BorderFactory.createLineBorder(AbsensiUI.GARIS));

        JPanel kartu = AbsensiUI.kartu();
        kartu.add(AbsensiUI.judulBagian("Registrasi wajah karyawan"), BorderLayout.NORTH);
        kartu.add(sc, BorderLayout.CENTER);

        JPanel aksi = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        aksi.setOpaque(false);
        JButton btnMuat = AbsensiUI.tombol("Muat Ulang", AbsensiUI.AKSEN_BIRU);
        JButton btnHapus = AbsensiUI.tombol("Hapus Template Terpilih", AbsensiUI.MERAH);
        btnMuat.addActionListener(e -> muatStatusWajah());
        btnHapus.addActionListener(e -> hapusTemplateWajah());
        aksi.add(btnMuat);
        aksi.add(btnHapus);

        JPanel bawah = new JPanel(new BorderLayout());
        bawah.setOpaque(false);
        bawah.add(aksi, BorderLayout.NORTH);
        kartu.add(bawah, BorderLayout.SOUTH);

        p.add(kartu, BorderLayout.CENTER);
        return p;
    }

    private void muatStatusWajah() {
        modelWajah.setRowCount(0);
        String sql = "SELECT k.id_karyawan, k.nik, k.nama_karyawan, "
                + "w.template IS NOT NULL AS punya, w.kualitas, w.didaftarkan_pada "
                + "FROM karyawan k LEFT JOIN wajah_karyawan w ON w.id_karyawan = k.id_karyawan "
                + "ORDER BY k.nama_karyawan";
        try (java.sql.Connection c = config.Koneksi.getConnection();
             java.sql.PreparedStatement ps = c.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                boolean punya = rs.getBoolean("punya");
                modelWajah.addRow(new Object[]{
                        rs.getInt("id_karyawan"),
                        rs.getString("nik") == null ? "-" : rs.getString("nik"),
                        rs.getString("nama_karyawan") == null ? "-" : rs.getString("nama_karyawan"),
                        punya ? "Terdaftar" : "Belum terdaftar",
                        punya ? rs.getInt("kualitas") + "/100" : "-",
                        rs.getString("didaftarkan_pada") == null ? "-" : rs.getString("didaftarkan_pada")});
            }
        } catch (Exception e) {
            modelWajah.addRow(new Object[]{"-", "Gagal memuat: " + e.getMessage(), "", "", "", ""});
        }
    }

    private void hapusTemplateWajah() {
        int baris = tblWajah.getSelectedRow();
        if (baris < 0) return;
        int idKaryawan = Integer.parseInt(String.valueOf(modelWajah.getValueAt(baris, 0)));
        int konfirmasi = javax.swing.JOptionPane.showConfirmDialog(this,
                "Hapus template wajah karyawan ID " + idKaryawan + "?\n"
                + "Karyawan tersebut harus registrasi ulang sebelum bisa absen.",
                "Konfirmasi", javax.swing.JOptionPane.YES_NO_OPTION);
        if (konfirmasi != javax.swing.JOptionPane.YES_OPTION) return;
        try {
            wajahDAO.hapusTemplate(Session.getIdUser(), idKaryawan);
            muatStatusWajah();
        } catch (Exception e) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Gagal menghapus template: " + e.getMessage());
        }
    }

    // ---------------------------------------------------------------- tab 3

    private JPanel bangunTabLokasi() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(12, 4, 4, 4));

        JPanel kartu = AbsensiUI.kartu();
        JPanel isi = new JPanel(new GridBagLayout());
        isi.setOpaque(false);

        txtRadius.setText(String.valueOf(LocationService.RADIUS_DEFAULT_METER));
        lblPetaPreview.setPreferredSize(new Dimension(320, 200));
        lblPetaPreview.setOpaque(true);
        lblPetaPreview.setBackground(AbsensiUI.LEMBUT);
        lblPetaPreview.setBorder(BorderFactory.createLineBorder(AbsensiUI.GARIS));
        lblPetaPreview.setFont(AbsensiUI.font(Font.PLAIN, 12));

        int r = 0;
        r = tambahBaris(isi, r, "Nama lokasi kantor", txtNamaLokasi);
        r = tambahBaris(isi, r, "Latitude", txtLat);
        r = tambahBaris(isi, r, "Longitude", txtLon);
        r = tambahBaris(isi, r, "Radius (meter, maks "
                + LocationService.RADIUS_MAKS_METER + ")", txtRadius);

        JButton btnSimpan = AbsensiUI.tombol("Simpan Konfigurasi", AbsensiUI.HIJAU);
        JButton btnIsiDariLokasi = AbsensiUI.tombolNetral("Isi dari Lokasi Saat Ini");
        btnSimpan.addActionListener(e -> simpanKonfigurasiLokasi());
        btnIsiDariLokasi.addActionListener(e -> isiDariLokasiSaatIni());

        JPanel tombol = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tombol.setOpaque(false);
        tombol.add(btnSimpan);
        tombol.add(btnIsiDariLokasi);
        GridBagConstraints g = AbsensiUI.gbc(0, r);
        g.gridwidth = 2;
        g.fill = GridBagConstraints.HORIZONTAL;
        isi.add(tombol, g);

        kartu.add(AbsensiUI.judulBagian("Pengaturan lokasi & radius kantor"), BorderLayout.NORTH);
        kartu.add(isi, BorderLayout.CENTER);
        kartu.add(lblStatusLokasi, BorderLayout.SOUTH);

        GridBagConstraints gg = AbsensiUI.gbc(0, 0);
        gg.weightx = 0.6; gg.weighty = 1;
        gg.fill = GridBagConstraints.BOTH;
        p.add(kartu, gg);
        gg = AbsensiUI.gbc(1, 0);
        gg.weightx = 0.4; gg.weighty = 1;
        gg.fill = GridBagConstraints.BOTH;
        p.add(lblPetaPreview, gg);
        return p;
    }

    private int tambahBaris(JPanel panel, int baris, String label, java.awt.Component field) {
        GridBagConstraints g = AbsensiUI.gbc(0, baris);
        g.weightx = 0; g.fill = GridBagConstraints.NONE;
        panel.add(AbsensiUI.label(label), g);
        g = AbsensiUI.gbc(1, baris);
        g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        panel.add(field, g);
        return baris + 1;
    }

    private void muatKonfigurasiLokasi() {
        try {
            LokasiKantor k = lokasiService.konfigurasiAktif();
            if (k == null) {
                lblStatusLokasi.setText("Belum ada konfigurasi lokasi tersimpan.");
                return;
            }
            txtNamaLokasi.setText(k.getNamaLokasi());
            txtLat.setText(String.valueOf(k.getLatitude()));
            txtLon.setText(String.valueOf(k.getLongitude()));
            txtRadius.setText(String.valueOf(k.getRadiusMeter()));
            lblStatusLokasi.setText("Konfigurasi aktif: " + k.getNamaLokasi() + " — radius "
                    + lokasiService.radiusEfektif(k) + " meter (batas keras "
                    + LocationService.RADIUS_MAKS_METER + " m).");
            lblStatusLokasi.setForeground(AbsensiUI.TEKS_LEMBUT);
            tampilkanPetaPreview();
        } catch (Exception e) {
            lblStatusLokasi.setText("Konfigurasi lokasi tidak dapat dibaca: " + e.getMessage());
            lblStatusLokasi.setForeground(AbsensiUI.MERAH);
        }
    }

    private void simpanKonfigurasiLokasi() {
        try {
            double lat = Double.parseDouble(txtLat.getText().trim().replace(',', '.'));
            double lon = Double.parseDouble(txtLon.getText().trim().replace(',', '.'));
            int radius = Integer.parseInt(txtRadius.getText().trim());
            String nama = txtNamaLokasi.getText().trim();
            lokasiService.simpanKonfigurasi(Session.isHRD(), Session.getIdUser(),
                    nama, lat, lon, radius);
            lblStatusLokasi.setText("Konfigurasi tersimpan: " + nama + " — radius "
                    + Math.min(radius, LocationService.RADIUS_MAKS_METER) + " meter.");
            lblStatusLokasi.setForeground(AbsensiUI.HIJAU);
            tampilkanPetaPreview();
        } catch (NumberFormatException nfe) {
            lblStatusLokasi.setText("Angka tidak valid. Periksa latitude/longitude/radius.");
            lblStatusLokasi.setForeground(AbsensiUI.MERAH);
        } catch (Exception e) {
            lblStatusLokasi.setText(e.getMessage());
            lblStatusLokasi.setForeground(AbsensiUI.MERAH);
        }
    }

    private void isiDariLokasiSaatIni() {
        new SwingWorker<service.LocationProvider.HasilLokasiPengguna, Void>() {
            @Override protected service.LocationProvider.HasilLokasiPengguna doInBackground() {
                return service.RantaiLocationProvider.standar().ambilLokasi();
            }
            @Override protected void done() {
                try {
                    service.LocationProvider.HasilLokasiPengguna h = get();
                    if (h.tersedia) {
                        txtLat.setText(String.valueOf(h.latitude));
                        txtLon.setText(String.valueOf(h.longitude));
                        lblStatusLokasi.setText("Koordinat diisi dari lokasi perangkat "
                                + "(akurasi ±" + Math.round(h.akurasiMeter) + " m).");
                        lblStatusLokasi.setForeground(AbsensiUI.TEKS_LEMBUT);
                        tampilkanPetaPreview();
                    } else {
                        lblStatusLokasi.setText("Lokasi perangkat tidak tersedia: " + h.keterangan);
                        lblStatusLokasi.setForeground(AbsensiUI.MERAH);
                    }
                } catch (Exception e) {
                    lblStatusLokasi.setText("Gagal membaca lokasi: " + e.getMessage());
                    lblStatusLokasi.setForeground(AbsensiUI.MERAH);
                }
            }
        }.execute();
    }

    private void tampilkanPetaPreview() {
        String lat = txtLat.getText().trim();
        String lon = txtLon.getText().trim();
        if (lat.isEmpty() || lon.isEmpty()) {
            lblPetaPreview.setText("Koordinat belum diisi");
            return;
        }
        lblPetaPreview.setText("<html><div style='text-align:center'>"
                + "<b>Lokasi kantor</b><br>" + lat + ", " + lon
                + "<br><br>radius " + txtRadius.getText().trim() + " m<br>"
                + "<span style='font-size:9px;color:#64748b'>"
                + "Pratinjau koordinat (bukan peta interaktif)</span></div></html>");
    }

    // ---------------------------------------------------------------- tab 4

    private JPanel bangunTabAudit() {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(12, 4, 4, 4));

        areaLog.setEditable(false);
        areaLog.setFont(AbsensiUI.font(Font.PLAIN, 12));
        JScrollPane sc = new JScrollPane(areaLog);
        sc.setBorder(BorderFactory.createLineBorder(AbsensiUI.GARIS));

        JPanel kartu = AbsensiUI.kartu();
        kartu.add(AbsensiUI.judulBagian("Riwayat verifikasi gagal (audit)"), BorderLayout.NORTH);
        kartu.add(sc, BorderLayout.CENTER);

        JPanel aksi = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        aksi.setOpaque(false);
        JButton btnMuat = AbsensiUI.tombol("Muat Ulang Log", AbsensiUI.AKSEN_BIRU);
        btnMuat.addActionListener(e -> muatLog());
        aksi.add(btnMuat);
        kartu.add(aksi, BorderLayout.SOUTH);

        p.add(kartu, BorderLayout.CENTER);
        return p;
    }

    private void muatLog() {
        StringBuilder sb = new StringBuilder();
        String sql = "SELECT l.tanggal, l.waktu, l.alasan, l.detail, k.nama_karyawan "
                + "FROM log_verifikasi_absensi l "
                + "LEFT JOIN karyawan k ON k.id_karyawan = l.id_karyawan "
                + "ORDER BY l.tanggal DESC, l.waktu DESC LIMIT 200";
        try (java.sql.Connection c = config.Koneksi.getConnection();
             java.sql.PreparedStatement ps = c.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                sb.append("[").append(rs.getString("tanggal")).append(" ")
                  .append(rs.getString("waktu")).append("] ")
                  .append(rs.getString("nama_karyawan") == null ? "(?)"
                          : rs.getString("nama_karyawan"))
                  .append(" — ").append(rs.getString("alasan"))
                  .append("\n    ").append(rs.getString("detail") == null ? "" : rs.getString("detail"))
                  .append("\n");
            }
        } catch (Exception e) {
            sb.append("Gagal memuat log: ").append(e.getMessage());
        }
        if (sb.length() == 0) sb.append("Belum ada catatan kegagalan verifikasi.");
        areaLog.setText(sb.toString());
        areaLog.setCaretPosition(0);
    }

    @Override
    public void removeNotify() {
        kamera.berhenti();
        super.removeNotify();
    }
}
