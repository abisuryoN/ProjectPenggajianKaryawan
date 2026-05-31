package dao;

import config.Koneksi;
import java.sql.*;
import javax.swing.table.DefaultTableModel;
import model.User;

public class UserDAO {
    public User login(String username, String password) throws SQLException {
        String sql = "SELECT u.*, k.nama_karyawan FROM users u LEFT JOIN karyawan k ON u.id_karyawan=k.id_karyawan WHERE u.username=? AND u.password=?";
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User u = new User();
                    u.setIdUser(rs.getInt("id_user"));
                    u.setIdKaryawan(rs.getInt("id_karyawan"));
                    u.setNamaKaryawan(rs.getString("nama_karyawan"));
                    u.setUsername(rs.getString("username"));
                    u.setPassword(rs.getString("password"));
                    u.setRole(rs.getString("role"));
                    return u;
                }
            }
        }
        return null;
    }

    public void insert(User u) throws SQLException {
        String sql = "INSERT INTO users (id_karyawan, username, password, role) VALUES (?, ?, ?, ?)";
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            if (u.getIdKaryawan() == 0) ps.setNull(1, Types.INTEGER); else ps.setInt(1, u.getIdKaryawan());
            ps.setString(2, u.getUsername());
            ps.setString(3, u.getPassword());
            ps.setString(4, u.getRole());
            ps.executeUpdate();
        }
    }

    public void update(User u) throws SQLException {
        String sql = "UPDATE users SET username=?, password=?, role=? WHERE id_user=?";
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPassword());
            ps.setString(3, u.getRole());
            ps.setInt(4, u.getIdUser());
            ps.executeUpdate();
        }
    }

    public void delete(int idUser) throws SQLException {
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id_user=?")) {
            ps.setInt(1, idUser);
            ps.executeUpdate();
        }
    }

    public DefaultTableModel getAll() throws SQLException {
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID User", "Nama Karyawan", "Username", "Role"}, 0);
        String sql = "SELECT u.id_user, k.nama_karyawan, u.username, u.role FROM users u LEFT JOIN karyawan k ON u.id_karyawan=k.id_karyawan ORDER BY u.id_user DESC";
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                model.addRow(new Object[]{rs.getInt("id_user"), rs.getString("nama_karyawan"), rs.getString("username"), rs.getString("role")});
            }
        }
        return model;
    }
}
