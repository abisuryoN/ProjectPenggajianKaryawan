package dao;

import config.Koneksi;
import java.sql.*;
import javax.swing.table.DefaultTableModel;
import model.Divisi;

public class DivisiDAO {
    public void insert(Divisi d) throws SQLException {
        String sql = "INSERT INTO divisi (nama_divisi, kepala_divisi, keterangan) VALUES (?, ?, ?)";
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, d.getNamaDivisi());
            ps.setString(2, d.getKepalaDivisi());
            ps.setString(3, d.getKeterangan());
            ps.executeUpdate();
        }
    }

    public void update(Divisi d) throws SQLException {
        String sql = "UPDATE divisi SET nama_divisi=?, kepala_divisi=?, keterangan=? WHERE id_divisi=?";
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, d.getNamaDivisi());
            ps.setString(2, d.getKepalaDivisi());
            ps.setString(3, d.getKeterangan());
            ps.setInt(4, d.getIdDivisi());
            ps.executeUpdate();
        }
    }

    public void delete(int idDivisi) throws SQLException {
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM divisi WHERE id_divisi=?")) {
            ps.setInt(1, idDivisi);
            ps.executeUpdate();
        }
    }

    public DefaultTableModel getAll() throws SQLException {
        return search("");
    }

    public DefaultTableModel search(String keyword) throws SQLException {
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID Divisi", "Nama Divisi", "Kepala Divisi", "Keterangan"}, 0);
        String sql = "SELECT * FROM divisi WHERE nama_divisi LIKE ? OR kepala_divisi LIKE ? ORDER BY id_divisi DESC";
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            String key = "%" + keyword + "%";
            ps.setString(1, key);
            ps.setString(2, key);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{rs.getInt("id_divisi"), rs.getString("nama_divisi"), rs.getString("kepala_divisi"), rs.getString("keterangan")});
                }
            }
        }
        return model;
    }
}
