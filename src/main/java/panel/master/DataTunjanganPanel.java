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
public class DataTunjanganPanel extends javax.swing.JPanel {

    public DataTunjanganPanel() {
        initComponents();
        loadJabatan();
        styleButton();
        tampilData();
        resetForm();
        util.DesignUtil.applyPage(this);
    }


    
    
    
    private void styleButton() {
        util.DesignUtil.styleFilledButton(jButton5, util.DesignUtil.TOMBOL_SEKUNDER, java.awt.Color.WHITE);
        util.DesignUtil.styleFilledButton(btnUbah, util.DesignUtil.TOMBOL_UTAMA, java.awt.Color.WHITE);
        util.DesignUtil.styleFilledButton(btnHapus, util.DesignUtil.TOMBOL_BAHAYA, java.awt.Color.WHITE);
        util.DesignUtil.styleFilledButton(btnReset, util.DesignUtil.TOMBOL_NETRAL, util.DesignUtil.TEKS_GELAP);
        util.DesignUtil.styleFilledButton(btnRefresh1, util.DesignUtil.TOMBOL_NETRAL, util.DesignUtil.TEKS_GELAP);
        util.DesignUtil.styleFilledButton(btnCariTunjangan, util.DesignUtil.TOMBOL_UTAMA, java.awt.Color.WHITE);
    }
    
    private double ambilNominal() {
    String nominalText = txtNominalTunjangan.getText()
            .replace("Rp.", "")
            .replace("Rp", "")
            .replace(".", "")
            .replace(",", "")
            .trim();

    if (nominalText.isEmpty()) {
        return 0;
    }

    return Double.parseDouble(nominalText);
}

private int getIdJabatan(String namaJabatan) throws Exception {
    String sql = "SELECT id_jabatan FROM jabatan WHERE nama_jabatan = ?";
    Connection conn = Koneksi.getConnection();
    PreparedStatement pst = conn.prepareStatement(sql);
    pst.setString(1, namaJabatan);

    ResultSet rs = pst.executeQuery();

    if (rs.next()) {
        return rs.getInt("id_jabatan");
    }

    return 0;
}

private void tampilData() {
    DefaultTableModel model = new DefaultTableModel() {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };

    model.addColumn("ID Tunjangan");
    model.addColumn("Nama Tunjangan");
    model.addColumn("Nominal");
    model.addColumn("Jabatan");
    model.addColumn("Keterangan");

    try {
        String sql = "SELECT t.id_tunjangan, t.nama_tunjangan, t.nominal, "
                + "j.nama_jabatan, t.keterangan "
                + "FROM tunjangan t "
                + "LEFT JOIN jabatan j ON t.id_jabatan = j.id_jabatan "
                + "ORDER BY t.id_tunjangan ASC";

        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement(sql);
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            model.addRow(new Object[]{
                rs.getString("id_tunjangan"),
                rs.getString("nama_tunjangan"),
                formatRupiah(rs.getString("nominal")),
                rs.getString("nama_jabatan"),
                rs.getString("keterangan")
            });
        }

        tblTunjangan.setModel(model);
        util.DesignUtil.applyTable(tblTunjangan);

    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal menampilkan data: " + e.getMessage());
    }
}

private void simpanData() {
    String namaTunjangan = txtNamaTunjangan.getText().trim();
    String jabatan = cmbJabatan.getSelectedItem().toString();
    String keterangan = txtKeterangan.getText().trim();

    if (namaTunjangan.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Nama tunjangan wajib diisi.");
        txtNamaTunjangan.requestFocus();
        return;
    }

    if (cmbJabatan.getSelectedIndex() == 0) {
        JOptionPane.showMessageDialog(this, "Pilih jabatan dulu.");
        cmbJabatan.requestFocus();
        return;
    }

    try {
        double nominal = ambilNominal();
        int idJabatan = getIdJabatan(jabatan);

        if (idJabatan == 0) {
            JOptionPane.showMessageDialog(this, "Jabatan tidak ditemukan di database.");
            return;
        }

        String sql = "INSERT INTO tunjangan "
                + "(nama_tunjangan, nominal, id_jabatan, keterangan) "
                + "VALUES (?, ?, ?, ?)";

        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement(sql);

        pst.setString(1, namaTunjangan);
        pst.setDouble(2, nominal);
        pst.setInt(3, idJabatan);
        pst.setString(4, keterangan);

        pst.executeUpdate();

        JOptionPane.showMessageDialog(this, "Data tunjangan berhasil disimpan.");
        tampilData();
        resetForm();

    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal menyimpan data: " + e.getMessage());
    }
}

private void ubahData() {
    String idTunjangan = txtidTunjangan.getText().trim();
    String namaTunjangan = txtNamaTunjangan.getText().trim();
    String jabatan = cmbJabatan.getSelectedItem().toString();
    String keterangan = txtKeterangan.getText().trim();

    if (idTunjangan.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Pilih data dari tabel dulu.");
        return;
    }

    if (namaTunjangan.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Nama tunjangan wajib diisi.");
        txtNamaTunjangan.requestFocus();
        return;
    }

    if (cmbJabatan.getSelectedIndex() == 0) {
        JOptionPane.showMessageDialog(this, "Pilih jabatan dulu.");
        cmbJabatan.requestFocus();
        return;
    }

    try {
        double nominal = ambilNominal();
        int idJabatan = getIdJabatan(jabatan);

        if (idJabatan == 0) {
            JOptionPane.showMessageDialog(this, "Jabatan tidak ditemukan di database.");
            return;
        }

        String sql = "UPDATE tunjangan SET "
                + "nama_tunjangan=?, nominal=?, id_jabatan=?, keterangan=? "
                + "WHERE id_tunjangan=?";

        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement(sql);

        pst.setString(1, namaTunjangan);
        pst.setDouble(2, nominal);
        pst.setInt(3, idJabatan);
        pst.setString(4, keterangan);
        pst.setString(5, idTunjangan);

        pst.executeUpdate();

        JOptionPane.showMessageDialog(this, "Data tunjangan berhasil diubah.");
        tampilData();
        resetForm();

    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal mengubah data: " + e.getMessage());
    }
}

private void hapusData() {
    String idTunjangan = txtidTunjangan.getText().trim();

    if (idTunjangan.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Pilih data dari tabel dulu.");
        return;
    }

    int konfirmasi = JOptionPane.showConfirmDialog(
            this,
            "Yakin ingin menghapus data tunjangan ini?",
            "Konfirmasi Hapus",
            JOptionPane.YES_NO_OPTION
    );

    if (konfirmasi == JOptionPane.YES_OPTION) {
        try {
            String sql = "DELETE FROM tunjangan WHERE id_tunjangan=?";
            Connection conn = Koneksi.getConnection();
            PreparedStatement pst = conn.prepareStatement(sql);

            pst.setString(1, idTunjangan);
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this, "Data tunjangan berhasil dihapus.");
            tampilData();
            resetForm();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal menghapus data: " + e.getMessage());
        }
    }
}

private void cariData() {
    DefaultTableModel model = new DefaultTableModel() {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };

    model.addColumn("ID Tunjangan");
    model.addColumn("Nama Tunjangan");
    model.addColumn("Nominal");
    model.addColumn("Jabatan");
    model.addColumn("Keterangan");

    String keyword = txtCariTunjangan.getText().trim();

    try {
        String sql = "SELECT t.id_tunjangan, t.nama_tunjangan, t.nominal, "
                + "j.nama_jabatan, t.keterangan "
                + "FROM tunjangan t "
                + "LEFT JOIN jabatan j ON t.id_jabatan = j.id_jabatan "
                + "WHERE t.nama_tunjangan LIKE ? "
                + "OR t.nominal LIKE ? "
                + "OR j.nama_jabatan LIKE ? "
                + "OR t.keterangan LIKE ? "
                + "ORDER BY t.id_tunjangan ASC";

        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement(sql);

        pst.setString(1, "%" + keyword + "%");
        pst.setString(2, "%" + keyword + "%");
        pst.setString(3, "%" + keyword + "%");
        pst.setString(4, "%" + keyword + "%");

        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            model.addRow(new Object[]{
                rs.getString("id_tunjangan"),
                rs.getString("nama_tunjangan"),
                formatRupiah(rs.getString("nominal")),
                rs.getString("nama_jabatan"),
                rs.getString("keterangan")
            });
        }

        tblTunjangan.setModel(model);
        util.DesignUtil.applyTable(tblTunjangan);

    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal mencari data: " + e.getMessage());
    }
}

private void resetForm() {
    txtidTunjangan.setText("");
    txtNamaTunjangan.setText("");
    txtNominalTunjangan.setText("Rp.");
    txtKeterangan.setText("");
    txtCariTunjangan.setText("");

    cmbJabatan.setSelectedIndex(0);

    txtidTunjangan.setEditable(false);

    jButton5.setEnabled(true);
    btnUbah.setEnabled(false);
    btnHapus.setEnabled(false);

    txtNamaTunjangan.requestFocus();
}

    private String safe(Object v) {
        return v == null ? "" : v.toString();
    }

    private void ambilDataDariTabel() {
    int baris = tblTunjangan.getSelectedRow();

    if (baris >= 0) {
        txtidTunjangan.setText(safe(tblTunjangan.getValueAt(baris, 0)));
        txtNamaTunjangan.setText(safe(tblTunjangan.getValueAt(baris, 1)));
        
        // Ambil nominal asli dari database, bukan dari tabel yang sudah diformat
        try {
            String idTunjangan = tblTunjangan.getValueAt(baris, 0).toString();
            Connection conn = Koneksi.getConnection();
            PreparedStatement pst = conn.prepareStatement(
                "SELECT nominal FROM tunjangan WHERE id_tunjangan = ?"
            );
            pst.setString(1, idTunjangan);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                txtNominalTunjangan.setText(String.format("%.0f", rs.getDouble("nominal")));
            }
        } catch (Exception e) {}

        String jabatan = safe(tblTunjangan.getValueAt(baris, 3));
        cmbJabatan.setSelectedItem(jabatan);

        txtKeterangan.setText(safe(tblTunjangan.getValueAt(baris, 4)));

        jButton5.setEnabled(false);
        btnUbah.setEnabled(true);
        btnHapus.setEnabled(true);
    }
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

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code manually kalau masih mau pakai tab Design NetBeans.
     */
        @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jLabelJudulForm = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        txtidTunjangan = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        txtNamaTunjangan = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        txtNominalTunjangan = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        cmbJabatan = new javax.swing.JComboBox<>();
        jLabel7 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        txtKeterangan = new javax.swing.JTextArea();
        jButton5 = new javax.swing.JButton();
        btnUbah = new javax.swing.JButton();
        btnHapus = new javax.swing.JButton();
        btnReset = new javax.swing.JButton();
        scrollTunjangan = new javax.swing.JPanel();
        jLabelJudulTabel = new javax.swing.JLabel();
        txtCariTunjangan = new javax.swing.JTextField();
        btnCariTunjangan = new javax.swing.JButton();
        btnRefresh1 = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        tblTunjangan = new javax.swing.JTable();

        setBackground(new java.awt.Color(244, 247, 250));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(22, 48, 92)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("DATA TUNJANGAN");

        jLabel2.setForeground(new java.awt.Color(75, 85, 99));
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("Kelola Data Tunjangan Karyawan");

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 226, 235)));

        jLabelJudulForm.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabelJudulForm.setForeground(new java.awt.Color(22, 48, 92)); // NOI18N
        jLabelJudulForm.setText("Form Tunjangan");

        jLabel3.setForeground(new java.awt.Color(75, 85, 99));
        jLabel3.setText("ID Tunjangan");

        txtidTunjangan.setEditable(false);
        txtidTunjangan.setBackground(new java.awt.Color(243, 244, 246));
        txtidTunjangan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtidTunjanganActionPerformed(evt);
            }
        });

        jLabel4.setForeground(new java.awt.Color(75, 85, 99));
        jLabel4.setText("Nama Tunjangan");

        jLabel5.setForeground(new java.awt.Color(75, 85, 99));
        jLabel5.setText("Nominal");

        txtNominalTunjangan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtNominalTunjanganActionPerformed(evt);
            }
        });

        jLabel6.setForeground(new java.awt.Color(75, 85, 99));
        jLabel6.setText("Jabatan");

        jLabel7.setForeground(new java.awt.Color(75, 85, 99));
        jLabel7.setText("Keterangan");

        txtKeterangan.setColumns(20);
        txtKeterangan.setRows(3);
        txtKeterangan.setWrapStyleWord(true);
        jScrollPane2.setViewportView(txtKeterangan);

        jButton5.setBackground(new java.awt.Color(20, 184, 166));
        jButton5.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jButton5.setForeground(new java.awt.Color(255, 255, 255));
        jButton5.setText("Simpan");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
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
                    .addComponent(jLabelJudulForm)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(12, 12, 12)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtidTunjangan, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(txtNominalTunjangan, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 300, Short.MAX_VALUE))
                        .addGap(24, 24, 24)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(12, 12, 12)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtNamaTunjangan, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(cmbJabatan, 0, 200, Short.MAX_VALUE)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(jButton5)
                                .addGap(10, 10, 10)
                                .addComponent(btnUbah)
                                .addGap(10, 10, 10)
                                .addComponent(btnHapus)
                                .addGap(10, 10, 10)
                                .addComponent(btnReset))))
                    .addComponent(jLabelJudulForm))
                .addGap(16, 16, 16))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jLabelJudulForm)
                .addGap(12, 12, 12)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(txtidTunjangan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(txtNamaTunjangan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(txtNominalTunjangan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6)
                    .addComponent(cmbJabatan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel7)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButton5)
                            .addComponent(btnUbah)
                            .addComponent(btnHapus)
                            .addComponent(btnReset))))
                .addGap(16, 16, 16))
        );

        scrollTunjangan.setBackground(new java.awt.Color(255, 255, 255));
        scrollTunjangan.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 226, 235)));

        jLabelJudulTabel.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabelJudulTabel.setForeground(new java.awt.Color(22, 48, 92)); // NOI18N
        jLabelJudulTabel.setText("Daftar Tunjangan");

        btnCariTunjangan.setText("Cari");
        btnCariTunjangan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCariTunjanganActionPerformed(evt);
            }
        });

        btnRefresh1.setBackground(new java.awt.Color(243, 244, 246));
        btnRefresh1.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnRefresh1.setForeground(new java.awt.Color(17, 24, 39));
        btnRefresh1.setText("Refresh");
        btnRefresh1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRefresh1ActionPerformed(evt);
            }
        });

        tblTunjangan.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null},
                {null, null, null, null, null}
            },
            new String [] {
                "ID Tunjangan", "Nama Tunjangan", "Nominal", "Jabatan", "Keterangan"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false
            };
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblTunjangan.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tblTunjanganMouseClicked(evt);
            }
        });
        jScrollPane3.setViewportView(tblTunjangan);

        javax.swing.GroupLayout scrollTunjanganLayout = new javax.swing.GroupLayout(scrollTunjangan);
        scrollTunjangan.setLayout(scrollTunjanganLayout);
        scrollTunjanganLayout.setHorizontalGroup(
            scrollTunjanganLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(scrollTunjanganLayout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(scrollTunjanganLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabelJudulTabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(scrollTunjanganLayout.createSequentialGroup()
                        .addComponent(txtCariTunjangan, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(8, 8, 8)
                        .addComponent(btnCariTunjangan)
                        .addGap(8, 8, 8)
                        .addComponent(btnRefresh1)))
                .addGap(16, 16, 16))
        );
        scrollTunjanganLayout.setVerticalGroup(
            scrollTunjanganLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(scrollTunjanganLayout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jLabelJudulTabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addGroup(scrollTunjanganLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtCariTunjangan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnCariTunjangan)
                    .addComponent(btnRefresh1))
                .addGap(12, 12, 12)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 230, Short.MAX_VALUE)
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
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(scrollTunjangan, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(2, 2, 2)
                .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(scrollTunjangan, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(16, 16, 16))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        simpanData();
    }//GEN-LAST:event_jButton5ActionPerformed

    private void txtidTunjanganActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtidTunjanganActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtidTunjanganActionPerformed

    private void txtNominalTunjanganActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtNominalTunjanganActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtNominalTunjanganActionPerformed

    private void btnUbahActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUbahActionPerformed
         ubahData();
    }//GEN-LAST:event_btnUbahActionPerformed

    private void btnHapusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHapusActionPerformed
        hapusData();
    }//GEN-LAST:event_btnHapusActionPerformed

    private void btnResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnResetActionPerformed
        resetForm();
    }//GEN-LAST:event_btnResetActionPerformed

    private void btnCariTunjanganActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCariTunjanganActionPerformed
        cariData(); // TODO add your handling code here:
    }//GEN-LAST:event_btnCariTunjanganActionPerformed

    private void btnRefresh1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefresh1ActionPerformed
         tampilData();
        resetForm();
    }//GEN-LAST:event_btnRefresh1ActionPerformed






    private void tblTunjanganMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblTunjanganMouseClicked
        ambilDataDariTabel();
    }//GEN-LAST:event_tblTunjanganMouseClicked

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCariTunjangan;
    private javax.swing.JButton btnHapus;
    private javax.swing.JButton btnRefresh1;
    private javax.swing.JButton btnReset;
    private javax.swing.JButton btnUbah;
    private javax.swing.JComboBox<String> cmbJabatan;
    private javax.swing.JButton jButton5;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabelJudulForm;
    private javax.swing.JLabel jLabelJudulTabel;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JPanel scrollTunjangan;
    private javax.swing.JTable tblTunjangan;
    private javax.swing.JTextField txtCariTunjangan;
    private javax.swing.JTextArea txtKeterangan;
    private javax.swing.JTextField txtNamaTunjangan;
    private javax.swing.JTextField txtNominalTunjangan;
    private javax.swing.JTextField txtidTunjangan;
    // End of variables declaration//GEN-END:variables

    private String formatRupiah(String angka) {
    try {
        double nilai = Double.parseDouble(angka);
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new java.util.Locale("id", "ID"));
        return "Rp " + nf.format(nilai);
    } catch (Exception e) {
        return angka;
    }
}
}
