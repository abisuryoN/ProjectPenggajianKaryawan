package dao;

import config.Koneksi;
import java.sql.*;
import javax.swing.table.DefaultTableModel;
import model.Absensi;

public class AbsensiDAO {
    public void insert(Absensi a) throws SQLException {
        String sql = "INSERT INTO absensi (id_karyawan, tanggal, jam_masuk, jam_pulang, status, keterangan) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, a.getIdKaryawan());
            ps.setString(2, a.getTanggal());
            ps.setString(3, a.getJamMasuk());
            ps.setString(4, a.getJamPulang());
            ps.setString(5, a.getStatus());
            ps.setString(6, a.getKeterangan());
            ps.executeUpdate();
        }
    }

    public void update(Absensi a) throws SQLException {
        String sql = "UPDATE absensi SET id_karyawan=?, tanggal=?, jam_masuk=?, jam_pulang=?, status=?, keterangan=? WHERE id_absensi=?";
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, a.getIdKaryawan());
            ps.setString(2, a.getTanggal());
            ps.setString(3, a.getJamMasuk());
            ps.setString(4, a.getJamPulang());
            ps.setString(5, a.getStatus());
            ps.setString(6, a.getKeterangan());
            ps.setInt(7, a.getIdAbsensi());
            ps.executeUpdate();
        }
    }

    public void absenMasuk(int idKaryawan, String tanggal, String jamMasuk) throws SQLException {
        String sql = "INSERT INTO absensi (id_karyawan, tanggal, jam_masuk, status, keterangan) VALUES (?, ?, ?, 'Hadir', 'Absen masuk')";
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idKaryawan);
            ps.setString(2, tanggal);
            ps.setString(3, jamMasuk);
            ps.executeUpdate();
        }
    }

    public void absenPulang(int idKaryawan, String tanggal, String jamPulang) throws SQLException {
        String sql = "UPDATE absensi SET jam_pulang=?, keterangan='Absen lengkap' WHERE id_karyawan=? AND tanggal=? AND jam_pulang IS NULL";
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, jamPulang);
            ps.setInt(2, idKaryawan);
            ps.setString(3, tanggal);
            ps.executeUpdate();
        }
    }

    public void delete(int idAbsensi) throws SQLException {
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM absensi WHERE id_absensi=?")) {
            ps.setInt(1, idAbsensi);
            ps.executeUpdate();
        }
    }

    public DefaultTableModel getAll() throws SQLException { return search("", 0); }

    public DefaultTableModel getByKaryawan(int idKaryawan) throws SQLException { return search("", idKaryawan); }

    public DefaultTableModel search(String keyword, int idKaryawanFilter) throws SQLException {
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID Absensi", "Nama Karyawan", "Tanggal", "Jam Masuk", "Jam Pulang", "Status", "Keterangan"}, 0);
        String sql = "SELECT a.*, k.nama_karyawan FROM absensi a JOIN karyawan k ON a.id_karyawan=k.id_karyawan "
                   + "WHERE (k.nama_karyawan LIKE ? OR a.status LIKE ? OR a.tanggal LIKE ?) "
                   + (idKaryawanFilter > 0 ? "AND a.id_karyawan=? " : "")
                   + "ORDER BY a.tanggal DESC, a.id_absensi DESC";
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            String key = "%" + keyword + "%";
            ps.setString(1, key); ps.setString(2, key); ps.setString(3, key);
            if (idKaryawanFilter > 0) ps.setInt(4, idKaryawanFilter);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{rs.getInt("id_absensi"), rs.getString("nama_karyawan"), rs.getDate("tanggal"), rs.getString("jam_masuk"), rs.getString("jam_pulang"), rs.getString("status"), rs.getString("keterangan")});
                }
            }
        }
        return model;
    }
}
