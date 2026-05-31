package dao;

import config.Koneksi;
import java.sql.*;
import javax.swing.table.DefaultTableModel;
import model.Penggajian;

public class PenggajianDAO {
    public double hitungTotal(double gajiPokok, double tunjangan, double potongan) {
        return gajiPokok + tunjangan - potongan;
    }

    public void insert(Penggajian p) throws SQLException {
        String sql = "INSERT INTO penggajian (id_karyawan, bulan, tahun, gaji_pokok, tunjangan, potongan, total_gaji, keterangan, tanggal_gaji) VALUES (?, ?, ?, ?, ?, ?, ?, ?, CURDATE())";
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, p.getIdKaryawan());
            ps.setString(2, p.getBulan());
            ps.setString(3, p.getTahun());
            ps.setDouble(4, p.getGajiPokok());
            ps.setDouble(5, p.getTunjangan());
            ps.setDouble(6, p.getPotongan());
            ps.setDouble(7, p.getTotalGaji());
            ps.setString(8, p.getKeterangan());
            ps.executeUpdate();
        }
    }

    public void update(Penggajian p) throws SQLException {
        String sql = "UPDATE penggajian SET id_karyawan=?, bulan=?, tahun=?, gaji_pokok=?, tunjangan=?, potongan=?, total_gaji=?, keterangan=? WHERE id_gaji=?";
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, p.getIdKaryawan());
            ps.setString(2, p.getBulan());
            ps.setString(3, p.getTahun());
            ps.setDouble(4, p.getGajiPokok());
            ps.setDouble(5, p.getTunjangan());
            ps.setDouble(6, p.getPotongan());
            ps.setDouble(7, p.getTotalGaji());
            ps.setString(8, p.getKeterangan());
            ps.setInt(9, p.getIdGaji());
            ps.executeUpdate();
        }
    }

    public void delete(int idGaji) throws SQLException {
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM penggajian WHERE id_gaji=?")) {
            ps.setInt(1, idGaji);
            ps.executeUpdate();
        }
    }

    public DefaultTableModel getAll() throws SQLException { return search("", 0); }
    public DefaultTableModel getByKaryawan(int idKaryawan) throws SQLException { return search("", idKaryawan); }

    public DefaultTableModel search(String keyword, int idKaryawanFilter) throws SQLException {
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID Gaji", "Nama Karyawan", "Bulan", "Tahun", "Gaji Pokok", "Tunjangan", "Potongan", "Total Gaji", "Keterangan"}, 0);
        String sql = "SELECT p.*, k.nama_karyawan FROM penggajian p JOIN karyawan k ON p.id_karyawan=k.id_karyawan "
                   + "WHERE (k.nama_karyawan LIKE ? OR p.bulan LIKE ? OR p.tahun LIKE ?) "
                   + (idKaryawanFilter > 0 ? "AND p.id_karyawan=? " : "")
                   + "ORDER BY p.id_gaji DESC";
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            String key = "%" + keyword + "%";
            ps.setString(1, key); ps.setString(2, key); ps.setString(3, key);
            if (idKaryawanFilter > 0) ps.setInt(4, idKaryawanFilter);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{rs.getInt("id_gaji"), rs.getString("nama_karyawan"), rs.getString("bulan"), rs.getString("tahun"), rs.getDouble("gaji_pokok"), rs.getDouble("tunjangan"), rs.getDouble("potongan"), rs.getDouble("total_gaji"), rs.getString("keterangan")});
                }
            }
        }
        return model;
    }
}
