package service.face;

import config.Koneksi;
import org.opencv.core.Mat;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Registrasi wajah MANDIRI oleh karyawan yang sedang login (3 foto).
 *
 * <p>Aturan keras (permintaan pengguna §3):
 * <ul>
 *   <li>ID karyawan WAJIB berasal dari sesi login — tidak ada parameter id
 *       bebas. Menolak registrasi atas nama orang lain.</li>
 *   <li>3 foto dari 3 pengambilan BERBEDA (depan, kiri, kanan). Frame yang
 *       sama tidak boleh dihitung dua kali (dicek lewat hash byte).</li>
 *   <li>Satu wajah per foto; dua+ wajah ditolak.</li>
 *   <li>Template hanya ditulis SETELAH 3 sampel valid; kegagalan di tengah
 *       JANGAN mengubah status registrasi jadi "lengkap".</li>
 *   <li>Foto referensi disimpan sebagai file berizin ketat (700) di
 *       {@code ~/.penggajian/wajah/<id>/}; embedding tetap di DB.</li>
 * </ul>
 */
public class RegistrasiWajahService {

    /** Jumlah sampel wajib satu sesi registrasi. */
    public static final int JUMLAH_SAMPEL = 3;

    /**
     * Ambang cosine similarity untuk menyatakan wajah KEMBAR lintas akun.
     * Sedikit di bawah ambang verifikasi (0,55) karena template pembanding
     * adalah rata-rata 3 pose (bisa sedikit lebih jauh dari frame tunggal),
     * tetapi tetap jauh di atas similarity antar orang berbeda.
     */
    public static final double AMBANG_WAJAH_KEMBAR = 0.50;

    /** Instruksi pose per tahap, urut. */
    public static final String[] INSTRUKSI_POSE = {
            "Foto 1: hadapkan wajah lurus ke kamera.",
            "Foto 2: putar kepala SEDIKIT ke KIRI Anda (wajah tetap terlihat).",
            "Foto 3: putar kepala SEDIKIT ke KANAN Anda (wajah tetap terlihat)."
    };

    /** Satu sampel valid sementara (belum tentu tersimpan permanen). */
    public static final class Sampel {
        public final byte[] jpeg;
        public final float[] embedding;
        public final int kualitas;
        public final String hash; // sha-256 dari jpeg — pendeteksi foto kembar

        Sampel(byte[] jpeg, float[] embedding, int kualitas, String hash) {
            this.jpeg = jpeg;
            this.embedding = embedding;
            this.kualitas = kualitas;
            this.hash = hash;
        }
    }

    private final List<Sampel> sampel = new ArrayList<>();
    private final WajahDAO wajahDAO = new WajahDAO();

    /** Jumlah sampel valid yang sudah terkumpul di sesi ini. */
    public synchronized int jumlahSampel() { return sampel.size(); }

    /** Sesi sudah lengkap 3/3. */
    public synchronized boolean lengkap() { return sampel.size() >= JUMLAH_SAMPEL; }

    /** Sesi dikosongkan (mulai dari awal). */
    public synchronized void reset() { sampel.clear(); }

    /**
     * Masukkan kembali sampel yang sudah tervalidasi (dipakai "Ulangi" pada
     * UI yang membangun ulang sesi dari sisa sampel). Tidak lolos untuk
     * sampel melebihi batas.
     */
    public synchronized void sisipkanSampel(Sampel s) {
        if (s != null && sampel.size() < JUMLAH_SAMPEL) {
            sampel.add(s);
        }
    }

    /**
     * Buang sampel TERAKHIR (tombol "Ulangi"). Progres turun satu langkah —
     * foto yang gagal tidak pernah dihitung sebagai sampel valid.
     *
     * @return sampel yang dibuang, atau null bila tidak ada.
     */
    public synchronized Sampel hapusTerakhir() {
        if (sampel.isEmpty()) return null;
        return sampel.remove(sampel.size() - 1);
    }

    /** Salinan daftar sampel (aman dibaca UI). */
    public synchronized List<Sampel> daftarSampel() {
        return new ArrayList<>(sampel);
    }

    /**
     * Proses SATU frame kamera untuk tahap berikutnya.
     *
     * <p>Alur: deteksi wajah -> satu wajah saja -> kualitas cukup -> hitung
     * embedding -> tolak foto kembar -> simpan sementara di memori.
     *
     * @return hasil berisi pesan siap tampil + berhasil/tidak.
     */
    public synchronized HasilAmbil ambilDariFrame(BufferedImage frameKamera) {
        return ambilDariFrame(frameKamera, 0);
    }

    /**
     * Proses SATU frame kamera untuk tahap berikutnya, dengan pemeriksaan
     * tambahan yang butuh identitas akun.
     *
     * <p>Aturan tambahan (permintaan pengguna §16):
     * <ul>
     *   <li>POSE WAJIB SESUAI instruksi tahap: foto 1 lurus, foto 2 menoleh
     *       KIRI fisik, foto 3 menoleh KANAN fisik. Lurus terus saat diminta
     *       menoleh akan DITOLAK.</li>
     *   <li>WAJAH KEMBAR lintas akun ditolak: bila embedding mirip template
     *       milik karyawan LAIN, registrasi dibatalkan (satu wajah satu akun).</li>
     * </ul>
     *
     * @param idKaryawanLogin id karyawan sesi login; bila &lt;= 0 pemeriksaan
     *                        duplikat lintas akun dilewati (dipakai uji lama).
     */
    public synchronized HasilAmbil ambilDariFrame(BufferedImage frameKamera, int idKaryawanLogin) {
        if (lengkap()) {
            return HasilAmbil.gagal("Registrasi sudah lengkap 3/3. "
                    + "Tekan \"Selesaikan Registrasi\".");
        }
        if (frameKamera == null) {
            return HasilAmbil.gagal("Tidak ada gambar kamera. Nyalakan kamera dulu.");
        }
        try {
            if (!OpenCvBridge.siap()) OpenCvBridge.siapkanModel();
        } catch (Throwable t) {
            return HasilAmbil.gagal("Model wajah belum siap: " + t.getMessage());
        }

        Mat mat = null;
        try {
            mat = kamera.KameraService.gambarKeMat(frameKamera);
            List<OpenCvBridge.WajahTerdeteksi> wajah = OpenCvBridge.deteksiWajah(mat);
            if (wajah.isEmpty()) {
                return HasilAmbil.gagal("Tidak ada wajah terdeteksi. Hadapkan wajah "
                        + "ke kamera dengan pencahayaan cukup, lalu ulangi.");
            }
            if (wajah.size() > 1) {
                return HasilAmbil.gagal("Terdeteksi " + wajah.size()
                        + " wajah. Pastikan hanya Anda yang berada di depan kamera.");
            }
            OpenCvBridge.WajahTerdeteksi w = wajah.get(0);
            org.opencv.core.Rect area = new org.opencv.core.Rect(w.x, w.y, w.lebar, w.tinggi);

            int[] skor = {0};
            if (!LivenessService.kualitasCukup(mat, area, skor)) {
                return HasilAmbil.gagal("Kualitas foto kurang baik (skor " + skor[0]
                        + "/100). Perbaiki pencahayaan atau dekatkan wajah, lalu ulangi.");
            }

            // 1. Hitung embedding dulu, lalu tolak WAJAH KEMBAR lintas akun
            //    sebelum apa pun — satu wajah hanya boleh satu akun.
            float[] emb = OpenCvBridge.hitungEmbedding(mat.submat(area));

            if (idKaryawanLogin > 0) {
                WajahDAO.TemplateWajah kembar;
                try {
                    kembar = cariKembarLintasAkun(emb, idKaryawanLogin);
                } catch (Exception e) {
                    return HasilAmbil.gagal("Gagal memeriksa duplikat wajah: "
                            + e.getMessage() + ". Coba lagi.");
                }
                if (kembar != null) {
                    return HasilAmbil.gagal("Wajah ini sudah terdaftar pada akun "
                            + aman(kembar.namaKaryawan) + " (ID " + kembar.idKaryawan
                            + "). Satu wajah hanya boleh terdaftar pada SATU akun. "
                            + "Registrasi dibatalkan.");
                }
            }

            // 2. POSE harus sesuai instruksi tahap ini (bukan sekadar teks).
            String galatPose = periksaPose(w, sampel.size());
            if (galatPose != null) {
                return HasilAmbil.gagal(galatPose);
            }

            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            javax.imageio.ImageIO.write(frameKamera, "jpg", buf);
            byte[] jpeg = buf.toByteArray();
            String hash = sha256(jpeg);
            for (Sampel s : sampel) {
                if (s.hash.equals(hash)) {
                    return HasilAmbil.gagal("Foto ini identik dengan foto sebelumnya. "
                            + "Ubah pose sedikit (ikuti instruksi) lalu ambil ulang.");
                }
            }

            sampel.add(new Sampel(jpeg, emb, skor[0], hash));
            return HasilAmbil.sukses(sampel.size(),
                    "Foto " + sampel.size() + "/" + JUMLAH_SAMPEL + " tersimpan sementara. "
                            + (lengkap() ? "Semua sampel lengkap!" : INSTRUKSI_POSE[sampel.size()]));
        } catch (Exception e) {
            return HasilAmbil.gagal("Gagal memproses foto: " + e.getMessage());
        } finally {
            if (mat != null) mat.release();
        }
    }

    /**
     * Ambang |offset| untuk registrasi: di bawah ini dianggap menghadap
     * LURUS, di atas ini dianggap menoleh (arah sesuai tanda). Dibuat lebih
     * besar dari ambang liveness karena foto registrasi butuh pose jelas,
     * sementara wajah "cukup lurus" saja (±0,1) masih wajar untuk foto 1.
     */
    private static final double AMBANG_TOLEH_REGISTRASI = 0.12;

    /**
     * Label arah memakai ambang REGISTRASI (bukan ambang netral liveness yang
     * lebih kecil), supaya pesan tidak menyebut "menoleh" untuk wajah yang
     * sebenarnya masih dianggap lurus oleh aturan registrasi.
     */
    private static String labelPoseRegistrasi(double offset) {
        if (offset >= AMBANG_TOLEH_REGISTRASI) return "KIRI";
        if (offset <= -AMBANG_TOLEH_REGISTRASI) return "KANAN";
        return "DEPAN";
    }

    /**
     * Periksa pose wajah terhadap instruksi tahap ke-{@code indeks} (0-based).
     *
     * @return null bila pose sudah sesuai; pesan penolakan bila belum.
     */
    private static String periksaPose(OpenCvBridge.WajahTerdeteksi w, int indeks) {
        LivenessService.PoseWajah pose = LivenessService.poseDari(w);
        if (pose == null || !pose.berlandmark) {
            return "Landmark wajah tidak terbaca, jadi pose tidak dapat dinilai. "
                    + "Dekatkan wajah ke kamera dengan pencahayaan cukup, lalu ulangi.";
        }
        double o = pose.offset;
        String arah = labelPoseRegistrasi(o);
        if (indeks == 0) {
            // Foto 1: wajah lurus ke kamera.
            if (Math.abs(o) >= AMBANG_TOLEH_REGISTRASI) {
                return String.format(
                        "Pose belum sesuai: foto 1 harus menghadap LURUS ke kamera, "
                                + "tapi terdeteksi menoleh ke %s. Hadapkan wajah lurus "
                                + "lalu ambil ulang.", arah);
            }
            return null;
        }
        String diminta = (indeks == 1) ? "KIRI" : "KANAN";
        boolean sesuai = (indeks == 1)
                ? o >= AMBANG_TOLEH_REGISTRASI      // positif = menoleh KIRI fisik
                : o <= -AMBANG_TOLEH_REGISTRASI;    // negatif = menoleh KANAN fisik
        if (!sesuai) {
            return String.format(
                    "Pose belum sesuai: foto %d harus menoleh ke %s Anda, tapi "
                            + "terdeteksi %s. Putar kepala lebih jelas ke %s lalu "
                            + "ambil ulang.", indeks + 1, diminta, arah, diminta);
        }
        return null;
    }

    /**
     * Cari template milik karyawan LAIN yang mirip dengan embedding ini
     * (wajah kembar lintas akun).
     *
     * @return template pemiliknya, atau null bila tidak ada yang cocok.
     */
    private WajahDAO.TemplateWajah cariKembarLintasAkun(float[] emb, int idKaryawanLogin)
            throws Exception {
        for (WajahDAO.TemplateWajah lain : wajahDAO.muatSemuaTemplateKecuali(idKaryawanLogin)) {
            double sim = WajahVerifier.cosine(emb, lain.embedding);
            if (sim >= AMBANG_WAJAH_KEMBAR) {
                return lain;
            }
        }
        return null;
    }

    private static String aman(String s) {
        return (s == null || s.trim().isEmpty()) ? "(tanpa nama)" : s.trim();
    }

    /** Salinan embedding seluruh sampel sesi (urut, untuk perhitungan rata-rata). */
    private synchronized List<float[]> semuaEmbedding() {
        List<float[]> hasil = new ArrayList<>();
        for (Sampel s : sampel) hasil.add(s.embedding);
        return hasil;
    }

    /**
     * Selesaikan registrasi untuk karyawan yang sedang login.
     *
     * <p>Menulis template embedding ke DB (upsert, satu baris per karyawan)
     * dan menyimpan 3 foto referensi ke disk berizin ketat. Bila penyimpanan
     * file gagal, baris DB ikut dibatalkan (tidak ada status "lengkap" palsu).
     *
     * @return null bila sukses, pesan kesalahan bila gagal.
     */
    public synchronized String selesaikan(int idKaryawanLogin, int idUserAuditor, boolean olehHRD) {
        if (!lengkap()) {
            return "Registrasi belum lengkap (" + sampel.size() + "/" + JUMLAH_SAMPEL
                    + "). Ambil foto yang kurang dulu.";
        }
        if (idKaryawanLogin <= 0) {
            return "Sesi login tidak valid. Login ulang lalu coba lagi.";
        }

        // Pengaman TERAKHIR: sebelum menulis, pastikan template rata-rata tidak
        // mirip milik akun lain (bisa lolos bila template akun lain didaftarkan
        // setelah foto-foto sesi ini diambil).
        // Dijalankan SEBELUM transaksi tulis supaya tidak ada efek samping.
        try {
            float[] templateUji = WajahDAO.rataRata(semuaEmbedding());
            WajahDAO.TemplateWajah kembar = cariKembarLintasAkun(templateUji, idKaryawanLogin);
            if (kembar != null) {
                return "Wajah ini sudah terdaftar pada akun " + aman(kembar.namaKaryawan)
                        + " (ID " + kembar.idKaryawan + "). Satu wajah hanya boleh "
                        + "terdaftar pada SATU akun. Registrasi dibatalkan.";
            }
        } catch (Exception e) {
            return "Gagal memeriksa duplikat wajah sebelum menyimpan: " + e.getMessage();
        }

        List<float[]> embeddingSampel = new ArrayList<>();
        for (Sampel s : sampel) embeddingSampel.add(s.embedding);

        List<byte[]> fotoSampel = new ArrayList<>();
        for (Sampel s : sampel) fotoSampel.add(s.jpeg);

        Connection c = null;
        boolean autokomitLama = true;
        Path dirWajah = null;
        List<Path> fileTersimpan = new ArrayList<>();
        try {
            c = Koneksi.getConnection();
            autokomitLama = c.getAutoCommit();
            c.setAutoCommit(false);

            // 1. tulis template (upsert) DI DALAM transaksi
            float[] template = WajahDAO.rataRata(embeddingSampel);
            byte[] blob = WajahDAO.embeddingKeBytes(template);
            String sql = "INSERT INTO wajah_karyawan "
                    + "(id_karyawan, template, algoritma, kualitas, didaftarkan_oleh) "
                    + "VALUES (?, ?, 'SFace-OpenCV', ?, ?) "
                    + "ON DUPLICATE KEY UPDATE template = VALUES(template), "
                    + "kualitas = VALUES(kualitas), didaftarkan_oleh = VALUES(didaftarkan_oleh), "
                    + "didaftarkan_pada = CURRENT_TIMESTAMP";
            int rataKualitas = 0;
            for (Sampel s : sampel) rataKualitas += s.kualitas;
            rataKualitas /= sampel.size();
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                ps.setInt(1, idKaryawanLogin);
                ps.setBytes(2, blob);
                ps.setInt(3, rataKualitas);
                if (idUserAuditor > 0) ps.setInt(4, idUserAuditor);
                else ps.setNull(4, java.sql.Types.INTEGER);
                ps.executeUpdate();
            }

            // 2. simpan 3 foto referensi ke disk (izin 700)
            dirWajah = direktoriWajah(idKaryawanLogin);
            Files.createDirectories(dirWajah);
            try {
                Set<PosixFilePermission> izin = PosixFilePermissions.fromString("rwx------");
                Files.setPosixFilePermissions(dirWajah, izin);
            } catch (UnsupportedOperationException ignored) { }

            for (int i = 0; i < fotoSampel.size(); i++) {
                Path f = dirWajah.resolve("wajah_" + idKaryawanLogin + "_"
                        + (i + 1) + "_" + System.currentTimeMillis() + "_"
                        + UUID.randomUUID().toString().substring(0, 8) + ".jpg");
                Files.write(f, fotoSampel.get(i));
                try {
                    Files.setPosixFilePermissions(f,
                            PosixFilePermissions.fromString("rw-------"));
                } catch (UnsupportedOperationException ignored) { }
                fileTersimpan.add(f);
            }

            c.commit();
            sampel.clear();
            return null; // sukses
        } catch (Exception e) {
            // batalkan transaksi + bersihkan file yang sempat tertulis
            if (c != null) {
                try { c.rollback(); } catch (Exception ignored) { }
            }
            for (Path f : fileTersimpan) {
                try { Files.deleteIfExists(f); } catch (Exception ignored) { }
            }
            return "Registrasi gagal dan TIDAK dihitung lengkap: " + e.getMessage();
        } finally {
            if (c != null) {
                try { c.setAutoCommit(autokomitLama); c.close(); } catch (Exception ignored) { }
            }
        }
    }

    /** Direktori foto referensi milik SATU karyawan (izin ketat). */
    public static Path direktoriWajah(int idKaryawan) {
        return java.nio.file.Paths.get(System.getProperty("user.home"),
                ".penggajian", "wajah", String.valueOf(idKaryawan));
    }

    private static String sha256(byte[] data) {
        try {
            java.security.MessageDigest md =
                    java.security.MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(data);
            StringBuilder sb = new StringBuilder();
            for (byte b : d) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            // fallback: hash ringan dari panjang + beberapa byte
            return "len" + data.length + "_" + data[data.length / 2];
        }
    }

    /** Hasil satu kali pengambilan foto. */
    public static final class HasilAmbil {
        public final boolean sukses;
        public final int jumlahValid;
        public final String pesan;

        private HasilAmbil(boolean sukses, int jumlah, String pesan) {
            this.sukses = sukses;
            this.jumlahValid = jumlah;
            this.pesan = pesan;
        }

        static HasilAmbil sukses(int jumlah, String pesan) {
            return new HasilAmbil(true, jumlah, pesan);
        }

        static HasilAmbil gagal(String pesan) {
            return new HasilAmbil(false, -1, pesan);
        }
    }
}
