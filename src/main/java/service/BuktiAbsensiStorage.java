package service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.UUID;

/**
 * Penyimpanan foto bukti absensi (Bagian C §13).
 *
 * <ul>
 *   <li>Setiap sesi menghasilkan file BARU dari frame kamera saat itu.</li>
 *   <li>Nama file memakai UUID + timestamp + id karyawan (tidak pernah
 *       {@code absensi.jpg}).</li>
 *   <li>Direktori {@code ~/.penggajian/bukti/} dengan izin 700 — tidak dapat
 *       dibaca pengguna lain tanpa autentikasi sistem.</li>
 *   <li>Foto referensi wajah TIDAK disimpan sebagai file; yang tersimpan
 *       hanya embedding biner di database.</li>
 * </ul>
 */
public final class BuktiAbsensiStorage {

    private BuktiAbsensiStorage() {}

    /** Direktori bukti (dibuat dengan izin ketat bila belum ada). */
    public static Path direktoriBukti() {
        Path dir = java.nio.file.Paths.get(
                System.getProperty("user.home"), ".penggajian", "bukti");
        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                try {
                    Set<PosixFilePermission> izin =
                            PosixFilePermissions.fromString("rwx------");
                    Files.setPosixFilePermissions(dir, izin);
                } catch (UnsupportedOperationException ignored) {
                    // sistem non-POSIX: biarkan default
                }
            }
        } catch (Exception e) {
            throw new IllegalStateException("Gagal menyiapkan folder bukti: " + e.getMessage(), e);
        }
        return dir;
    }

    /** Nama file unik: bukti_<idKaryawan>_<timestamp>_<uuid>.jpg */
    public static String namaFileUnik(int idKaryawan) {
        String waktu = java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        return "bukti_" + idKaryawan + "_" + waktu + "_"
                + UUID.randomUUID().toString().substring(0, 8) + ".jpg";
    }

    /** Path lengkap untuk file bukti baru. */
    public static File fileBuktiBaru(int idKaryawan) {
        return direktoriBukti().resolve(namaFileUnik(idKaryawan)).toFile();
    }

    /**
     * Simpan byte JPEG sebagai bukti baru. Mengembalikan path absolut
     * (yang disimpan ke kolom {@code foto_bukti}), bukan isi gambarnya.
     */
    public static String simpan(byte[] jpeg, int idKaryawan) {
        if (jpeg == null || jpeg.length == 0) {
            throw new IllegalArgumentException("Data foto bukti kosong.");
        }
        File f = fileBuktiBaru(idKaryawan);
        try {
            Files.write(f.toPath(), jpeg);
            try {
                Files.setPosixFilePermissions(f.toPath(),
                        PosixFilePermissions.fromString("rw-------"));
            } catch (UnsupportedOperationException ignored) {
            }
            return f.getAbsolutePath();
        } catch (Exception e) {
            throw new IllegalStateException("Gagal menyimpan foto bukti: " + e.getMessage(), e);
        }
    }
}
