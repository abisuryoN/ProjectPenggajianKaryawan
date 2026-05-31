package dao;

import config.Koneksi;
import java.sql.*;
import javax.swing.table.DefaultTableModel;
import model.Karyawan;

public class KaryawanDAO {
    public void insert(Karyawan k) throws SQLException {
        String sqlKaryawan = "INSERT INTO karyawan (nik, nama_karyawan, jenis_kelamin, no_hp, alamat, id_jabatan, id_divisi) VALUES (?, ?, ?, ?, ?, ?, ?)";
        String sqlUser = "INSERT INTO users (id_karyawan, username, password, role) VALUES (?, ?, ?, 'KARYAWAN')";
        try (Connection c = Koneksi.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sqlKaryawan, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, k.getNik());
                ps.setString(2, k.getNamaKaryawan());
                ps.setString(3, k.getJenisKelamin());
                ps.setString(4, k.getNoHp());
                ps.setString(5, k.getAlamat());
                ps.setInt(6, k.getIdJabatan());
                ps.setInt(7, k.getIdDivisi());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) k.setIdKaryawan(keys.getInt(1));
                }
            }
            if (k.getUsername() != null && !k.getUsername().trim().isEmpty()) {
                try (PreparedStatement psUser = c.prepareStatement(sqlUser)) {
                    psUser.setInt(1, k.getIdKaryawan());
                    psUser.setString(2, k.getUsername());
                    psUser.setString(3, k.getPassword());
                    psUser.executeUpdate();
                }
            }
            c.commit();
        }
    }

    public void update(Karyawan k) throws SQLException {
        String sql = "UPDATE karyawan SET nik=?, nama_karyawan=?, jenis_kelamin=?, no_hp=?, alamat=?, id_jabatan=?, id_divisi=? WHERE id_karyawan=?";
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, k.getNik());
            ps.setString(2, k.getNamaKaryawan());
            ps.setString(3, k.getJenisKelamin());
            ps.setString(4, k.getNoHp());
            ps.setString(5, k.getAlamat());
            ps.setInt(6, k.getIdJabatan());
            ps.setInt(7, k.getIdDivisi());
            ps.setInt(8, k.getIdKaryawan());
            ps.executeUpdate();
        }
    }

    public void updateUser(int idKaryawan, String username, String password) throws SQLException {
        String cek = "SELECT id_user FROM users WHERE id_karyawan=?";
        try (Connection c = Koneksi.getConnection(); PreparedStatement psCek = c.prepareStatement(cek)) {
            psCek.setInt(1, idKaryawan);
            try (ResultSet rs = psCek.executeQuery()) {
                if (rs.next()) {
                    String sql = "UPDATE users SET username=?, password=? WHERE id_karyawan=?";
                    try (PreparedStatement ps = c.prepareStatement(sql)) {
                        ps.setString(1, username);
                        ps.setString(2, password);
                        ps.setInt(3, idKaryawan);
                        ps.executeUpdate();
                    }
                } else {
                    String sql = "INSERT INTO users (id_karyawan, username, password, role) VALUES (?, ?, ?, 'KARYAWAN')";
                    try (PreparedStatement ps = c.prepareStatement(sql)) {
                        ps.setInt(1, idKaryawan);
                        ps.setString(2, username);
                        ps.setString(3, password);
                        ps.executeUpdate();
                    }
                }
            }
        }
    }

    public void delete(int idKaryawan) throws SQLException {
        try (Connection c = Koneksi.getConnection()) {
            c.setAutoCommit(false);
            try (PreparedStatement psUser = c.prepareStatement("DELETE FROM users WHERE id_karyawan=?")) {
                psUser.setInt(1, idKaryawan);
                psUser.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement("DELETE FROM karyawan WHERE id_karyawan=?")) {
                ps.setInt(1, idKaryawan);
                ps.executeUpdate();
            }
            c.commit();
        }
    }

    public DefaultTableModel getAll() throws SQLException { return search(""); }

    public DefaultTableModel search(String keyword) throws SQLException {
        DefaultTableModel model = new DefaultTableModel(new String[]{"ID Karyawan", "NIK", "Nama Karyawan", "Jenis Kelamin", "No HP", "Alamat", "Jabatan", "Divisi", "Username"}, 0);
        String sql = "SELECT k.*, j.nama_jabatan, d.nama_divisi, u.username FROM karyawan k "
                   + "LEFT JOIN jabatan j ON k.id_jabatan=j.id_jabatan "
                   + "LEFT JOIN divisi d ON k.id_divisi=d.id_divisi "
                   + "LEFT JOIN users u ON k.id_karyawan=u.id_karyawan "
                   + "WHERE k.nik LIKE ? OR k.nama_karyawan LIKE ? OR j.nama_jabatan LIKE ? OR d.nama_divisi LIKE ? ORDER BY k.id_karyawan DESC";
        try (Connection c = Koneksi.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            String key = "%" + keyword + "%";
            ps.setString(1, key); ps.setString(2, key); ps.setString(3, key); ps.setString(4, key);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{rs.getInt("id_karyawan"), rs.getString("nik"), rs.getString("nama_karyawan"), rs.getString("jenis_kelamin"), rs.getString("no_hp"), rs.getString("alamat"), rs.getString("nama_jabatan"), rs.getString("nama_divisi"), rs.getString("username")});
                }
            }
        }
        return model;
    }
}
