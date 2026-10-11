package panel.absensi;

import kamera.KameraService;
import service.AbsensiService;
import service.IdentitasAbsensi;
import service.BuktiAbsensiStorage;
import service.LocationProvider;
import service.LocationService;
import service.RantaiLocationProvider;
import service.face.LivenessService;
import service.face.OpenCvBridge;
import service.face.WajahDAO;
import service.face.WajahVerifier;
import util.Session;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Halaman ABSENSI KARYAWAN (kode, bukan .form lama).
 *
 * <p>Alur:
 * <ol>
 *   <li>Gerbang registrasi: bila akun login belum punya template wajah,
 *       halaman ini menampilkan {@link RegistrasiWajahPanel} lebih dulu.
 *       Bila sudah terdaftar, langsung ke halaman absensi (tidak diminta
 *       registrasi ulang).</li>
 *   <li>Absensi: kamera -> deteksi wajah -> liveness (tantangan tengok ke
 *       arah fisik KIRI/KANAN) -> verifikasi identitas (embedding) ->
 *       validasi lokasi -> foto bukti BARU -> simpan.</li>
 * </ol>
 *
 * <p>Semua pemrosesan berat (OpenCV, DB, GPS) berjalan di {@link SwingWorker};
 * pembaruan UI selalu lewat EDT.
 */
public class AbsensiKaryawanPanel extends JPanel {

    private final CardLayout kartu = new CardLayout();
    private final JPanel lapisan = new JPanel(kartu);

    private final KameraService kamera = new KameraService();
    private final AbsensiService absensiService = new AbsensiService();
    private final WajahVerifier verifier = new WajahVerifier();
    private final WajahDAO wajahDAO = new WajahDAO();
    private final LivenessService liveness = new LivenessService();
    private final LocationProvider penyediaLokasi = RantaiLocationProvider.standar();
    private final LocationService lokasiService = new LocationService();

    /** Identitas absensi hasil resolusi; HRD tanpa id_karyawan tetap bisa absen. */
    private IdentitasAbsensi.Hasil identitas;

    // ---- status
    private boolean wajahTerverifikasi;
    private double skorWajahTerverifikasi = -1;
    private boolean livenessLulus;
    private boolean lokasiLolos;
    private LocationProvider.HasilLokasiPengguna lokasiTerakhir;

    private final JLabel lblPreview = new JLabel("Kamera belum dinyalakan", JLabel.CENTER);
    private final JLabel lblStatusKamera = AbsensiUI.label("Kamera: belum dinyalakan");
    private final JLabel lblStatusWajah = AbsensiUI.label("Wajah: belum diperiksa");
    private final JLabel lblStatusLiveness = AbsensiUI.label("Liveness: belum dimulai");
    private final JLabel lblInstruksiLiveness = new JLabel(" ");
    private final JLabel lblJarak = AbsensiUI.label("Jarak ke kantor: -");
    private final JLabel lblRadius = AbsensiUI.label("Radius absensi: -");
    private final JLabel lblArea = AbsensiUI.label("Area: belum diketahui");
    private final JLabel lblJam = new JLabel();
    private final JLabel lblTanggal = new JLabel();
    private final JLabel lblChipWajah = AbsensiUI.chip("Belum diverifikasi", AbsensiUI.TEKS_LEMBUT);
    private final JLabel lblChipLiveness = AbsensiUI.chip("Liveness: menunggu", AbsensiUI.AMBER);
    private final JLabel lblChipLokasi = AbsensiUI.chip("Lokasi: belum", AbsensiUI.TEKS_LEMBUT);
    private final JTextArea areaCatatan = new JTextArea(4, 30);

    private final JButton btnKamera = AbsensiUI.tombol("Mulai Kamera", AbsensiUI.AKSEN_BIRU);
    private final JButton btnVerifikasi = AbsensiUI.tombol("Verifikasi Wajah", AbsensiUI.AKSEN);
    private final JButton btnLokasi = AbsensiUI.tombolNetral("Perbarui Lokasi");
    private final JButton btnAbsenMasuk = AbsensiUI.tombol("Absen Masuk", AbsensiUI.HIJAU);
    private final JButton btnAbsenPulang = AbsensiUI.tombol("Absen Pulang", AbsensiUI.AMBER);

    private final DefaultTableModel modelRiwayat = new DefaultTableModel(new Object[0][0],
            new String[]{"Tanggal", "Masuk", "Pulang", "Status", "Verifikasi", "Jarak (m)"}) {
        @Override public boolean isCellEditable(int r, int c) { return false; }
    };
    private final JTable tblRiwayat = new JTable(modelRiwayat);
    private final Timer timerJam;

    public AbsensiKaryawanPanel() {
        setLayout(new BorderLayout());
        setBackground(AbsensiUI.PAGE);

        lapisan.setOpaque(false);
        add(lapisan, BorderLayout.CENTER);

        timerJam = new Timer(1000, e -> perbaruiJam());
        timerJam.start();
        perbaruiJam();
        sambungkanKamera();
        pasangAksiKamera();

        cekStatusRegistrasi();
    }

    // ------------------------------------------------------ gerbang registrasi

    /** Periksa apakah akun login sudah punya template wajah, di background. */
    private void cekStatusRegistrasi() {
        lapisan.add(muatLayar(), "memuat");
        lapisan.add(muatanAbsensi(), "absensi");
        kartu.show(lapisan, "memuat");

        new SwingWorker<Object[], Void>() {
            @Override protected Object[] doInBackground() {
                // Resolusi identitas: kolom users.id_karyawan boleh NULL (akun HRD),
                // jadi JANGAN langsung menolak hanya karena Session.getIdKaryawan()==0.
                try {
                    IdentitasAbsensi.Hasil h = IdentitasAbsensi.resolve(
                            Session.getIdKaryawan(), Session.getUsername());
                    if (h.idKaryawan <= 0) {
                        return new Object[]{"tanpa_identitas", h.alasan, null};
                    }
                    boolean terdaftar = wajahDAO.sudahTerdaftar(h.idKaryawan);
                    return new Object[]{"ok", h, terdaftar};
                } catch (Exception e) {
                    return new Object[]{"galat", e.getMessage(), null};
                }
            }
            @Override protected void done() {
                Object[] hasil;
                try { hasil = get(); } catch (Exception e) {
                    hasil = new Object[]{"galat", String.valueOf(e.getMessage()), null};
                }
                String status = (String) hasil[0];
                if ("galat".equals(status)) {
                    lapisan.add(pesanGalat(String.valueOf(hasil[1])), "galat");
                    kartu.show(lapisan, "galat");
                    return;
                }
                if ("tanpa_identitas".equals(status)) {
                    String sebab = String.valueOf(hasil[1]);
                    // HRD boleh menautkan akunnya ke kartu karyawan SEKALI (eksplisit),
                    // supaya HRD juga bisa absen seperti karyawan lain.
                    lapisan.add(Session.isHRD() ? panelPenautan(sebab) : pesanGalat(sebab), "galat");
                    kartu.show(lapisan, "galat");
                    return;
                }
                identitas = (IdentitasAbsensi.Hasil) hasil[1];
                boolean terdaftar = Boolean.TRUE.equals(hasil[2]);
                if (!terdaftar) {
                    // Belum punya foto referensi -> WAJIB registrasi dulu
                    lapisan.add(new RegistrasiWajahPanel(identitas.idKaryawan, () -> {
                        perbaruiKesiapan();
                    }), "registrasi");
                    kartu.show(lapisan, "registrasi");
                } else {
                    kartu.show(lapisan, "absensi");
                }
            }
        }.execute();
    }

    private JPanel muatLayar() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        JLabel l = AbsensiUI.label("Memeriksa status registrasi wajah...");
        l.setFont(AbsensiUI.font(Font.BOLD, 14));
        p.add(l);
        return p;
    }

    private JPanel pesanGalat() {
        return pesanGalat(null);
    }

    /**
     * Layar galat dengan sebab yang spesifik. Pesan default hanya dipakai bila
     * penyebabnya tidak diketahui — supaya pengguna tidak menebak-nebak.
     */
    private JPanel pesanGalat(String sebab) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        JPanel kartu = AbsensiUI.kartu();
        String isi = (sebab == null || sebab.trim().isEmpty())
                ? "Status registrasi wajah tidak dapat dibaca. Periksa koneksi database "
                  + "lalu buka ulang halaman absensi."
                : sebab.trim();
        JLabel l = new JLabel("<html><body style='width:430px'>" + isi + "</body></html>");
        l.setForeground(AbsensiUI.MERAH);
        l.setFont(AbsensiUI.font(Font.PLAIN, 13));
        kartu.add(l, BorderLayout.CENTER);
        p.add(kartu);
        return p;
    }

    /**
     * Penautan akun HRD ke kartu karyawan (sekali saja, eksplisit oleh HRD).
     *
     * <p>Latar: pada tabel {@code users}, akun HRD umumnya punya
     * {@code id_karyawan = NULL} sehingga tidak punya identitas untuk absen.
     * Alih-alih mengarang identitas, HRD diminta memilih kartu karyawannya
     * sendiri; pilihan itu disimpan ke {@code users.id_karyawan} yang memang
     * sudah ada — tanpa mengubah skema.
     */
    private JPanel panelPenautan(String sebab) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        JPanel kartu = AbsensiUI.kartu();
        JPanel isi = new JPanel();
        isi.setOpaque(false);
        isi.setLayout(new BoxLayout(isi, BoxLayout.Y_AXIS));

        JLabel judul = AbsensiUI.judul("Tautkan akun ke data karyawan");
        judul.setAlignmentX(LEFT_ALIGNMENT);
        isi.add(judul);
        isi.add(Box.createVerticalStrut(8));

        JLabel ket = new JLabel("<html><body style='width:440px'>" + sebab + "</body></html>");
        ket.setForeground(AbsensiUI.TEKS_LEMBUT);
        ket.setFont(AbsensiUI.font(Font.PLAIN, 12));
        ket.setAlignmentX(LEFT_ALIGNMENT);
        isi.add(ket);
        isi.add(Box.createVerticalStrut(12));

        JLabel lblPilih = new JLabel("Pilih kartu karyawan Anda (NIK | Nama):");
        lblPilih.setFont(AbsensiUI.font(Font.BOLD, 12));
        lblPilih.setForeground(AbsensiUI.TEKS);
        lblPilih.setAlignmentX(LEFT_ALIGNMENT);
        isi.add(lblPilih);
        isi.add(Box.createVerticalStrut(6));

        JComboBox<String> cmb = new JComboBox<>();
        cmb.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        cmb.setPreferredSize(new Dimension(420, 34));
        cmb.setAlignmentX(LEFT_ALIGNMENT);
        cmb.setEnabled(false);
        isi.add(cmb);

        JLabel lblInfo = new JLabel("Memuat daftar karyawan...");
        lblInfo.setFont(AbsensiUI.font(Font.PLAIN, 11));
        lblInfo.setForeground(AbsensiUI.TEKS_LEMBUT);
        lblInfo.setAlignmentX(LEFT_ALIGNMENT);
        lblInfo.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));
        isi.add(lblInfo);
        isi.add(Box.createVerticalStrut(14));

        JButton tautkan = AbsensiUI.tombol("Tautkan & lanjutkan", AbsensiUI.AKSEN);
        tautkan.setAlignmentX(LEFT_ALIGNMENT);
        tautkan.setEnabled(false);
        isi.add(tautkan);

        // Muat daftar DI LUAR EDT dan KUMPULKAN dulu; jangan membaca ResultSet di
        // dalam invokeLater (ResultSet sudah tertutup saat lambda berjalan -> dropdown kosong).
        new SwingWorker<java.util.List<String[]>, Void>() {
            @Override protected java.util.List<String[]> doInBackground() throws Exception {
                return IdentitasAbsensi.daftarKaryawan();
            }
            @Override protected void done() {
                java.util.List<String[]> daftar;
                try {
                    daftar = get();
                } catch (Exception e) {
                    lblInfo.setText("Gagal memuat daftar karyawan: " + e.getMessage());
                    lblInfo.setForeground(AbsensiUI.MERAH);
                    return;
                }
                cmb.removeAllItems();
                if (daftar == null || daftar.isEmpty()) {
                    lblInfo.setText("Belum ada data karyawan. Tambahkan data karyawan dulu "
                            + "di menu Data Karyawan.");
                    lblInfo.setForeground(AbsensiUI.MERAH);
                    return;
                }
                String username = Session.getUsername();
                int pilihIndex = 0;
                for (int i = 0; i < daftar.size(); i++) {
                    String[] baris = daftar.get(i);
                    cmb.addItem(baris[1] + " | " + baris[2] + "  (id " + baris[0] + ")");
                    // Pra-pilih kartu yang NIK/nama-nya sama dengan username.
                    if (username != null && !username.trim().isEmpty()
                            && (username.equalsIgnoreCase(baris[1])
                                || username.equalsIgnoreCase(baris[2]))) {
                        pilihIndex = i;
                    }
                }
                cmb.setSelectedIndex(pilihIndex);
                cmb.setEnabled(true);
                tautkan.setEnabled(true);
                lblInfo.setText(daftar.size() + " karyawan tersedia. Pilihan disimpan ke akun "
                        + "\"" + Session.getUsername() + "\" dan hanya perlu sekali.");
                lblInfo.setForeground(AbsensiUI.TEKS_LEMBUT);
            }
        }.execute();

        tautkan.addActionListener(e -> {
            Object dipilih = cmb.getSelectedItem();
            if (dipilih == null) {
                JOptionPane.showMessageDialog(AbsensiKaryawanPanel.this,
                        "Pilih dulu kartu karyawan Anda.");
                return;
            }
            String teks = String.valueOf(dipilih);
            int id;
            try {
                // Format: "NIK | Nama  (id 12)"
                String dalamKurung = teks.substring(teks.lastIndexOf("(id ") + 4, teks.lastIndexOf(")"));
                id = Integer.parseInt(dalamKurung.trim());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(AbsensiKaryawanPanel.this,
                        "Pilihan tidak dapat dibaca. Pilih ulang dari daftar.");
                return;
            }
            final int idKaryawan = id;
            tautkan.setEnabled(false);
            lblInfo.setText("Menautkan...");
            new SwingWorker<Boolean, Void>() {
                @Override protected Boolean doInBackground() throws Exception {
                    return IdentitasAbsensi.tautkanAkunKeKaryawan(Session.getUsername(), idKaryawan);
                }
                @Override protected void done() {
                    try {
                        if (Boolean.TRUE.equals(get())) {
                            // Muat ulang halaman: resolver kini membaca users.id_karyawan.
                            lapisan.removeAll();
                            cekStatusRegistrasi();
                            lapisan.revalidate();
                            lapisan.repaint();
                        } else {
                            tautkan.setEnabled(true);
                            lblInfo.setText("Penautan gagal. Periksa data karyawan.");
                            lblInfo.setForeground(AbsensiUI.MERAH);
                        }
                    } catch (Exception ex) {
                        tautkan.setEnabled(true);
                        lblInfo.setText("Penautan gagal: " + ex.getMessage());
                        lblInfo.setForeground(AbsensiUI.MERAH);
                    }
                }
            }.execute();
        });

        kartu.add(isi, BorderLayout.CENTER);
        p.add(kartu);
        return p;
    }

    // ----------------------------------------------------------- layar absensi

    private JScrollPane muatanAbsensi() {
        JPanel isi = new JPanel(new GridBagLayout());
        isi.setOpaque(false);
        isi.setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        GridBagConstraints g = AbsensiUI.gbc(0, 0);
        g.gridwidth = 2;
        g.weighty = 0;
        isi.add(bangunHeader(), g);

        g = AbsensiUI.gbc(0, 1);
        g.weightx = 0.6;
        g.weighty = 1;
        g.fill = GridBagConstraints.BOTH;
        isi.add(bangunKartuKamera(), g);

        g = AbsensiUI.gbc(1, 1);
        g.weightx = 0.4;
        g.weighty = 1;
        g.fill = GridBagConstraints.BOTH;
        isi.add(bangunKartuStatus(), g);

        g = AbsensiUI.gbc(0, 2);
        g.gridwidth = 2;
        g.weighty = 1;
        g.fill = GridBagConstraints.BOTH;
        isi.add(bangunKartuRiwayat(), g);

        JScrollPane sc = new JScrollPane(isi);
        sc.setBorder(null);
        sc.getViewport().setOpaque(false);
        sc.setOpaque(false);
        sc.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        sc.getVerticalScrollBar().setUnitIncrement(16);
        return sc;
    }

    private JPanel bangunHeader() {
        AbsensiUI.RoundedPanel h = AbsensiUI.header();

        JPanel kiri = new JPanel();
        kiri.setOpaque(false);
        kiri.setLayout(new BoxLayout(kiri, BoxLayout.Y_AXIS));
        JLabel judul = AbsensiUI.judul("Absensi Karyawan");
        JLabel identitas = AbsensiUI.subJudul("Selamat datang, " + aman(namaAbsensi())
                + "  •  ID " + idAbsensi());
        judul.setAlignmentX(LEFT_ALIGNMENT);
        identitas.setAlignmentX(LEFT_ALIGNMENT);
        kiri.add(judul);
        kiri.add(Box.createVerticalStrut(4));
        kiri.add(identitas);

        JPanel kanan = new JPanel();
        kanan.setOpaque(false);
        kanan.setLayout(new BoxLayout(kanan, BoxLayout.Y_AXIS));
        lblJam.setFont(AbsensiUI.font(Font.BOLD, 22));
        lblJam.setForeground(Color.WHITE);
        lblTanggal.setFont(AbsensiUI.font(Font.PLAIN, 12));
        lblTanggal.setForeground(new Color(203, 213, 225));
        lblJam.setAlignmentX(RIGHT_ALIGNMENT);
        lblTanggal.setAlignmentX(RIGHT_ALIGNMENT);
        kanan.add(lblJam);
        kanan.add(Box.createVerticalStrut(2));
        kanan.add(lblTanggal);

        h.add(kiri, BorderLayout.CENTER);
        h.add(kanan, BorderLayout.EAST);
        return h;
    }

    private JPanel bangunKartuKamera() {
        JPanel kartu = AbsensiUI.kartu();
        JPanel atas = new JPanel(new BorderLayout());
        atas.setOpaque(false);
        atas.add(AbsensiUI.judulBagian("Verifikasi wajah (preview mirror)"), BorderLayout.WEST);
        JPanel chip = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        chip.setOpaque(false);
        chip.add(lblChipWajah);
        chip.add(lblChipLiveness);
        atas.add(chip, BorderLayout.EAST);
        kartu.add(atas, BorderLayout.NORTH);

        lblPreview.setPreferredSize(new Dimension(520, 390));
        lblPreview.setMinimumSize(new Dimension(320, 240));
        lblPreview.setOpaque(true);
        lblPreview.setBackground(new Color(15, 27, 51));
        lblPreview.setForeground(new Color(203, 213, 225));
        lblPreview.setFont(AbsensiUI.font(Font.PLAIN, 14));
        lblPreview.setBorder(BorderFactory.createLineBorder(AbsensiUI.GARIS));
        kartu.add(lblPreview, BorderLayout.CENTER);

        JPanel bawah = new JPanel();
        bawah.setOpaque(false);
        bawah.setLayout(new BoxLayout(bawah, BoxLayout.Y_AXIS));
        lblStatusKamera.setAlignmentX(LEFT_ALIGNMENT);
        lblStatusWajah.setAlignmentX(LEFT_ALIGNMENT);
        lblStatusLiveness.setAlignmentX(LEFT_ALIGNMENT);
        lblInstruksiLiveness.setAlignmentX(LEFT_ALIGNMENT);
        lblInstruksiLiveness.setFont(AbsensiUI.font(Font.PLAIN, 12));
        lblInstruksiLiveness.setForeground(AbsensiUI.TEKS_LEMBUT);
        bawah.add(lblStatusKamera);
        bawah.add(Box.createVerticalStrut(3));
        bawah.add(lblStatusWajah);
        bawah.add(Box.createVerticalStrut(3));
        bawah.add(lblStatusLiveness);
        bawah.add(Box.createVerticalStrut(3));
        bawah.add(lblInstruksiLiveness);

        JPanel barisTombol = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        barisTombol.setOpaque(false);
        barisTombol.add(btnKamera);
        barisTombol.add(btnVerifikasi);
        bawah.add(Box.createVerticalStrut(4));
        bawah.add(barisTombol);
        kartu.add(bawah, BorderLayout.SOUTH);
        return kartu;
    }

    private JPanel bangunKartuStatus() {
        JPanel kartu = AbsensiUI.kartu();
        JPanel isi = new JPanel();
        isi.setOpaque(false);
        isi.setLayout(new BoxLayout(isi, BoxLayout.Y_AXIS));

        isi.add(AbsensiUI.judulBagian("Status kehadiran"));
        isi.add(Box.createVerticalStrut(8));
        isi.add(lblChipLokasi);
        isi.add(Box.createVerticalStrut(10));
        isi.add(AbsensiUI.garisPemisah());
        isi.add(Box.createVerticalStrut(10));

        lblJarak.setAlignmentX(LEFT_ALIGNMENT);
        lblRadius.setAlignmentX(LEFT_ALIGNMENT);
        lblArea.setAlignmentX(LEFT_ALIGNMENT);
        isi.add(lblJarak);
        isi.add(Box.createVerticalStrut(4));
        isi.add(lblRadius);
        isi.add(Box.createVerticalStrut(4));
        isi.add(lblArea);
        isi.add(Box.createVerticalStrut(10));
        JPanel barisLokasi = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        barisLokasi.setOpaque(false);
        barisLokasi.add(btnLokasi);
        isi.add(barisLokasi);

        isi.add(Box.createVerticalStrut(12));
        isi.add(AbsensiUI.garisPemisah());
        isi.add(Box.createVerticalStrut(12));
        isi.add(AbsensiUI.judulBagian("Absen"));

        JPanel tombolAbsen = new JPanel(new java.awt.GridLayout(1, 2, 8, 8));
        tombolAbsen.setOpaque(false);
        tombolAbsen.add(btnAbsenMasuk);
        tombolAbsen.add(btnAbsenPulang);
        isi.add(Box.createVerticalStrut(8));
        isi.add(tombolAbsen);

        areaCatatan.setEditable(false);
        areaCatatan.setLineWrap(true);
        areaCatatan.setWrapStyleWord(true);
        areaCatatan.setFont(AbsensiUI.font(Font.PLAIN, 12));
        JScrollPane sc = new JScrollPane(areaCatatan);
        sc.setBorder(BorderFactory.createTitledBorder("Catatan & hasil"));
        sc.setPreferredSize(new Dimension(360, 130));
        sc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));
        isi.add(Box.createVerticalStrut(10));
        isi.add(sc);

        kartu.add(isi, BorderLayout.CENTER);
        return kartu;
    }

    private JPanel bangunKartuRiwayat() {
        JPanel kartu = AbsensiUI.kartu();
        kartu.add(AbsensiUI.judulBagian("Riwayat absensi terbaru"), BorderLayout.NORTH);
        tblRiwayat.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        util.DesignUtil.applyTable(tblRiwayat);
        tblRiwayat.setRowHeight(26);
        JScrollPane sc = new JScrollPane(tblRiwayat);
        sc.setPreferredSize(new Dimension(800, 170));
        sc.setBorder(BorderFactory.createLineBorder(AbsensiUI.GARIS));
        kartu.add(sc, BorderLayout.CENTER);
        muatRiwayat();
        return kartu;
    }

    // ----------------------------------------------------------------- kamera

    private void sambungkanKamera() {
        kamera.tambahPendengar(new KameraService.PendengarFrame() {
            @Override public void frameBaru(BufferedImage gambar) {
                // Cermin HANYA untuk tampilan; analisis liveness tetap pakai
                // frame asli (orientasi driver) agar pemetaan arah konsisten.
                final BufferedImage tampil = KameraService.cermin(gambar);
                SwingUtilities.invokeLater(() -> {
                    int w = lblPreview.getWidth() > 0 ? lblPreview.getWidth() : 520;
                    int h = lblPreview.getHeight() > 0 ? lblPreview.getHeight() : 390;
                    Image skala = tampil.getScaledInstance(w, h, Image.SCALE_FAST);
                    lblPreview.setIcon(new javax.swing.ImageIcon(skala));
                    lblPreview.setText("");
                    lblStatusKamera.setText("Kamera: aktif (preview mirror)");
                    lblStatusKamera.setForeground(AbsensiUI.HIJAU);
                });
                // analisis frame di latar (jangan di EDT) — umpan liveness
                analisisFrameDiLatar(gambar);
            }
            @Override public void kameraGagal(String pesan) {
                SwingUtilities.invokeLater(() -> {
                    lblStatusKamera.setText("Kamera: " + pesan);
                    lblStatusKamera.setForeground(AbsensiUI.MERAH);
                    catat("Kamera gagal: " + pesan);
                });
            }
        });
    }

    /**
     * Deteksi wajah ringan per frame untuk liveness. Berjalan di worker —
     * EDT hanya menerima hasil. TIDAK ada transformasi frame di sini.
     */
    private void analisisFrameDiLatar(BufferedImage gambar) {
        if (!kamera.berjalan()) return;
        new SwingWorker<List<OpenCvBridge.WajahTerdeteksi>, Void>() {
            @Override protected List<OpenCvBridge.WajahTerdeteksi> doInBackground() {
                try {
                    if (!OpenCvBridge.siap()) {
                        OpenCvBridge.siapkanModel();
                        if (!OpenCvBridge.siap()) return null;
                    }
                    org.opencv.core.Mat mat = KameraService.gambarKeMat(gambar);
                    try {
                        return OpenCvBridge.deteksiWajah(mat);
                    } finally {
                        mat.release();
                    }
                } catch (Throwable t) {
                    return null;
                }
            }
            @Override protected void done() {
                try {
                    List<OpenCvBridge.WajahTerdeteksi> wajah = get();
                    if (wajah == null || wajah.isEmpty()) return;
                    if (!liveness.sedangAktif()) return;
                    LivenessService.PoseWajah pose = LivenessService.poseDari(wajah.get(0));
                    boolean lulus = liveness.umpan(pose);
                    lblStatusLiveness.setText(lulus
                            ? "Liveness: lulus — gerakan ke " + liveness.labelArahDiminta()
                              + " terdeteksi lalu kembali"
                            : "Liveness: " + liveness.instruksi());
                    lblStatusLiveness.setForeground(lulus ? AbsensiUI.HIJAU : AbsensiUI.AMBER);
                    if (lulus) {
                        livenessLulus = true;
                        lblChipLiveness.setText("Liveness: lulus");
                        lblChipLiveness.setForeground(AbsensiUI.HIJAU);
                        catat("Liveness lulus (" + liveness.labelArahDiminta() + ").");
                    }
                } catch (Exception ignored) { }
            }
        }.execute();
    }

    private void pasangAksiKamera() {
        btnKamera.addActionListener(e -> {
            if (kamera.berjalan()) {
                kamera.berhenti();
                liveness.batal();
                lblPreview.setIcon(null);
                lblPreview.setText("Kamera dimatikan");
                lblStatusKamera.setText("Kamera: dimatikan");
                lblStatusKamera.setForeground(AbsensiUI.TEKS_LEMBUT);
                catat("Kamera dimatikan.");
            } else {
                if (kamera.mulai()) {
                    // mulai tantangan liveness baru setiap kali kamera menyala
                    liveness.mulai();
                    lblStatusLiveness.setText("Liveness: " + liveness.instruksi());
                    lblInstruksiLiveness.setText("Arah diminta: " + liveness.labelArahDiminta()
                            + " (kanan/kiri fisik Anda, bukan sisi layar).");
                    lblChipLiveness.setText("Liveness: menunggu");
                    lblChipLiveness.setForeground(AbsensiUI.AMBER);
                    catat("Kamera aktif. " + liveness.instruksi());
                } else {
                    lblStatusKamera.setText("Kamera: " + kamera.status());
                    lblStatusKamera.setForeground(AbsensiUI.MERAH);
                    catat(kamera.status());
                }
            }
            btnKamera.setText(kamera.berjalan() ? "Matikan Kamera" : "Mulai Kamera");
            perbaruiKesiapan();
        });

        btnVerifikasi.addActionListener(e -> verifikasiPenuh());
        btnLokasi.addActionListener(e -> perbaruiLokasi());
        btnAbsenMasuk.addActionListener(e -> simpanAbsensi(true));
        btnAbsenPulang.addActionListener(e -> simpanAbsensi(false));

        btnAbsenMasuk.setEnabled(false);
        btnAbsenPulang.setEnabled(false);
    }

    // ------------------------------------------------------------ verifikasi

    /** Deteksi + liveness + identitas, di background. */
    private void verifikasiPenuh() {
        BufferedImage frame = kamera.frameTerakhir();
        if (frame == null) {
            catat("Belum ada gambar kamera. Tekan \"Mulai Kamera\" dulu.");
            return;
        }
        btnVerifikasi.setEnabled(false);
        btnVerifikasi.setText("Memeriksa...");
        new SwingWorker<WajahVerifier.HasilVerifikasi, Void>() {
            @Override protected WajahVerifier.HasilVerifikasi doInBackground() {
                try {
                    if (!OpenCvBridge.siap()) OpenCvBridge.siapkanModel();
                    org.opencv.core.Mat mat = KameraService.gambarKeMat(frame);
                    try {
                        List<OpenCvBridge.WajahTerdeteksi> wajah = OpenCvBridge.deteksiWajah(mat);
                        if (wajah.isEmpty()) {
                            return WajahVerifier.HasilVerifikasi.lainnya("TANPA_WAJAH",
                                    "Tidak ada wajah terdeteksi. Hadapkan wajah ke kamera.");
                        }
                        if (wajah.size() > 1) {
                            return WajahVerifier.HasilVerifikasi.lainnya("MULTI_WAJAH",
                                    "Ada " + wajah.size() + " wajah. Pastikan hanya Anda "
                                            + "yang berada di depan kamera.");
                        }
                        org.opencv.core.Rect area = new org.opencv.core.Rect(
                                wajah.get(0).x, wajah.get(0).y,
                                wajah.get(0).lebar, wajah.get(0).tinggi);
                        if (!LivenessService.kualitasCukup(mat, area)) {
                            return WajahVerifier.HasilVerifikasi.lainnya("KUALITAS_BURUK",
                                    "Kualitas gambar kurang baik. Perbaiki pencahayaan lalu ulangi.");
                        }
                        float[] emb = OpenCvBridge.hitungEmbedding(mat.submat(area));
                        return verifier.verifikasi(emb, idAbsensi(), namaAbsensi());
                    } finally {
                        mat.release();
                    }
                } catch (Throwable t) {
                    return WajahVerifier.HasilVerifikasi.lainnya("GAGAL_TEKNIS",
                            "Verifikasi gagal secara teknis: " + t.getMessage());
                }
            }

            @Override protected void done() {
                btnVerifikasi.setText("Verifikasi Wajah");
                btnVerifikasi.setEnabled(true);
                try {
                    WajahVerifier.HasilVerifikasi h = get();
                    wajahTerverifikasi = h.cocok();
                    skorWajahTerverifikasi = h.similarity;
                    lblStatusWajah.setText("Wajah: " + h.pesan);
                    lblStatusWajah.setForeground(h.cocok() ? AbsensiUI.HIJAU
                            : "TANPA_WAJAH".equals(h.status) ? AbsensiUI.TEKS_LEMBUT : AbsensiUI.MERAH);
                    if (h.cocok()) {
                        setChip(lblChipWajah, "Identitas cocok", AbsensiUI.HIJAU);
                    } else if ("TANPA_WAJAH".equals(h.status)) {
                        setChip(lblChipWajah, "Wajah tidak terlihat", AbsensiUI.TEKS_LEMBUT);
                    } else {
                        setChip(lblChipWajah, "Wajah tidak cocok", AbsensiUI.MERAH);
                    }
                    catat(h.pesan + (h.similarity >= 0
                            ? String.format("  (skor %.2f)", h.similarity) : ""));
                } catch (Exception ex) {
                    catat("Verifikasi gagal: " + ex.getMessage());
                } finally {
                    perbaruiKesiapan();
                }
            }
        }.execute();
    }

    /** Versi sinkron saat submit absensi (validasi ulang wajib). */
    private void verifikasiSinkron() {
        BufferedImage frame = kamera.frameTerakhir();
        wajahTerverifikasi = false;
        if (frame == null) return;
        try {
            if (!OpenCvBridge.siap()) OpenCvBridge.siapkanModel();
            org.opencv.core.Mat mat = KameraService.gambarKeMat(frame);
            try {
                List<OpenCvBridge.WajahTerdeteksi> wajah = OpenCvBridge.deteksiWajah(mat);
                if (wajah.size() != 1) return;
                org.opencv.core.Rect area = new org.opencv.core.Rect(
                        wajah.get(0).x, wajah.get(0).y,
                        wajah.get(0).lebar, wajah.get(0).tinggi);
                if (!LivenessService.kualitasCukup(mat, area)) return;
                float[] emb = OpenCvBridge.hitungEmbedding(mat.submat(area));
                WajahVerifier.HasilVerifikasi h = verifier.verifikasi(emb,
                        idAbsensi(), namaAbsensi());
                wajahTerverifikasi = h.cocok();
                skorWajahTerverifikasi = h.similarity;
            } finally {
                mat.release();
            }
        } catch (Throwable t) {
            wajahTerverifikasi = false;
        }
    }

    // ---------------------------------------------------------------- lokasi

    private void perbaruiLokasi() {
        btnLokasi.setEnabled(false);
        btnLokasi.setText("Meminta posisi...");
        new SwingWorker<LocationProvider.HasilLokasiPengguna, Void>() {
            @Override protected LocationProvider.HasilLokasiPengguna doInBackground() {
                return penyediaLokasi.ambilLokasi();
            }
            @Override protected void done() {
                btnLokasi.setEnabled(true);
                btnLokasi.setText("Perbarui Lokasi");
                try {
                    LocationProvider.HasilLokasiPengguna h = get();
                    lokasiTerakhir = h;
                    if (h.tersedia) {
                        LocationService.Hasil cek = lokasiService.validasi(
                                h.latitude, h.longitude, h.akurasiMeter);
                        lokasiLolos = cek.lolos;
                        lblJarak.setText("Jarak ke kantor: " + (cek.jarakMeter >= 0
                                ? Math.round(cek.jarakMeter) + " meter" : "belum dapat dihitung"));
                        lblJarak.setForeground(cek.lolos ? AbsensiUI.HIJAU : AbsensiUI.MERAH);
                        lblRadius.setText("Radius absensi (dari HRD): " + cek.radiusMeter + " meter");
                        lblArea.setText(cek.lolos
                                ? "Area: DI DALAM area absensi"
                                : "Area: DI LUAR area absensi");
                        lblArea.setForeground(cek.lolos ? AbsensiUI.HIJAU : AbsensiUI.MERAH);
                        setChip(lblChipLokasi, cek.lolos ? "Lokasi valid" : "Di luar area",
                                cek.lolos ? AbsensiUI.HIJAU : AbsensiUI.MERAH);
                        catat(cek.pesan);
                    } else {
                        lokasiLolos = false;
                        lblJarak.setText("Jarak ke kantor: belum dapat dihitung");
                        lblJarak.setForeground(AbsensiUI.MERAH);
                        lblArea.setText("Area: belum diketahui");
                        lblArea.setForeground(AbsensiUI.MERAH);
                        setChip(lblChipLokasi, "Lokasi belum tersedia", AbsensiUI.MERAH);
                        catat("Lokasi belum tersedia: " + h.keterangan
                                + " (periksa izin lokasi browser/GPS perangkat, lalu coba lagi)");
                    }
                } catch (Exception ex) {
                    lokasiLolos = false;
                    lblJarak.setText("Jarak ke kantor: gagal diperiksa");
                    lblJarak.setForeground(AbsensiUI.MERAH);
                    catat("Pemeriksaan lokasi gagal: " + ex.getMessage());
                } finally {
                    perbaruiKesiapan();
                }
            }
        }.execute();
    }

    private void perbaruiKesiapan() {
        // Liveness (tantangan tengok kiri/kanan) wajib lulus — mencegah foto diam.
        boolean siap = wajahTerverifikasi && lokasiLolos && livenessLulus;
        btnAbsenMasuk.setEnabled(siap);
        btnAbsenPulang.setEnabled(siap);
        String tip = siap ? null
                : "Wajib: identitas wajah cocok, liveness lulus, DAN lokasi di dalam radius.";
        btnAbsenMasuk.setToolTipText(tip);
        btnAbsenPulang.setToolTipText(tip);
    }

    // ----------------------------------------------------------------- absen

    private void simpanAbsensi(boolean masuk) {
        // WAJIB pakai identitas hasil resolusi: akun HRD boleh punya
        // users.id_karyawan NULL dan tetap berhak absen setelah menautkan.
        final int idKaryawan = idAbsensi();
        if (idKaryawan <= 0) {
            catat("Identitas karyawan belum tersedia — lakukan penautan akun dulu.");
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
        new SwingWorker<AbsensiService.HasilAbsen, Void>() {
            @Override protected AbsensiService.HasilAbsen doInBackground() {
                try {
                    BufferedImage frame = kamera.frameTerakhir();
                    if (frame == null) {
                        return AbsensiService.HasilAbsen.gagal(
                                "Absensi ditolak: kamera tidak aktif.");
                    }
                    // validasi lokasi ULANG saat submit (bukan cache)
                    if (lokasiTerakhir == null || !lokasiTerakhir.tersedia) {
                        return AbsensiService.HasilAbsen.gagal(
                                "Absensi ditolak: lokasi belum diperiksa. "
                                        + "Tekan \"Perbarui Lokasi\" dulu.");
                    }
                    LocationProvider.HasilLokasiPengguna ulang = penyediaLokasi.ambilLokasi();
                    if (!ulang.tersedia) {
                        return AbsensiService.HasilAbsen.gagal(
                                "Absensi ditolak: lokasi perangkat belum tersedia.");
                    }
                    LocationService.Hasil cek = lokasiService.validasi(
                            ulang.latitude, ulang.longitude, ulang.akurasiMeter);
                    if (!cek.lolos) {
                        return AbsensiService.HasilAbsen.gagal(cek.pesan);
                    }

                    // validasi identitas ULANG saat submit
                    verifikasiSinkron();
                    if (!wajahTerverifikasi) {
                        AbsensiService.catatLog(idKaryawan, LocalDate.now(),
                                AbsensiService.ALASAN_WAJAH_TIDAK_COCOK,
                                "Percobaan absen " + (masuk ? "masuk" : "pulang")
                                        + " dengan wajah tidak cocok");
                        return AbsensiService.HasilAbsen.gagal(
                                "Absensi ditolak: verifikasi wajah tidak cocok dengan akun "
                                        + namaAbsensi() + ".");
                    }

                    ByteArrayOutputStream buf = new ByteArrayOutputStream();
                    ImageIO.write(frame, "jpg", buf);
                    String foto = BuktiAbsensiStorage.simpan(buf.toByteArray(), idKaryawan);

                    AbsensiService.BuktiAbsen bukti = new AbsensiService.BuktiAbsen();
                    bukti.verifikasiWajah = AbsensiService.WAJAH_COCOK;
                    bukti.skorWajah = skorWajahTerverifikasi;
                    bukti.lat = ulang.latitude;
                    bukti.lon = ulang.longitude;
                    bukti.jarakMeter = cek.jarakMeter;
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

            @Override protected void done() {
                try {
                    AbsensiService.HasilAbsen h = get();
                    catat((h.sukses ? "BERHASIL: " : "DITOLAK: ") + h.pesan
                            + (h.jam != null ? "  (jam " + h.jam + ")" : ""));
                    if (h.sukses) muatRiwayat();
                } catch (Exception ex) {
                    catat("Gagal menyimpan absensi: " + ex.getMessage());
                } finally {
                    perbaruiKesiapan();
                }
            }
        }.execute();
    }

    // --------------------------------------------------------------- riwayat

    private void muatRiwayat() {
        modelRiwayat.setRowCount(0);
        int idKaryawan = idAbsensi();
        if (idKaryawan <= 0) {
            catat("Riwayat tidak dimuat: identitas karyawan belum tersedia.");
            return;
        }
        String sql = "SELECT tanggal, jam_masuk, jam_pulang, status, verifikasi_wajah, jarak_meter "
                + "FROM absensi WHERE id_karyawan = ? ORDER BY tanggal DESC, jam_masuk DESC LIMIT 10";
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
                            rs.getString("verifikasi_wajah") == null ? "-"
                                    : rs.getString("verifikasi_wajah"),
                            jarak == null ? "-" : String.format("%.0f", rs.getDouble("jarak_meter"))});
                }
            }
        } catch (Exception e) {
            catat("Riwayat absensi tidak dapat dimuat: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------ util

    private void perbaruiJam() {
        LocalDateTime sekarang = LocalDateTime.now();
        lblJam.setText(sekarang.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        lblTanggal.setText(sekarang.format(DateTimeFormatter.ofPattern(
                "EEEE, d MMMM yyyy", new Locale("id", "ID"))));
    }

    private void setChip(JLabel chip, String teks, Color warna) {
        chip.setText(teks);
        chip.setForeground(warna);
        chip.setBackground(AbsensiUI.campur(warna, Color.WHITE, 0.88));
        chip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AbsensiUI.campur(warna, Color.WHITE, 0.62)),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)));
    }

    private void catat(String pesan) {
        SwingUtilities.invokeLater(() -> {
            areaCatatan.append("[" + LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("HH:mm:ss")) + "] " + pesan + "\n");
            areaCatatan.setCaretPosition(areaCatatan.getDocument().getLength());
        });
    }

    /** ID karyawan untuk absensi (hasil resolusi; HRD tanpa id_karyawan tetap tertangani). */
    private int idAbsensi() {
        if (identitas != null && identitas.idKaryawan > 0) return identitas.idKaryawan;
        return Session.getIdKaryawan();
    }

    /** Nama untuk tampilan (hasil resolusi bila ada). */
    private String namaAbsensi() {
        if (identitas != null && identitas.nama != null && !identitas.nama.trim().isEmpty()) {
            return identitas.nama;
        }
        return Session.getNamaKaryawan();
    }

    private static String aman(String s) {
        return (s == null || s.trim().isEmpty()) ? "(tanpa nama)" : s.trim();
    }

    @Override
    public void removeNotify() {
        timerJam.stop();
        liveness.batal();
        kamera.berhenti();
        super.removeNotify();
    }
}
