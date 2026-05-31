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
    
    tblKaryawan.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            ambilDataDariTabel();
        }
    });
    
    btnRefresh.addActionListener(new java.awt.event.ActionListener() {
        public void actionPerformed(java.awt.event.ActionEvent evt) {
            tampilData();
            resetForm();
        }
    });
}
   
   private void styleButton() {
    styleFilledButton(btnSimpan, new java.awt.Color(20, 184, 166), java.awt.Color.WHITE);
    styleFilledButton(btnUbah, new java.awt.Color(37, 99, 235), java.awt.Color.WHITE);
    styleFilledButton(btnHapus, new java.awt.Color(239, 68, 68), java.awt.Color.WHITE);
    styleFilledButton(btnReset, new java.awt.Color(243, 244, 246), new java.awt.Color(17, 24, 39));
}

private void styleFilledButton(javax.swing.JButton button, java.awt.Color bg, java.awt.Color fg) {
    button.setUI(new javax.swing.plaf.basic.BasicButtonUI());
    button.setBackground(bg);
    button.setForeground(fg);
    button.setOpaque(true);
    button.setContentAreaFilled(true);
    button.setBorderPainted(false);
    button.setFocusPainted(false);
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
    DefaultTableModel model = new DefaultTableModel();
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
                String sqlUser = "INSERT INTO users (id_karyawan, username, password, role) VALUES (?, ?, ?, 'KARYAWAN')";
                PreparedStatement pstUser = conn.prepareStatement(sqlUser);
                pstUser.setInt(1, idKaryawan);
                pstUser.setString(2, username);
                pstUser.setString(3, password);
                pstUser.executeUpdate();
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

        JOptionPane.showMessageDialog(this, "Data karyawan berhasil diubah.");
        tampilData();
        resetForm();
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal mengubah: " + e.getMessage());
    }
}

private void hapusData() {
    String idKaryawan = txtIdKaryawan.getText().trim();
    if (idKaryawan.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Pilih data dari tabel dulu.");
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
            JOptionPane.showMessageDialog(this, "Gagal menghapus: " + e.getMessage());
        }
    }
}

private void cariData() {
    DefaultTableModel model = new DefaultTableModel();
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

private void ambilDataDariTabel() {
    int baris = tblKaryawan.getSelectedRow();
    if (baris >= 0) {
        txtIdKaryawan.setText(tblKaryawan.getValueAt(baris, 0).toString());
        txtNik.setText(tblKaryawan.getValueAt(baris, 1).toString());
        txtNamaKaryawan.setText(tblKaryawan.getValueAt(baris, 2).toString());
        cmbJenisKelamin.setSelectedItem(tblKaryawan.getValueAt(baris, 3).toString());
        txtNoHp.setText(tblKaryawan.getValueAt(baris, 4) != null ? tblKaryawan.getValueAt(baris, 4).toString() : "");
        cmbJabatan.setSelectedItem(tblKaryawan.getValueAt(baris, 5) != null ? tblKaryawan.getValueAt(baris, 5).toString() : "");
        cmbDivisi.setSelectedItem(tblKaryawan.getValueAt(baris, 6) != null ? tblKaryawan.getValueAt(baris, 6).toString() : "");

        btnSimpan.setEnabled(false);
        btnUbah.setEnabled(true);
        btnHapus.setEnabled(true);
    }
}

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code manually kalau masih mau pakai tab Design NetBeans.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel3 = new javax.swing.JPanel();
        txtCari = new javax.swing.JTextField();
        btnCari = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblKaryawan = new javax.swing.JTable();
        btnRefresh = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        txtIdKaryawan = new javax.swing.JTextField();
        txtNik = new javax.swing.JTextField();
        txtNamaKaryawan = new javax.swing.JTextField();
        txtNoHp = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        txtUsername = new javax.swing.JTextField();
        txtPassword = new javax.swing.JTextField();
        btnReset = new javax.swing.JButton();
        btnHapus = new javax.swing.JButton();
        btnUbah = new javax.swing.JButton();
        btnSimpan = new javax.swing.JButton();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        cmbJenisKelamin = new javax.swing.JComboBox<>();
        cmbJabatan = new javax.swing.JComboBox<>();
        cmbDivisi = new javax.swing.JComboBox<>();

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        btnCari.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnCari.setText("Cari");
        btnCari.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCariActionPerformed(evt);
            }
        });

        tblKaryawan.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane1.setViewportView(tblKaryawan);

        btnRefresh.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnRefresh.setText("Refresh");
        btnRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRefreshActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addComponent(txtCari, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCari)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnRefresh)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtCari, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnCari)
                    .addComponent(btnRefresh))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(44, Short.MAX_VALUE))
        );

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(0, 51, 102));
        jLabel1.setText("Data Karyawan");

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Form Karyawan", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI", 0, 12), new java.awt.Color(0, 51, 102))); // NOI18N

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel5.setText("Nama Karyawan");

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel4.setText("Username");

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel3.setText("NIK");

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel2.setText("ID Karyawan");

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel6.setText("No. HP");

        txtIdKaryawan.setEditable(false);

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel7.setText("Password");

        btnReset.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnReset.setText("Reset");
        btnReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnResetActionPerformed(evt);
            }
        });

        btnHapus.setBackground(new java.awt.Color(255, 51, 51));
        btnHapus.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnHapus.setForeground(new java.awt.Color(255, 255, 255));
        btnHapus.setText("Hapus");
        btnHapus.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHapusActionPerformed(evt);
            }
        });

        btnUbah.setBackground(new java.awt.Color(102, 102, 255));
        btnUbah.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnUbah.setForeground(new java.awt.Color(255, 255, 255));
        btnUbah.setText("Ubah");
        btnUbah.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUbahActionPerformed(evt);
            }
        });

        btnSimpan.setBackground(new java.awt.Color(51, 255, 204));
        btnSimpan.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnSimpan.setForeground(new java.awt.Color(255, 255, 255));
        btnSimpan.setText("Simpan");
        btnSimpan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSimpanActionPerformed(evt);
            }
        });

        jLabel9.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel9.setText("Jenis Kelamin");

        jLabel10.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel10.setText("Jabatan");

        jLabel11.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel11.setText("Divisi");

        cmbJenisKelamin.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Laki-Laki", "Perempuan" }));
        cmbJenisKelamin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbJenisKelaminActionPerformed(evt);
            }
        });

        cmbJabatan.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Bos", "Presiden", "Bahlil" }));

        cmbDivisi.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Perjamuan", "Percintaan", "Pertempuran" }));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnSimpan)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnUbah)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnHapus)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnReset)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                        .addGap(22, 22, 22)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel5)
                                    .addComponent(jLabel3)
                                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGap(12, 12, 12))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel10)
                                    .addComponent(jLabel9)
                                    .addComponent(jLabel11)
                                    .addComponent(jLabel4)
                                    .addComponent(jLabel7)
                                    .addComponent(jLabel6))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(txtIdKaryawan)
                            .addComponent(txtNik, javax.swing.GroupLayout.DEFAULT_SIZE, 578, Short.MAX_VALUE)
                            .addComponent(txtNamaKaryawan, javax.swing.GroupLayout.DEFAULT_SIZE, 578, Short.MAX_VALUE)
                            .addComponent(cmbJenisKelamin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addComponent(cmbJabatan, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(cmbDivisi, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addComponent(txtNoHp)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap(127, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(txtUsername, javax.swing.GroupLayout.PREFERRED_SIZE, 578, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(txtPassword, javax.swing.GroupLayout.PREFERRED_SIZE, 578, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(14, 14, 14))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(txtIdKaryawan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(txtNik, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(txtNamaKaryawan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cmbJenisKelamin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(txtNoHp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cmbJabatan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cmbDivisi, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel11))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(txtUsername, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(txtPassword, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 43, Short.MAX_VALUE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnReset)
                    .addComponent(btnHapus)
                    .addComponent(btnUbah)
                    .addComponent(btnSimpan))
                .addGap(25, 25, 25))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(36, 36, 36)
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 182, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(23, 23, 23)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap(24, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
