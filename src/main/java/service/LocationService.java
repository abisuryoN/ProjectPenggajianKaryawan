package service;

import dao.LokasiDAO;
import model.LokasiKantor;
import util.GeoUtil;

/**
 * Service validasi lokasi absensi — batas keras 200 m (Bagian D).
 *
 * <p>Radius maksimum 200 m ditetapkan DI SINI. Tidak ada jalur simpan atau
 * validasi yang boleh melewatinya: pengaturan HRD memanggil
 * {@link #simpanKonfigurasi} dan pemeriksaan absensi memanggil {@link #validasi}.
 */
public class LocationService {

    /** Radius maksimum, meter — aturan bisnis proyek ini. */
    public static final int RADIUS_MAKS_METER = 200;

    /** Radius default bila belum ada pengaturan tersimpan. */
    public static final int RADIUS_DEFAULT_METER = 200;

    /** Akurasi minimum yang diterima (meter). Lebih buruk = ditolak. */
    public static final double AKURASI_MAKS_METER = 150.0;

    private final LokasiDAO lokasiDAO = new LokasiDAO();

    /** Konfigurasi aktif; null bila belum ada (belum diatur HRD). */
    public LokasiKantor konfigurasiAktif() {
        try {
            return lokasiDAO.ambilAktif();
        } catch (Exception e) {
            throw new IllegalStateException("Konfigurasi lokasi tidak dapat dibaca: " + e.getMessage(), e);
        }
    }

    /** Radius efektif: min(radius DB, 200) — database tidak bisa melonggarkan aturan. */
    public int radiusEfektif(LokasiKantor k) {
        int radius = (k == null) ? RADIUS_DEFAULT_METER : k.getRadiusMeter();
        return Math.min(radius, RADIUS_MAKS_METER);
    }

    /**
     * Validasi HRD sebelum menyimpan konfigurasi.
     * Melempar IllegalArgumentException dengan pesan siap tampil bila salah.
     */
    public void validasiInput(double lat, double lon, int radiusMeter, String nama) {
        if (nama == null || nama.trim().isEmpty()) {
            throw new IllegalArgumentException("Nama lokasi wajib diisi.");
        }
        if (!GeoUtil.latitudeValid(lat)) {
            throw new IllegalArgumentException("Latitude harus di antara -90 dan 90.");
        }
        if (!GeoUtil.longitudeValid(lon)) {
            throw new IllegalArgumentException("Longitude harus di antara -180 dan 180.");
        }
        if (radiusMeter <= 0) {
            throw new IllegalArgumentException("Radius harus lebih dari 0 meter.");
        }
        if (radiusMeter > RADIUS_MAKS_METER) {
            throw new IllegalArgumentException(
                    "Radius maksimum " + RADIUS_MAKS_METER + " meter (diminta " + radiusMeter + " m).");
        }
    }

    /**
     * Simpan konfigurasi lokasi. WAJIB dipanggil dengan hak HRD terverifikasi.
     * Service menolak sendiri bila {@code olehHRD} = false (validasi izin di
     * lapisan bisnis, bukan hanya menyembunyikan tombol).
     */
    public void simpanKonfigurasi(boolean olehHRD, int idUser,
                                  String nama, double lat, double lon, int radiusMeter) {
        if (!olehHRD) {
            throw new SecurityException("Hanya HRD yang boleh mengubah konfigurasi lokasi absensi.");
        }
        validasiInput(lat, lon, radiusMeter, nama);
        try {
            LokasiKantor lama = lokasiDAO.ambilAktif();
            LokasiKantor baru = new LokasiKantor(0, nama.trim(), lat, lon, radiusMeter, true);
            lokasiDAO.simpan(lama, baru, idUser);
        } catch (SecurityException | IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Gagal menyimpan konfigurasi lokasi: " + e.getMessage(), e);
        }
    }

    /** Hasil pemeriksaan lokasi pengguna. */
    public static final class Hasil {
        public final boolean lolos;
        public final double jarakMeter;   // -1 bila tidak diketahui
        public final int radiusMeter;     // radius efektif yang dipakai
        public final String pesan;

        Hasil(boolean lolos, double jarak, int radius, String pesan) {
            this.lolos = lolos;
            this.jarakMeter = jarak;
            this.radiusMeter = radius;
            this.pesan = pesan;
        }
    }

    /**
     * Pemeriksaan penuh satu titik lokasi pengguna terhadap kantor.
     *
     * @param latPengguna null bila lokasi belum tersedia
     * @param lonPengguna null bila lokasi belum tersedia
     * @param akurasiMeter null / -1 bila tidak diketahui
     */
    public Hasil validasi(Double latPengguna, Double lonPengguna, Double akurasiMeter) {
        LokasiKantor kantor;
        try {
            kantor = konfigurasiAktif();
        } catch (IllegalStateException e) {
            return new Hasil(false, -1, RADIUS_MAKS_METER,
                    "Absensi ditolak: " + e.getMessage());
        }
        int radius = radiusEfektif(kantor);
        if (kantor == null) {
            return new Hasil(false, -1, radius,
                    "Belum ada titik kantor. Minta HRD mengatur lokasi absensi dulu.");
        }
        if (latPengguna == null || lonPengguna == null
                || latPengguna.isNaN() || lonPengguna.isNaN()) {
            return new Hasil(false, -1, radius,
                    "Absensi tidak dapat dilakukan karena lokasi perangkat belum tersedia. "
                  + "Aktifkan layanan lokasi, lalu tekan \"Perbarui Lokasi\".");
        }
        if (!GeoUtil.latitudeValid(latPengguna) || !GeoUtil.longitudeValid(lonPengguna)) {
            return new Hasil(false, -1, radius,
                    "Koordinat lokasi tidak valid. Tekan \"Perbarui Lokasi\" untuk mencoba lagi.");
        }
        if (akurasiMeter != null && akurasiMeter >= 0 && akurasiMeter > AKURASI_MAKS_METER) {
            return new Hasil(false, -1, radius,
                    "Akurasi lokasi terlalu rendah (±" + Math.round(akurasiMeter)
                  + " m). Pindah ke tempat terbuka lalu perbarui lokasi.");
        }
        double jarak = GeoUtil.jarakMeter(latPengguna, lonPengguna,
                kantor.getLatitude(), kantor.getLongitude());
        if (jarak <= radius) {
            return new Hasil(true, jarak, radius,
                    "Anda berada sekitar " + Math.round(jarak)
                  + " m dari kantor — di dalam area absensi.");
        }
        return new Hasil(false, jarak, radius,
                "Absensi ditolak. Anda berada sekitar " + Math.round(jarak)
              + " meter dari lokasi kantor. Batas absensi adalah " + radius + " meter.");
    }
}
