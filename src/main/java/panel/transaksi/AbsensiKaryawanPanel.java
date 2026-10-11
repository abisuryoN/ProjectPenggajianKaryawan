package panel.transaksi;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import org.opencv.core.Mat;
import org.opencv.core.Rect;
import service.AbsensiService;
import service.BuktiAbsensiStorage;
import service.GeoClueLocationProvider;
import service.LocationProvider;
import service.LocationService;
import service.face.LivenessService;
import service.face.OpenCvBridge;
import service.face.WajahVerifier;
import kamera.KameraService;
import util.Session;

/**
 * Absensi karyawan — UI Java Swing dibangun melalui kode (BAGIAN B), bukan
 * desain .form lama.
 *
 * Struktur (B2):
 *   header   : judul, tanggal+jam, identitas login, badge status verifikasi
 *   kolom kiri : preview kamera (tanpa mirror), status kamera, indikator wajah,
 *                hasil pencocokan, liveness, tombol kamera & ambil foto
 *   kolom kanan: status lokasi, jarak ke kantor, radius HRD, status area,
 *                tombol perbarui lokasi, tombol absen masuk/pulang
 *   bawah    : riwayat absensi terbaru + notifikasi inline (bukan popup berulang)
 *
 * Pada jendela sempit, susunan otomatis menjadi satu kolom dengan scroll
 * vertikal (tanpa scroll horizontal).
 *
 * Alur absen (§18): buka kamera -> deteksi wajah -> liveness -> verifikasi
 * identitas -> lokasi valid -> foto bukti baru -> simpan (service validasi ulang).
 */
public class AbsensiKaryawanPanel extends JPanel {

    // --------------------------------------------------------------- tema
    private static final Color BIRU_TUA = new Color(18, 40, 90);
    private static final Color ABU = new Color(90, 105, 125);
    private static final Color HIJAU = new Color(20, 184, 166);
    private static final Color MERAH = new Color(239, 68, 68);
    private static final Color ORANYE = new Color(249, 115, 22);
    private static final Color ABU_MUDA = new Color(243, 244, 246);
    private static final Color GARIS = new Color(226, 232, 240);

    /** Lebar minimum sebelum layout turun menjadi satu kolom. */
    private static final int BATAS_SATU_KOLOM = 1100;

    private final KameraService kamera = new KameraService();
    private final AbsensiService absensiService = new AbsensiService();
    private final WajahVerifier verifier = new WajahVerifier();
    private final LivenessService liveness = new LivenessService();
    private final LocationProvider penyediaLokasi = new GeoClueLocationProvider();
    private final LocationService LOKASI = new LocationService();

    // status terpisah per aspek (§B2/B4) — ditampilkan sebagai notifikasi inline
    private final JLabel lblStatusKamera = labelStatus("Kamera: belum dimulai", ABU);
    private final JLabel lblStatusWajah = labelStatus("Wajah: belum diperiksa", ABU);
    private final JLabel lblStatusLokasi = labelStatus("Lokasi: belum diperiksa", ABU);
    private final JLabel lblJarak = labelStatus("Jarak ke kantor: -", ABU);
    private final JLabel lblRadius = labelStatus("Radius absensi: -", ABU);
    private final JLabel lblArea = labelStatus("Area: belum diketahui", ABU);
    private final JLabel lblPreview = new JLabel("Preview kamera muncul di sini", JLabel.CENTER);
    private final JLabel lblIdentitas = new JLabel();
    private final JLabel lblJam = new JLabel();
    private final JLabel lblBadgeVerifikasi = badge("Belum diverifikasi", ABU_MUDA, ABU);
    private final JTextArea areaHasil = new JTextArea(6, 30);
    private final JProgressBar barLiveness = new JProgressBar(0, 100);

    private final JButton btnMulaiKamera = tombol("Mulai Kamera", BIRU_TUA);
    private final JButton btnAmbilFoto = tombol("Ambil Foto & Verifikasi", HIJAU);
    private final JButton btnAbsenMasuk = tombol("Absen Masuk", HIJAU);
    private final JButton btnAbsenPulang = tombol("Absen Pulang", ORANYE);
    private final JButton btnPerbaruiLokasi = tombol("Perbarui Lokasi", ABU_MUDA, Color.BLACK);

    private final DefaultTableModel modelRiwayat = new DefaultTableModel(
            new Object[0][0],
            new String[]{"Tanggal", "Masuk", "Pulang", "Status", "Verifikasi", "Jarak (m)"}) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblRiwayat = new JTable(modelRiwayat);

    // hasil pemeriksaan terakhir — diperiksa ULANG saat submit (§18)
    private volatile boolean wajahTerverifikasi = false;
    private volatile double skorWajahTerverifikasi = -1;
    private volatile boolean lokasiLolos = false;
    private volatile boolean livenessLulus = false;
    private volatile LocationProvider.HasilLokasiPengguna lokasiTerakhir;

    // panel yang disusun ulang saat jendela sempit
    private final JPanel pnlKolom = new JPanel(new BorderLayout(16, 0));
    private final JPanel pnlKiri = new JPanel(new BorderLayout(0, 10));
    private final JPanel pnlKanan = new JPanel();
    private boolean modeSatuKolom = false;

    private final Timer timerJam = new Timer(1000, e ->
        lblJam.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern(
            "EEEE, dd MMMM yyyy  •  HH:mm:ss", new Locale("id", "ID")))));

    public AbsensiKaryawanPanel() {
        setLayout(new BorderLayout(0, 12));
        setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));
        setBackground(Color.WHITE);

        add(bangunHeader(), BorderLayout.NORTH);
        add(bangunIsi(), BorderLayout.CENTER);

        sambungkanKamera();
        pasangAksi();
        perbaruiKesiapan();   // tombol absen terkunci sampai pemeriksaan lolos
        muatRiwayat();
        perbaruiLokasi();     // coba sekali saat dibuka (§18 langkah 4-6)
        timerJam.start();

        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) { sesuaikanLebar(); }
        });
    }

    // ------------------------------------------------------------------ UI

    private JPanel bangunHeader() {
        JPanel p = new JPanel(new BorderLayout(12, 0));
        p.setOpaque(false);

        JPanel kiri = new JPanel();
        kiri.setOpaque(false);
        kiri.setLayout(new BoxLayout(kiri, BoxLayout.Y_AXIS));
        JLabel judul = new JLabel("Absensi Karyawan");
        judul.setFont(new Font(util.DesignUtil.UI_FONT, Font.BOLD, 24));
        judul.setForeground(BIRU_TUA);
        judul.setAlignmentX(LEFT_ALIGNMENT);
        lblIdentitas.setText("Login sebagai: " + Session.getNamaKaryawan()
                + "  (ID " + Session.getIdKaryawan() + ")  •  Role: " + Session.getRole());
        lblIdentitas.setFont(new Font(util.DesignUtil.UI_FONT, Font.PLAIN, 13));
        lblIdentitas.setForeground(ABU);
        lblIdentitas.setAlignmentX(LEFT_ALIGNMENT);
        lblJam.setFont(new Font(util.DesignUtil.UI_FONT, Font.PLAIN, 13));
        lblJam.setForeground(HIJAU);
        lblJam.setAlignmentX(LEFT_ALIGNMENT);
        lblJam.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern(
                "EEEE, dd MMMM yyyy  •  HH:mm:ss", new Locale("id", "ID"))));
        kiri.add(judul);
        kiri.add(Box.createVerticalStrut(4));
        kiri.add(lblIdentitas);
        kiri.add(Box.createVerticalStrut(2));
        kiri.add(lblJam);
        p.add(kiri, BorderLayout.WEST);

        JPanel kanan = new JPanel();
        kanan.setOpaque(false);
        kanan.setLayout(new BoxLayout(kanan, BoxLayout.Y_AXIS));
        lblBadgeVerifikasi.setAlignmentX(LEFT_ALIGNMENT);
        kanan.add(Box.createVerticalStrut(6));
        kanan.add(lblBadgeVerifikasi);
        p.add(kanan, BorderLayout.EAST);
        return p;
    }

    private JPanel bangunIsi() {
        // ---------- kolom kiri: kamera + liveness
        lblPreview.setPreferredSize(new Dimension(640, 480));
        lblPreview.setMinimumSize(new Dimension(320, 240));
        lblPreview.setBorder(BorderFactory.createLineBorder(GARIS));
        lblPreview.setBackground(new Color(15, 27, 51));
        lblPreview.setOpaque(true);
        lblPreview.setForeground(new Color(205, 213, 223));
        lblPreview.setFont(new Font(util.DesignUtil.UI_FONT, Font.PLAIN, 14));

        JPanel pnlPreview = new JPanel(new BorderLayout());
        pnlPreview.setBorder(BorderFactory.createTitledBorder("Kamera (tanpa mirror)"));
        pnlPreview.add(lblPreview, BorderLayout.CENTER);

        barLiveness.setStringPainted(true);
        barLiveness.setString("Liveness: belum dimulai");
        JPanel pnlLiveness = new JPanel(new BorderLayout());
        pnlLiveness.setBorder(BorderFactory.createTitledBorder("Pemeriksaan keaktifan (anti-foto)"));
        pnlLiveness.add(barLiveness, BorderLayout.CENTER);

        pnlKiri.setOpaque(false);
        pnlKiri.add(pnlPreview, BorderLayout.CENTER);
        pnlKiri.add(pnlLiveness, BorderLayout.SOUTH);

        // ---------- kolom kanan: status + lokasi + aksi + hasil
        JPanel status = new JPanel(new GridLayout(3, 1, 0, 4));
        status.setOpaque(false);
        status.add(lblStatusKamera);
        status.add(lblStatusWajah);
        status.add(lblStatusLokasi);

        JPanel pnlLokasiInfo = new JPanel();
        pnlLokasiInfo.setOpaque(false);
        pnlLokasiInfo.setBorder(BorderFactory.createTitledBorder("Validasi lokasi"));
        pnlLokasiInfo.setLayout(new BoxLayout(pnlLokasiInfo, BoxLayout.Y_AXIS));
        lblJarak.setAlignmentX(LEFT_ALIGNMENT);
        lblRadius.setAlignmentX(LEFT_ALIGNMENT);
        lblArea.setAlignmentX(LEFT_ALIGNMENT);
        pnlLokasiInfo.add(lblJarak);
        pnlLokasiInfo.add(Box.createVerticalStrut(2));
        pnlLokasiInfo.add(lblRadius);
        pnlLokasiInfo.add(Box.createVerticalStrut(2));
        pnlLokasiInfo.add(lblArea);

        JPanel aksi = new JPanel(new GridLayout(2, 2, 8, 8));
        aksi.setOpaque(false);
        aksi.add(btnMulaiKamera);
        aksi.add(btnAmbilFoto);
        aksi.add(btnAbsenMasuk);
        aksi.add(btnAbsenPulang);

        JPanel barisLokasi = new JPanel(new BorderLayout(8, 0));
        barisLokasi.setOpaque(false);
        barisLokasi.add(btnPerbaruiLokasi, BorderLayout.WEST);

        areaHasil.setEditable(false);
        areaHasil.setLineWrap(true);
        areaHasil.setWrapStyleWord(true);
        areaHasil.setFont(new Font(util.DesignUtil.UI_FONT, Font.PLAIN, 13));
        areaHasil.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JScrollPane scroll = new JScrollPane(areaHasil);
        scroll.setBorder(BorderFactory.createTitledBorder("Notifikasi & hasil"));
        scroll.setPreferredSize(new Dimension(380, 150));

        pnlKanan.setOpaque(false);
        pnlKanan.setLayout(new BoxLayout(pnlKanan, BoxLayout.Y_AXIS));
        pnlKanan.add(status);
        pnlKanan.add(Box.createVerticalStrut(10));
        pnlKanan.add(pnlLokasiInfo);
        pnlKanan.add(Box.createVerticalStrut(10));
        pnlKanan.add(aksi);
        pnlKanan.add(Box.createVerticalStrut(8));
        pnlKanan.add(barisLokasi);
        pnlKanan.add(Box.createVerticalStrut(10));
        pnlKanan.add(scroll);

        // ---------- riwayat absensi terbaru (bawah)
        JPanel pnlRiwayat = new JPanel(new BorderLayout(0, 6));
        pnlRiwayat.setOpaque(false);
        tblRiwayat.setRowHeight(24);
        util.DesignUtil.applyTable(tblRiwayat);
        DefaultTableCellRenderer tengah = new DefaultTableCellRenderer();
        tengah.setHorizontalAlignment(JLabel.CENTER);
        for (int i = 0; i < tblRiwayat.getColumnCount(); i++) {
            tblRiwayat.getColumnModel().getColumn(i).setCellRenderer(tengah);
        }
        JScrollPane scRiwayat = new JScrollPane(tblRiwayat);
        scRiwayat.setPreferredSize(new Dimension(400, 150));
        scRiwayat.setBorder(BorderFactory.createTitledBorder("Riwayat absensi terbaru (10 terakhir)"));
        pnlRiwayat.add(scRiwayat, BorderLayout.CENTER);

        // ---------- susun + bungkus scroll vertikal (tanpa scroll horizontal)
        pnlKolom.setOpaque(false);
        pnlKolom.add(pnlKiri, BorderLayout.CENTER);
        pnlKolom.add(pnlKanan, BorderLayout.EAST);

        JPanel isi = new JPanel(new BorderLayout(0, 12));
        isi.setOpaque(false);
        isi.add(pnlKolom, BorderLayout.CENTER);
        isi.add(pnlRiwayat, BorderLayout.SOUTH);

        JScrollPane luar = new JScrollPane(isi);
        luar.setBorder(null);
        luar.setOpaque(false);
        luar.getViewport().setOpaque(false);
        luar.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        luar.getVerticalScrollBar().setUnitIncrement(16);

        JPanel bungkus = new JPanel(new BorderLayout());
        bungkus.setOpaque(false);
        bungkus.add(luar, BorderLayout.CENTER);
        return bungkus;
    }

    /** Susun ulang kolom: dua kolom bila lebar, satu kolom bila sempit (§B2). */
    private void sesuaikanLebar() {
        boolean sempit = getWidth() > 0 && getWidth() < BATAS_SATU_KOLOM;
        if (sempit == modeSatuKolom) return;
        modeSatuKolom = sempit;
        pnlKolom.removeAll();
        if (sempit) {
            lblPreview.setPreferredSize(new Dimension(420, 315));
            pnlKolom.setLayout(new BorderLayout(0, 12));
            pnlKolom.add(pnlKiri, BorderLayout.NORTH);
            pnlKolom.add(pnlKanan, BorderLayout.CENTER);
            catat("Jendela sempit: tampilan diubah menjadi satu kolom (geser ke bawah).");
        } else {
            lblPreview.setPreferredSize(new Dimension(640, 480));
            pnlKolom.setLayout(new BorderLayout(16, 0));
            pnlKolom.add(pnlKiri, BorderLayout.CENTER);
            pnlKolom.add(pnlKanan, BorderLayout.EAST);
        }
        pnlKolom.revalidate();
        pnlKolom.repaint();
    }

    private static JLabel labelStatus(String teks, Color warna) {
        JLabel l = new JLabel(teks);
        l.setFont(new Font(util.DesignUtil.UI_FONT, Font.PLAIN, 13));
        l.setForeground(warna);
        return l;
    }

    private static JLabel badge(String teks, Color bg, Color fg) {
        JLabel l = new JLabel("  " + teks + "  ");
        l.setOpaque(true);
        l.setBackground(bg);
        l.setForeground(fg);
        l.setFont(new Font(util.DesignUtil.UI_FONT, Font.BOLD, 12));
        l.setBorder(BorderFactory.createLineBorder(GARIS));
        return l;
    }

    private void setBadge(String teks, Color bg, Color fg) {
        lblBadgeVerifikasi.setText("  " + teks + "  ");
        lblBadgeVerifikasi.setBackground(bg);
        lblBadgeVerifikasi.setForeground(fg);
    }

    private static JButton tombol(String teks, Color bg) {
        return tombol(teks, bg, Color.WHITE);
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

    // ------------------------------------------------------------- kamera

    private void sambungkanKamera() {
        kamera.tambahPendengar(new KameraService.PendengarFrame() {
            @Override
            public void frameBaru(BufferedImage gambar) {
                SwingUtilities.invokeLater(() -> {
                    Image skala = gambar.getScaledInstance(lblPreview.getWidth() > 0
                            ? lblPreview.getWidth() : 640,
                            lblPreview.getHeight() > 0 ? lblPreview.getHeight() : 480,
                            Image.SCALE_FAST);
                    lblPreview.setIcon(new javax.swing.ImageIcon(skala));
                    lblPreview.setText("");
                    lblStatusKamera.setText("Kamera: aktif — " + kamera.status());
                    lblStatusKamera.setForeground(HIJAU);
                });
                analisisFrameDiLatar(gambar);
            }

            @Override
            public void kameraGagal(String pesan) {
                SwingUtilities.invokeLater(() -> {
                    lblStatusKamera.setText("Kamera: tidak tersedia — " + pesan);
                    lblStatusKamera.setForeground(MERAH);
                    catat("Kamera tidak tersedia: " + pesan);
                });
            }
        });
    }

    private void pasangAksi() {
        btnMulaiKamera.addActionListener(e -> {
            if (kamera.berjalan()) {
                kamera.berhenti();
                lblPreview.setIcon(null);
                lblPreview.setText("Kamera dimatikan");
                lblStatusKamera.setText("Kamera: dimatikan");
                lblStatusKamera.setForeground(ABU);
                btnMulaiKamera.setText("Mulai Kamera");
                catat("Kamera dimatikan.");
            } else {
                boolean ok = kamera.mulai();
                btnMulaiKamera.setText(ok ? "Matikan Kamera" : "Mulai Kamera");
                if (ok) catat("Kamera aktif.");
            }
        });

        btnAmbilFoto.addActionListener(e -> jalankanVerifikasiPenuh());

        btnAbsenMasuk.addActionListener(e -> simpanAbsensi(true));
        btnAbsenPulang.addActionListener(e -> simpanAbsensi(false));

        btnPerbaruiLokasi.addActionListener(e -> perbaruiLokasi());
    }

    // ------------------------------------------------- analisis frame (latar)

    private void analisisFrameDiLatar(BufferedImage gambar) {
        SwingWorker<List<OpenCvBridge.WajahTerdeteksi>, Void> w =
                new SwingWorker<List<OpenCvBridge.WajahTerdeteksi>, Void>() {
            @Override
            protected List<OpenCvBridge.WajahTerdeteksi> doInBackground() {
                if (!OpenCvBridge.siap()) {
                    try { OpenCvBridge.siapkanModel(); } catch (Throwable t) { return null; }
                    if (!OpenCvBridge.siap()) return null;
                }
                Mat mat = KameraService.gambarKeMat(gambar);
                try {
                    List<OpenCvBridge.WajahTerdeteksi> wajah = OpenCvBridge.deteksiWajah(mat);
                    if (!wajah.isEmpty()) {
                        boolean aktif = liveness.umpan(
                                wajah.get(0).x + wajah.get(0).lebar / 2, wajah.get(0).lebar);
                        long sisa = liveness.sisaWaktuMs();
                        long progres = (sisa < 0) ? 100L : Math.max(0L, 100L - (sisa / 120L));
                        SwingUtilities.invokeLater(() ->
                                barLiveness.setValue((int) Math.min(100L, progres)));
                        if (aktif) {
                            livenessLulus = true;
                            SwingUtilities.invokeLater(() -> barLiveness.setString(
                                    "Liveness: gerakan kepala terdeteksi — sah"));
                        }
                    }
                    return wajah;
                } finally {
                    mat.release();
                }
            }

            @Override
            protected void done() {
                try {
                    List<OpenCvBridge.WajahTerdeteksi> wajah = get();
                    if (wajah == null) {
                        lblStatusWajah.setText("Wajah: model belum siap ("
                                + OpenCvBridge.alasanGagal() + ")");
                        lblStatusWajah.setForeground(ORANYE);
                        return;
                    }
                    if (wajah.isEmpty()) {
                        lblStatusWajah.setText("Wajah: tidak ada wajah terdeteksi");
                        lblStatusWajah.setForeground(ABU);
                    } else if (wajah.size() > 1) {
                        lblStatusWajah.setText("Wajah: " + wajah.size()
                                + " wajah — pastikan hanya Anda di depan kamera");
                        lblStatusWajah.setForeground(ORANYE);
                    } else {
                        lblStatusWajah.setText("Wajah: 1 wajah terdeteksi (belum diverifikasi)");
                        lblStatusWajah.setForeground(ABU);
                    }
                } catch (Exception ignored) {}
            }
        };
        w.execute();
    }

    // ------------------------------------------------------------- verifikasi

    /** Deteksi + liveness + identitas; hasil disimpan untuk pemeriksaan ulang. */
    private void jalankanVerifikasiPenuh() {
        BufferedImage frame = kamera.frameTerakhir();
        if (frame == null) {
            catat("Tidak ada frame kamera. Tekan \"Mulai Kamera\" dulu.");
            return;
        }
        btnAmbilFoto.setEnabled(false);
        btnAmbilFoto.setText("Memeriksa...");
        SwingWorker<WajahVerifier.HasilVerifikasi, Void> w =
                new SwingWorker<WajahVerifier.HasilVerifikasi, Void>() {
            @Override
            protected WajahVerifier.HasilVerifikasi doInBackground() {
                try {
                    if (!OpenCvBridge.siap()) OpenCvBridge.siapkanModel();
                    Mat mat = KameraService.gambarKeMat(frame);
                    try {
                        List<OpenCvBridge.WajahTerdeteksi> wajah = OpenCvBridge.deteksiWajah(mat);
                        if (wajah.isEmpty()) {
                            return WajahVerifier.HasilVerifikasi.lainnya("TANPA_WAJAH",
                                    "Tidak ada wajah terdeteksi. Hadapkan wajah ke kamera dengan pencahayaan cukup.");
                        }
                        if (wajah.size() > 1) {
                            return WajahVerifier.HasilVerifikasi.lainnya("MULTI_WAJAH",
                                    "Ada " + wajah.size() + " wajah. Pastikan hanya Anda yang di depan kamera.");
                        }
                        if (!liveness.sedangAktif() || liveness.sisaWaktuMs() < 0) liveness.mulai();
                        Rect area = new Rect(wajah.get(0).x, wajah.get(0).y,
                                wajah.get(0).lebar, wajah.get(0).tinggi);
                        if (!LivenessService.kualitasCukup(mat, area)) {
                            return WajahVerifier.HasilVerifikasi.lainnya("KUALITAS_BURUK",
                                    "Kualitas gambar terlalu buruk/gelap. Perbaiki pencahayaan lalu ulangi.");
                        }
                        float[] embedding = OpenCvBridge.hitungEmbedding(mat.submat(area));
                        return verifier.verifikasi(embedding,
                                Session.getIdKaryawan(), Session.getNamaKaryawan());
                    } finally {
                        mat.release();
                    }
                } catch (Throwable t) {
                    return WajahVerifier.HasilVerifikasi.lainnya("GAGAL_TEKNIS",
                            "Verifikasi gagal secara teknis: " + t.getMessage());
                }
            }

            @Override
            protected void done() {
                btnAmbilFoto.setEnabled(true);
                btnAmbilFoto.setText("Ambil Foto & Verifikasi");
                try {
                    WajahVerifier.HasilVerifikasi h = get();
                    wajahTerverifikasi = h.cocok();
                    skorWajahTerverifikasi = h.similarity;
                    lblStatusWajah.setText("Wajah: " + h.pesan);
                    lblStatusWajah.setForeground(h.cocok() ? HIJAU
                            : "TANPA_WAJAH".equals(h.status) ? ABU : MERAH);
                    if (h.cocok()) {
                        setBadge("Wajah cocok", new Color(209, 250, 229), new Color(6, 95, 70));
                    } else if ("TANPA_WAJAH".equals(h.status)) {
                        setBadge("Wajah tidak terlihat", ABU_MUDA, ABU);
                    } else if ("MULTI_WAJAH".equals(h.status)) {
                        setBadge("Lebih dari satu wajah", new Color(254, 243, 199), new Color(146, 64, 14));
                    } else {
                        setBadge("Wajah tidak cocok", new Color(254, 226, 226), new Color(153, 27, 27));
                    }
                    catat(h.pesan + (h.similarity >= 0 ? String.format("  (skor %.2f)", h.similarity) : ""));
                } catch (Exception ex) {
                    catat("Verifikasi gagal: " + ex.getMessage());
                } finally {
                    perbaruiKesiapan();
                }
            }
        };
        w.execute();
    }

    // ---------------------------------------------------------------- lokasi

    private void perbaruiLokasi() {
        lblStatusLokasi.setText("Lokasi: meminta posisi...");
        lblStatusLokasi.setForeground(ABU);
        SwingWorker<LocationProvider.HasilLokasiPengguna, Void> w =
                new SwingWorker<LocationProvider.HasilLokasiPengguna, Void>() {
            @Override
            protected LocationProvider.HasilLokasiPengguna doInBackground() {
                return penyediaLokasi.ambilLokasi();
            }

            @Override
            protected void done() {
                try {
                    LocationProvider.HasilLokasiPengguna h = get();
                    lokasiTerakhir = h;
                    if (h.tersedia) {
                        LocationService.Hasil cek = LOKASI.validasi(
                                h.latitude, h.longitude, h.akurasiMeter);
                        lokasiLolos = cek.lolos;
                        lblStatusLokasi.setText("Lokasi: " + cek.pesan);
                        lblStatusLokasi.setForeground(cek.lolos ? HIJAU : MERAH);
                        lblJarak.setText(cek.jarakMeter >= 0
                                ? "Jarak ke kantor: " + Math.round(cek.jarakMeter) + " meter"
                                : "Jarak ke kantor: belum dapat dihitung");
                        lblJarak.setForeground(cek.lolos ? HIJAU : MERAH);
                        lblRadius.setText("Radius absensi (dari HRD): " + cek.radiusMeter + " meter");
                        lblRadius.setForeground(BIRU_TUA);
                        lblArea.setText(cek.lolos
                                ? "Area: DI DALAM area absensi"
                                : "Area: DI LUAR area absensi");
                        lblArea.setForeground(cek.lolos ? HIJAU : MERAH);
                        if (cek.lolos) {
                            setBadge("Lokasi valid", new Color(209, 250, 229), new Color(6, 95, 70));
                        } else if (lblBadgeVerifikasi.getText().contains("Wajah cocok")) {
                            setBadge("Wajah cocok • lokasi bermasalah",
                                    new Color(254, 243, 199), new Color(146, 64, 14));
                        }
                        catat(cek.pesan);
                    } else {
                        lokasiLolos = false;
                        lblStatusLokasi.setText("Lokasi: belum tersedia — " + h.keterangan);
                        lblStatusLokasi.setForeground(MERAH);
                        lblJarak.setText("Jarak ke kantor: belum dapat dihitung");
                        lblJarak.setForeground(MERAH);
                        lblArea.setText("Area: belum diketahui");
                        lblArea.setForeground(MERAH);
                        setBadge("Lokasi belum tersedia", new Color(254, 226, 226), new Color(153, 27, 27));
                        catat("Lokasi belum tersedia: " + h.keterangan
                                + "\n(Aktifkan layanan lokasi sistem / sambungkan WiFi, lalu tekan Perbarui Lokasi.)");
                    }
                } catch (Exception ex) {
                    lokasiLolos = false;
                    lblStatusLokasi.setText("Lokasi: gagal diperiksa — " + ex.getMessage());
                    lblStatusLokasi.setForeground(MERAH);
                    lblArea.setText("Area: belum diketahui");
                    lblArea.setForeground(MERAH);
                } finally {
                    perbaruiKesiapan();
                }
            }
        };
        w.execute();
    }

    /**
     * Tombol absen hanya aktif bila SELURUH pemeriksaan wajib sudah lolos
     * (wajah terverifikasi + lokasi valid). Validasi tetap diulang di service
     * saat menyimpan (§B4) — tombol bukan pengganti validasi.
     */
    private void perbaruiKesiapan() {
        boolean siap = wajahTerverifikasi && lokasiLolos;
        btnAbsenMasuk.setEnabled(siap);
        btnAbsenPulang.setEnabled(siap);
        btnAbsenMasuk.setToolTipText(siap ? null
                : "Wajib: wajah terverifikasi DAN lokasi di dalam radius.");
        btnAbsenPulang.setToolTipText(btnAbsenMasuk.getToolTipText());
    }

    // ----------------------------------------------------------------- absen

    private void simpanAbsensi(boolean masuk) {
        final int idKaryawan = Session.getIdKaryawan();
        if (idKaryawan <= 0) {
            catat("Sesi login tidak sah — silakan login ulang.");
            return;
        }
        if (!wajahTerverifikasi || !lokasiLolos) {
            catat("Absensi belum dapat dilakukan: " + (!wajahTerverifikasi
                    ? "wajah belum terverifikasi. " : "")
                    + (!lokasiLolos ? "lokasi belum valid." : ""));
            return;
        }
        btnAbsenMasuk.setEnabled(false);
        btnAbsenPulang.setEnabled(false);
        SwingWorker<AbsensiService.HasilAbsen, Void> w =
                new SwingWorker<AbsensiService.HasilAbsen, Void>() {
            @Override
            protected AbsensiService.HasilAbsen doInBackground() {
                try {
                    // --- validasi ulang WAJIB saat submit (§18) ---
                    BufferedImage frame = kamera.frameTerakhir();
                    boolean kameraSah = frame != null;
                    boolean lokasiSah = false;
                    Double lat = null, lon = null, jarak = null;

                    if (lokasiTerakhir != null && lokasiTerakhir.tersedia) {
                        // ambil ulang — bukan memakai cache lama (§18 langkah 14)
                        LocationProvider.HasilLokasiPengguna ulang = penyediaLokasi.ambilLokasi();
                        if (ulang.tersedia) {
                            LocationService.Hasil cek = LOKASI.validasi(
                                    ulang.latitude, ulang.longitude, ulang.akurasiMeter);
                            if (cek.lolos) {
                                lokasiSah = true;
                                lat = ulang.latitude;
                                lon = ulang.longitude;
                                jarak = cek.jarakMeter;
                            } else {
                                return AbsensiService.HasilAbsen.gagal(cek.pesan);
                            }
                        } else {
                            return AbsensiService.HasilAbsen.gagal(
                                    "Absensi ditolak: lokasi perangkat belum tersedia. "
                                  + "Tekan \"Perbarui Lokasi\" lalu ulangi.");
                        }
                    } else {
                        return AbsensiService.HasilAbsen.gagal(
                                "Absensi ditolak: lokasi belum diperiksa. "
                              + "Tekan \"Perbarui Lokasi\" dulu.");
                    }
                    if (!lokasiSah) {
                        return AbsensiService.HasilAbsen.gagal(
                                "Absensi ditolak: lokasi tidak valid.");
                    }

                    if (kameraSah) {
                        jalankanVerifikasiSinkron();
                        if (!wajahTerverifikasi) {
                            AbsensiService.catatLog(idKaryawan, LocalDate.now(),
                                    AbsensiService.ALASAN_WAJAH_TIDAK_COCOK,
                                    "Percobaan absen " + (masuk ? "masuk" : "pulang")
                                  + " dengan wajah tidak cocok/tidak terverifikasi");
                            return AbsensiService.HasilAbsen.gagal(
                                    "Absensi ditolak: verifikasi wajah tidak cocok dengan akun "
                                  + Session.getNamaKaryawan() + ". Lihat status Wajah untuk detail.");
                        }
                    } else {
                        return AbsensiService.HasilAbsen.gagal(
                                "Absensi ditolak: kamera tidak aktif, wajah tidak dapat diverifikasi.");
                    }

                    // foto bukti BARU dari frame saat ini
                    String foto = null;
                    ByteArrayOutputStream buf = new ByteArrayOutputStream();
                    ImageIO.write(frame, "jpg", buf);
                    foto = BuktiAbsensiStorage.simpan(buf.toByteArray(), idKaryawan);

                    AbsensiService.BuktiAbsen bukti = new AbsensiService.BuktiAbsen();
                    bukti.verifikasiWajah = AbsensiService.WAJAH_COCOK;
                    bukti.skorWajah = skorWajahTerverifikasi;
                    bukti.lat = lat;
                    bukti.lon = lon;
                    bukti.jarakMeter = jarak;
                    bukti.fotoBukti = foto;
                    bukti.sumber = "KAMERA";
                    bukti.keterangan = "Liveness: " + (livenessLulus ? "lulus" : "tidak dijalankan");

                    return masuk
                            ? absensiService.absenMasuk(idKaryawan, bukti, true)
                            : absensiService.absenPulang(idKaryawan, bukti, true);
                } catch (Exception e) {
                    return AbsensiService.HasilAbsen.gagal("Kesalahan teknis: " + e.getMessage());
                }
            }

            @Override
            protected void done() {
                try {
                    AbsensiService.HasilAbsen h = get();
                    catat((h.sukses ? "BERHASIL: " : "DITOLAK: ") + h.pesan
                            + (h.jam != null ? "  (jam " + h.jam + ")" : ""));
                    if (h.sukses) muatRiwayat();
                } catch (Exception ex) {
                    catat("Gagal menyimpan: " + ex.getMessage());
                } finally {
                    perbaruiKesiapan();
                }
            }
        };
        w.execute();
    }

    /** Versi sinkron dari verifikasi penuh — dipakai di dalam alur simpan. */
    private void jalankanVerifikasiSinkron() {
        BufferedImage frame = kamera.frameTerakhir();
        if (frame == null) { wajahTerverifikasi = false; return; }
        try {
            if (!OpenCvBridge.siap()) OpenCvBridge.siapkanModel();
            Mat mat = KameraService.gambarKeMat(frame);
            try {
                List<OpenCvBridge.WajahTerdeteksi> wajah = OpenCvBridge.deteksiWajah(mat);
                wajahTerverifikasi = false;
                if (wajah.size() != 1) return;
                Rect area = new Rect(wajah.get(0).x, wajah.get(0).y,
                        wajah.get(0).lebar, wajah.get(0).tinggi);
                if (!LivenessService.kualitasCukup(mat, area)) return;
                float[] embedding = OpenCvBridge.hitungEmbedding(mat.submat(area));
                WajahVerifier.HasilVerifikasi h = verifier.verifikasi(embedding,
                        Session.getIdKaryawan(), Session.getNamaKaryawan());
                wajahTerverifikasi = h.cocok();
                skorWajahTerverifikasi = h.similarity;
            } finally {
                mat.release();
            }
        } catch (Throwable t) {
            wajahTerverifikasi = false;
        }
    }

    // --------------------------------------------------------------- riwayat

    /** Riwayat absensi terbaru milik karyawan yang login (B2 bagian bawah). */
    private void muatRiwayat() {
        modelRiwayat.setRowCount(0);
        int idKaryawan = Session.getIdKaryawan();
        if (idKaryawan <= 0) return;
        String sql = "SELECT tanggal, jam_masuk, jam_pulang, status, verifikasi_wajah, jarak_meter "
                   + "FROM absensi WHERE id_karyawan = ? "
                   + "ORDER BY tanggal DESC, jam_masuk DESC LIMIT 10";
        try (java.sql.Connection c = config.Koneksi.getConnection();
             java.sql.PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idKaryawan);
            try (java.sql.ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Object jarak = rs.getObject("jarak_meter");
                    modelRiwayat.addRow(new Object[]{
                        rs.getString("tanggal"),
                        rs.getString("jam_masuk") == null ? "-" : rs.getString("jam_masuk"),
                        rs.getString("jam_pulang") == null ? "-" : rs.getString("jam_pulang"),
                        rs.getString("status") == null ? "-" : rs.getString("status"),
                        rs.getString("verifikasi_wajah") == null ? "-" : rs.getString("verifikasi_wajah"),
                        jarak == null ? "-" : String.format("%.0f", rs.getDouble("jarak_meter"))});
                }
            }
        } catch (Exception e) {
            catat("Riwayat absensi tidak dapat dimuat: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ util

    private void catat(String pesan) {
        SwingUtilities.invokeLater(() -> {
            areaHasil.append("[" + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + pesan + "\n");
            areaHasil.setCaretPosition(areaHasil.getDocument().getLength());
        });
    }

    /** Lepas kamera & timer saat panel ditutup (tidak bocor). */
    @Override
    public void removeNotify() {
        timerJam.stop();
        liveness.batal();
        kamera.berhenti();
        super.removeNotify();
    }
}
