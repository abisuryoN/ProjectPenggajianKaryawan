package dao;

import config.Koneksi;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import model.LokasiKantor;

/**
 * Akses tabel {@code pengaturan_lokasi} + {@code audit_pengaturan_lokasi}.
 *
 * <p>Aturan radius maksimum 200 m ditegakkan di {@code service.LocationService},
 * bukan di sini, agar aturan bisnis tidak tersebar.
 */
public class LokasiDAO {

    /** Konfigurasi aktif (baris dengan aktif=1 terbaru), null bila belum ada. */
    public LokasiKantor ambilAktif() throws Exception {
        String sql = "SELECT id_pengaturan, nama_lokasi, latitude, longitude, radius_meter, aktif "
                   + "FROM pengaturan_lokasi WHERE aktif = 1 "
                   + "ORDER BY id_pengaturan DESC LIMIT 1";
        try (Connection c = Koneksi.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) return null;
            return new LokasiKantor(
                    rs.getInt("id_pengaturan"),
                    rs.getString("nama_lokasi"),
                    rs.getDouble("latitude"),
                    rs.getDouble("longitude"),
                    rs.getInt("radius_meter"),
                    rs.getBoolean("aktif"));
        }
    }

    /**
     * Simpan konfigurasi (menonaktifkan yang lama, menyisipkan yang baru) dan
     * catat audit. Dijalankan dalam satu transaksi.
     *
     * @param idUser id users pelaku (HRD), 0 bila tidak diketahui.
     */
    public void simpan(LokasiKantor lama, LokasiKantor baru, int idUser) throws Exception {
        String nonaktifkan = "UPDATE pengaturan_lokasi SET aktif = 0 WHERE aktif = 1";
        String insert = "INSERT INTO pengaturan_lokasi "
                + "(nama_lokasi, latitude, longitude, radius_meter, aktif, diubah_oleh) "
                + "VALUES (?, ?, ?, ?, 1, ?)";
        String audit = "INSERT INTO audit_pengaturan_lokasi "
                + "(id_pengaturan, diubah_oleh, latitude_lama, longitude_lama, radius_lama, "
                + " latitude_baru, longitude_baru, radius_baru) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection c = Koneksi.getConnection()) {
            boolean auto = c.getAutoCommit();
            c.setAutoCommit(false);
            try {
                try (PreparedStatement ps = c.prepareStatement(nonaktifkan)) {
                    ps.executeUpdate();
                }
                int idBaru;
                try (PreparedStatement ps = c.prepareStatement(insert,
                        PreparedStatement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, baru.getNamaLokasi());
                    ps.setDouble(2, baru.getLatitude());
                    ps.setDouble(3, baru.getLongitude());
                    ps.setInt(4, baru.getRadiusMeter());
                    if (idUser > 0) {
                        ps.setInt(5, idUser);
                    } else {
                        ps.setNull(5, java.sql.Types.INTEGER);
                    }
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        idBaru = keys.getInt(1);
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(audit)) {
                    ps.setInt(1, idBaru);
                    if (idUser > 0) {
                        ps.setInt(2, idUser);
                    } else {
                        ps.setNull(2, java.sql.Types.INTEGER);
                    }
                    if (lama == null) {
                        ps.setNull(3, java.sql.Types.DOUBLE);
                        ps.setNull(4, java.sql.Types.DOUBLE);
                        ps.setNull(5, java.sql.Types.INTEGER);
                    } else {
                        ps.setDouble(3, lama.getLatitude());
                        ps.setDouble(4, lama.getLongitude());
                        ps.setInt(5, lama.getRadiusMeter());
                    }
                    ps.setDouble(6, baru.getLatitude());
                    ps.setDouble(7, baru.getLongitude());
                    ps.setInt(8, baru.getRadiusMeter());
                    ps.executeUpdate();
                }
                c.commit();
            } catch (Exception e) {
                c.rollback();
                throw e;
            } finally {
                c.setAutoCommit(auto);
            }
        }
    }
}
