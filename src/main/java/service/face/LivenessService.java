package service.face;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Liveness sederhana berbasis tantangan TENGOKAN KEPALA (kiri/kanan fisik).
 *
 * <h2>Kerangka arah — dasar perbaikan bug kiri/kanan</h2>
 *
 * <p>Frame yang DIANALISIS di sini adalah frame mentah dari driver — TIDAK
 * di-mirror (lihat {@code kamera.KameraService}: {@code cermin()} hanya dipakai
 * untuk tampilan preview, bukan untuk frame analisis). Jadi citra yang
 * dianalisis sama persis dengan apa yang dilihat orang yang berdiri menghadap
 * pengguna. Akibatnya berlaku fakta berikut:
 *
 * <pre>
 *   Sisi KIRI pengguna  ->  tampak di sisi KANAN citra (X membesar)
 *   Sisi KANAN pengguna ->  tampak di sisi KIRI  citra (X mengecil)
 * </pre>
 *
 * <p>Contoh sehari-hari: pada citra kamera mentah (tanpa mirror), tangan KANAN
 * Anda tampil di sisi KIRI citra. (Preview di layar justru di-mirror agar
 * terasa seperti bercermin — itu tampilan saja dan tidak memengaruhi analisis.)
 *
 * <p>Ketika pengguna menoleh ke kiri fisiknya, ujung hidungnya bergeser ke sisi
 * kiri tubuhnya, yaitu ke arah X citra yang MEMBESAR. Karena itu arah tengokan
 * ditentukan dari <b>geometri lipatan wajah</b> (posisi hidung relatif terhadap
 * titik tengah kedua mata, dinormalkan dengan jarak antarmata) — BUKAN dari
 * posisi wajah di layar. Ini memenuhi aturan: jangan menukar label hanya karena
 * objek berada di sisi kiri/kanan layar.
 *
 * <p>Ukuran yang dipakai adalah <i>rasio</i>, sehingga tidak berubah bila
 * pengguna bergerak mendekat/menjauh dari kamera atau berpindah posisi.
 *
 * <h2>Keterbatasan (jujur, tanpa klaim berlebihan)</h2>
 * <ul>
 *   <li>Bukan anti-spoofing kelas produksi. Ia menyulitkan foto diam dan foto
 *       cetak, tetapi TIDAK dapat dipercaya melawan video rekaman berkualitas,
 *       deepfake, atau topeng.</li>
 *   <li>Liveness hanya pertahanan tambahan: absensi tetap mewajibkan verifikasi
 *       identitas (embedding) terhadap template milik akun yang login.</li>
 *   <li>Arah hanya dapat dinilai bila landmark wajah tersedia (YuNet
 *       menyediakannya). Tanpa landmark, frame dianggap tidak dapat dinilai
 *       dan TIDAK dihitung — bukan ditebak.</li>
 * </ul>
 */
public class LivenessService {

    /** Arah fisik pengguna: -1 = kiri pengguna, +1 = kanan pengguna. */
    public static final int ARAH_KIRI = -1;
    public static final int ARAH_KANAN = 1;

    /**
     * Ambang |offset| agar dianggap benar-benar menoleh.
     * offset = (hidung.x - tengahMata.x) / jarakAntarMata.
     */
    private static final double AMBANG_TOLEH = 0.08;

    /** Ambang kembali netral (harus kembali mendekati depan). */
    private static final double AMBANG_NETRAL = 0.04;

    /** Jendela waktu maksimum sesi liveness, milidetik. */
    private static final long JENDELA_MS = 20_000;

    /** Minimal jumlah frame berlandmark sebelum penilaian dianggap sah. */
    private static final int MIN_FRAME = 3;

    /**
     * Pose kepala terkuantifikasi dari landmark pada citra NON-MIRROR.
     * Semua koordinat dalam ruang citra asli (tanpa transformasi).
     */
    public static final class PoseWajah {
        /**
         * Rasio tengokan: (hidung.x - tengahMata.x) / jarakAntarMata.
         * <b>Positif = pengguna menoleh ke KIRI fisik</b>, negatif = ke kanan.
         */
        public final double offset;
        /** Lebar kotak wajah (piksel), untuk penyaringan ukuran. */
        public final double lebarWajah;
        /** Landmark tersedia? Bila false, pose ini tidak dapat dinilai. */
        public final boolean berlandmark;

        public PoseWajah(double offset, double lebarWajah, boolean berlandmark) {
            this.offset = offset;
            this.lebarWajah = lebarWajah;
            this.berlandmark = berlandmark;
        }

        /** Pose tanpa landmark — tidak dapat dipakai menilai arah. */
        public static PoseWajah tanpaLandmark(double lebarWajah) {
            return new PoseWajah(0, lebarWajah, false);
        }
    }

    private final Deque<Double> riwayatOffset = new ArrayDeque<Double>();
    private long mulaiMs;
    private boolean aktif;
    private int arahDiminta = ARAH_KANAN;
    private boolean sudahMenoleh;
    private double offsetPuncak;

    /** Instruksi ke pengguna, menyebut arah tubuh pengguna sendiri. */
    public String instruksi() {
        return arahDiminta == ARAH_KANAN
                ? "Tantangan liveness: tolong TENGOK KE KANAN Anda (kanan tubuh Anda), "
                  + "lalu kembali menghadap depan."
                : "Tantangan liveness: tolong TENGOK KE KIRI Anda (kiri tubuh Anda), "
                  + "lalu kembali menghadap depan.";
    }

    /** Arah fisik yang sedang diminta (ARAH_KIRI / ARAH_KANAN). */
    public int arahDiminta() { return arahDiminta; }

    /** Label arah fisik untuk UI: "KIRI" / "KANAN". */
    public String labelArahDiminta() {
        return arahDiminta == ARAH_KANAN ? "KANAN" : "KIRI";
    }

    /**
     * Label arah dari sebuah nilai offset, memakai pemetaan non-mirror:
     * offset positif = KIRI fisik, negatif = KANAN fisik.
     */
    public static String labelDariOffset(double offset) {
        if (offset > AMBANG_NETRAL) return "KIRI";
        if (offset < -AMBANG_NETRAL) return "KANAN";
        return "DEPAN";
    }

    /** Mulai sesi liveness dengan arah acak. */
    public void mulai() {
        arahDiminta = (System.nanoTime() % 2 == 0) ? ARAH_KANAN : ARAH_KIRI;
        riwayatOffset.clear();
        sudahMenoleh = false;
        offsetPuncak = 0;
        mulaiMs = System.currentTimeMillis();
        aktif = true;
    }

    public boolean sedangAktif() { return aktif; }

    /** True bila pengguna sudah menoleh ke arah yang diminta (belum kembali). */
    public boolean sudahMenoleh() { return sudahMenoleh; }

    /** Sisa waktu jendela liveness (ms). */
    public long sisaWaktuMs() {
        if (!aktif) return 0;
        return Math.max(0, JENDELA_MS - (System.currentTimeMillis() - mulaiMs));
    }

    /**
     * Susun pose dari satu wajah terdeteksi (memakai landmark YuNet).
     *
     * <p>Urutan landmark YuNet (x,y berpasangan): mata kanan-citra, mata
     * kiri-citra, hidung, mulut kanan, mulut kiri. Arah tidak bergantung pada
     * urutan penamaan ini karena hanya memakai titik tengah kedua mata dan
     * titik hidung.
     */
    public static PoseWajah poseDari(OpenCvBridge.WajahTerdeteksi w) {
        if (w == null) return null;
        double[] lm = w.landmark;
        if (lm == null || lm.length < 10) {
            return PoseWajah.tanpaLandmark(w.lebar);
        }
        // landmark: [0,1]=mata A, [2,3]=mata B, [4,5]=hidung
        double mataAX = lm[0], mataBX = lm[2], hidungX = lm[4];
        double tengahMataX = (mataAX + mataBX) / 2.0;
        double jarakMata = Math.abs(mataBX - mataAX);
        if (jarakMata < 1.0) {
            return PoseWajah.tanpaLandmark(w.lebar);
        }
        double offset = (hidungX - tengahMataX) / jarakMata;
        return new PoseWajah(offset, w.lebar, true);
    }

    /**
     * Umpan satu pose.
     *
     * @return true bila liveness TERBUKTI: pengguna menoleh ke arah yang
     *         diminta (tanda offset sesuai) LALU kembali mendekati depan,
     *         seluruhnya dalam urutan waktu dan jendela waktu.
     */
    public boolean umpan(PoseWajah pose) {
        if (!aktif) return false;
        if (System.currentTimeMillis() - mulaiMs > JENDELA_MS) {
            aktif = false;
            return false;
        }
        if (pose == null || !pose.berlandmark) {
            // Tidak dapat dinilai — jangan dihitung, jangan ditebak.
            return false;
        }
        riwayatOffset.addLast(pose.offset);
        while (riwayatOffset.size() > 90) riwayatOffset.removeFirst();
        if (riwayatOffset.size() < MIN_FRAME) return false;

        // Pemetaan arah fisik (citra non-mirror):
        //   offset > 0  -> hidung bergeser ke X membesar -> pengguna menoleh KIRI
        //   offset < 0  -> pengguna menoleh KANAN
        double offsetSekarang = pose.offset;
        int arahTerbaca = offsetSekarang > 0 ? ARAH_KIRI : ARAH_KANAN;

        if (Math.abs(offsetSekarang) >= AMBANG_TOLEH) {
            if (arahTerbaca == arahDiminta) {
                sudahMenoleh = true;
                if (Math.abs(offsetSekarang) > Math.abs(offsetPuncak)) {
                    offsetPuncak = offsetSekarang;
                }
            } else if (!sudahMenoleh) {
                // Menoleh ke arah yang SALAH: tidak dihitung, sesi tetap jalan
                // supaya pengguna bisa memperbaiki (bukan gagal permanen).
                return false;
            }
        }

        // Selesai bila sudah menoleh ke arah benar dan kembali mendekati depan.
        if (sudahMenoleh && Math.abs(offsetSekarang) <= AMBANG_NETRAL) {
            aktif = false;
            return true;
        }
        return false;
    }

    /** Batal (wajah hilang / kamera dimatikan). */
    public void batal() {
        aktif = false;
        sudahMenoleh = false;
        riwayatOffset.clear();
    }

    /**
     * Pemeriksaan kualitas gambar (bukan liveness): wajah terlalu kecil,
     * gambar datar/blur ekstrem, atau pencahayaan buruk ditolak sejak awal.
     */
    public static boolean kualitasCukup(Mat frameBgr, Rect areaWajah) {
        return kualitasCukup(frameBgr, areaWajah, null);
    }

    /**
     * Versi yang juga mengisi skor kualitas 0..100 ke {@code kualitasKeluar[0]}
     * bila array diberikan — dipakai UI untuk menampilkan skor.
     */
    public static boolean kualitasCukup(Mat frameBgr, Rect areaWajah, int[] kualitasKeluar) {
        if (frameBgr == null || frameBgr.empty() || areaWajah == null) return false;
        if (areaWajah.width < 60 || areaWajah.height < 60) {
            if (kualitasKeluar != null && kualitasKeluar.length > 0) kualitasKeluar[0] = 0;
            return false;
        }
        Mat potong = null, abu = null;
        try {
            potong = new Mat(frameBgr, areaWajah);
            abu = new Mat();
            Imgproc.cvtColor(potong, abu, Imgproc.COLOR_BGR2GRAY);
            org.opencv.core.MatOfDouble mean = new org.opencv.core.MatOfDouble();
            org.opencv.core.MatOfDouble dev = new org.opencv.core.MatOfDouble();
            org.opencv.core.Core.meanStdDev(abu, mean, dev);
            double sigma = dev.toArray()[0];
            double terang = mean.toArray()[0];
            if (kualitasKeluar != null && kualitasKeluar.length > 0) {
                double skorKetajaman = Math.min(1.0, sigma / 25.0);
                double skorCahaya = 1.0 - Math.min(1.0, Math.abs(terang - 128) / 128.0);
                kualitasKeluar[0] = (int) Math.round((skorKetajaman * 0.6 + skorCahaya * 0.4) * 100);
            }
            return sigma >= 8.0 && terang > 35 && terang < 235;
        } catch (Exception e) {
            return false;
        } finally {
            if (potong != null) potong.release();
            if (abu != null) abu.release();
        }
    }
}
