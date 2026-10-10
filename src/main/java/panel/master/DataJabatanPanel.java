package panel.master;
import config.Koneksi;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

public class DataJabatanPanel extends javax.swing.JPanel {

    public DataJabatanPanel() {
        initComponents();
        styleButton();
        tampilData();
        resetForm();
        util.DesignUtil.applyPage(this);
    }



    private void styleButton() {
        util.DesignUtil.styleFilledButton(btnSimpan, util.DesignUtil.TOMBOL_SEKUNDER, java.awt.Color.WHITE);
        util.DesignUtil.styleFilledButton(btnUbah, util.DesignUtil.TOMBOL_UTAMA, java.awt.Color.WHITE);
        util.DesignUtil.styleFilledButton(btnHapus, util.DesignUtil.TOMBOL_BAHAYA, java.awt.Color.WHITE);
        util.DesignUtil.styleFilledButton(btnReset, util.DesignUtil.TOMBOL_NETRAL, util.DesignUtil.TEKS_GELAP);
        util.DesignUtil.styleFilledButton(btnCari3, util.DesignUtil.TOMBOL_UTAMA, java.awt.Color.WHITE);
        util.DesignUtil.styleFilledButton(btnRefresh, util.DesignUtil.TOMBOL_NETRAL, util.DesignUtil.TEKS_GELAP);
    }

    // ✅ FIX: ganti kode_jabatan -> keterangan
    private void tampilData() {
        DefaultTableModel model = new DefaultTableModel() {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
        model.addColumn("ID Jabatan");
        model.addColumn("Nama Jabatan");
        model.addColumn("Gaji Pokok");
        model.addColumn("Keterangan");

        try {
            String sql = "SELECT * FROM jabatan ORDER BY id_jabatan ASC";
            Connection conn = Koneksi.getConnection();
            PreparedStatement pst = conn.prepareStatement(sql);
            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("id_jabatan"),
                    rs.getString("nama_jabatan"),
                    formatRupiah(rs.getString("gaji_pokok")),
                    rs.getString("keterangan") // ✅ FIX: was kode_jabatan
                });
            }
            tblKaryawan.setModel(model);
        util.DesignUtil.applyTable(tblKaryawan);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal menampilkan data: " + e.getMessage());
        }
    }

    // ✅ FIX: tambah keterangan di INSERT
    private void simpanData() {
        String namaJabatan = txtNamaJabatan.getText().trim();
        String gaji = txtGaji.getText().trim();
        String keterangan = txtKeterangan.getText().trim();

        if (namaJabatan.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Nama jabatan wajib diisi.");
            return;
        }

        try {
            String kodeJabatan = "JBT" + System.currentTimeMillis();
            String sql = "INSERT INTO jabatan (kode_jabatan, nama_jabatan, gaji_pokok, keterangan) VALUES (?, ?, ?, ?)";
            Connection conn = Koneksi.getConnection();
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, kodeJabatan);
            pst.setString(2, namaJabatan);
            pst.setDouble(3, gaji.isEmpty() ? 0 : Double.parseDouble(gaji));
            pst.setString(4, keterangan); // ✅ FIX: tambah keterangan
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this, "Data jabatan berhasil disimpan.");
            tampilData();
            resetForm();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal menyimpan: " + e.getMessage());
        }
    }

    // ✅ FIX: tambah keterangan di UPDATE
    private void ubahData() {
        String idJabatan = txtIdJabatan.getText().trim();
        String namaJabatan = txtNamaJabatan.getText().trim();
        String gaji = txtGaji.getText().trim();
        String keterangan = txtKeterangan.getText().trim();

        if (idJabatan.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Pilih data dari tabel dulu.");
            return;
        }

        try {
            String sql = "UPDATE jabatan SET nama_jabatan=?, gaji_pokok=?, keterangan=? WHERE id_jabatan=?";
            Connection conn = Koneksi.getConnection();
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, namaJabatan);
            pst.setDouble(2, gaji.isEmpty() ? 0 : Double.parseDouble(gaji));
            pst.setString(3, keterangan); // ✅ FIX: tambah keterangan
            pst.setString(4, idJabatan);  // ✅ FIX: geser ke index 4
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this, "Data jabatan berhasil diubah.");
            tampilData();
            resetForm();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal mengubah: " + e.getMessage());
        }
    }

    private void hapusData() {
        String idJabatan = txtIdJabatan.getText().trim();

        if (idJabatan.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Pilih data dari tabel dulu.");
            return;
        }

        int konfirmasi = JOptionPane.showConfirmDialog(this,
            "Yakin ingin menghapus jabatan ini?", "Konfirmasi", JOptionPane.YES_NO_OPTION);

        if (konfirmasi == JOptionPane.YES_OPTION) {
            try {
                String sql = "DELETE FROM jabatan WHERE id_jabatan=?";
                Connection conn = Koneksi.getConnection();
                PreparedStatement pst = conn.prepareStatement(sql);
                pst.setString(1, idJabatan);
                pst.executeUpdate();

                JOptionPane.showMessageDialog(this, "Data jabatan berhasil dihapus.");
                tampilData();
                resetForm();
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Gagal menghapus: " + e.getMessage());
            }
        }
    }

    private void resetForm() {
        txtIdJabatan.setText("");
        txtNamaJabatan.setText("");
        txtGaji.setText("");
        txtKeterangan.setText("");
        txtIdJabatan.setEditable(false);

        btnSimpan.setEnabled(true);
        btnUbah.setEnabled(false);
        btnHapus.setEnabled(false);
    }

    // ✅ FIX: bersihkan format Rupiah sebelum isi txtGaji
    private void ambilDataDariTabel() {
        int baris = tblKaryawan.getSelectedRow();
        if (baris >= 0) {
            txtIdJabatan.setText(tblKaryawan.getValueAt(baris, 0).toString());
            txtNamaJabatan.setText(tblKaryawan.getValueAt(baris, 1).toString());

            String gajiRaw = tblKaryawan.getValueAt(baris, 2).toString()
                .replace("Rp ", "")
                .replace(".", "")
                .replace(",", "")
                .trim();
            txtGaji.setText(gajiRaw); // ✅ FIX: angka bersih tanpa "Rp"

            txtKeterangan.setText(tblKaryawan.getValueAt(baris, 3) != null ?
                tblKaryawan.getValueAt(baris, 3).toString() : "");

            btnSimpan.setEnabled(false);
            btnUbah.setEnabled(true);
            btnHapus.setEnabled(true);
        }
    }

    // ✅ FIX: ganti kode_jabatan -> keterangan
    private void cariData() {
        DefaultTableModel model = new DefaultTableModel() {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };
        model.addColumn("ID Jabatan");
        model.addColumn("Nama Jabatan");
        model.addColumn("Gaji Pokok");
        model.addColumn("Keterangan");

        String keyword = txtCari3.getText().trim();

        try {
            String sql = "SELECT * FROM jabatan "
                    + "WHERE nama_jabatan LIKE ? "
                    + "OR kode_jabatan LIKE ? "
                    + "ORDER BY id_jabatan ASC";

            Connection conn = Koneksi.getConnection();
            PreparedStatement pst = conn.prepareStatement(sql);
            pst.setString(1, "%" + keyword + "%");
            pst.setString(2, "%" + keyword + "%");

            ResultSet rs = pst.executeQuery();

            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getString("id_jabatan"),
                    rs.getString("nama_jabatan"),
                    formatRupiah(rs.getString("gaji_pokok")),
                    rs.getString("keterangan") // ✅ FIX: was kode_jabatan
                });
            }
            tblKaryawan.setModel(model);
        util.DesignUtil.applyTable(tblKaryawan);

        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal mencari data: " + e.getMessage());
        }
    }

    private String formatRupiah(String angka) {
        try {
            double nilai = Double.parseDouble(angka);
            java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new java.util.Locale("id", "ID"));
            return "Rp " + nf.format(nilai);
        } catch (Exception e) {
            return angka;
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
        jLabelJudul2 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jLabelJudulForm = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        txtIdJabatan = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        txtNamaJabatan = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        txtGaji = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        txtKeterangan = new javax.swing.JTextField();
        btnSimpan = new javax.swing.JButton();
        btnUbah = new javax.swing.JButton();
        btnHapus = new javax.swing.JButton();
        btnReset = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        jLabelJudulTabel = new javax.swing.JLabel();
        txtCari3 = new javax.swing.JTextField();
        btnCari3 = new javax.swing.JButton();
        btnRefresh = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        tblKaryawan = new javax.swing.JTable();

        setBackground(new java.awt.Color(244, 247, 250));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(22, 48, 92)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("DATA JABATAN");

        jLabelJudul2.setForeground(new java.awt.Color(75, 85, 99));
        jLabelJudul2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelJudul2.setText("Kelola Data Jabatan Karyawan");

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 226, 235)));

        jLabelJudulForm.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabelJudulForm.setForeground(new java.awt.Color(22, 48, 92)); // NOI18N
        jLabelJudulForm.setText("Form Jabatan");

        jLabel2.setForeground(new java.awt.Color(75, 85, 99));
        jLabel2.setText("ID Jabatan");

        txtIdJabatan.setEditable(false);
        txtIdJabatan.setBackground(new java.awt.Color(243, 244, 246));

        jLabel3.setForeground(new java.awt.Color(75, 85, 99));
        jLabel3.setText("Nama Jabatan");

        jLabel6.setForeground(new java.awt.Color(75, 85, 99));
        jLabel6.setText("Gaji Pokok");

        jLabel9.setForeground(new java.awt.Color(75, 85, 99));
        jLabel9.setText("Keterangan");

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
                    .addComponent(jLabelJudulForm)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(12, 12, 12)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtIdJabatan, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(txtGaji, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(24, 24, 24)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(12, 12, 12)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtNamaJabatan, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(txtKeterangan, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
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
                .addComponent(jLabelJudulForm)
                .addGap(12, 12, 12)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(txtIdJabatan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3)
                    .addComponent(txtNamaJabatan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(txtGaji, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9)
                    .addComponent(txtKeterangan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(14, 14, 14)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnSimpan)
                    .addComponent(btnUbah)
                    .addComponent(btnHapus)
                    .addComponent(btnReset))
                .addGap(16, 16, 16))
        );

        jPanel6.setBackground(new java.awt.Color(255, 255, 255));
        jPanel6.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 226, 235)));

        jLabelJudulTabel.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabelJudulTabel.setForeground(new java.awt.Color(22, 48, 92)); // NOI18N
        jLabelJudulTabel.setText("Daftar Jabatan");

        btnCari3.setText("Cari");
        btnCari3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCari3ActionPerformed(evt);
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
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "ID Jabatan", "Nama Jabatan", "Gaji Pokok", "Keterangan"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
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
        jScrollPane4.setViewportView(tblKaryawan);

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabelJudulTabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(txtCari3, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(8, 8, 8)
                        .addComponent(btnCari3)
                        .addGap(8, 8, 8)
                        .addComponent(btnRefresh)))
                .addGap(16, 16, 16))
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jLabelJudulTabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtCari3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnCari3)
                    .addComponent(btnRefresh))
                .addGap(12, 12, 12)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 230, Short.MAX_VALUE)
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
                    .addComponent(jLabelJudul2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(2, 2, 2)
                .addComponent(jLabelJudul2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(16, 16, 16))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnCari3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCari3ActionPerformed
        cariData();
    }//GEN-LAST:event_btnCari3ActionPerformed

    private void btnSimpanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSimpanActionPerformed
        simpanData();
    }//GEN-LAST:event_btnSimpanActionPerformed

    private void btnUbahActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUbahActionPerformed
        ubahData();
    }//GEN-LAST:event_btnUbahActionPerformed

    private void btnHapusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHapusActionPerformed
        hapusData();
    }//GEN-LAST:event_btnHapusActionPerformed

    private void btnResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnResetActionPerformed
         resetForm();
    }//GEN-LAST:event_btnResetActionPerformed



    private void btnRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshActionPerformed
        tampilData();
        resetForm();
    }//GEN-LAST:event_btnRefreshActionPerformed

    private void tblKaryawanMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblKaryawanMouseClicked
        ambilDataDariTabel();
    }//GEN-LAST:event_tblKaryawanMouseClicked

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCari3;
    private javax.swing.JButton btnHapus;
    private javax.swing.JButton btnRefresh;
    private javax.swing.JButton btnReset;
    private javax.swing.JButton btnSimpan;
    private javax.swing.JButton btnUbah;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabelJudul2;
    private javax.swing.JLabel jLabelJudulForm;
    private javax.swing.JLabel jLabelJudulTabel;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JTable tblKaryawan;
    private javax.swing.JTextField txtCari3;
    private javax.swing.JTextField txtGaji;
    private javax.swing.JTextField txtIdJabatan;
    private javax.swing.JTextField txtKeterangan;
    private javax.swing.JTextField txtNamaJabatan;
    // End of variables declaration//GEN-END:variables
}
