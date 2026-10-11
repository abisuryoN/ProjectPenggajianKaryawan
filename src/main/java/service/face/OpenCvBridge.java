package service.face;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.Size;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.FaceDetectorYN;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Jembatan OpenCV: muat native library, deteksi wajah (YuNet), dan hitung
 * embedding wajah (SFace 128-D via DNN).
 *
 * <p>Model ONNX diunduh sekali ke {@code ~/.penggajian/models/} (lihat
 * {@link #siapkanModel()}). URL resmi dari OpenCV Zoo:
 * <ul>
 *   <li>YuNet: https://github.com/opencv/opencv_zoo (face_detection_yunet_2023mar.onnx)</li>
 *   <li>SFace: https://github.com/opencv/opencv_zoo (face_recognition_sface_2021dec.onnx)</li>
 * </ul>
 *
 * <p>VERIFIKASI IDENTITAS memakai embedding + cosine similarity
 * (lihat {@link WajahVerifier}), BUKAN deteksi wajah saja. Deteksi hanya
 * menjawab "ada wajah / di mana".
 */
public final class OpenCvBridge {

    private static boolean dimuat = false;
    private static boolean nativeDimuat = false;
    private static FaceDetectorYN detektor;
    private static Net jaringWajah;
    private static String alasanGagal = "";

    public static final String NAMA_FILE_YUNET = "face_detection_yunet_2023mar.onnx";
    public static final String NAMA_FILE_SFACE = "face_recognition_sface_2021dec.onnx";

    public static final String URL_YUNET =
            "https://github.com/opencv/opencv_zoo/raw/main/models/face_detection_yunet/"
            + NAMA_FILE_YUNET;
    public static final String URL_SFACE =
            "https://github.com/opencv/opencv_zoo/raw/main/models/face_recognition_sface/"
            + NAMA_FILE_SFACE;

    private OpenCvBridge() {}

    /** Direktori model di komputer pengguna. */
    public static Path direktoriModel() {
        return java.nio.file.Paths.get(System.getProperty("user.home"), ".penggajian", "models");
    }

    /** Path file model YuNet & SFace. */
    public static Path pathYuNet() { return direktoriModel().resolve(NAMA_FILE_YUNET); }
    public static Path pathSFace() { return direktoriModel().resolve(NAMA_FILE_SFACE); }

    /**
     * Pastikan OpenCV native + kedua model siap. Melempar IllegalStateException
     * dengan pesan jelas bila ada yang hilang.
     */
    public static synchronized void siapkanModel() {
        if (dimuat) return;
        try {
            nu.pattern.OpenCV.loadLocally();
        } catch (Throwable t) {
            throw new IllegalStateException("Library OpenCV gagal dimuat: " + t.getMessage(), t);
        }
        try {
            Files.createDirectories(direktoriModel());
            unduhBilaKosong(pathYuNet(), URL_YUNET);
            unduhBilaKosong(pathSFace(), URL_SFACE);
        } catch (Exception e) {
            alasanGagal = "Model wajah belum tersedia: " + e.getMessage();
            throw new IllegalStateException(alasanGagal, e);
        }
        try {
            detektor = FaceDetectorYN.create(
                    pathYuNet().toString(), "", new Size(320, 320), 0.6f, 0.3f, 5000);
            jaringWajah = Dnn.readNetFromONNX(pathSFace().toString());
            dimuat = true;
        } catch (Throwable t) {
            alasanGagal = "Model wajah gagal dibuka: " + t.getMessage();
            throw new IllegalStateException(alasanGagal, t);
        }
    }

    private static void unduhBilaKosong(Path tujuan, String url) throws Exception {
        if (Files.exists(tujuan) && Files.size(tujuan) > 100_000) return;
        // Unduh via curl (tersedia di Ubuntu) supaya proxy/user-agent sama
        // dengan cara user memperbaiki sendiri di terminal.
        ProcessBuilder pb = new ProcessBuilder("curl", "-L", "--fail", "--progress-bar",
                "-o", tujuan.toString(), url);
        pb.inheritIO();
        Process p = pb.start();
        if (p.waitFor() != 0 || !Files.exists(tujuan) || Files.size(tujuan) < 100_000) {
            throw new IllegalStateException("Gagal mengunduh " + tujuan.getFileName()
                    + " dari " + url + ". Unduh manual lalu letakkan di " + direktoriModel());
        }
    }

    /** Alasan terakhir kegagalan siapkanModel (untuk UI). */
    public static String alasanGagal() { return alasanGagal; }

    public static boolean siap() { return dimuat; }

    /**
     * Muat HANYA native library (tanpa model ONNX) — dipakai KameraService
     * sebelum menyentuh {@code VideoCapture} agar tidak UnsatisfiedLinkError.
     */
    public static synchronized void pastikanNative() {
        if (nativeDimuat) return;
        try {
            nu.pattern.OpenCV.loadLocally();
            nativeDimuat = true;
        } catch (Throwable t) {
            throw new IllegalStateException("Library OpenCV gagal dimuat: " + t.getMessage(), t);
        }
    }

    // ---------------------------------------------------------------- deteksi

    /** Satu wajah terdeteksi pada satu frame. */
    public static final class WajahTerdeteksi {
        /** x, y, w, h (piksel). */
        public final int x, y, lebar, tinggi;
        /** Skor keyakinan deteksi 0..1. */
        public final float skor;

        WajahTerdeteksi(int x, int y, int w, int h, float skor) {
            this.x = x; this.y = y; this.lebar = w; this.tinggi = h; this.skor = skor;
        }
    }

    /**
     * Deteksi semua wajah pada frame (BGR).
     * TIDAK membalik/mirror apa pun — orientasi frame dipertahankan persis.
     */
    public static synchronized List<WajahTerdeteksi> deteksiWajah(Mat frameBgr) {
        pastiSiap();
        Mat salinan = frameBgr.clone();
        try {
            // YuNet menskala input sendiri lewat inputSize; set sesuai frame
            detektor.setInputSize(new Size(salinan.cols(), salinan.rows()));
            Mat hasil = new Mat();
            detektor.detect(salinan, hasil);
            List<WajahTerdeteksi> daftar = new ArrayList<>();
            if (!hasil.empty() && hasil.rows() > 0) {
                for (int i = 0; i < hasil.rows(); i++) {
                    double[] b = hasil.get(i, 0);
                    // kolom: x y w h landmark(5x2) skor
                    daftar.add(new WajahTerdeteksi(
                            (int) b[0], (int) b[1], (int) b[2], (int) b[3], (float) b[14]));
                }
            }
            hasil.release();
            return daftar;
        } finally {
            salinan.release();
        }
    }

    /**
     * Hitung embedding 128-D untuk SATU wajah yang sudah dicrop (BGR).
     * Praproses: resize 112x112 + (img - 127.5) / 127.5, BGR->RGB,
     * HWC -> NCHW, tanpa re-scale ganda.
     */
    public static synchronized float[] hitungEmbedding(Mat wajahBgr) {
        pastiSiap();
        Mat rgb = new Mat();
        Imgproc.cvtColor(wajahBgr, rgb, Imgproc.COLOR_BGR2RGB);
        Imgproc.resize(rgb, rgb, new Size(112, 112));
        Mat blob = Dnn.blobFromImage(rgb, 1.0 / 127.5, new Size(112, 112),
                new org.opencv.core.Scalar(127.5, 127.5, 127.5), true, false);
        rgb.release();
        try {
            jaringWajah.setInput(blob);
            Mat keluar = jaringWajah.forward();
            Mat rata = keluar.reshape(1, 1);
            MatOfFloat mf = new MatOfFloat(rata);
            float[] v = mf.toArray();
            mf.release();
            rata.release();
            keluar.release();
            // normalisasi L2 supaya cosine similarity stabil
            double jumlah = 0;
            for (float f : v) jumlah += f * f;
            double norma = Math.sqrt(jumlah);
            if (norma > 0) {
                for (int i = 0; i < v.length; i++) v[i] = (float) (v[i] / norma);
            }
            return v;
        } finally {
            blob.release();
        }
    }

    private static void pastiSiap() {
        if (!dimuat) {
            throw new IllegalStateException(alasanGagal.isEmpty()
                    ? "OpenCV belum disiapkan. Panggil siapkanModel() dulu."
                    : alasanGagal);
        }
    }
}
