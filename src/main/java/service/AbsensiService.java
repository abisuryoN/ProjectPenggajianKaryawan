package service;

import config.Koneksi;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Aturan bisnis absensi (Bagian E §18–19).
 *
 * <p>Prinsip: semua syarat diverifikasi ulang di sini saat proses simpan —
 * status "siap" dari UI tidak pernah dipercaya begitu saja.
 *
 * <p>ID karyawan selalu berasal dari parameter yang sudah divalidasi pemanggil
 * terhadap sesi login ({@code auth.Session}), bukan dari input bebas pengguna.
 */
public class AbsensiService {

    public static final DateTimeFormatter JAM = DateTimeFormatter.ofPattern("HH:mm:ss");

    /** Status verifikasi wajah yang mungkin tercatat. */
    public static final String WAJAH_COCOK = "COCOK";
    public static final String WAJAH_TIDAK_COCOK = "TIDAK_COCOK";
    public static final String WAJAH_TAK_DIKENAL = "TAK_DIKENAL";
    public static final String WAJAH_MANUAL = "MANUAL";

    /** Alasan kegagalan yang dicatat ke log audit. */
    public static final String ALASAN_WAJAH_TIDAK_COCOK = "WAJAH_TIDAK_COCOK";
    public static final String ALASAN_WAJAH_TAK_DIKENAL = "WAJAH_TAK_DIKENAL";
    public static final String ALASAN_TANPA_WAJAH = "TANPA_WAJAH";
    public static final String ALASAN_MULTI_WAJAH = "MULTI_WAJAH";
    public static final String ALASAN_LOKASI_DI_LUAR = "LOKASI_DI_LUAR";
    public static final String ALASAN_LOKASI_TAK_TERSEDIA = "LOKASI_TAK_TERSEDIA";
    public static final String ALASAN_LIVENESS_GAGAL = "LIVENESS_GAGAL";
    public static final String ALASAN_DUPLIKAT = "DUPLIKAT";

    /** Hasil akhir satu percobaan absensi. */
    public static final class HasilAbsen {
        public final boolean sukses;
        public final String pesan;
        public final String jam;
        public final int idAbsensi;

        private HasilAbsen(boolean sukses, String pesan, String jam, int idAbsensi) {
            this.sukses = sukses;
            this.pesan = pesan;
            this.jam = jam;
            this.idAbsensi = idAbsensi;
        }

        public static HasilAbsen gagal(String pesan) {
            return new HasilAbsen(false, pesan, null, 0);
        }

        public static HasilAbsen berhasil(String pesan, String jam, int id) {
            return new HasilAbsen(true, pesan, jam, id);
        }
    }

    /** Data pendukung penyimpanan satu absensi. */
    public static final class BuktiAbsen {
        public String verifikasiWajah = WAJAH_MANUAL;
        public Double skorWajah;
        public Double lat;
        public Double lon;
        public Double jarakMeter;
        public String fotoBukti;
        public String sumber = "MANUAL";
        public String keterangan;
    }

    // ---------------------------------------------------------------- cek ganda

    /** True bila karyawan sudah absen masuk pada tanggal tsb. */
    public static boolean sudahAbsenMasuk(int idKaryawan, LocalDate tanggal) {
        return adaBaris("SELECT 1 FROM absensi WHERE id_karyawan=? AND tanggal=? "
                + "AND jam_masuk IS NOT NULL LIMIT 1", idKaryawan, tanggal);
    }

    /** True bila karyawan sudah absen pulang pada tanggal tsb. */
    public static boolean sudahAbsenPulang(int idKaryawan, LocalDate tanggal) {
        return adaBaris("SELECT 1 FROM absensi WHERE id_karyawan=? AND tanggal=? "
                + "AND jam_pulang IS NOT NULL LIMIT 1", idKaryawan, tanggal);
    }

    /** True bila sudah ada baris absensi (masuk) untuk mencegah pencatatan ganda. */
    public static boolean sudahAdaBarisHariIni(int idKaryawan, LocalDate tanggal) {
        return adaBaris("SELECT 1 FROM absensi WHERE id_karyawan=? AND tanggal=? LIMIT 1",
                idKaryawan, tanggal);
    }

    private static boolean adaBaris(String sql, int idKaryawan, LocalDate tanggal) {
        try (Connection c = Koneksi.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idKaryawan);
            ps.setDate(2, java.sql.Date.valueOf(tanggal));
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Gagal memeriksa absensi: " + e.getMessage(), e);
        }
    }

    // ------------------------------------------------------------------ simpan

    /**
     * Absen masuk: INSERT baris baru. Menolak bila sudah absen masuk hari ini.
     *
     * @param wajibWajahCocok true untuk absensi FACE_GPS; mewajibkan
     *                        verifikasiWajah == COCOK dan lokasi sudah divalidasi.
     */
    public HasilAbsen absenMasuk(int idKaryawan, BuktiAbsen bukti, boolean wajibWajahCocok) {
        if (idKaryawan <= 0) {
            return HasilAbsen.gagal("Identitas karyawan tidak valid. Silakan login ulang.");
        }
        if (wajibWajahCocok && !WAJAH_COCOK.equals(bukti.verifikasiWajah)) {
            return HasilAbsen.gagal("Wajah belum terverifikasi sebagai pemilik akun. Absensi ditolak.");
        }
        LocalDate hari = LocalDate.now();
        if (sudahAbsenMasuk(idKaryawan, hari)) {
            catatLog(idKaryawan, hari, ALASAN_DUPLIKAT, "Sudah absen masuk hari ini");
            return HasilAbsen.gagal("Anda sudah melakukan absen masuk hari ini.");
        }
        String jam = LocalTime.now().format(JAM);
        String sql = "INSERT INTO absensi (id_karyawan, tanggal, jam_masuk, status, keterangan, "
                + "verifikasi_wajah, skor_wajah, lat_absen, lon_absen, jarak_meter, foto_bukti, sumber) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = Koneksi.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idKaryawan);
            ps.setDate(2, java.sql.Date.valueOf(hari));
            ps.setString(3, jam);
            ps.setString(4, "Hadir");
            ps.setString(5, bukti.keterangan != null ? bukti.keterangan : "Absen masuk");
            ps.setString(6, bukti.verifikasiWajah);
            setDouble(ps, 7, bukti.skorWajah);
            setDouble(ps, 8, bukti.lat);
            setDouble(ps, 9, bukti.lon);
            setDouble(ps, 10, bukti.jarakMeter);
            ps.setString(11, bukti.fotoBukti);
            ps.setString(12, bukti.sumber);
            ps.executeUpdate();
            int id = 0;
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) id = keys.getInt(1);
            }
            return HasilAbsen.berhasil("Absen masuk berhasil pukul " + jam + ".", jam, id);
        } catch (Exception e) {
            return HasilAbsen.gagal("Gagal menyimpan absen masuk: " + e.getMessage());
        }
    }

    /**
     * Absen pulang: UPDATE baris hari ini. Tidak pernah menimpa data masuk.
     * Menolak bila belum absen masuk atau sudah absen pulang.
     */
    public HasilAbsen absenPulang(int idKaryawan, BuktiAbsen bukti, boolean wajibWajahCocok) {
        if (idKaryawan <= 0) {
            return HasilAbsen.gagal("Identitas karyawan tidak valid. Silakan login ulang.");
        }
        if (wajibWajahCocok && !WAJAH_COCOK.equals(bukti.verifikasiWajah)) {
            return HasilAbsen.gagal("Wajah belum terverifikasi sebagai pemilik akun. Absensi ditolak.");
        }
        LocalDate hari = LocalDate.now();
        if (!sudahAbsenMasuk(idKaryawan, hari)) {
            return HasilAbsen.gagal("Belum ada absen masuk hari ini. Lakukan absen masuk dulu.");
        }
        if (sudahAbsenPulang(idKaryawan, hari)) {
            return HasilAbsen.gagal("Anda sudah melakukan absen pulang hari ini.");
        }
        String jam = LocalTime.now().format(JAM);
        String sql = "UPDATE absensi SET jam_pulang=?, keterangan=?, verifikasi_wajah=?, "
                + "skor_wajah=?, lat_absen=?, lon_absen=?, jarak_meter=?, foto_bukti=?, sumber=? "
                + "WHERE id_karyawan=? AND tanggal=? AND jam_pulang IS NULL AND jam_masuk IS NOT NULL";
        try (Connection c = Koneksi.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, jam);
            ps.setString(2, bukti.keterangan != null ? bukti.keterangan : "Absen lengkap");
            ps.setString(3, bukti.verifikasiWajah);
            setDouble(ps, 4, bukti.skorWajah);
            setDouble(ps, 5, bukti.lat);
            setDouble(ps, 6, bukti.lon);
            setDouble(ps, 7, bukti.jarakMeter);
            ps.setString(8, bukti.fotoBukti);
            ps.setString(9, bukti.sumber);
            ps.setInt(10, idKaryawan);
            ps.setDate(11, java.sql.Date.valueOf(hari));
            int n = ps.executeUpdate();
            if (n == 0) {
                return HasilAbsen.gagal("Absen pulang gagal: data absen masuk hari ini tidak ditemukan.");
            }
            return HasilAbsen.berhasil("Absen pulang berhasil pukul " + jam + ".", jam, 0);
        } catch (Exception e) {
            return HasilAbsen.gagal("Gagal menyimpan absen pulang: " + e.getMessage());
        }
    }

    // ------------------------------------------------------------------- audit

    /** Catat kegagalan verifikasi (tanpa foto mentah, proporsional). */
    public static void catatLog(int idKaryawan, LocalDate tanggal, String alasan, String detail) {
        String sql = "INSERT INTO log_verifikasi_absensi (id_karyawan, tanggal, waktu, alasan, detail) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection c = Koneksi.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idKaryawan);
            ps.setDate(2, java.sql.Date.valueOf(tanggal != null ? tanggal : LocalDate.now()));
            ps.setTime(3, java.sql.Time.valueOf(LocalTime.now()));
            ps.setString(4, alasan);
            ps.setString(5, detail);
            ps.executeUpdate();
        } catch (Exception e) {
            // Log audit tidak boleh menjatuhkan alur utama; cukup ke stderr.
            System.err.println("Gagal mencatat log verifikasi: " + e.getMessage());
        }
    }

    private static void setDouble(PreparedStatement ps, int idx, Double nilai) throws Exception {
        if (nilai == null) {
            ps.setNull(idx, java.sql.Types.DOUBLE);
        } else {
            ps.setDouble(idx, nilai);
        }
    }
}
