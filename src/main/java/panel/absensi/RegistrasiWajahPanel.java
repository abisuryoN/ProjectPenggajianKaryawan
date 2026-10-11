package panel.absensi;

import kamera.KameraService;
import service.face.RegistrasiWajahService;
import util.Session;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Halaman REGISTRASI WAJAH (3 foto dalam satu sesi) untuk karyawan yang
 * sedang login. Dibangun dengan kode, bukan desain .form lama.
 *
 * <p>Susunan: header navy (judul + nama karyawan + alasan) / area utama
 * (preview kamera besar + satu lingkaran progres + instruksi pose + status
 * kualitas) / strip thumbnail 3 foto / baris tombol yang aktif sesuai tahap.
 *
 * <p>ID karyawan selalu dari {@link Session} — tidak ada input ID manual.
 */
public class RegistrasiWajahPanel extends JPanel {

    private static final float LEFT_ALIGNMENT = java.awt.Component.LEFT_ALIGNMENT;

    /** Callback ketika registrasi selesai & tersimpan. */
    public interface SelesaiListener {
        void registrasiSelesai();
    }

    private final RegistrasiWajahService registrasi = new RegistrasiWajahService();
    private final KameraService kamera = new KameraService();
    private final SelesaiListener selesaiListener;

    private final JLabel lblPreview = new JLabel("Kamera belum dinyalakan", JLabel.CENTER);
    private final LingkaranProgres lingkaran = new LingkaranProgres();
    private final JLabel lblTahap = AbsensiUI.label("-");
    private final JLabel lblInstruksi = new JLabel();
    private final JLabel lblStatusKamera = AbsensiUI.label("Kamera: belum dinyalakan");
    private final JLabel lblKualitas = AbsensiUI.label("Kualitas foto: -");
    private final JLabel lblPesan = new JLabel(" ");
    private final JTextArea areaCatatan = new JTextArea(4, 30);

    private final JButton btnKamera = AbsensiUI.tombol("Mulai Kamera", AbsensiUI.AKSEN_BIRU);
    private final JButton btnAmbil = AbsensiUI.tombol("Ambil Foto", AbsensiUI.AKSEN);
    private final JButton btnUlangi = AbsensiUI.tombolNetral("Ulangi Foto Terakhir");
    private final JButton btnSelesai = AbsensiUI.tombol("Selesaikan Registrasi", AbsensiUI.HIJAU);
    private final JButton btnMulaiUlang = AbsensiUI.tombolNetral("Mulai Ulang Sesi");

    private final List<JLabel> thumbnail = new ArrayList<>();
    private final CardLayout kartu = new CardLayout();
    private final JPanel pnlKartu = new JPanel(kartu);

    /** ID karyawan pemilik registrasi — SELALU dari sesi terverifikasi, bukan input bebas. */
    private final int idKaryawanSesi;

    /** Pertahankan konstruktor lama (ID dari sesi) demi kompatibilitas. */
    public RegistrasiWajahPanel(SelesaiListener selesaiListener) {
        this(util.Session.getIdKaryawan(), selesaiListener);
    }

    /**
     * @param idKaryawan id karyawan dari sesi autentikasi yang sudah divalidasi
     *                   (tidak boleh berasal dari input pengguna).
     */
    public RegistrasiWajahPanel(int idKaryawan, SelesaiListener selesaiListener) {
        this.idKaryawanSesi = idKaryawan;
        this.selesaiListener = selesaiListener;

        setLayout(new BorderLayout(0, 14));
        setBackground(AbsensiUI.PAGE);
        setBorder(BorderFactory.createEmptyBorder(16, 18, 16, 18));

        add(bangunHeader(), BorderLayout.NORTH);
        pnlKartu.setOpaque(false);
        pnlKartu.add(bangunAlurRegistrasi(), "alur");
        pnlKartu.add(bangunKonfirmasi(), "selesai");
        add(pnlKartu, BorderLayout.CENTER);

        sambungkanKamera();
        pasangAksi();
        perbaruiStatus();
    }

    // --------------------------------------------------------------- header

    private JPanel bangunHeader() {
        AbsensiUI.RoundedPanel h = AbsensiUI.header();
        JPanel kiri = new JPanel();
        kiri.setOpaque(false);
        kiri.setLayout(new javax.swing.BoxLayout(kiri, javax.swing.BoxLayout.Y_AXIS));
        JLabel judul = AbsensiUI.judul("Registrasi Wajah");
        JLabel nama = AbsensiUI.subJudul("Nama: " + aman(util.Session.getNamaKaryawan())
                + "  •  ID Karyawan: " + idKaryawanSesi);
        JLabel alasan = AbsensiUI.subJudul("Registrasi diperlukan karena akun Anda "
                + "belum memiliki foto referensi wajah untuk verifikasi absensi.");
        judul.setAlignmentX(LEFT_ALIGNMENT);
        nama.setAlignmentX(LEFT_ALIGNMENT);
        alasan.setAlignmentX(LEFT_ALIGNMENT);
        kiri.add(judul);
        kiri.add(javax.swing.Box.createVerticalStrut(4));
        kiri.add(nama);
        kiri.add(javax.swing.Box.createVerticalStrut(2));
        kiri.add(alasan);
        h.add(kiri, BorderLayout.CENTER);
        return h;
    }

    // ----------------------------------------------------------- alur utama

    private JPanel bangunAlurRegistrasi() {
        JPanel isi = new JPanel(new GridBagLayout());
        isi.setOpaque(false);

        // -------- kiri: preview kamera
        lblPreview.setPreferredSize(new Dimension(560, 420));
        lblPreview.setMinimumSize(new Dimension(360, 270));
        lblPreview.setOpaque(true);
        lblPreview.setBackground(new Color(15, 27, 51));
        lblPreview.setForeground(new Color(203, 213, 225));
        lblPreview.setFont(AbsensiUI.font(Font.PLAIN, 14));
        lblPreview.setBorder(BorderFactory.createLineBorder(AbsensiUI.GARIS));

        JPanel kartuKamera = AbsensiUI.kartu();
        kartuKamera.add(AbsensiUI.judulBagian("Kamera (preview mirror)"), BorderLayout.NORTH);
        kartuKamera.add(lblPreview, BorderLayout.CENTER);

        JPanel statusKamera = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        statusKamera.setOpaque(false);
        statusKamera.add(lblStatusKamera);
        statusKamera.add(lblKualitas);
        kartuKamera.add(statusKamera, BorderLayout.SOUTH);

        // -------- kanan: progres + instruksi
        JPanel kartuProgres = AbsensiUI.kartu();
        JPanel isiProgres = new JPanel();
        isiProgres.setOpaque(false);
        isiProgres.setLayout(new javax.swing.BoxLayout(isiProgres, javax.swing.BoxLayout.Y_AXIS));

        JPanel barisLingkaran = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        barisLingkaran.setOpaque(false);
        barisLingkaran.add(lingkaran);
        isiProgres.add(AbsensiUI.judulBagian("Progres registrasi (3 foto)"));
        isiProgres.add(javax.swing.Box.createVerticalStrut(6));
        isiProgres.add(barisLingkaran);
        isiProgres.add(javax.swing.Box.createVerticalStrut(10));
        isiProgres.add(AbsensiUI.garisPemisah());
        isiProgres.add(javax.swing.Box.createVerticalStrut(10));

        lblTahap.setFont(AbsensiUI.font(Font.BOLD, 13));
        lblTahap.setForeground(AbsensiUI.NAVY);
        lblInstruksi.setFont(AbsensiUI.font(Font.PLAIN, 13));
        lblInstruksi.setForeground(AbsensiUI.TEKS);
        isiProgres.add(lblTahap);
        isiProgres.add(javax.swing.Box.createVerticalStrut(4));
        isiProgres.add(lblInstruksi);
        isiProgres.add(javax.swing.Box.createVerticalStrut(10));
        isiProgres.add(AbsensiUI.judulBagian("Sampel tersimpan"));
        isiProgres.add(javax.swing.Box.createVerticalStrut(6));

        JPanel strip = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        strip.setOpaque(false);
        for (int i = 0; i < RegistrasiWajahService.JUMLAH_SAMPEL; i++) {
            JLabel t = new JLabel("Foto " + (i + 1), JLabel.CENTER);
            t.setPreferredSize(new Dimension(96, 72));
            t.setOpaque(true);
            t.setBackground(AbsensiUI.LEMBUT);
            t.setForeground(AbsensiUI.TEKS_LEMBUT);
            t.setFont(AbsensiUI.font(Font.PLAIN, 11));
            t.setBorder(BorderFactory.createLineBorder(AbsensiUI.GARIS));
            thumbnail.add(t);
            strip.add(t);
        }
        isiProgres.add(strip);

        lblPesan.setFont(AbsensiUI.font(Font.BOLD, 12));
        lblPesan.setForeground(AbsensiUI.TEKS_LEMBUT);
        isiProgres.add(javax.swing.Box.createVerticalStrut(10));
        isiProgres.add(lblPesan);
        kartuProgres.add(isiProgres, BorderLayout.CENTER);

        // -------- baris tombol (aktif sesuai tahap)
        JPanel aksi = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        aksi.setOpaque(false);
        aksi.add(btnKamera);
        aksi.add(btnAmbil);
        aksi.add(btnUlangi);
        aksi.add(btnSelesai);
        aksi.add(btnMulaiUlang);

        areaCatatan.setEditable(false);
        areaCatatan.setLineWrap(true);
        areaCatatan.setWrapStyleWord(true);
        areaCatatan.setFont(AbsensiUI.font(Font.PLAIN, 12));
        JScrollPane scCatatan = new JScrollPane(areaCatatan);
        scCatatan.setBorder(BorderFactory.createTitledBorder("Catatan proses"));
        scCatatan.setPreferredSize(new Dimension(300, 110));

        JPanel bawah = new JPanel(new BorderLayout(0, 8));
        bawah.setOpaque(false);
        bawah.add(aksi, BorderLayout.NORTH);
        bawah.add(scCatatan, BorderLayout.CENTER);

        GridBagConstraints g = AbsensiUI.gbc(0, 0);
        g.weightx = 0.62;
        g.weighty = 1;
        g.fill = GridBagConstraints.BOTH;
        isi.add(kartuKamera, g);

        g = AbsensiUI.gbc(1, 0);
        g.weightx = 0.38;
        g.weighty = 1;
        g.fill = GridBagConstraints.BOTH;
        isi.add(kartuProgres, g);

        g = AbsensiUI.gbc(0, 1);
        g.gridwidth = 2;
        g.weighty = 0;
        g.fill = GridBagConstraints.HORIZONTAL;
        isi.add(bawah, g);

        JScrollPane luar = new JScrollPane(isi);
        luar.setBorder(null);
        luar.setOpaque(false);
        luar.getViewport().setOpaque(false);
        luar.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        luar.getVerticalScrollBar().setUnitIncrement(16);

        JPanel bungkus = new JPanel(new BorderLayout());
        bungkus.setOpaque(false);
        bungkus.add(luar, BorderLayout.CENTER);
        return bungkus;
    }

    private JPanel bangunKonfirmasi() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setOpaque(false);
        JPanel kartu = AbsensiUI.kartu();
        JPanel isi = new JPanel();
        isi.setOpaque(false);
        isi.setLayout(new javax.swing.BoxLayout(isi, javax.swing.BoxLayout.Y_AXIS));

        JLabel t = AbsensiUI.judulBagian("Registrasi wajah berhasil.");
        JLabel p2 = new JLabel("Registrasi wajah berhasil. Anda sekarang dapat melanjutkan "
                + "ke verifikasi absensi.");
        p2.setFont(AbsensiUI.font(Font.PLAIN, 14));
        p2.setForeground(AbsensiUI.HIJAU);
        JButton lanjut = AbsensiUI.tombol("Lanjut ke Absensi", AbsensiUI.AKSEN_BIRU);
        lanjut.addActionListener(e -> {
            if (selesaiListener != null) selesaiListener.registrasiSelesai();
        });

        isi.add(t);
        isi.add(javax.swing.Box.createVerticalStrut(8));
        isi.add(p2);
        isi.add(javax.swing.Box.createVerticalStrut(14));
        JPanel baris = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        baris.setOpaque(false);
        baris.add(lanjut);
        isi.add(baris);
        kartu.add(isi, BorderLayout.CENTER);
        p.add(kartu);
        return p;
    }

    // ---------------------------------------------------------------- kamera

    private void sambungkanKamera() {
        kamera.tambahPendengar(new KameraService.PendengarFrame() {
            @Override
            public void frameBaru(BufferedImage gambar) {
                // Cermin HANYA untuk tampilan; `gambar` (orientasi asli) tetap
                // dipakai untuk analisis & foto bukti.
                final BufferedImage tampil = KameraService.cermin(gambar);
                SwingUtilities.invokeLater(() -> {
                    int w = lblPreview.getWidth() > 0 ? lblPreview.getWidth() : 560;
                    int h = lblPreview.getHeight() > 0 ? lblPreview.getHeight() : 420;
                    Image skala = tampil.getScaledInstance(w, h, Image.SCALE_FAST);
                    lblPreview.setIcon(new javax.swing.ImageIcon(skala));
                    lblPreview.setText("");
                    lblStatusKamera.setText("Kamera: aktif (preview mirror)");
                    lblStatusKamera.setForeground(AbsensiUI.HIJAU);
                });
            }

            @Override
            public void kameraGagal(String pesan) {
                SwingUtilities.invokeLater(() -> {
                    lblStatusKamera.setText("Kamera: gagal — " + pesan);
                    lblStatusKamera.setForeground(AbsensiUI.MERAH);
                    catat("Kamera gagal: " + pesan);
                    perbaruiStatus();
                });
            }
        });
    }

    private void pasangAksi() {
        btnKamera.addActionListener(e -> {
            if (kamera.berjalan()) {
                kamera.berhenti();
                lblPreview.setIcon(null);
                lblPreview.setText("Kamera dimatikan");
                lblStatusKamera.setText("Kamera: dimatikan");
                lblStatusKamera.setForeground(AbsensiUI.TEKS_LEMBUT);
                catat("Kamera dimatikan.");
            } else {
                if (kamera.mulai()) {
                    catat("Kamera aktif. Ambil foto sesuai instruksi.");
                } else {
                    lblStatusKamera.setText("Kamera: " + kamera.status());
                    lblStatusKamera.setForeground(AbsensiUI.MERAH);
                    catat(kamera.status());
                }
            }
            perbaruiStatus();
        });

        btnAmbil.addActionListener(e -> ambilFoto());
        btnUlangi.addActionListener(e -> ulangiTerakhir());
        btnMulaiUlang.addActionListener(e -> {
            registrasi.reset();
            // Lepaskan kamera agar tidak ada resource tertinggal saat sesi
            // dimulai ulang (perangkat bisa langsung dipakai sesi berikutnya).
            kamera.berhenti();
            lblPreview.setIcon(null);
            lblPreview.setText("Kamera dimatikan — tekan \"Mulai Kamera\" untuk sesi baru");
            lblStatusKamera.setText("Kamera: dimatikan");
            lblStatusKamera.setForeground(AbsensiUI.TEKS_LEMBUT);
            for (int i = 0; i < thumbnail.size(); i++) setThumb(i, null);
            catat("Sesi registrasi dikosongkan; kamera dimatikan.");
            perbaruiStatus();
        });
        btnSelesai.addActionListener(e -> selesaikanRegistrasi());
    }

    private void ambilFoto() {
        if (!kamera.berjalan()) {
            catat("Nyalakan kamera dulu sebelum mengambil foto.");
            return;
        }
        BufferedImage frame = kamera.frameTerakhir();
        if (frame == null) {
            catat("Belum ada gambar dari kamera. Tunggu sebentar lalu coba lagi.");
            return;
        }
        btnAmbil.setEnabled(false);
        btnAmbil.setText("Memproses...");
        new SwingWorker<RegistrasiWajahService.HasilAmbil, Void>() {
            @Override protected RegistrasiWajahService.HasilAmbil doInBackground() {
                return registrasi.ambilDariFrame(frame, idKaryawanSesi);
            }
            @Override protected void done() {
                btnAmbil.setText("Ambil Foto");
                try {
                    RegistrasiWajahService.HasilAmbil h = get();
                    catat(h.pesan);
                    lblPesan.setText(h.pesan);
                    lblPesan.setForeground(h.sukses ? AbsensiUI.HIJAU : AbsensiUI.MERAH);
                    if (h.sukses) {
                        setThumb(h.jumlahValid - 1, frame);
                        lblKualitas.setText("Kualitas foto terakhir: "
                                + registrasi.daftarSampel().get(h.jumlahValid - 1).kualitas + "/100");
                    }
                } catch (Exception ex) {
                    catat("Gagal memproses foto: " + ex.getMessage());
                } finally {
                    perbaruiStatus();
                }
            }
        }.execute();
    }

    private void ulangiTerakhir() {
        RegistrasiWajahService.Sampel dibuang = registrasi.hapusTerakhir();
        if (dibuang == null) {
            catat("Belum ada foto untuk diulang.");
            return;
        }
        int n = registrasi.jumlahSampel();
        setThumb(n, null);
        catat("Foto terakhir dibatalkan. Silakan ambil ulang sesuai instruksi tahap "
                + (n + 1) + ".");
        perbaruiStatus();
    }

    private void selesaikanRegistrasi() {
        if (!registrasi.lengkap()) {
            catat("Registrasi belum lengkap (" + registrasi.jumlahSampel() + "/3).");
            return;
        }
        btnSelesai.setEnabled(false);
        btnSelesai.setText("Menyimpan...");
        new SwingWorker<String, Void>() {
            @Override protected String doInBackground() {
                return registrasi.selesaikan(idKaryawanSesi,
                        Session.getIdUser(), Session.isHRD());
            }
            @Override protected void done() {
                btnSelesai.setText("Selesaikan Registrasi");
                try {
                    String galat = get();
                    if (galat == null) {
                        kamera.berhenti();
                        catat("Registrasi wajah berhasil disimpan.");
                        kartu.show(pnlKartu, "selesai");
                    } else {
                        catat(galat);
                        lblPesan.setText(galat);
                        lblPesan.setForeground(AbsensiUI.MERAH);
                    }
                } catch (Exception ex) {
                    catat("Gagal menyelesaikan registrasi: " + ex.getMessage());
                } finally {
                    perbaruiStatus();
                }
            }
        }.execute();
    }

    // --------------------------------------------------------------- status

    private void perbaruiStatus() {
        int n = registrasi.jumlahSampel();
        lingkaran.setProgres(n, RegistrasiWajahService.JUMLAH_SAMPEL);
        boolean kameraAktif = kamera.berjalan();

        if (n >= RegistrasiWajahService.JUMLAH_SAMPEL) {
            lblTahap.setText("Tahap 3 dari 3 — semua foto terambil");
            lblInstruksi.setText("Periksa thumbnail. Bila sudah sesuai, tekan "
                    + "\"Selesaikan Registrasi\".");
        } else {
            lblTahap.setText("Tahap " + (n + 1) + " dari 3");
            lblInstruksi.setText("<html><body style='width:260px'>"
                    + RegistrasiWajahService.INSTRUKSI_POSE[n] + "</body></html>");
        }

        btnKamera.setText(kameraAktif ? "Matikan Kamera" : "Mulai Kamera");
        btnAmbil.setEnabled(kameraAktif && n < RegistrasiWajahService.JUMLAH_SAMPEL);
        btnUlangi.setEnabled(n > 0 && n < RegistrasiWajahService.JUMLAH_SAMPEL);
        btnSelesai.setEnabled(n >= RegistrasiWajahService.JUMLAH_SAMPEL);
        btnMulaiUlang.setEnabled(n > 0 || kameraAktif);
    }

    private void setThumb(int index, BufferedImage gambar) {
        if (index < 0 || index >= thumbnail.size()) return;
        JLabel t = thumbnail.get(index);
        if (gambar == null) {
            t.setIcon(null);
            t.setText("Foto " + (index + 1));
            t.setBackground(AbsensiUI.LEMBUT);
        } else {
            Image skala = gambar.getScaledInstance(96, 72, Image.SCALE_FAST);
            t.setIcon(new javax.swing.ImageIcon(skala));
            t.setText("");
            t.setBackground(Color.WHITE);
        }
        t.revalidate();
        t.repaint();
    }

    private void catat(String pesan) {
        SwingUtilities.invokeLater(() -> {
            areaCatatan.append("• " + pesan + "\n");
            areaCatatan.setCaretPosition(areaCatatan.getDocument().getLength());
        });
    }

    private static String aman(String s) {
        return (s == null || s.trim().isEmpty()) ? "(tanpa nama)" : s.trim();
    }

    @Override
    public void removeNotify() {
        kamera.berhenti();
        super.removeNotify();
    }
}
