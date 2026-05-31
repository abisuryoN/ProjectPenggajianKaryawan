package dao;

import config.Koneksi;
import java.sql.*;
import javax.swing.table.DefaultTableModel;
import model.Jabatan;

public class JabatanDAO {
    public void insert(Jabatan j) throws SQLException {
        String sql = "INSERT INTO jabatan (nama_jabatan, gaji_pokok, keterangan) VALUES (?, ?, ?)";
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, j.getNamaJabatan());
            ps.setDouble(2, j.getGajiPokok());
            ps.setString(3, j.getKeterangan());
            ps.executeUpdate();
        }
    }

    public void update(Jabatan j) throws SQLException {
        String sql = "UPDATE jabatan SET nama_jabatan=?, gaji_pokok=?, keterangan=? WHERE id_jabatan=?";
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, j.getNamaJabatan());
            ps.setDouble(2, j.getGajiPokok());
            ps.setString(3, j.getKeterangan());
            ps.setInt(4, j.getIdJabatan());
            ps.executeUpdate();
        }
    }

    public void delete(int idJabatan) throws SQLException {
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM jabatan WHERE id_jabatan=?")) {
            ps.setInt(1, idJabatan);
            ps.executeUpdate();
        }
    }

    public DefaultTableModel getAll() throws SQLException { return search(""); }

    public DefaultTableModel search(String keyword) throws SQLException {
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID Jabatan", "Nama Jabatan", "Gaji Pokok", "Keterangan"}, 0);
        String sql = "SELECT * FROM jabatan WHERE nama_jabatan LIKE ? OR keterangan LIKE ? ORDER BY id_jabatan DESC";
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            String key = "%" + keyword + "%";
            ps.setString(1, key);
            ps.setString(2, key);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{rs.getInt("id_jabatan"), rs.getString("nama_jabatan"), rs.getDouble("gaji_pokok"), rs.getString("keterangan")});
                }
            }
        }
        return model;
    }

    public double getGajiPokokByKaryawan(int idKaryawan) throws SQLException {
        String sql = "SELECT j.gaji_pokok FROM karyawan k JOIN jabatan j ON k.id_jabatan=j.id_jabatan WHERE k.id_karyawan=?";
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idKaryawan);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("gaji_pokok");
            }
        }
        return 0;
    }
}
