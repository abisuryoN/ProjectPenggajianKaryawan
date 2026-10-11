package service;

import config.Koneksi;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Akar masalah "column 'nama_karyawan' not found" DAN "HRD tidak bisa absen".
 *
 * <p>Kolom {@code nama_karyawan} HANYA ada di tabel {@code karyawan}. Tabel
 * {@code absensi}, {@code log_verifikasi_absensi}, dan {@code users} TIDAK
 * memilikinya. Setiap query yang menyebut {@code nama_karyawan} tanpa
 * {@code JOIN karyawan} pasti gagal dengan error tersebut. Kelas ini
 * menyediakan satu sumber query yang aman + penjelasan cara memperbaikinya.
 *
 * <p>Selain itu, akun HRD pada tabel {@code users} sering punya
 * {@code id_karyawan = NULL}. Akibatnya {@code Session.getIdKaryawan()}
 * mengembalikan 0 dan SEMUA modul absensi (termasuk HRD yang ingin absen
 * sendiri) menolak karena "identitas tidak valid". Kelas ini menyelesaikan
 * identitas absensi untuk kedua peran.
 */
public final class IdentitasAbsensi {

    private IdentitasAbsensi() {}

    /** Nama kolom yang benar pada tiap tabel (dokumentasi untuk pemelihara). */
    public static final String KOLOM_NAMA_DI_KARYAWAN = "k.nama_karyawan";
    public static final String CATATAN_TABEL_ABSENSI = "tabel absensi tidak punya nama_karyawan";

    /** Hasil resolusi identitas absensi. */
    public static final class Hasil {
        /** id_karyawan yang valid (> 0) atau 0 bila tidak dapat ditentukan. */
        public final int idKaryawan;
        /** Nama tampilan (dari tabel karyawan), tidak pernah kosong. */
        public final String nama;
        /** True bila HRD memakai kartu karyawannya sendiri untuk absen. */
        public final boolean viaKartuKaryawan;
        /** Alasan bila idKaryawan == 0. */
        public final String alasan;

        private Hasil(int idKaryawan, String nama, boolean viaKartuKaryawan, String alasan) {
            this.idKaryawan = idKaryawan;
            this.nama = nama;
            this.viaKartuKaryawan = viaKartuKaryawan;
            this.alasan = alasan;
        }
    }

    /**
     * Resolusi identitas untuk absen.
     *
     * <p>Aturan:
     * <ol>
     *   <li>Bila {@code idKaryawanSesi > 0} dan ada di tabel karyawan -> pakai itu.</li>
     *   <li>Bila {@code idKaryawanSesi} 0 (akun HRD dengan {@code users.id_karyawan}
     *       NULL) -> baca kolom {@code users.id_karyawan} untuk username tersebut.
     *       Nilai ini diisi tombol "Tautkan &amp; lanjutkan", jadi penautan yang
     *       sudah dikerjakan langsung berlaku pada percobaan berikutnya.</li>
     *   <li>Bila belum tertaut, cocokkan NIK/nama dengan username HANYA untuk
     *       memberi petunjuk; identitas tidak dipakai sampai HRD menautkan.</li>
     *   <li>Selain itu -> idKaryawan 0 beserta alasan yang jelas (jangan
     *       mengarang identitas).</li>
     * </ol>
     */
    public static Hasil resolve(int idKaryawanSesi, String username) throws Exception {
        if (idKaryawanSesi > 0) {
            String nama = namaKaryawan(idKaryawanSesi);
            if (nama != null) {
                return new Hasil(idKaryawanSesi, nama, false, null);
            }
            return new Hasil(0, "", false,
                    "id_karyawan pada sesi (" + idKaryawanSesi + ") tidak ada di tabel karyawan.");
        }

        String user = username == null ? "" : username.trim();
        if (user.isEmpty()) {
            return new Hasil(0, "", false, "Sesi tidak memuat username. Silakan login ulang.");
        }

        // (a) PENAUTAN RESMI: kolom users.id_karyawan. Inilah yang diisi tombol
        //     "Tautkan & lanjutkan". Tanpa langkah ini, penautan yang sudah
        //     dikerjakan tidak pernah terbaca dan layar penautan muncul terus.
        int idDariUsers = idKaryawanDariUsername(user);
        if (idDariUsers > 0) {
            String namaTertaut = namaKaryawan(idDariUsers);
            if (namaTertaut != null) {
                return new Hasil(idDariUsers, namaTertaut, true, null);
            }
            // Tautan ada tetapi kartu karyawannya sudah dihapus -> beri tahu jelas.
            return new Hasil(0, "", false,
                    "Akun \"" + user + "\" tertaut ke id_karyawan " + idDariUsers
                    + ", tetapi data karyawan tersebut tidak ditemukan. Perbaiki tautan "
                    + "di tabel users (kolom id_karyawan).");
        }

        // (b) Tanpa penautan resmi: cocokkan NIK atau nama dengan username. Ini
        //     penerimaan TIDAK otomatis — HRD tetap harus menekan tombol tautkan
        //     sekali agar tersimpan permanen di users.id_karyawan.
        String sql = "SELECT id_karyawan, nama_karyawan FROM karyawan "
                + "WHERE nik = ? OR nama_karyawan = ? ORDER BY "
                + "CASE WHEN nik = ? THEN 0 ELSE 1 END LIMIT 1";
        try (Connection c = Koneksi.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, user);
            ps.setString(2, user);
            ps.setString(3, user);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // Jangan langsung dipakai: kembalikan petunjuk untuk menautkan.
                    return new Hasil(0, "", false,
                            "Akun \"" + user + "\" belum tertaut secara resmi. Ditemukan "
                            + "kartu karyawan dengan NIK/nama yang sama (id "
                            + rs.getInt("id_karyawan") + "). Pilih kartu itu di daftar "
                            + "berikut lalu tekan \"Tautkan & lanjutkan\" sekali saja.");
                }
            }
        }
        return new Hasil(0, "", false,
                "Akun \"" + user + "\" belum tertaut ke data karyawan. Minta HRD menautkan "
                + "akun ini ke data karyawan (kolom users.id_karyawan) atau mendaftarkan "
                + "NIK yang sama dengan username.");
    }

    /** Nama karyawan dari tabel karyawan; null bila id tidak ada. */
    public static String namaKaryawan(int idKaryawan) throws Exception {
        if (idKaryawan <= 0) return null;
        try (Connection c = Koneksi.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT nama_karyawan FROM karyawan WHERE id_karyawan = ?")) {
            ps.setInt(1, idKaryawan);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("nama_karyawan") : null;
            }
        }
    }

    /** Ambil id_karyawan dari username (null bila belum tertaut). */
    public static int idKaryawanDariUsername(String username) {
        if (username == null || username.trim().isEmpty()) return 0;
        try (Connection c = Koneksi.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT id_karyawan FROM users WHERE username = ?")) {
            ps.setString(1, username.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int id = rs.getInt("id_karyawan");
                    return rs.wasNull() ? 0 : id;
                }
            }
        } catch (Exception e) {
            System.err.println("idKaryawanDariUsername gagal: " + e.getMessage());
        }
        return 0;
    }

    /**
     * Daftar semua karyawan sebagai [id, nik, nama].
     *
     * @return list baris; tidak pernah null.
     */
    public static java.util.List<String[]> daftarKaryawan() throws Exception {
        java.util.List<String[]> hasil = new java.util.ArrayList<>();
        String sql = "SELECT id_karyawan, nik, nama_karyawan FROM karyawan ORDER BY nama_karyawan";
        try (Connection c = Koneksi.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                hasil.add(new String[]{
                        String.valueOf(rs.getInt("id_karyawan")),
                        rs.getString("nik") == null ? "-" : rs.getString("nik"),
                        rs.getString("nama_karyawan") == null ? "-" : rs.getString("nama_karyawan")});
            }
        }
        return hasil;
    }

    /**
     * Tautkan akun users ke data karyawan (dipakai HRD/admin saat HRD ingin
     * absen sebagai dirinya sendiri). Tidak mengubah skema, hanya mengisi
     * kolom users.id_karyawan yang memang sudah ada.
     *
     * @return true bila satu baris ter-update.
     */
    public static boolean tautkanAkunKeKaryawan(String username, int idKaryawan)
            throws Exception {
        if (username == null || username.trim().isEmpty() || idKaryawan <= 0) return false;
        try (Connection c = Koneksi.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "UPDATE users SET id_karyawan = ? WHERE username = ?")) {
            ps.setInt(1, idKaryawan);
            ps.setString(2, username.trim());
            return ps.executeUpdate() == 1;
        }
    }
}
