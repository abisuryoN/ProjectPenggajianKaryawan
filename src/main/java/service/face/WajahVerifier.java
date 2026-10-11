package service.face;

import config.Koneksi;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Verifikasi identitas wajah: bandingkan embedding frame kamera dengan
 * template milik karyawan yang login (cosine similarity).
 *
 * <p>Aturan keras (Bagian C §10, §12):
 * <ul>
 *   <li>Deteksi saja TIDAK PERNAH berarti cocok.</li>
 *   <li>Kecocokan = similarity &gt;= ambang terhadap template milik akun login.</li>
 *   <li>Hasil selalu menyebut NAMA HASIL COCOKAN bila ada, bukan nama akun.</li>
 * </ul>
 */
public class WajahVerifier {

    /** Ambang cosine similarity SFace (0..1). Di atas/equal = cocok. */
    public static final double AMBANG_COCOK = 0.55;

    /** Tingkat keyakinan minimum agar nama cocok berani ditampilkan. */
    public static final double AMBANG_NAMA_PASTI = 0.70;

    /** Hasil satu percobaan verifikasi — immutable. */
    public static final class HasilVerifikasi {
        /** COCOK | TIDAK_COCOK | TAK_DIKENAL | TANPA_WAJAH | MULTI_WAJAH | TANPA_TEMPLATE */
        public final String status;
        public final double similarity;      // -1 bila tidak ada pembanding
        public final String namaTerdeteksi;  // nama pemilik template paling mirip, "" bila tak ada
        public final String pesan;           // pesan Indonesia siap tampil

        private HasilVerifikasi(String status, double similarity, String nama, String pesan) {
            this.status = status;
            this.similarity = similarity;
            this.namaTerdeteksi = nama == null ? "" : nama;
            this.pesan = pesan;
        }

        public boolean cocok() { return "COCOK".equals(status); }

        /** Factory untuk UI: hasil non-verifikasi (tanpa wajah, multi wajah, dll). */
        public static HasilVerifikasi lainnya(String status, String pesan) {
            return new HasilVerifikasi(status, -1, "", pesan);
        }

        @Override
        public String toString() {
            return "HasilVerifikasi{" + status + ", sim=" + similarity
                    + ", nama='" + namaTerdeteksi + "'}";
        }
    }

    private final WajahDAO wajahDAO = new WajahDAO();

    /**
     * Verifikasi embedding wajah dari SATU wajah terdeteksi terhadap identitas
     * akun yang login.
     *
     * @param embeddingWajah embedding 128-D ternormalisasi dari frame saat ini.
     * @param idKaryawanLogin id karyawan dari sesi login (sudah divalidasi).
     * @param namaLogin nama akun yang login (untuk pesan).
     */
    public HasilVerifikasi verifikasi(float[] embeddingWajah, int idKaryawanLogin, String namaLogin) {
        if (embeddingWajah == null) {
            return new HasilVerifikasi("TANPA_WAJAH", -1, "",
                    "Tidak ada wajah terdeteksi. Hadap kamera dengan pencahayaan cukup.");
        }
        WajahDAO.TemplateWajah template;
        try {
            template = wajahDAO.muatTemplate(idKaryawanLogin);
        } catch (Exception e) {
            return new HasilVerifikasi("TANPA_TEMPLATE", -1, "",
                    "Gagal membaca data wajah terdaftar: " + e.getMessage());
        }
        if (template == null) {
            return new HasilVerifikasi("TANPA_TEMPLATE", -1, "",
                    "Wajah akun " + aman(namaLogin) + " belum didaftarkan. "
                    + "Minta HRD mendaftarkan wajah Anda dulu.");
        }
        double sim = cosine(embeddingWajah, template.embedding);
        if (sim >= AMBANG_COCOK) {
            return new HasilVerifikasi("COCOK", sim, template.namaKaryawan,
                    "Identitas wajah cocok dengan akun " + aman(namaLogin) + ".");
        }
        // Tidak cocok dengan pemilik akun — cari TAHu mirip siapa TANPA
        // menampilkan skor mentah: bandingkan ke semua template lain.
        String miripSiapa = cariPemilikPalingMirip(embeddingWajah, idKaryawanLogin);
        if (!miripSiapa.isEmpty() && sim >= AMBANG_COCOK - 0.15) {
            return new HasilVerifikasi("TIDAK_COCOK", sim, miripSiapa,
                    "Wajah terdeteksi sebagai " + miripSiapa + ". Anda sedang login sebagai "
                    + aman(namaLogin) + ". Absensi ditolak.");
        }
        if (!miripSiapa.isEmpty()) {
            return new HasilVerifikasi("TIDAK_COCOK", sim, miripSiapa,
                    "Verifikasi gagal. Wajah yang terdeteksi tidak cocok dengan identitas "
                    + aman(namaLogin) + ". Absensi ditolak.");
        }
        return new HasilVerifikasi("TAK_DIKENAL", sim, "",
                "Wajah tidak dikenali. Pastikan wajah terdaftar dan terlihat jelas di kamera.");
    }

    /**
     * Cari pemilik template paling mirip SELAIN akun login. Mengembalikan ""
     * bila tidak ada yang melewati ambang nama-pasti. Dipakai agar pesan
     * "terdeteksi sebagai X" benar-benar dari hasil pencocokan, bukan tebakan.
     */
    private String cariPemilikPalingMirip(float[] embedding, int kecualiIdKaryawan) {
        String sql = "SELECT w.id_karyawan, k.nama_karyawan, w.template "
                + "FROM wajah_karyawan w JOIN karyawan k ON k.id_karyawan = w.id_karyawan "
                + "WHERE w.id_karyawan <> ?";
        String namaTerbaik = "";
        double simTerbaik = AMBANG_NAMA_PASTI;
        try (Connection c = Koneksi.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, kecualiIdKaryawan);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    float[] lain = WajahDAO.bytesKeEmbedding(rs.getBytes("template"));
                    if (lain == null) continue;
                    double sim = cosine(embedding, lain);
                    if (sim > simTerbaik) {
                        simTerbaik = sim;
                        namaTerbaik = rs.getString("nama_karyawan");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Gagal mencari pemilik mirip: " + e.getMessage());
        }
        return namaTerbaik == null ? "" : namaTerbaik;
    }

    /** Cosine similarity dua vektor ternormalisasi (fallback aman bila belum). */
    public static double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) return -1;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            na += (double) a[i] * a[i];
            nb += (double) b[i] * b[i];
        }
        if (na <= 0 || nb <= 0) return -1;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    private static String aman(String s) {
        return (s == null || s.trim().isEmpty()) ? "(tanpa nama)" : s.trim();
    }
}
