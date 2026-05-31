package dao;

import config.Koneksi;
import java.sql.*;
import javax.swing.table.DefaultTableModel;
import model.Tunjangan;

public class TunjanganDAO {
    public void insert(Tunjangan t) throws SQLException {
        String sql = "INSERT INTO tunjangan (nama_tunjangan, nominal_tunjangan, id_jabatan, keterangan) VALUES (?, ?, ?, ?)";
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, t.getNamaTunjangan());
            ps.setDouble(2, t.getNominalTunjangan());
            ps.setInt(3, t.getIdJabatan());
            ps.setString(4, t.getKeterangan());
            ps.executeUpdate();
        }
    }

    public void update(Tunjangan t) throws SQLException {
        String sql = "UPDATE tunjangan SET nama_tunjangan=?, nominal_tunjangan=?, id_jabatan=?, keterangan=? WHERE id_tunjangan=?";
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, t.getNamaTunjangan());
            ps.setDouble(2, t.getNominalTunjangan());
            ps.setInt(3, t.getIdJabatan());
            ps.setString(4, t.getKeterangan());
            ps.setInt(5, t.getIdTunjangan());
            ps.executeUpdate();
        }
    }

    public void delete(int idTunjangan) throws SQLException {
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM tunjangan WHERE id_tunjangan=?")) {
            ps.setInt(1, idTunjangan);
            ps.executeUpdate();
        }
    }

    public DefaultTableModel getAll() throws SQLException { return search(""); }

    public DefaultTableModel search(String keyword) throws SQLException {
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID Tunjangan", "Nama Tunjangan", "Nominal", "Jabatan", "Keterangan"}, 0);
        String sql = "SELECT t.*, j.nama_jabatan FROM tunjangan t LEFT JOIN jabatan j ON t.id_jabatan=j.id_jabatan "
                   + "WHERE t.nama_tunjangan LIKE ? OR j.nama_jabatan LIKE ? ORDER BY t.id_tunjangan DESC";
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            String key = "%" + keyword + "%";
            ps.setString(1, key);
            ps.setString(2, key);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{rs.getInt("id_tunjangan"), rs.getString("nama_tunjangan"), rs.getDouble("nominal_tunjangan"), rs.getString("nama_jabatan"), rs.getString("keterangan")});
                }
            }
        }
        return model;
    }

    public double getTotalTunjanganByKaryawan(int idKaryawan) throws SQLException {
        String sql = "SELECT COALESCE(SUM(t.nominal_tunjangan),0) AS total FROM karyawan k JOIN tunjangan t ON k.id_jabatan=t.id_jabatan WHERE k.id_karyawan=?";
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idKaryawan);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("total");
            }
        }
        return 0;
    }
}
