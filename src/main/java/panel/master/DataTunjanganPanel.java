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

    tblTunjangan.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            ambilDataDariTabel();
        }
    });
}
    
    
    
    private void styleButton() {
    styleFilledButton(jButton5, new java.awt.Color(20, 184, 166), java.awt.Color.WHITE); // Simpan
    styleFilledButton(btnUbah, new java.awt.Color(37, 99, 235), java.awt.Color.WHITE);   // Ubah
    styleFilledButton(btnHapus, new java.awt.Color(239, 68, 68), java.awt.Color.WHITE);  // Hapus

    styleFilledButton(btnReset, new java.awt.Color(243, 244, 246), new java.awt.Color(17, 24, 39));
    styleFilledButton(btnRefresh1, new java.awt.Color(243, 244, 246), new java.awt.Color(17, 24, 39));
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
    DefaultTableModel model = new DefaultTableModel();

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
    DefaultTableModel model = new DefaultTableModel();

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
                rs.getString("nominal"),
                rs.getString("nama_jabatan"),
                rs.getString("keterangan")
            });
        }

        tblTunjangan.setModel(model);

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

private void ambilDataDariTabel() {
    int baris = tblTunjangan.getSelectedRow();

    if (baris >= 0) {
        txtidTunjangan.setText(tblTunjangan.getValueAt(baris, 0).toString());
        txtNamaTunjangan.setText(tblTunjangan.getValueAt(baris, 1).toString());
        
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

        String jabatan = tblTunjangan.getValueAt(baris, 3) != null ? 
            tblTunjangan.getValueAt(baris, 3).toString() : "";
        cmbJabatan.setSelectedItem(jabatan);

        txtKeterangan.setText(tblTunjangan.getValueAt(baris, 4) != null ? 
            tblTunjangan.getValueAt(baris, 4).toString() : "");

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

        jButton5 = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        txtidTunjangan = new javax.swing.JTextField();
        txtNamaTunjangan = new javax.swing.JTextField();
        txtNominalTunjangan = new javax.swing.JTextField();
        cmbJabatan = new javax.swing.JComboBox<>();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtKeterangan = new javax.swing.JTextArea();
        jLabel3 = new javax.swing.JLabel();
        btnUbah = new javax.swing.JButton();
        btnHapus = new javax.swing.JButton();
        btnReset = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        scrollTunjangan = new javax.swing.JPanel();
        btnFirst1 = new javax.swing.JButton();
        btnRefresh1 = new javax.swing.JButton();
        btnPrev1 = new javax.swing.JButton();
        btnNext1 = new javax.swing.JButton();
        btnLast1 = new javax.swing.JButton();
        lblPage1 = new javax.swing.JLabel();
        jScrollPane3 = new javax.swing.JScrollPane();
        tblTunjangan = new javax.swing.JTable();
        txtCariTunjangan = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        btnCariTunjangan = new javax.swing.JButton();

        jButton5.setBackground(new java.awt.Color(20, 184, 166));
        jButton5.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jButton5.setForeground(new java.awt.Color(255, 255, 255));
        jButton5.setText("Simpan");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        jPanel1.setForeground(new java.awt.Color(255, 255, 255));

        jLabel4.setText("ID Tunjangan");

        jLabel5.setText("Nama Tunjangan");

        jLabel6.setText("Nominal");

        jLabel7.setText("Jabatan");

        jLabel8.setText("Keterangan");

        txtidTunjangan.setForeground(new java.awt.Color(204, 204, 204));
        txtidTunjangan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtidTunjanganActionPerformed(evt);
            }
        });

        txtNominalTunjangan.setText("Rp.");
        txtNominalTunjangan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtNominalTunjanganActionPerformed(evt);
            }
        });

        cmbJabatan.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "-- Pilih jabatan --", "Manager", "Supervisor", "Staff", "Admin", "HRD", "Keuangan", "IT Support", "Marketing" }));

        txtKeterangan.setColumns(20);
        txtKeterangan.setRows(5);
        jScrollPane1.setViewportView(txtKeterangan);

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(15, 118, 110));
        jLabel3.setText("Form Tunjangan");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel6)
                                    .addComponent(jLabel7))
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addGap(53, 53, 53)
                                        .addComponent(cmbJabatan, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addGap(56, 56, 56)
                                        .addComponent(txtNominalTunjangan))))
                            .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel5)
                                    .addComponent(jLabel4))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(jPanel1Layout.createSequentialGroup()
                                        .addComponent(txtidTunjangan, javax.swing.GroupLayout.PREFERRED_SIZE, 145, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(0, 0, Short.MAX_VALUE))
                                    .addComponent(txtNamaTunjangan)))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel8)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 39, Short.MAX_VALUE)
                                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 214, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(14, 14, 14))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(txtidTunjangan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtNamaTunjangan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtNominalTunjangan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(16, 16, 16)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cmbJabatan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel7))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel8)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(89, Short.MAX_VALUE))
        );

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

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(23, 55, 94));
        jLabel1.setText("DATA TUNJANGAN ");

        jLabel2.setForeground(new java.awt.Color(23, 55, 94));
        jLabel2.setText("Kelola Data Tunjangan Karyawan");

        btnFirst1.setFont(new java.awt.Font("Segoe UI", 1, 10)); // NOI18N
        btnFirst1.setText("<<");

        btnRefresh1.setBackground(new java.awt.Color(243, 244, 246));
        btnRefresh1.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnRefresh1.setForeground(new java.awt.Color(17, 24, 39));
        btnRefresh1.setText("Refresh");
        btnRefresh1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRefresh1ActionPerformed(evt);
            }
        });

        btnPrev1.setFont(new java.awt.Font("Segoe UI", 1, 10)); // NOI18N
        btnPrev1.setText("<");
        btnPrev1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnPrev1ActionPerformed(evt);
            }
        });

        btnNext1.setFont(new java.awt.Font("Segoe UI", 1, 10)); // NOI18N
        btnNext1.setText(">");
        btnNext1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnNext1ActionPerformed(evt);
            }
        });

        btnLast1.setFont(new java.awt.Font("Segoe UI", 1, 10)); // NOI18N
        btnLast1.setText(">>");
        btnLast1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnLast1ActionPerformed(evt);
            }
        });

        lblPage1.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblPage1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblPage1.setText("1");

        tblTunjangan.setForeground(new java.awt.Color(23, 55, 94));
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
        ));
        tblTunjangan.setShowHorizontalLines(true);
        tblTunjangan.setShowVerticalLines(true);
        jScrollPane3.setViewportView(tblTunjangan);

        jLabel10.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel10.setForeground(new java.awt.Color(15, 118, 110));
        jLabel10.setText("Daftar Tunjangan");

        btnCariTunjangan.setText("🔍");
        btnCariTunjangan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCariTunjanganActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout scrollTunjanganLayout = new javax.swing.GroupLayout(scrollTunjangan);
        scrollTunjangan.setLayout(scrollTunjanganLayout);
        scrollTunjanganLayout.setHorizontalGroup(
            scrollTunjanganLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(scrollTunjanganLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                .addGroup(scrollTunjanganLayout.createSequentialGroup()
                    .addComponent(btnFirst1, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(btnPrev1, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(lblPage1, javax.swing.GroupLayout.PREFERRED_SIZE, 24, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(5, 5, 5)
                    .addComponent(btnNext1, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(btnLast1, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 515, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(scrollTunjanganLayout.createSequentialGroup()
                .addComponent(jLabel10)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(txtCariTunjangan, javax.swing.GroupLayout.PREFERRED_SIZE, 138, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnCariTunjangan)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(btnRefresh1)
                .addGap(9, 9, 9))
        );
        scrollTunjanganLayout.setVerticalGroup(
            scrollTunjanganLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(scrollTunjanganLayout.createSequentialGroup()
                .addGroup(scrollTunjanganLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(scrollTunjanganLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addComponent(txtCariTunjangan, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(scrollTunjanganLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnCariTunjangan)
                            .addComponent(btnRefresh1)))
                    .addComponent(jLabel10))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 169, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(scrollTunjanganLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnFirst1)
                    .addComponent(btnPrev1)
                    .addComponent(btnNext1)
                    .addComponent(btnLast1)
                    .addComponent(lblPage1))
                .addGap(0, 13, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(27, 27, 27)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(scrollTunjangan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(30, 30, 30)
                                .addComponent(jButton5)
                                .addGap(18, 18, 18)
                                .addComponent(btnUbah)
                                .addGap(18, 18, 18)
                                .addComponent(btnHapus)
                                .addGap(18, 18, 18)
                                .addComponent(btnReset))))
                    .addComponent(jLabel1)
                    .addComponent(jLabel2))
                .addContainerGap(29, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(24, 24, 24)
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(120, 120, 120)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jButton5)
                            .addComponent(btnUbah)
                            .addComponent(btnHapus)
                            .addComponent(btnReset))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(scrollTunjangan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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

    private void btnPrev1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnPrev1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnPrev1ActionPerformed

    private void btnNext1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnNext1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnNext1ActionPerformed

    private void btnLast1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnLast1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnLast1ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCariTunjangan;
    private javax.swing.JButton btnFirst1;
    private javax.swing.JButton btnHapus;
    private javax.swing.JButton btnLast1;
    private javax.swing.JButton btnNext1;
    private javax.swing.JButton btnPrev1;
    private javax.swing.JButton btnRefresh1;
    private javax.swing.JButton btnReset;
    private javax.swing.JButton btnUbah;
    private javax.swing.JComboBox<String> cmbJabatan;
    private javax.swing.JButton jButton5;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JLabel lblPage1;
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
