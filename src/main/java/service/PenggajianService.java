package service;

import config.Koneksi;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Aturan bisnis penggajian (Bagian A §5).
 *
 * <p>Rumus yang dipakai project ini (sesuai skema DB — tidak ada kolom lembur):
 * <pre>Gaji Bersih = Gaji Pokok + Total Tunjangan - Potongan</pre>
 * Perhitungan UI memanggil {@link #hitungTotal} yang sama, sehingga tidak ada
 * dua rumus yang bisa berbeda.
 */
public class PenggajianService {

    /** Gaji Bersih = pokok + tunjangan - potongan. Semua argumen wajib >= 0. */
    public static BigDecimal hitungTotal(BigDecimal pokok, BigDecimal tunjangan, BigDecimal potongan) {
        if (pokok == null || tunjangan == null || potongan == null) {
            throw new IllegalArgumentException("Nominal gaji tidak boleh kosong.");
        }
        if (pokok.signum() < 0 || tunjangan.signum() < 0 || potongan.signum() < 0) {
            throw new IllegalArgumentException("Nominal tidak boleh negatif.");
        }
        return pokok.add(tunjangan).subtract(potongan);
    }

    /** Validasi periode format YYYY-MM. */
    public static void validasiPeriode(String periode) {
        if (periode == null || !periode.matches("\\d{4}-(0[1-9]|1[0-2])")) {
            throw new IllegalArgumentException(
                    "Periode tidak valid (diharapkan format YYYY-MM, mis. 2026-10).");
        }
    }

    /** True bila karyawan+periode sudah punya baris penggajian (anti-duplikat). */
    public static boolean periodeSudahAda(int idKaryawan, String periode) {
        String sql = "SELECT 1 FROM penggajian WHERE id_karyawan = ? AND periode = ? LIMIT 1";
        try (Connection c = Koneksi.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idKaryawan);
            ps.setString(2, periode);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Gagal memeriksa duplikat periode: " + e.getMessage(), e);
        }
    }

    /**
     * Validasi penuh sebelum simpan/ubah. Melempar IllegalArgumentException /
     * IllegalStateException dengan pesan Indonesia yang siap tampil.
     */
    public static void validasiSebelumSimpan(int idKaryawan, String periode,
                                             BigDecimal pokok, BigDecimal tunjangan,
                                             BigDecimal potongan, boolean cekDuplikat) {
        if (idKaryawan <= 0) {
            throw new IllegalArgumentException("Pilih karyawan dulu.");
        }
        validasiPeriode(periode);
        hitungTotal(pokok, tunjangan, potongan); // validasi nominal
        if (cekDuplikat && periodeSudahAda(idKaryawan, periode)) {
            throw new IllegalStateException(
                    "Penggajian untuk karyawan ini pada periode " + periode + " sudah ada. "
                    + "Ubah data yang ada, jangan membuat duplikat.");
        }
    }
}
