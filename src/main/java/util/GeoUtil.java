package util;

/**
 * Perhitungan geodesik sederhana (Haversine) untuk validasi radius absensi.
 *
 * <p>Jari-jari bumi memakai rata-rata WGS-84 (6371000 m). Hasil dalam meter,
 * tanpa pembulatan dini — pembulatan hanya dilakukan saat menampilkan.
 */
public final class GeoUtil {

    /** Jari-jari bumi rata-rata (WGS-84), meter. */
    public static final double JARI_JARI_BUMI_METER = 6371000.0;

    private GeoUtil() {}

    /** Jarak Haversine antara dua titik, meter. */
    public static double jarakMeter(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                 + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                 * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return JARI_JARI_BUMI_METER * c;
    }

    /** Validasi rentang latitude. */
    public static boolean latitudeValid(double lat) {
        return lat >= -90.0 && lat <= 90.0;
    }

    /** Validasi rentang longitude. */
    public static boolean longitudeValid(double lon) {
        return lon >= -180.0 && lon <= 180.0;
    }
}
