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
public class DataDivisiPanel extends javax.swing.JPanel {

    public DataDivisiPanel() {
        initComponents();
        styleButton();
        tampilData();
        resetForm();
        util.DesignUtil.applyPage(this);
    }


    private void tampilData() {
    DefaultTableModel model = new DefaultTableModel() {
        @Override public boolean isCellEditable(int row, int column) { return false; }
    };

    model.addColumn("ID Divisi");
    model.addColumn("Nama Divisi");
    model.addColumn("Kepala Divisi");
    model.addColumn("Keterangan");

    try {
        String sql = "SELECT * FROM divisi ORDER BY id_divisi ASC";
        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement(sql);
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            model.addRow(new Object[]{
                rs.getString("id_divisi"),
                rs.getString("nama_divisi"),
                rs.getString("kepala_divisi"),
                rs.getString("keterangan")
            });
        }

        tblDivisi.setModel(model);
        util.DesignUtil.applyTable(tblDivisi);

    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal menampilkan data: " + e.getMessage());
    }
}

private void simpanData() {
    String namaDivisi = txtNamaDivisi.getText().trim();
    String kepalaDivisi = txtKepalaDivisi.getText().trim();
    String keterangan = txtKeterangan.getText().trim();

    if (namaDivisi.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Nama divisi wajib diisi.");
        txtNamaDivisi.requestFocus();
        return;
    }

    try {
        String sql = "INSERT INTO divisi (nama_divisi, kepala_divisi, keterangan) VALUES (?, ?, ?)";
        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement(sql);

        pst.setString(1, namaDivisi);
        pst.setString(2, kepalaDivisi);
        pst.setString(3, keterangan);

        pst.executeUpdate();

        JOptionPane.showMessageDialog(this, "Data divisi berhasil disimpan.");
        tampilData();
        resetForm();

    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal menyimpan data: " + e.getMessage());
    }
}

private void ubahData() {
    String idDivisi = txtidDivisi.getText().trim();
    String namaDivisi = txtNamaDivisi.getText().trim();
    String kepalaDivisi = txtKepalaDivisi.getText().trim();
    String keterangan = txtKeterangan.getText().trim();

    if (idDivisi.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Pilih data dari tabel dulu.");
        return;
    }

    if (namaDivisi.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Nama divisi wajib diisi.");
        txtNamaDivisi.requestFocus();
        return;
    }

    try {
        String sql = "UPDATE divisi SET nama_divisi=?, kepala_divisi=?, keterangan=? WHERE id_divisi=?";
        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement(sql);

        pst.setString(1, namaDivisi);
        pst.setString(2, kepalaDivisi);
        pst.setString(3, keterangan);
        pst.setString(4, idDivisi);

        pst.executeUpdate();

        JOptionPane.showMessageDialog(this, "Data divisi berhasil diubah.");
        tampilData();
        resetForm();

    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal mengubah data: " + e.getMessage());
    }
}

private void hapusData() {
    String idDivisi = txtidDivisi.getText().trim();

    if (idDivisi.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Pilih data dari tabel dulu.");
        return;
    }

    int konfirmasi = JOptionPane.showConfirmDialog(
            this,
            "Yakin ingin menghapus data divisi ini?",
            "Konfirmasi Hapus",
            JOptionPane.YES_NO_OPTION
    );

    if (konfirmasi == JOptionPane.YES_OPTION) {
        try {
            String sql = "DELETE FROM divisi WHERE id_divisi=?";
            Connection conn = Koneksi.getConnection();
            PreparedStatement pst = conn.prepareStatement(sql);

            pst.setString(1, idDivisi);
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this, "Data divisi berhasil dihapus.");
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

    model.addColumn("ID Divisi");
    model.addColumn("Nama Divisi");
    model.addColumn("Kepala Divisi");
    model.addColumn("Keterangan");

    String keyword = txtCariDivisi.getText().trim();

    try {
        String sql = "SELECT * FROM divisi "
                + "WHERE nama_divisi LIKE ? "
                + "OR kepala_divisi LIKE ? "
                + "OR keterangan LIKE ? "
                + "ORDER BY id_divisi ASC";

        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement(sql);

        pst.setString(1, "%" + keyword + "%");
        pst.setString(2, "%" + keyword + "%");
        pst.setString(3, "%" + keyword + "%");

        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            model.addRow(new Object[]{
                rs.getString("id_divisi"),
                rs.getString("nama_divisi"),
                rs.getString("kepala_divisi"),
                rs.getString("keterangan")
            });
        }

        tblDivisi.setModel(model);
        util.DesignUtil.applyTable(tblDivisi);

    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal mencari data: " + e.getMessage());
    }
}

private void resetForm() {
    txtidDivisi.setText("");
    txtNamaDivisi.setText("");
    txtKepalaDivisi.setText("");
    txtKeterangan.setText("");
    txtCariDivisi.setText("");

    txtidDivisi.setEditable(false);

    jButton5.setEnabled(true);
    btnUbah.setEnabled(false);
    btnHapus.setEnabled(false);

    txtNamaDivisi.requestFocus();
}

private void ambilDataDariTabel() {
    int baris = tblDivisi.getSelectedRow();

    if (baris >= 0) {
        txtidDivisi.setText(tblDivisi.getValueAt(baris, 0).toString());
        txtNamaDivisi.setText(tblDivisi.getValueAt(baris, 1).toString());
        txtKepalaDivisi.setText(tblDivisi.getValueAt(baris, 2).toString());
        txtKeterangan.setText(tblDivisi.getValueAt(baris, 3).toString());

        jButton5.setEnabled(false);
        btnUbah.setEnabled(true);
        btnHapus.setEnabled(true);
    }
}

    private void styleButton() {
        util.DesignUtil.styleFilledButton(jButton5, util.DesignUtil.TOMBOL_SEKUNDER, java.awt.Color.WHITE); // Simpan
        util.DesignUtil.styleFilledButton(btnUbah, util.DesignUtil.TOMBOL_UTAMA, java.awt.Color.WHITE);     // Ubah
        util.DesignUtil.styleFilledButton(btnHapus, util.DesignUtil.TOMBOL_BAHAYA, java.awt.Color.WHITE);    // Hapus
        util.DesignUtil.styleFilledButton(btnReset, util.DesignUtil.TOMBOL_NETRAL, util.DesignUtil.TEKS_GELAP);
        util.DesignUtil.styleFilledButton(btnRefresh, util.DesignUtil.TOMBOL_NETRAL, util.DesignUtil.TEKS_GELAP);
        util.DesignUtil.styleFilledButton(btnCariDivisi, util.DesignUtil.TOMBOL_UTAMA, java.awt.Color.WHITE);
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
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        txtidDivisi = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        txtNamaDivisi = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        txtKepalaDivisi = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtKeterangan = new javax.swing.JTextArea();
        jButton5 = new javax.swing.JButton();
        btnUbah = new javax.swing.JButton();
        btnHapus = new javax.swing.JButton();
        btnReset = new javax.swing.JButton();
        scrollDivisi = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        txtCariDivisi = new javax.swing.JTextField();
        btnCariDivisi = new javax.swing.JButton();
        btnRefresh = new javax.swing.JButton();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblDivisi = new javax.swing.JTable();

        setBackground(new java.awt.Color(244, 247, 250));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(22, 48, 92)); // NOI18N
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("DATA DIVISI");

        jLabel2.setForeground(new java.awt.Color(75, 85, 99));
        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel2.setText("Kelola Data Divisi Karyawan");

        jPanel1.setBackground(new java.awt.Color(255, 255, 255));
        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 226, 235)));

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(22, 48, 92)); // NOI18N
        jLabel3.setText("Form Divisi");

        jLabel4.setForeground(new java.awt.Color(75, 85, 99));
        jLabel4.setText("ID Divisi");

        txtidDivisi.setEditable(false);
        txtidDivisi.setBackground(new java.awt.Color(243, 244, 246));
        txtidDivisi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtidDivisiActionPerformed(evt);
            }
        });

        jLabel5.setForeground(new java.awt.Color(75, 85, 99));
        jLabel5.setText("Nama Divisi");

        jLabel6.setForeground(new java.awt.Color(75, 85, 99));
        jLabel6.setText("Kepala Divisi");

        jLabel8.setForeground(new java.awt.Color(75, 85, 99));
        jLabel8.setText("Keterangan");

        txtKeterangan.setColumns(20);
        txtKeterangan.setRows(4);
        txtKeterangan.setWrapStyleWord(true);
        jScrollPane1.setViewportView(txtKeterangan);

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
                    .addComponent(jLabel3)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 12, 12)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(txtidDivisi, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(txtNamaDivisi, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(txtKepalaDivisi, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
                .addGap(16, 16, 16))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jButton5)
                .addGap(10, 10, 10)
                .addComponent(btnUbah)
                .addGap(10, 10, 10)
                .addComponent(btnHapus)
                .addGap(10, 10, 10)
                .addComponent(btnReset)
                .addGap(16, 16, 16))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jLabel3)
                .addGap(12, 12, 12)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(txtidDivisi, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(txtNamaDivisi, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(txtKepalaDivisi, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(8, 8, 8)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel8)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 70, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(14, 14, 14)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton5)
                    .addComponent(btnUbah)
                    .addComponent(btnHapus)
                    .addComponent(btnReset))
                .addGap(16, 16, 16))
        );

        scrollDivisi.setBackground(new java.awt.Color(255, 255, 255));
        scrollDivisi.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 226, 235)));

        jLabel9.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel9.setForeground(new java.awt.Color(22, 48, 92)); // NOI18N
        jLabel9.setText("Daftar Divisi");

        btnCariDivisi.setText("Cari");
        btnCariDivisi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCariDivisiActionPerformed(evt);
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

        tblDivisi.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null},
                {null, null, null, null}
            },
            new String [] {
                "ID Divisi", "Nama Divisi", "Kepala Divisi", "Keterangan"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblDivisi.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tblDivisiMouseClicked(evt);
            }
        });
        jScrollPane2.setViewportView(tblDivisi);

        javax.swing.GroupLayout scrollDivisiLayout = new javax.swing.GroupLayout(scrollDivisi);
        scrollDivisi.setLayout(scrollDivisiLayout);
        scrollDivisiLayout.setHorizontalGroup(
            scrollDivisiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(scrollDivisiLayout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(scrollDivisiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, scrollDivisiLayout.createSequentialGroup()
                        .addComponent(jLabel9)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(txtCariDivisi, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(8, 8, 8)
                        .addComponent(btnCariDivisi)
                        .addGap(8, 8, 8)
                        .addComponent(btnRefresh)))
                .addGap(16, 16, 16))
        );
        scrollDivisiLayout.setVerticalGroup(
            scrollDivisiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(scrollDivisiLayout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(scrollDivisiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(txtCariDivisi, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnCariDivisi)
                    .addComponent(btnRefresh))
                .addGap(12, 12, 12)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 230, Short.MAX_VALUE)
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
                    .addComponent(scrollDivisi, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 30, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(2, 2, 2)
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(scrollDivisi, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(16, 16, 16))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void txtidDivisiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtidDivisiActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtidDivisiActionPerformed




    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        simpanData();
    }//GEN-LAST:event_jButton5ActionPerformed

    private void btnUbahActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUbahActionPerformed
        ubahData();
    }//GEN-LAST:event_btnUbahActionPerformed

    private void btnHapusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHapusActionPerformed
        hapusData();
    }//GEN-LAST:event_btnHapusActionPerformed

    private void btnResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnResetActionPerformed
        resetForm();
    }//GEN-LAST:event_btnResetActionPerformed

    private void btnCariDivisiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCariDivisiActionPerformed
         cariData();
    }//GEN-LAST:event_btnCariDivisiActionPerformed

    private void btnRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshActionPerformed
        tampilData();
        resetForm();    
    }//GEN-LAST:event_btnRefreshActionPerformed

    private void tblDivisiMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblDivisiMouseClicked
         ambilDataDariTabel();
    }//GEN-LAST:event_tblDivisiMouseClicked


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCariDivisi;
    private javax.swing.JButton btnHapus;
    private javax.swing.JButton btnRefresh;
    private javax.swing.JButton btnReset;
    private javax.swing.JButton btnUbah;
    private javax.swing.JButton jButton5;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JPanel scrollDivisi;
    private javax.swing.JTable tblDivisi;
    private javax.swing.JTextField txtCariDivisi;
    private javax.swing.JTextField txtKepalaDivisi;
    private javax.swing.JTextArea txtKeterangan;
    private javax.swing.JTextField txtNamaDivisi;
    private javax.swing.JTextField txtidDivisi;
    // End of variables declaration//GEN-END:variables
}
