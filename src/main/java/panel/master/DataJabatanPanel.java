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

    // ✅ FIX: ganti kode_jabatan -> keterangan
    private void tampilData() {
        DefaultTableModel model = new DefaultTableModel();
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
        DefaultTableModel model = new DefaultTableModel();
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

        jPanel6 = new javax.swing.JPanel();
        txtCari3 = new javax.swing.JTextField();
        btnCari3 = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        tblKaryawan = new javax.swing.JTable();
        btnRefresh = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        txtIdJabatan = new javax.swing.JTextField();
        txtNamaJabatan = new javax.swing.JTextField();
        txtGaji = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        txtKeterangan = new javax.swing.JTextField();
        btnSimpan = new javax.swing.JButton();
        btnUbah = new javax.swing.JButton();
        btnHapus = new javax.swing.JButton();
        btnReset = new javax.swing.JButton();

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder(""));

        btnCari3.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnCari3.setText("Cari");
        btnCari3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCari3ActionPerformed(evt);
            }
        });

        tblKaryawan.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "Title 1", "Title 2", "Title 3", "Title 4"
            }
        ));
        jScrollPane4.setViewportView(tblKaryawan);

        btnRefresh.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnRefresh.setText("Refresh");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane4)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addComponent(txtCari3, javax.swing.GroupLayout.PREFERRED_SIZE, 144, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCari3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnRefresh)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtCari3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnCari3)
                    .addComponent(btnRefresh))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 168, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(44, Short.MAX_VALUE))
        );

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(0, 51, 153));
        jLabel1.setText("Data Jabatan");

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createEtchedBorder(), "Form Jabatan", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI", 0, 12), new java.awt.Color(0, 51, 153))); // NOI18N

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel3.setText("Jabatan");

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel2.setText("ID Jabatan");

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel6.setText("Gaji");

        jLabel9.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel9.setText("Keterangan");

        btnSimpan.setBackground(new java.awt.Color(51, 255, 204));
        btnSimpan.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnSimpan.setForeground(new java.awt.Color(255, 255, 255));
        btnSimpan.setText("Simpan");
        btnSimpan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSimpanActionPerformed(evt);
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

        btnHapus.setBackground(new java.awt.Color(255, 51, 51));
        btnHapus.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnHapus.setForeground(new java.awt.Color(255, 255, 255));
        btnHapus.setText("Hapus");
        btnHapus.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHapusActionPerformed(evt);
            }
        });

        btnReset.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
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
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(btnSimpan)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnUbah)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnHapus)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnReset)
                .addGap(92, 92, 92))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 93, Short.MAX_VALUE))
                        .addGap(12, 12, 12))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel9)
                            .addComponent(jLabel6))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                        .addComponent(txtIdJabatan)
                        .addComponent(txtNamaJabatan, javax.swing.GroupLayout.DEFAULT_SIZE, 578, Short.MAX_VALUE))
                    .addComponent(txtGaji, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 592, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtKeterangan)))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(txtIdJabatan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(txtNamaJabatan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel9)
                    .addComponent(txtKeterangan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel6)
                    .addComponent(txtGaji, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 49, Short.MAX_VALUE)
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
                            .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addContainerGap(56, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jPanel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(40, Short.MAX_VALUE))
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
