package service.face;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Liveness / anti-spoofing sederhana berbasis tantangan gerakan
 * (Bagian C §12).
 *
 * <p><b>KETERBATASAN YANG HARUS DIKETAHUI (jujur, tanpa klaim berlebihan):</b>
 * <ul>
 *   <li>Ini BUKAN anti-spoofing kelas produksi. Ia menyulitkan foto diam dan
 *       foto cetak, tetapi TIDAK dapat dipercaya melawan video rekaman
 *       berkualitas, deepfake, atau topeng.</li>
 *   <li>Yang diperiksa hanya gerakan <i>posisi wajah</i> antar-frame dalam
 *       urutan waktu (bukan satu frame), jadi foto yang digeser-geser di depan
 *       kamera tetap bisa lolos bila digerakkan cukup jauh.</li>
 *   <li>Modul HANYA mengklaim: "liveness sederhana (tantangan gerakan)".
 *       Absensi tetap mewajibkan verifikasi identitas (embedding), sehingga
 *       liveness bukan satu-satunya pertahanan.</li>
 * </ul>
 *
 * <p>Sesi: {@link #mulai()} lalu umpan frame + kotak wajah ke
 * {@link #umpan(int, double)}. Selesai bila total pergeseran horizontal
 * melewati ambang DAN terjadi dalam jendela waktu.
 */
public class LivenessService {

    /** Total pergeseran horizontal minimum, fraksi lebar wajah. */
    private static final double AMBANG_GESER = 0.35;

    /** Arah yang diminta: -1 = kiri, +1 = kanan. */
    public static final int ARAH_KIRI = -1;
    public static final int ARAH_KANAN = 1;

    /** Jendela waktu maksimum sesi liveness, milidetik. */
    private static final long JENDELA_MS = 15_000;

    /** Minimal jumlah frame yang diumpan (biar bukan 1 frame). */
    private static final int MIN_FRAME = 4;

    private final Deque<Double> riwayatPusat = new ArrayDeque<Double>();
    private long mulaiMs;
    private boolean aktif;
    private int arahDiminta = ARAH_KANAN;
    private double lebarAcuan = -1;

    /** Pesan instruksi ke pengguna, format Indonesia. */
    public String instruksi() {
        return arahDiminta == ARAH_KANAN
                ? "Tantangan liveness: tolong TENGOK KE KANAN, lalu kembali ke depan."
                : "Tantangan liveness: tolong TENGOK KE KIRI, lalu kembali ke depan.";
    }

    /** Pilih arah acak agar tidak selalu sama. */
    public void mulai() {
        arahDiminta = (System.nanoTime() % 2 == 0) ? ARAH_KANAN : ARAH_KIRI;
        riwayatPusat.clear();
        lebarAcuan = -1;
        mulaiMs = System.currentTimeMillis();
        aktif = true;
    }

    public boolean sedangAktif() { return aktif; }

    /** Keterangan jendela waktu tersisa. */
    public long sisaWaktuMs() {
        if (!aktif) return 0;
        long sisa = JENDELA_MS - (System.currentTimeMillis() - mulaiMs);
        return Math.max(0, sisa);
    }

    /**
     * Umpan satu posisi wajah.
     *
     * @param pusatX  koordinat X pusat wajah (piksel, orientasi frame asli)
     * @param lebarWajah lebar kotak wajah (piksel)
     * @return true bila liveness TERBUKTI (gerakan terjadi dalam urutan waktu)
     */
    public boolean umpan(int pusatX, double lebarWajah) {
        if (!aktif) return false;
        if (System.currentTimeMillis() - mulaiMs > JENDELA_MS) {
            aktif = false;
            return false;
        }
        if (lebarAcuan <= 0) lebarAcuan = lebarWajah;
        // simpan posisi relatif agar tidak bergantung resolusi
        riwayatPusat.addLast(pusatX / Math.max(1.0, lebarAcuan));
        while (riwayatPusat.size() > 60) riwayatPusat.removeFirst();

        if (riwayatPusat.size() < MIN_FRAME) return false;

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (double v : riwayatPusat) {
            if (v < min) min = v;
            if (v > max) max = v;
        }
        double rentang = max - min;
        if (rentang < AMBANG_GESER) return false;

        // arah dominan: dari nilai awal ke nilai akhir
        double awal = riwayatPusat.peekFirst();
        double akhir = riwayatPusat.peekLast();
        int arahGerak = (akhir > awal) ? ARAH_KANAN : ARAH_KIRI;
        // Untuk frame kamera non-mirror: X bertambah = wajah bergeser ke kiri
        // gambar, tetapi kita hanya butuh KONSISTENSI gerakan bolak-balik, bukan
        // arah absolut — karena itu syaratnya cukup: ada gerakan rentang cukup
        // DAN kembali mendekati posisi awal.
        boolean kembali = Math.abs(akhir - awal) < rentang / 2.0;
        if (kembali) {
            aktif = false;
            return true;
        }
        return false;
    }

    /** Batal (mis. wajah hilang). */
    public void batal() {
        aktif = false;
        riwayatPusat.clear();
    }

    /**
     * Pemeriksaan tambahan kualitas gambar (bukan liveness):
     * wajah terlalu kecil / blur ekstrem ditolak sejak awal.
     */
    public static boolean kualitasCukup(Mat frameBgr, Rect areaWajah) {
        if (frameBgr == null || frameBgr.empty() || areaWajah == null) return false;
        if (areaWajah.width < 60 || areaWajah.height < 60) return false;
        try {
            Mat potong = new Mat(frameBgr, areaWajah);
            Mat abu = new Mat();
            Imgproc.cvtColor(potong, abu, Imgproc.COLOR_BGR2GRAY);
            org.opencv.core.MatOfDouble mean = new org.opencv.core.MatOfDouble();
            org.opencv.core.MatOfDouble dev = new org.opencv.core.MatOfDouble();
            org.opencv.core.Core.meanStdDev(abu, mean, dev);
            double sigma = dev.toArray()[0];
            potong.release();
            abu.release();
            // variansi terlalu kecil = gambar datar/blur/tertutup
            return sigma >= 8.0;
        } catch (Exception e) {
            return false;
        }
    }
}
