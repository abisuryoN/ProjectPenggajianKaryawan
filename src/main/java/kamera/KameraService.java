package kamera;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;

/**
 * Pembungkus kamera V4L2 (Linux) di background thread (Bagian C §8).
 *
 * <p>Kontrak orientasi: frame mentah dari driver TIDAK pernah diubah —
 * {@code frameTerakhir()} dan semua pendengar menerima citra orientasi asli,
 * dipakai untuk analisis wajah, liveness, dan foto bukti. Untuk TAMPILAN
 * preview, panel memanggil {@link #cermin} (flip horizontal) sesuai
 * {@link #mirrorPreview}; frame analisis/disimpan tidak pernah di-flip.
 */
public class KameraService {

    /**
     * Preview tampil seperti cermin (selfie view). Dengan mirror, gerakan
     * Anda terasa "searah": angkat tangan kanan, di layar tangan itu tampil di
     * sisi KANAN — sama seperti bercermin.
     *
     * <p>PENTING: ini HANYA untuk tampilan. Frame yang dipakai verifikasi
     * wajah dan disimpan sebagai bukti TIDAK di-flip; lihat {@link #cermin}.
     */
    public static final boolean mirrorPreview = true;

    /**
     * Siapkan gambar untuk DITAMPILKAN sesuai {@link #mirrorPreview}.
     *
     * <p>Jangan pakai method ini untuk frame yang dianalisis/disimpan — frame
     * itu harus tetap orientasi asli agar hasil verifikasi konsisten dengan
     * data yang tersimpan.
     */
    public static BufferedImage cermin(BufferedImage gambar) {
        if (!mirrorPreview || gambar == null) return gambar;
        int w = gambar.getWidth(), h = gambar.getHeight();
        BufferedImage hasil = new BufferedImage(w, h, gambar.getType() == 0
                ? BufferedImage.TYPE_INT_RGB : gambar.getType());
        java.awt.Graphics2D g = hasil.createGraphics();
        // flip horizontal: gambar sumber digambar terbalik kiri-kanan
        g.drawImage(gambar, w, 0, -w, h, null);
        g.dispose();
        return hasil;
    }

    /** Pendengar frame baru (dipanggil dari thread kamera — jangan blokir). */
    public interface PendengarFrame {
        void frameBaru(BufferedImage gambar);
        default void kameraGagal(String pesan) {}
    }

    private VideoCapture tangkapan;
    private Thread utas;
    private final AtomicBoolean jalan = new AtomicBoolean(false);
    private final List<PendengarFrame> pendengar = new CopyOnWriteArrayList<>();
    private volatile String statusTerakhir = "Kamera belum dibuka.";
    private volatile BufferedImage frameTerakhir;

    public synchronized void tambahPendengar(PendengarFrame p) {
        pendengar.add(p);
    }

    public synchronized void hapusPendengar(PendengarFrame p) {
        pendengar.remove(p);
    }

    public String status() { return statusTerakhir; }
    public boolean berjalan() { return jalan.get(); }
    public BufferedImage frameTerakhir() { return frameTerakhir; }

    /**
     * Buka kamera (index 0 dulu, fallback 1) dan mulai streaming ~20fps.
     * Mengembalikan true bila berhasil; bila gagal, status berisi pesan
     * jelas + cara pemulihan.
     */
    public synchronized boolean mulai() {
        if (jalan.get()) return true;
        // Muat native OpenCV DULU — tanpa ini VideoCapture melempar
        // UnsatisfiedLinkError (Error, bukan Exception) dan aplikasi crash.
        try {
            service.face.OpenCvBridge.pastikanNative();
        } catch (Throwable t) {
            statusTerakhir = "Library kamera (OpenCV) gagal dimuat: " + pesan(t);
            beritahuGagal(statusTerakhir);
            return false;
        }
        for (int index : new int[]{0, 1}) {
            VideoCapture vc = new VideoCapture();
            try {
                if (vc.open(index, Videoio.CAP_V4L2)) {
                    // resolusi wajar, biarkan driver menyesuaikan bila ditolak
                    vc.set(Videoio.CAP_PROP_FRAME_WIDTH, 640);
                    vc.set(Videoio.CAP_PROP_FRAME_HEIGHT, 480);
                    // Minta format BERWARNA: YUYV dulu, MJPG cadangan. Tanpa
                    // ini OpenCV bisa menerima format apapun yang ditawarkan
                    // driver — termasuk node monokrom (IR) — sehingga preview
                    // tampil abu-abu.
                    boolean formatWarna = vc.set(Videoio.CAP_PROP_FOURCC,
                            org.opencv.videoio.VideoWriter.fourcc('Y', 'U', 'Y', 'V'));
                    if (!formatWarna) {
                        vc.set(Videoio.CAP_PROP_FOURCC,
                                org.opencv.videoio.VideoWriter.fourcc('M', 'J', 'P', 'G'));
                    }
                    Mat uji = new Mat();
                    // Kamera lambat start: beri beberapa kesempatan membaca
                    // frame pertama sebelum menyimpulkan node ini kosong.
                    boolean adaFrame = false;
                    for (int coba = 0; coba < 10 && !adaFrame; coba++) {
                        adaFrame = vc.read(uji);
                        if (!adaFrame) {
                            try { Thread.sleep(150); } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                break;
                            }
                        }
                    }
                    int kanal = adaFrame ? uji.channels() : 0;
                    uji.release();
                    if (!adaFrame || kanal < 3) {
                        // Node IR/metadata: frame abu-abu atau kosong -> jangan dipakai.
                        vc.release();
                        statusTerakhir = "/dev/video" + index
                                + " bukan kamera berwarna (kanal=" + kanal
                                + "); mencoba node lain...";
                        continue;
                    }
                    tangkapan = vc;
                    jalan.set(true);
                    statusTerakhir = "Kamera aktif (/dev/video" + index
                            + ", " + (formatWarna ? "YUYV" : "MJPG") + ").";
                    utas = new Thread(this::lingkaranBaca, "kamera-reader");
                    utas.setDaemon(true);
                    utas.start();
                    return true;
                }
                vc.release();
            } catch (Throwable t) {
                try { vc.release(); } catch (Throwable ignored) {}
                statusTerakhir = "Kamera gagal dibuka: " + pesan(t);
            }
        }
        if (statusTerakhir == null || statusTerakhir.startsWith("Kamera belum")) {
            statusTerakhir = "Kamera tidak ditemukan. Pastikan webcam terpasang dan "
                    + "tidak dipakai aplikasi lain (Zoom/Meet/OBS), lalu tekan \"Mulai Kamera\".";
        }
        beritahuGagal(statusTerakhir);
        return false;
    }

    /** Hentikan streaming dan lepaskan perangkat (§8: tidak boleh bocor). */
    public synchronized void berhenti() {
        jalan.set(false);
        Thread t = utas;
        utas = null;
        if (t != null) {
            try { t.join(1500); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
        }
        VideoCapture vc = tangkapan;
        tangkapan = null;
        if (vc != null) {
            try { vc.release(); } catch (Throwable ignored) {}
        }
        statusTerakhir = "Kamera dimatikan.";
    }

    private void lingkaranBaca() {
        Mat mat = new Mat();
        try {
            while (jalan.get()) {
                boolean ok;
                try {
                    ok = tangkapan != null && tangkapan.isOpened() && tangkapan.read(mat);
                } catch (Throwable t) {
                    gagal("Kamera terputus: " + pesan(t)
                            + " Tekan \"Mulai Kamera\" untuk mencoba lagi.");
                    break;
                }
                if (!ok || mat.empty()) {
                    gagal("Tidak ada gambar dari kamera. Pastikan kamera tidak dipakai "
                            + "aplikasi lain, lalu tekan \"Mulai Kamera\".");
                    break;
                }
                // TANPA flip — orientasi persis dari driver (§9)
                BufferedImage img = matKeGambar(mat);
                frameTerakhir = img;
                for (PendengarFrame p : pendengar) {
                    try { p.frameBaru(img); } catch (Throwable ignored) {}
                }
                try { Thread.sleep(50); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        } finally {
            mat.release();
            jalan.set(false);
        }
    }

    private void gagal(String pesan) {
        statusTerakhir = pesan;
        beritahuGagal(pesan);
    }

    private void beritahuGagal(String pesan) {
        for (PendengarFrame p : pendengar) {
            try { p.kameraGagal(pesan); } catch (Throwable ignored) {}
        }
    }

    private static String pesan(Throwable t) {
        String m = t.getMessage();
        return (m == null || m.isEmpty()) ? t.getClass().getSimpleName() : m;
    }

    /** Mat BGR (OpenCV) -> BufferedImage BGR untuk ditampilkan (tanpa flip). */
    public static BufferedImage matKeGambar(Mat mat) {
        int lebar = mat.cols(), tinggi = mat.rows();
        int kanal = mat.channels();
        byte[] data = new byte[lebar * tinggi * kanal];
        mat.get(0, 0, data);
        BufferedImage img;
        if (kanal == 3) {
            img = new BufferedImage(lebar, tinggi, BufferedImage.TYPE_3BYTE_BGR);
            // Salin byte BGR LANGSUNG ke buffer citra (urutan memori sama:
            // kanal 0 = Blue). Jangan pakai setDataElements(byte[]): pada JDK
            // ini method itu menafsirkan array sebagai R,G,B sehingga kanal
            // merah/biru tertukar dan warna tampil pucat/salah.
            byte[] tujuan = ((java.awt.image.DataBufferByte)
                    img.getRaster().getDataBuffer()).getData();
            System.arraycopy(data, 0, tujuan, 0, data.length);
        } else if (kanal == 1) {
            img = new BufferedImage(lebar, tinggi, BufferedImage.TYPE_BYTE_GRAY);
            img.getRaster().setDataElements(0, 0, lebar, tinggi, data);
        } else {
            img = new BufferedImage(lebar, tinggi, BufferedImage.TYPE_3BYTE_BGR);
        }
        return img;
    }

    /**
     * BufferedImage (dari {@link #frameTerakhir}) -> Mat BGR OpenCV.
     * Kebalikan dari {@link #matKeGambar}; TIDAK membalik/mirror (§9).
     */
    public static Mat gambarKeMat(java.awt.image.BufferedImage gambar) {
        int lebar = gambar.getWidth(), tinggi = gambar.getHeight();
        java.awt.image.BufferedImage bgr = new java.awt.image.BufferedImage(
                lebar, tinggi, java.awt.image.BufferedImage.TYPE_3BYTE_BGR);
        java.awt.Graphics2D g = bgr.createGraphics();
        g.drawImage(gambar, 0, 0, null);
        g.dispose();
        byte[] data = ((java.awt.image.DataBufferByte) bgr.getRaster().getDataBuffer()).getData();
        Mat mat = new Mat(tinggi, lebar, org.opencv.core.CvType.CV_8UC3);
        mat.put(0, 0, data);
        return mat;
    }

    /** Dimensi yang disarankan untuk preview. */
    public static Dimension dimensiPreview() {
        return new Dimension(640, 480);
    }
}
