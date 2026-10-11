package service;

/**
 * Provider lokasi berantai (Bagian D §15): coba sumber berurutan sampai satu
 * berhasil. Urutan default: GeoClue2 (paling akurat bila layanan sistem aktif)
 * lalu GPS perangkat lewat browser (selalu tersedia di desktop modern).
 *
 * <p>Tujuan: absensi tidak lagi mustahil hanya karena layanan geolokasi sistem
 * mati/diblokir ({@code Geolocation disabled for UID 1000}), TANPA melonggarkan
 * aturan: setiap koordinat dari sumber mana pun tetap divalidasi radius +
 * akurasi oleh {@link LocationService#validasi}.
 */
public class RantaiLocationProvider implements LocationProvider {

    private final LocationProvider[] sumber;

    public RantaiLocationProvider(LocationProvider... sumber) {
        this.sumber = sumber;
    }

    /** Urutan standar aplikasi: GeoClue dulu, lalu GPS perangkat/browser. */
    public static RantaiLocationProvider standar() {
        return new RantaiLocationProvider(
                new GeoClueLocationProvider(),
                new BrowserLocationProvider());
    }

    @Override
    public String nama() {
        StringBuilder sb = new StringBuilder();
        for (LocationProvider p : sumber) {
            if (sb.length() > 0) sb.append(" -> ");
            sb.append(p.nama());
        }
        return sb.toString();
    }

    @Override
    public HasilLokasiPengguna ambilLokasi() {
        StringBuilder alasan = new StringBuilder();
        for (LocationProvider p : sumber) {
            try {
                HasilLokasiPengguna h = p.ambilLokasi();
                if (h != null && h.tersedia) {
                    return h;
                }
                if (alasan.length() > 0) alasan.append(" | ");
                alasan.append(p.nama()).append(": ")
                      .append(h == null ? "tidak menjawab" : h.keterangan);
            } catch (Exception e) {
                if (alasan.length() > 0) alasan.append(" | ");
                alasan.append(p.nama()).append(": ").append(ringkas(e.getMessage()));
            }
        }
        return HasilLokasiPengguna.tidakAda(
                "Lokasi belum tersedia dari semua sumber (" + alasan + ").");
    }

    private static String ringkas(String pesan) {
        if (pesan == null) return "tidak diketahui";
        return pesan.length() > 100 ? pesan.substring(0, 100) + "…" : pesan;
    }
}
