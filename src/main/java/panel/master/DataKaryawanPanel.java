package panel.master;
import config.Koneksi;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 * Panel kosong untuk diedit lewat NetBeans GUI Builder.
 * Buka tab Design, lalu drag komponen sesuai kebutuhan.
 */
public class DataKaryawanPanel extends javax.swing.JPanel {

    public DataKaryawanPanel() {
        initComponents();
        styleButton();
        loadJabatan();
        loadDivisi();
        tampilData();
        resetForm();
        util.DesignUtil.applyPage(this);
    }


   
   private void styleButton() {
        util.DesignUtil.styleFilledButton(btnSimpan, util.DesignUtil.TOMBOL_SEKUNDER, java.awt.Color.WHITE);
        util.DesignUtil.styleFilledButton(btnUbah, util.DesignUtil.TOMBOL_UTAMA, java.awt.Color.WHITE);
        util.DesignUtil.styleFilledButton(btnHapus, util.DesignUtil.TOMBOL_BAHAYA, java.awt.Color.WHITE);
        util.DesignUtil.styleFilledButton(btnReset, util.DesignUtil.TOMBOL_NETRAL, util.DesignUtil.TEKS_GELAP);
        util.DesignUtil.styleFilledButton(btnCari, util.DesignUtil.TOMBOL_UTAMA, java.awt.Color.WHITE);
        util.DesignUtil.styleFilledButton(btnRefresh, util.DesignUtil.TOMBOL_NETRAL, util.DesignUtil.TEKS_GELAP);
    }

private void loadJabatan() {
    cmbJabatan.removeAllItems();
    cmbJabatan.addItem("-- Pilih jabatan --");
    try {
        String sql = "SELECT nama_jabatan FROM jabatan ORDER BY nama_jabatan";
        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement(sql);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            cmbJabatan.addItem(rs.getString("nama_jabatan"));
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal load jabatan: " + e.getMessage());
    }
}

private void loadDivisi() {
    cmbDivisi.removeAllItems();
    cmbDivisi.addItem("-- Pilih divisi --");
    try {
        String sql = "SELECT nama_divisi FROM divisi ORDER BY nama_divisi";
        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement(sql);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            cmbDivisi.addItem(rs.getString("nama_divisi"));
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal load divisi: " + e.getMessage());
    }
}

private void tampilData() {
    DefaultTableModel model = new DefaultTableModel() {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    model.addColumn("ID");
    model.addColumn("NIK");
    model.addColumn("Nama Karyawan");
    model.addColumn("Jenis Kelamin");
    model.addColumn("No. HP");
    model.addColumn("Jabatan");
    model.addColumn("Divisi");

    try {
        String sql = "SELECT k.id_karyawan, k.nik, k.nama_karyawan, k.jenis_kelamin, "
                + "k.no_hp, j.nama_jabatan, d.nama_divisi "
                + "FROM karyawan k "
                + "LEFT JOIN jabatan j ON k.id_jabatan = j.id_jabatan "
                + "LEFT JOIN divisi d ON k.id_divisi = d.id_divisi "
                + "ORDER BY k.id_karyawan ASC";

        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement(sql);
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            model.addRow(new Object[]{
                rs.getString("id_karyawan"),
                rs.getString("nik"),
                rs.getString("nama_karyawan"),
                rs.getString("jenis_kelamin"),
                rs.getString("no_hp"),
                rs.getString("nama_jabatan"),
                rs.getString("nama_divisi")
            });
        }
        tblKaryawan.setModel(model);
        util.DesignUtil.applyTable(tblKaryawan);
        util.DesignUtil.applyTable(tblKaryawan);
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal menampilkan data: " + e.getMessage());
    }
}

private int getIdJabatan(String namaJabatan) throws Exception {
    String sql = "SELECT id_jabatan FROM jabatan WHERE nama_jabatan = ?";
    Connection conn = Koneksi.getConnection();
    PreparedStatement pst = conn.prepareStatement(sql);
    pst.setString(1, namaJabatan);
    ResultSet rs = pst.executeQuery();
    if (rs.next()) return rs.getInt("id_jabatan");
    return 0;
}

private int getIdDivisi(String namaDivisi) throws Exception {
    String sql = "SELECT id_divisi FROM divisi WHERE nama_divisi = ?";
    Connection conn = Koneksi.getConnection();
    PreparedStatement pst = conn.prepareStatement(sql);
    pst.setString(1, namaDivisi);
    ResultSet rs = pst.executeQuery();
    if (rs.next()) return rs.getInt("id_divisi");
    return 0;
}

private void simpanData() {
    String nik = txtNik.getText().trim();
    String nama = txtNamaKaryawan.getText().trim();
    String jenisKelamin = cmbJenisKelamin.getSelectedItem().toString();
    String noHp = txtNoHp.getText().trim();
    String jabatan = cmbJabatan.getSelectedItem().toString();
    String divisi = cmbDivisi.getSelectedItem().toString();
    String username = txtUsername.getText().trim();
    String password = txtPassword.getText().trim();

    if (nik.isEmpty() || nama.isEmpty()) {
        JOptionPane.showMessageDialog(this, "NIK dan Nama wajib diisi.");
        return;
    }
    if (cmbJabatan.getSelectedIndex() == 0) {
        JOptionPane.showMessageDialog(this, "Pilih jabatan dulu.");
        return;
    }
    if (cmbDivisi.getSelectedIndex() == 0) {
        JOptionPane.showMessageDialog(this, "Pilih divisi dulu.");
        return;
    }

    try {
        int idJabatan = getIdJabatan(jabatan);
        int idDivisi = getIdDivisi(divisi);

        String sql = "INSERT INTO karyawan (nik, nama_karyawan, jenis_kelamin, no_hp, id_jabatan, id_divisi) "
                + "VALUES (?, ?, ?, ?, ?, ?)";
        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS);
        pst.setString(1, nik);
        pst.setString(2, nama);
        pst.setString(3, jenisKelamin);
        pst.setString(4, noHp);
        pst.setInt(5, idJabatan);
        pst.setInt(6, idDivisi);
        pst.executeUpdate();


        if (!username.isEmpty() && !password.isEmpty()) {
            ResultSet generatedKeys = pst.getGeneratedKeys();
            if (generatedKeys.next()) {
                int idKaryawan = generatedKeys.getInt(1);
                // Pakai aturan yang sama dengan ubahData (cek duplikat dsb).
                simpanAkunLogin(conn, idKaryawan, username, password);
            }
        }       

        JOptionPane.showMessageDialog(this, "Data karyawan berhasil disimpan.");
        tampilData();
        resetForm();
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal menyimpan: " + e.getMessage());
    }
}

private void ubahData() {
    String idKaryawan = txtIdKaryawan.getText().trim();
    String nik = txtNik.getText().trim();
    String nama = txtNamaKaryawan.getText().trim();
    String jenisKelamin = cmbJenisKelamin.getSelectedItem().toString();
    String noHp = txtNoHp.getText().trim();
    String jabatan = cmbJabatan.getSelectedItem().toString();
    String divisi = cmbDivisi.getSelectedItem().toString();
    String username = txtUsername.getText().trim();
    String password = txtPassword.getText().trim();

    if (idKaryawan.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Pilih data dari tabel dulu.");
        return;
    }

    try {
        int idJabatan = getIdJabatan(jabatan);
        int idDivisi = getIdDivisi(divisi);

        String sql = "UPDATE karyawan SET nik=?, nama_karyawan=?, jenis_kelamin=?, no_hp=?, id_jabatan=?, id_divisi=? WHERE id_karyawan=?";
        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setString(1, nik);
        pst.setString(2, nama);
        pst.setString(3, jenisKelamin);
        pst.setString(4, noHp);
        pst.setInt(5, idJabatan);
        pst.setInt(6, idDivisi);
        pst.setString(7, idKaryawan);
        pst.executeUpdate();

        // Akun login disimpan di tabel users, BUKAN di tabel karyawan. Tanpa
        // langkah ini, username/password yang diubah di form tidak tersimpan
        // sehingga karyawan tidak bisa login dengan akun baru.
        simpanAkunLogin(conn, Integer.parseInt(idKaryawan), username, password);

        JOptionPane.showMessageDialog(this, "Data karyawan berhasil diubah.");
        tampilData();
        resetForm();
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal mengubah: " + e.getMessage());
    }
}

/**
 * Terapkan perubahan username/password ke tabel users.
 *
 * <p>Aturan:
 * <ul>
 *   <li>Username tidak berubah DAN password kosong -> akun tidak diubah.</li>
 *   <li>Username berubah -> password wajib diisi.</li>
 *   <li>Password diisi -> password diperbarui (username ikut diperbarui bila diubah).</li>
 *   <li>Username yang dipakai akun lain -> ditolak dengan pesan jelas.</li>
 *   <li>Belum punya akun dan kedua kolom kosong -> tidak dibuatkan akun.</li>
 * </ul>
 */
private void simpanAkunLogin(Connection conn, int idKaryawan, String usernameBaru, String passwordBaru)
        throws Exception {
    String usernameLama = "";
    try (PreparedStatement cek = conn.prepareStatement(
            "SELECT username FROM users WHERE id_karyawan = ?")) {
        cek.setInt(1, idKaryawan);
        try (ResultSet rs = cek.executeQuery()) {
            if (rs.next()) usernameLama = rs.getString("username");
        }
    }
    if (usernameLama == null) usernameLama = "";
    boolean punyaAkun = !usernameLama.isEmpty();

    if (usernameBaru.isEmpty()) {
        if (passwordBaru.isEmpty()) {
            return; // tidak ada perubahan akun
        }
        if (!punyaAkun) {
            throw new IllegalArgumentException(
                "Isi username dulu sebelum menentukan password.");
        }
        usernameBaru = usernameLama; // ganti password saja
    }
    boolean usernameBerubah = !usernameBaru.equals(usernameLama);
    if (!punyaAkun && passwordBaru.isEmpty()) {
        throw new IllegalArgumentException(
            "Isi password untuk akun baru \"" + usernameBaru + "\".");
    }
    if (punyaAkun && usernameBerubah && passwordBaru.isEmpty()) {
        throw new IllegalArgumentException(
            "Isi password baru karena username diubah menjadi \"" + usernameBaru + "\".");
    }
    if (!punyaAkun && !passwordBaru.isEmpty()) {
        // akun baru: pakai passwordBaru apa adanya
    }
    if (punyaAkun && passwordBaru.isEmpty() && !usernameBerubah) {
        return; // tidak ada yang berubah
    }

    // Username tidak boleh dipakai akun lain (termasuk akun HRD).
    try (PreparedStatement cek = conn.prepareStatement(
            "SELECT id_karyawan FROM users WHERE username = ?")) {
        cek.setString(1, usernameBaru);
        try (ResultSet rs = cek.executeQuery()) {
            if (rs.next()) {
                int pemilik = rs.getInt("id_karyawan");
                boolean milikSendiri = punyaAkun && !rs.wasNull() && pemilik == idKaryawan;
                if (!milikSendiri) {
                    throw new IllegalArgumentException(
                        "Username \"" + usernameBaru + "\" sudah dipakai akun lain.");
                }
            }
        }
    }

    if (punyaAkun) {
        if (passwordBaru.isEmpty()) {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE users SET username = ? WHERE id_karyawan = ?")) {
                ps.setString(1, usernameBaru);
                ps.setInt(2, idKaryawan);
                ps.executeUpdate();
            }
        } else {
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE users SET username = ?, password = ? WHERE id_karyawan = ?")) {
                ps.setString(1, usernameBaru);
                ps.setString(2, passwordBaru);
                ps.setInt(3, idKaryawan);
                ps.executeUpdate();
            }
        }
    } else {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO users (id_karyawan, username, password, role) "
                + "VALUES (?, ?, ?, 'KARYAWAN')")) {
            ps.setInt(1, idKaryawan);
            ps.setString(2, usernameBaru);
            ps.setString(3, passwordBaru);
            ps.executeUpdate();
        }
    }
}

private void hapusData() {
    String idKaryawan = txtIdKaryawan.getText().trim();
    if (idKaryawan.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Pilih data dari tabel dulu.");
        return;
    }

    String penahan = cekPemakaianKaryawan(idKaryawan);
    if (penahan != null) {
        JOptionPane.showMessageDialog(this, penahan, "Tidak Bisa Dihapus", JOptionPane.WARNING_MESSAGE);
        return;
    }

    int konfirmasi = JOptionPane.showConfirmDialog(this,
        "Yakin ingin menghapus karyawan ini?", "Konfirmasi", JOptionPane.YES_NO_OPTION);

    if (konfirmasi == JOptionPane.YES_OPTION) {
        try {
            Connection conn = Koneksi.getConnection();
            // Hapus user dulu
            PreparedStatement pstUser = conn.prepareStatement("DELETE FROM users WHERE id_karyawan=?");
            pstUser.setString(1, idKaryawan);
            pstUser.executeUpdate();

            // Baru hapus karyawan
            PreparedStatement pst = conn.prepareStatement("DELETE FROM karyawan WHERE id_karyawan=?");
            pst.setString(1, idKaryawan);
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this, "Data karyawan berhasil dihapus.");
            tampilData();
            resetForm();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal menghapus: " + pesanHapusAman(e));
        }
    }
}

private String cekPemakaianKaryawan(String idKaryawan) {
    try {
        Connection conn = Koneksi.getConnection();
        int jmlAbsensi = hitungBaris(conn, "SELECT COUNT(*) FROM absensi WHERE id_karyawan=?", idKaryawan);
        int jmlGaji = hitungBaris(conn, "SELECT COUNT(*) FROM penggajian WHERE id_karyawan=?", idKaryawan);
        if (jmlAbsensi > 0 || jmlGaji > 0) {
            StringBuilder sb = new StringBuilder("Karyawan ini tidak bisa dihapus karena masih dipakai:\n");
            if (jmlAbsensi > 0) sb.append("- ").append(jmlAbsensi).append(" data absensi\n");
            if (jmlGaji > 0) sb.append("- ").append(jmlGaji).append(" data penggajian\n");
            sb.append("Hubungi HRD bila data terkaitnya memang harus dihapus dulu.");
            return sb.toString();
        }
    } catch (Exception e) {
        return "Gagal memeriksa data terkait: " + e.getMessage();
    }
    return null;
}

private int hitungBaris(Connection conn, String sql, String id) throws Exception {
    PreparedStatement pst = conn.prepareStatement(sql);
    pst.setString(1, id);
    ResultSet rs = pst.executeQuery();
    return rs.next() ? rs.getInt(1) : 0;
}

private String pesanHapusAman(Exception e) {
    String msg = e.getMessage();
    if (msg != null && (msg.contains("foreign key") || msg.contains("Cannot delete"))) {
        return "Data masih dipakai di tabel lain, jadi tidak bisa dihapus.";
    }
    return msg;
}

private void cariData() {
    DefaultTableModel model = new DefaultTableModel() {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
    model.addColumn("ID");
    model.addColumn("NIK");
    model.addColumn("Nama Karyawan");
    model.addColumn("Jenis Kelamin");
    model.addColumn("No. HP");
    model.addColumn("Jabatan");
    model.addColumn("Divisi");

    String keyword = txtCari.getText().trim();
    try {
        String sql = "SELECT k.id_karyawan, k.nik, k.nama_karyawan, k.jenis_kelamin, "
                + "k.no_hp, j.nama_jabatan, d.nama_divisi "
                + "FROM karyawan k "
                + "LEFT JOIN jabatan j ON k.id_jabatan = j.id_jabatan "
                + "LEFT JOIN divisi d ON k.id_divisi = d.id_divisi "
                + "WHERE k.nik LIKE ? OR k.nama_karyawan LIKE ? "
                + "OR j.nama_jabatan LIKE ? OR d.nama_divisi LIKE ? "
                + "ORDER BY k.id_karyawan ASC";

        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setString(1, "%" + keyword + "%");
        pst.setString(2, "%" + keyword + "%");
        pst.setString(3, "%" + keyword + "%");
        pst.setString(4, "%" + keyword + "%");
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            model.addRow(new Object[]{
                rs.getString("id_karyawan"),
                rs.getString("nik"),
                rs.getString("nama_karyawan"),
                rs.getString("jenis_kelamin"),
                rs.getString("no_hp"),
                rs.getString("nama_jabatan"),
                rs.getString("nama_divisi")
            });
        }
        tblKaryawan.setModel(model);
        util.DesignUtil.applyTable(tblKaryawan);
        util.DesignUtil.applyTable(tblKaryawan);
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal mencari: " + e.getMessage());
    }
}

private void resetForm() {
    txtIdKaryawan.setText("");
    txtNik.setText("");
    txtNamaKaryawan.setText("");
    txtNoHp.setText("");
    txtUsername.setText("");
    txtPassword.setText("");
    cmbJenisKelamin.setSelectedIndex(0);
    cmbJabatan.setSelectedIndex(0);
    cmbDivisi.setSelectedIndex(0);

    btnSimpan.setEnabled(true);
    btnUbah.setEnabled(false);
    btnHapus.setEnabled(false);
}

private String safe(Object v) {
    return v == null ? "" : v.toString();
}

private void ambilDataDariTabel() {
    int baris = tblKaryawan.getSelectedRow();
    if (baris >= 0) {
        txtIdKaryawan.setText(safe(tblKaryawan.getValueAt(baris, 0)));
        txtNik.setText(safe(tblKaryawan.getValueAt(baris, 1)));
        txtNamaKaryawan.setText(safe(tblKaryawan.getValueAt(baris, 2)));
        cmbJenisKelamin.setSelectedItem(safe(tblKaryawan.getValueAt(baris, 3)));
        txtNoHp.setText(safe(tblKaryawan.getValueAt(baris, 4)));
        cmbJabatan.setSelectedItem(safe(tblKaryawan.getValueAt(baris, 5)));
        cmbDivisi.setSelectedItem(safe(tblKaryawan.getValueAt(baris, 6)));

        // Tampilkan username yang SUDAH tertaut agar HRD tidak menimpa akun
        // yang salah. Password sengaja dikosongkan (tidak boleh tampil di
        // layar); kolom kosong berarti password TIDAK diubah.
        txtUsername.setText("");
        txtPassword.setText("");
        muatUsernameTertaut();

        btnSimpan.setEnabled(false);
        btnUbah.setEnabled(true);
        btnHapus.setEnabled(true);
    }
}

/** Isi field username dengan akun yang sudah tertaut (kosong bila belum ada). */
private void muatUsernameTertaut() {
    String idKaryawan = txtIdKaryawan.getText().trim();
    if (idKaryawan.isEmpty()) return;
    try (Connection conn = Koneksi.getConnection();
         PreparedStatement ps = conn.prepareStatement(
                 "SELECT username FROM users WHERE id_karyawan = ?")) {
        ps.setInt(1, Integer.parseInt(idKaryawan));
        try (ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                txtUsername.setText(rs.getString("username"));
            }
        }
    } catch (Exception e) {
        // cukup biarkan kosong; perubahan akun tetap bisa dilakukan manual
    }
}

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code manually kalau masih mau pakai tab Design NetBeans.
     */
        @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jLabelForm = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        txtIdKaryawan = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        txtNik = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        txtNamaKaryawan = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        txtNoHp = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        txtUsername = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        txtPassword = new javax.swing.JPasswordField();
        jLabel9 = new javax.swing.JLabel();
        cmbJenisKelamin = new javax.swing.JComboBox<>();
        jLabel10 = new javax.swing.JLabel();
        cmbJabatan = new javax.swing.JComboBox<>();
        jLabel11 = new javax.swing.JLabel();
        cmbDivisi = new javax.swing.JComboBox<>();
        btnSimpan = new javax.swing.JButton();
        btnUbah = new javax.swing.JButton();
        btnHapus = new javax.swing.JButton();
        btnReset = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jLabelJudulTabel = new javax.swing.JLabel();
        txtCari = new javax.swing.JTextField();
        btnCari = new javax.swing.JButton();
        btnRefresh = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblKaryawan = new javax.swing.JTable();

        setBackground(new java.awt.Color(244, 247, 250));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(22, 48, 92)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("DATA KARYAWAN");

        jLabel8.setForeground(new java.awt.Color(75, 85, 99));
        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel8.setText("Kelola Data Karyawan");

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 226, 235)));

        jLabelForm.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabelForm.setForeground(new java.awt.Color(22, 48, 92)); // NOI18N
        jLabelForm.setText("Form Karyawan");

        jLabel2.setForeground(new java.awt.Color(75, 85, 99));
        jLabel2.setText("ID Karyawan");

        txtIdKaryawan.setEditable(false);
        txtIdKaryawan.setBackground(new java.awt.Color(243, 244, 246));

        jLabel3.setForeground(new java.awt.Color(75, 85, 99));
        jLabel3.setText("NIK");

        jLabel4.setForeground(new java.awt.Color(75, 85, 99));
        jLabel4.setText("Nama Karyawan");

        jLabel5.setForeground(new java.awt.Color(75, 85, 99));
        jLabel5.setText("No. HP");

        jLabel6.setForeground(new java.awt.Color(75, 85, 99));
        jLabel6.setText("Username");

        jLabel7.setForeground(new java.awt.Color(75, 85, 99));
        jLabel7.setText("Password");

        jLabel9.setForeground(new java.awt.Color(75, 85, 99));
        jLabel9.setText("Jenis Kelamin");

        cmbJenisKelamin.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "-- Pilih --", "Laki-laki", "Perempuan" }));
        cmbJenisKelamin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbJenisKelaminActionPerformed(evt);
            }
        });

        jLabel10.setForeground(new java.awt.Color(75, 85, 99));
        jLabel10.setText("Jabatan");

        jLabel11.setForeground(new java.awt.Color(75, 85, 99));
        jLabel11.setText("Divisi");

        btnSimpan.setBackground(new java.awt.Color(20, 184, 166));
        btnSimpan.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnSimpan.setForeground(new java.awt.Color(255, 255, 255));
        btnSimpan.setText("Simpan");
        btnSimpan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSimpanActionPerformed(evt);
            }
        });

        btnUbah.setBackground(new java.awt.Color(37, 99, 235));
        btnUbah.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnUbah.setForeground(new java.awt.Color(255, 255, 255));
        btnUbah.setText("Ubah");
        btnUbah.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUbahActionPerformed(evt);
            }
        });

        btnHapus.setBackground(new java.awt.Color(239, 68, 68));
        btnHapus.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnHapus.setForeground(new java.awt.Color(255, 255, 255));
        btnHapus.setText("Hapus");
        btnHapus.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHapusActionPerformed(evt);
            }
        });

        btnReset.setBackground(new java.awt.Color(243, 244, 246));
        btnReset.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnReset.setForeground(new java.awt.Color(17, 24, 39));
        btnReset.setText("Reset");
        btnReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnResetActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabelForm)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(12, 12, 12)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtIdKaryawan, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(txtNik, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(txtNamaKaryawan, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(20, 20, 20)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(12, 12, 12)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtNoHp, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(txtUsername, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(txtPassword, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(20, 20, 20)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel10, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel11, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(12, 12, 12)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(cmbJenisKelamin, 0, 180, Short.MAX_VALUE)
                            .addComponent(cmbJabatan, 0, 180, Short.MAX_VALUE)
                            .addComponent(cmbDivisi, 0, 180, Short.MAX_VALUE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(btnSimpan)
                        .addGap(10, 10, 10)
                        .addComponent(btnUbah)
                        .addGap(10, 10, 10)
                        .addComponent(btnHapus)
                        .addGap(10, 10, 10)
                        .addComponent(btnReset)))
                .addGap(16, 16, 16))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jLabelForm)
                .addGap(12, 12, 12)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(txtIdKaryawan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5)
                    .addComponent(txtNoHp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9)
                    .addComponent(cmbJenisKelamin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(txtNik, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6)
                    .addComponent(txtUsername, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10)
                    .addComponent(cmbJabatan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(txtNamaKaryawan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7)
                    .addComponent(txtPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel11)
                    .addComponent(cmbDivisi, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(14, 14, 14)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnSimpan)
                    .addComponent(btnUbah)
                    .addComponent(btnHapus)
                    .addComponent(btnReset))
                .addGap(16, 16, 16))
        );

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));
        jPanel3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 226, 235)));

        jLabelJudulTabel.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabelJudulTabel.setForeground(new java.awt.Color(22, 48, 92)); // NOI18N
        jLabelJudulTabel.setText("Daftar Karyawan");

        btnCari.setText("Cari");
        btnCari.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCariActionPerformed(evt);
            }
        });

        btnRefresh.setBackground(new java.awt.Color(243, 244, 246));
        btnRefresh.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnRefresh.setForeground(new java.awt.Color(17, 24, 39));
        btnRefresh.setText("Refresh");
        btnRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRefreshActionPerformed(evt);
            }
        });

        tblKaryawan.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null}
            },
            new String [] {
                "ID", "NIK", "Nama", "JK", "No HP", "Jabatan", "Divisi", "Username", "Password"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false
            };
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblKaryawan.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tblKaryawanMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(tblKaryawan);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabelJudulTabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(txtCari, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(8, 8, 8)
                        .addComponent(btnCari)
                        .addGap(8, 8, 8)
                        .addComponent(btnRefresh)))
                .addGap(16, 16, 16))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jLabelJudulTabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtCari, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnCari)
                    .addComponent(btnRefresh))
                .addGap(12, 12, 12)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 230, Short.MAX_VALUE)
                .addGap(16, 16, 16))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(2, 2, 2)
                .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(16, 16, 16))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnCariActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCariActionPerformed
        cariData();
    }//GEN-LAST:event_btnCariActionPerformed

    private void btnRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnRefreshActionPerformed

    private void btnHapusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHapusActionPerformed
        hapusData();
    }//GEN-LAST:event_btnHapusActionPerformed

    private void btnUbahActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUbahActionPerformed
        ubahData();
    }//GEN-LAST:event_btnUbahActionPerformed

    private void btnSimpanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSimpanActionPerformed
        simpanData();
    }//GEN-LAST:event_btnSimpanActionPerformed

    private void cmbJenisKelaminActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbJenisKelaminActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_cmbJenisKelaminActionPerformed

    private void btnResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnResetActionPerformed
        resetForm();
    }//GEN-LAST:event_btnResetActionPerformed



    private void tblKaryawanMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblKaryawanMouseClicked
        ambilDataDariTabel();
    }//GEN-LAST:event_tblKaryawanMouseClicked

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCari;
    private javax.swing.JButton btnHapus;
    private javax.swing.JButton btnRefresh;
    private javax.swing.JButton btnReset;
    private javax.swing.JButton btnSimpan;
    private javax.swing.JButton btnUbah;
    private javax.swing.JComboBox<String> cmbDivisi;
    private javax.swing.JComboBox<String> cmbJabatan;
    private javax.swing.JComboBox<String> cmbJenisKelamin;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabelForm;
    private javax.swing.JLabel jLabelJudulTabel;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable tblKaryawan;
    private javax.swing.JTextField txtCari;
    private javax.swing.JTextField txtIdKaryawan;
    private javax.swing.JTextField txtNamaKaryawan;
    private javax.swing.JTextField txtNik;
    private javax.swing.JTextField txtNoHp;
    private javax.swing.JTextField txtPassword;
    private javax.swing.JTextField txtUsername;
    // End of variables declaration//GEN-END:variables
}
