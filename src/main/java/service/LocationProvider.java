package service;

/**
 * Sumber koordinat pengguna (Bagian D §15).
 *
 * <p>Abstraksi ini membuat modul absensi tidak terikat satu teknologi lokasi.
 * Implementasi nyata: {@link GeoClueLocationProvider} (GeoClue2 via D-Bus).
 * Bila di perangkat tidak ada sumber lokasi yang layak, provider mengembalikan
 * {@link HasilLokasi#tersedia} = false — dan absensi WAJIB ditolak. Tidak ada
 * fallback koordinat palsu, dan tidak ada input koordinat manual untuk karyawan.
 */
public interface LocationProvider {

    /** Hasil pengambilan lokasi. */
    final class HasilLokasiPengguna {
        public final boolean tersedia;
        public final double latitude;
        public final double longitude;
        public final double akurasiMeter; // -1 bila tidak diketahui
        public final String keterangan;   // untuk status di UI

        private HasilLokasiPengguna(boolean tersedia, double lat, double lon,
                                    double akurasi, String keterangan) {
            this.tersedia = tersedia;
            this.latitude = lat;
            this.longitude = lon;
            this.akurasiMeter = akurasi;
            this.keterangan = keterangan;
        }

        public static HasilLokasiPengguna ada(double lat, double lon, double akurasi, String ket) {
            return new HasilLokasiPengguna(true, lat, lon, akurasi, ket);
        }

        public static HasilLokasiPengguna tidakAda(String alasan) {
            return new HasilLokasiPengguna(false, Double.NaN, Double.NaN, -1, alasan);
        }
    }

    /** Nama provider, untuk ditampilkan di UI/log. */
    String nama();

    /**
     * Ambil lokasi saat ini. Implementasi harus punya timeout sendiri dan
     * tidak boleh menggantung selamanya (dipanggil dari background thread).
     */
    HasilLokasiPengguna ambilLokasi();
}
