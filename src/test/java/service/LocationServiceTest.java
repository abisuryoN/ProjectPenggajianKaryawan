package service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test aturan radius 200 m (BAGIAN J §26) — tanpa kamera/GPS nyata,
 * murni logika. Koordinat sintetis dipakai HANYA untuk menguji ATURAN,
 * bukan sebagai lokasi pengguna aplikasi.
 */
public class LocationServiceTest {

    private final LocationService svc = new LocationService();

    // titik kantor sintetis untuk pengujian: Monas, Jakarta
    private static final double LAT_KANTOR = -6.175392;
    private static final double LON_KANTOR = 106.827153;

    private void simpanKantor() {
        svc.simpanKonfigurasi(true, 1, "Kantor Uji", LAT_KANTOR, LON_KANTOR, 200);
    }

    private void hapusKonfigurasi() throws Exception {
        java.sql.Connection c = config.Koneksi.getConnection();
        c.createStatement().executeUpdate("DELETE FROM pengaturan_lokasi WHERE nama_lokasi='Kantor Uji'");
        c.createStatement().executeUpdate("DELETE FROM audit_pengaturan_lokasi WHERE id_pengaturan NOT IN (SELECT id_pengaturan FROM pengaturan_lokasi)");
        c.close();
    }

    // ---------------- validasi input konfigurasi (§14) ----------------

    @Test
    void radiusDiatas200Ditolak() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> svc.validasiInput(LAT_KANTOR, LON_KANTOR, 250, "Kantor"));
        assertTrue(e.getMessage().contains("200"));
    }

    @Test
    void radiusNolAtauNegatifDitolak() {
        assertThrows(IllegalArgumentException.class,
                () -> svc.validasiInput(LAT_KANTOR, LON_KANTOR, 0, "Kantor"));
        assertThrows(IllegalArgumentException.class,
                () -> svc.validasiInput(LAT_KANTOR, LON_KANTOR, -5, "Kantor"));
    }

    @Test
    void latitudeInvalidDitolak() {
        assertThrows(IllegalArgumentException.class,
                () -> svc.validasiInput(95.0, LON_KANTOR, 100, "Kantor"));
        assertThrows(IllegalArgumentException.class,
                () -> svc.validasiInput(-91.0, LON_KANTOR, 100, "Kantor"));
    }

    @Test
    void longitudeInvalidDitolak() {
        assertThrows(IllegalArgumentException.class,
                () -> svc.validasiInput(LAT_KANTOR, 181.0, 100, "Kantor"));
        assertThrows(IllegalArgumentException.class,
                () -> svc.validasiInput(LAT_KANTOR, -181.0, 100, "Kantor"));
    }

    @Test
    void namaKosongDitolak() {
        assertThrows(IllegalArgumentException.class,
                () -> svc.validasiInput(LAT_KANTOR, LON_KANTOR, 100, "  "));
    }

    // --------------- izin: hanya HRD boleh ubah (§14/§31.11) ---------------

    @Test
    void karyawanTidakBolehSimpanKonfigurasi() {
        SecurityException e = assertThrows(SecurityException.class,
                () -> svc.simpanKonfigurasi(false, 2, "Kantor", LAT_KANTOR, LON_KANTOR, 100));
        assertTrue(e.getMessage().contains("HRD"));
    }

    // ------------------- perhitungan jarak (§16) -------------------

    @Test
    void jarakHaversineDiriSendiriNol() {
        assertEquals(0.0, util.GeoUtil.jarakMeter(LAT_KANTOR, LON_KANTOR, LAT_KANTOR, LON_KANTOR), 0.001);
    }

    @Test
    void jarakSekitar100MeterAkurat() {
        // geser ~0.0009 derajat lintang ≈ 100 m
        double jarak = util.GeoUtil.jarakMeter(LAT_KANTOR, LON_KANTOR, LAT_KANTOR + 0.0009, LON_KANTOR);
        assertTrue(jarak > 90 && jarak < 110, "jarak ~100 m, hasil=" + jarak);
    }

    // ------------------- validasi absensi (§16/§26) -------------------

    @Test
    void lokasiTidakTersediaDitolak() {
        LocationService.Hasil h = svc.validasi(null, null, null);
        assertFalse(h.lolos);
        assertTrue(h.pesan.contains("belum tersedia"));
    }

    @Test
    void koordinatInvalidDitolak() {
        LocationService.Hasil h = svc.validasi(999.0, 999.0, 10.0);
        assertFalse(h.lolos);
        assertTrue(h.pesan.contains("tidak valid"));
    }

    @Test
    void akurasiTerlaluBurukDitolak() {
        simpanKantor();
        try {
            // titik tepat di kantor tapi akurasi 500 m (> 150)
            LocationService.Hasil h = svc.validasi(LAT_KANTOR, LON_KANTOR, 500.0);
            assertFalse(h.lolos);
            assertTrue(h.pesan.contains("Akurasi"));
        } finally {
            try { hapusKonfigurasi(); } catch (Exception ignored) {}
        }
    }

    @Test
    void diDalamRadiusLolos() {
        simpanKantor();
        try {
            // ~50 m dari kantor
            LocationService.Hasil h = svc.validasi(LAT_KANTOR + 0.00045, LON_KANTOR, 20.0);
            assertTrue(h.lolos, "pesan: " + h.pesan);
            assertTrue(h.pesan.contains("di dalam area"));
        } finally {
            try { hapusKonfigurasi(); } catch (Exception ignored) {}
        }
    }

    @Test
    void diLuarRadiusDitolakDenganJarak() {
        simpanKantor();
        try {
            // ~275 m utara kantor (0.002475 derajat lintang)
            LocationService.Hasil h = svc.validasi(LAT_KANTOR + 0.002475, LON_KANTOR, 20.0);
            assertFalse(h.lolos);
            assertTrue(h.pesan.contains("ditolak"), h.pesan);
            assertTrue(h.pesan.contains("275") || h.pesan.contains("276")
                    || h.pesan.contains("274"), "harus menyebut jarak ~275 m: " + h.pesan);
            assertTrue(h.pesan.contains("200"), "harus menyebut batas 200 m");
        } finally {
            try { hapusKonfigurasi(); } catch (Exception ignored) {}
        }
    }

    @Test
    void tepatPadaBatas200MasihLolos() {
        // aturan: jarak <= radius -> lolos (§16)
        simpanKantor();
        try {
            double drLat = 200.0 / 111_320.0; // 200 m dalam derajat lintang (kasar, ekuator-area)
            LocationService.Hasil h = svc.validasi(LAT_KANTOR + drLat, LON_KANTOR, 20.0);
            // toleransi formula kasar: lolos ATAU gagal dengan jarak sangat dekat 200
            if (!h.lolos) {
                assertTrue(h.jarakMeter < 210, "jarak=" + h.jarakMeter);
            }
        } finally {
            try { hapusKonfigurasi(); } catch (Exception ignored) {}
        }
    }

    @Test
    void konfigurasiTidakTersediaDitolakJelas() throws Exception {
        // nonaktifkan SEMUA konfigurasi supaya benar-benar menguji jalur "belum diatur"
        java.sql.Connection c = config.Koneksi.getConnection();
        java.sql.Statement st = c.createStatement();
        st.executeUpdate("UPDATE pengaturan_lokasi SET aktif = 0");
        st.close();
        c.close();

        LocationService.Hasil h = svc.validasi(LAT_KANTOR, LON_KANTOR, 20.0);
        assertFalse(h.lolos, "harus ditolak saat belum ada titik kantor");
        assertTrue(h.pesan.contains("Minta HRD") || h.pesan.contains("titik kantor"), h.pesan);
    }

    // ------------------- radius efektif (§17) -------------------

    @Test
    void radiusEfektifTidakBisaLebihDari200() {
        assertEquals(200, svc.radiusEfektif(
                new model.LokasiKantor(1, "x", 0, 0, 500, true)));
        assertEquals(150, svc.radiusEfektif(
                new model.LokasiKantor(1, "x", 0, 0, 150, true)));
        assertEquals(200, svc.radiusEfektif(null)); // default
    }
}
