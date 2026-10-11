package service;

/**
 * Batas-batas keras absensi. Satu-satunya sumber kebenaran untuk angka 200.
 * Semua service + UI membaca dari sini — tidak ada angka 200 yang ditanam
 * di tempat lain.
 */
public final class BatasAbsensi {
    private BatasAbsensi() {}

    /** Radius maksimum absensi, meter. */
    public static final int RADIUS_MAKS_METER = 200;

    /** Radius default bila HRD belum mengatur. */
    public static final int RADIUS_DEFAULT_METER = 200;

    /** Toleransi perbandingan batas (meter). Jarak <= radius + toleransi = lolos. */
    public static final double TOLERANSI_BATAS_METER = 0.5;
}
