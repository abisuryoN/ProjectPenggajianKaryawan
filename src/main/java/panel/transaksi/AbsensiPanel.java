package panel.transaksi;
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
public class AbsensiPanel extends javax.swing.JPanel {

    public AbsensiPanel() {
    initComponents();
    styleButton();
    loadKaryawan();
    tampilData();
    resetForm();
    setTanggalHariIni();
    util.DesignUtil.applyPage(this);

    tblAbsensi.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            ambilDataDariTabel();
        }
    });

    btnSimpan.addActionListener(e -> simpanData());
    btnUbah.addActionListener(e -> ubahData());
    btnHapus.addActionListener(e -> hapusData());
    btnReset.addActionListener(e -> resetForm());
    btnRefresh.addActionListener(e -> { tampilData(); resetForm(); });
    btnCari.addActionListener(e -> cariData());
    btnAbsenMasuk.addActionListener(e -> absenMasuk());
    btnAbsenPulang.addActionListener(e -> absenPulang());
}
    private void styleButton() {
    styleFilledButton(btnAbsenMasuk, new java.awt.Color(20, 184, 166), java.awt.Color.WHITE);
    styleFilledButton(btnAbsenPulang, new java.awt.Color(249, 115, 22), java.awt.Color.WHITE);
    styleFilledButton(btnSimpan, new java.awt.Color(20, 184, 166), java.awt.Color.WHITE);
    styleFilledButton(btnUbah, new java.awt.Color(37, 99, 235), java.awt.Color.WHITE);
    styleFilledButton(btnHapus, new java.awt.Color(239, 68, 68), java.awt.Color.WHITE);
    styleFilledButton(btnReset, new java.awt.Color(243, 244, 246), new java.awt.Color(17, 24, 39));
    styleFilledButton(btnRefresh, new java.awt.Color(243, 244, 246), new java.awt.Color(17, 24, 39));
    styleFilledButton(btnCari, new java.awt.Color(243, 244, 246), new java.awt.Color(17, 24, 39));
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

private void setTanggalHariIni() {
    dateTanggal.setText(java.time.LocalDate.now().toString());
}

private void loadKaryawan() {
    cmbKaryawan.removeAllItems();
    cmbKaryawan.addItem("-- Pilih Karyawan --");
    try {
        Connection conn = Koneksi.getConnection();
        String sql;
        if ("HRD".equalsIgnoreCase(util.Session.getRole())) {
            sql = "SELECT id_karyawan, nama_karyawan FROM karyawan ORDER BY nama_karyawan";
        } else {
            sql = "SELECT id_karyawan, nama_karyawan FROM karyawan WHERE id_karyawan = " + util.Session.getIdKaryawan();
        }
        PreparedStatement pst = conn.prepareStatement(sql);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            cmbKaryawan.addItem(rs.getString("id_karyawan") + " - " + rs.getString("nama_karyawan"));
        }
        if (!"HRD".equalsIgnoreCase(util.Session.getRole())) {
            cmbKaryawan.setSelectedIndex(1);
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal load karyawan: " + e.getMessage());
    }
}

private void tampilData() {
    DefaultTableModel model = new DefaultTableModel();
    model.addColumn("ID");
    model.addColumn("Nama Karyawan");
    model.addColumn("Tanggal");
    model.addColumn("Jam Masuk");
    model.addColumn("Jam Pulang");
    model.addColumn("Status");
    model.addColumn("Keterangan");

    try {
        String sql;
        if ("HRD".equalsIgnoreCase(util.Session.getRole())) {
            sql = "SELECT a.*, k.nama_karyawan FROM absensi a "
                + "JOIN karyawan k ON a.id_karyawan = k.id_karyawan "
                + "ORDER BY a.tanggal DESC, a.id_absensi DESC";
        } else {
            sql = "SELECT a.*, k.nama_karyawan FROM absensi a "
                + "JOIN karyawan k ON a.id_karyawan = k.id_karyawan "
                + "WHERE a.id_karyawan = " + util.Session.getIdKaryawan()
                + " ORDER BY a.tanggal DESC";
        }
        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement(sql);
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            model.addRow(new Object[]{
                rs.getString("id_absensi"),
                rs.getString("nama_karyawan"),
                rs.getString("tanggal"),
                rs.getString("jam_masuk"),
                rs.getString("jam_pulang"),
                rs.getString("status"),
                rs.getString("keterangan") != null ? rs.getString("keterangan") : ""
            });
        }
        tblAbsensi.setModel(model);
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal tampil data: " + e.getMessage());
    }
}

private void absenMasuk() {
    if (cmbKaryawan.getSelectedIndex() == 0) {
        JOptionPane.showMessageDialog(this, "Pilih karyawan dulu.");
        return;
    }
    try {
        String selected = cmbKaryawan.getSelectedItem().toString();
        int idKaryawan = Integer.parseInt(selected.split(" - ")[0]);
        String tanggal = java.time.LocalDate.now().toString();
        String jamMasuk = java.time.LocalTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));

        // Cek sudah absen masuk hari ini belum
        Connection conn = Koneksi.getConnection();
        PreparedStatement cek = conn.prepareStatement(
            "SELECT id_absensi FROM absensi WHERE id_karyawan=? AND tanggal=?"
        );
        cek.setInt(1, idKaryawan);
        cek.setString(2, tanggal);
        ResultSet rs = cek.executeQuery();
        if (rs.next()) {
            JOptionPane.showMessageDialog(this, "Sudah absen masuk hari ini!");
            return;
        }

        PreparedStatement pst = conn.prepareStatement(
            "INSERT INTO absensi (id_karyawan, tanggal, jam_masuk, status) VALUES (?, ?, ?, 'Hadir')"
        );
        pst.setInt(1, idKaryawan);
        pst.setString(2, tanggal);
        pst.setString(3, jamMasuk);
        pst.executeUpdate();

        JOptionPane.showMessageDialog(this, "Absen masuk berhasil! Jam: " + jamMasuk);
        tampilData();
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal absen masuk: " + e.getMessage());
    }
}

private void absenPulang() {
    if (cmbKaryawan.getSelectedIndex() == 0) {
        JOptionPane.showMessageDialog(this, "Pilih karyawan dulu.");
        return;
    }
    try {
        String selected = cmbKaryawan.getSelectedItem().toString();
        int idKaryawan = Integer.parseInt(selected.split(" - ")[0]);
        String tanggal = java.time.LocalDate.now().toString();
        String jamPulang = java.time.LocalTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));

        Connection conn = Koneksi.getConnection();
        PreparedStatement cek = conn.prepareStatement(
            "SELECT id_absensi FROM absensi WHERE id_karyawan=? AND tanggal=? AND jam_masuk IS NOT NULL"
        );
        cek.setInt(1, idKaryawan);
        cek.setString(2, tanggal);
        ResultSet rs = cek.executeQuery();
        if (!rs.next()) {
            JOptionPane.showMessageDialog(this, "Belum absen masuk hari ini!");
            return;
        }

        PreparedStatement pst = conn.prepareStatement(
            "UPDATE absensi SET jam_pulang=? WHERE id_karyawan=? AND tanggal=?"
        );
        pst.setString(1, jamPulang);
        pst.setInt(2, idKaryawan);
        pst.setString(3, tanggal);
        pst.executeUpdate();

        JOptionPane.showMessageDialog(this, "Absen pulang berhasil! Jam: " + jamPulang);
        tampilData();
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal absen pulang: " + e.getMessage());
    }
}

private void simpanData() {
    if (cmbKaryawan.getSelectedIndex() == 0) {
        JOptionPane.showMessageDialog(this, "Pilih karyawan dulu.");
        return;
    }
    try {
        String selected = cmbKaryawan.getSelectedItem().toString();
        int idKaryawan = Integer.parseInt(selected.split(" - ")[0]);
        String tanggal = dateTanggal.getText().trim();
        String jamMasuk = txtJamMasuk.getText().trim();
        String jamPulang = txtJamPulang.getText().trim();
        String status = cmbStatus.getSelectedItem().toString();
        String keterangan = txtKeterangan.getText().trim();

        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement(
            "INSERT INTO absensi (id_karyawan, tanggal, jam_masuk, jam_pulang, status, keterangan) "
            + "VALUES (?, ?, ?, ?, ?, ?)"
        );
        pst.setInt(1, idKaryawan);
        pst.setString(2, tanggal);
        pst.setString(3, jamMasuk.isEmpty() ? null : jamMasuk);
        pst.setString(4, jamPulang.isEmpty() ? null : jamPulang);
        pst.setString(5, status);
        pst.setString(6, keterangan);
        pst.executeUpdate();

        JOptionPane.showMessageDialog(this, "Data absensi berhasil disimpan.");
        tampilData();
        resetForm();
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal simpan: " + e.getMessage());
    }
}

private void ubahData() {
    String idAbsensi = txtIdAbsensi.getText().trim();
    if (idAbsensi.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Pilih data dari tabel dulu.");
        return;
    }
    try {
        String jamMasuk = txtJamMasuk.getText().trim();
        String jamPulang = txtJamPulang.getText().trim();
        String status = cmbStatus.getSelectedItem().toString();
        String keterangan = txtKeterangan.getText().trim();

        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement(
            "UPDATE absensi SET jam_masuk=?, jam_pulang=?, status=?, keterangan=? WHERE id_absensi=?"
        );
        pst.setString(1, jamMasuk.isEmpty() ? null : jamMasuk);
        pst.setString(2, jamPulang.isEmpty() ? null : jamPulang);
        pst.setString(3, status);
        pst.setString(4, keterangan);
        pst.setString(5, idAbsensi);
        pst.executeUpdate();

        JOptionPane.showMessageDialog(this, "Data absensi berhasil diubah.");
        tampilData();
        resetForm();
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal ubah: " + e.getMessage());
    }
}

private void hapusData() {
    String idAbsensi = txtIdAbsensi.getText().trim();
    if (idAbsensi.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Pilih data dari tabel dulu.");
        return;
    }
    int konfirmasi = JOptionPane.showConfirmDialog(this,
        "Yakin hapus data absensi ini?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
    if (konfirmasi == JOptionPane.YES_OPTION) {
        try {
            Connection conn = Koneksi.getConnection();
            PreparedStatement pst = conn.prepareStatement(
                "DELETE FROM absensi WHERE id_absensi=?"
            );
            pst.setString(1, idAbsensi);
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Data berhasil dihapus.");
            tampilData();
            resetForm();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal hapus: " + e.getMessage());
        }
    }
}

private void cariData() {
    String keyword = txtCari.getText().trim();
    DefaultTableModel model = new DefaultTableModel();
    model.addColumn("ID");
    model.addColumn("Nama Karyawan");
    model.addColumn("Tanggal");
    model.addColumn("Jam Masuk");
    model.addColumn("Jam Pulang");
    model.addColumn("Status");
    model.addColumn("Keterangan");

    try {
        String sql = "SELECT a.*, k.nama_karyawan FROM absensi a "
                + "JOIN karyawan k ON a.id_karyawan = k.id_karyawan "
                + "WHERE k.nama_karyawan LIKE ? OR a.tanggal LIKE ? OR a.status LIKE ? "
                + "ORDER BY a.tanggal DESC";
        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setString(1, "%" + keyword + "%");
        pst.setString(2, "%" + keyword + "%");
        pst.setString(3, "%" + keyword + "%");
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            model.addRow(new Object[]{
                rs.getString("id_absensi"),
                rs.getString("nama_karyawan"),
                rs.getString("tanggal"),
                rs.getString("jam_masuk"),
                rs.getString("jam_pulang"),
                rs.getString("status"),
                rs.getString("keterangan") != null ? rs.getString("keterangan") : ""
            });
        }
        tblAbsensi.setModel(model);
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal cari: " + e.getMessage());
    }
}

private void resetForm() {
    txtIdAbsensi.setText("");
    txtJamMasuk.setText("");
    txtJamPulang.setText("");
    txtKeterangan.setText("");
    setTanggalHariIni();
    cmbStatus.setSelectedIndex(0);
    if ("HRD".equalsIgnoreCase(util.Session.getRole())) cmbKaryawan.setSelectedIndex(0);

    btnSimpan.setEnabled(true);
    btnUbah.setEnabled(false);
    btnHapus.setEnabled(false);
}

private void ambilDataDariTabel() {
    int baris = tblAbsensi.getSelectedRow();
    if (baris >= 0) {
        txtIdAbsensi.setText(tblAbsensi.getValueAt(baris, 0).toString());
        dateTanggal.setText(tblAbsensi.getValueAt(baris, 2) != null ?
            tblAbsensi.getValueAt(baris, 2).toString() : "");
        txtJamMasuk.setText(tblAbsensi.getValueAt(baris, 3) != null ?
            tblAbsensi.getValueAt(baris, 3).toString() : "");
        txtJamPulang.setText(tblAbsensi.getValueAt(baris, 4) != null ?
            tblAbsensi.getValueAt(baris, 4).toString() : "");
        cmbStatus.setSelectedItem(tblAbsensi.getValueAt(baris, 5) != null ?
            tblAbsensi.getValueAt(baris, 5).toString() : "Hadir");
        txtKeterangan.setText(tblAbsensi.getValueAt(baris, 6) != null ?
            tblAbsensi.getValueAt(baris, 6).toString() : "");

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

        btnAbsenMasuk = new javax.swing.JButton();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        txtIdAbsensi = new javax.swing.JTextField();
        txtJamMasuk = new javax.swing.JTextField();
        txtJamPulang = new javax.swing.JTextField();
        cmbStatus = new javax.swing.JComboBox<>();
        txtKeterangan = new javax.swing.JTextField();
        dateTanggal = new javax.swing.JTextField();
        cmbKaryawan = new javax.swing.JComboBox<>();
        btnAbsenPulang = new javax.swing.JButton();
        btnSimpan = new javax.swing.JButton();
        btnUbah = new javax.swing.JButton();
        btnRefresh = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        btnReset = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblAbsensi = new javax.swing.JTable();
        txtCari = new javax.swing.JTextField();
        btnCari = new javax.swing.JButton();
        btnHapus = new javax.swing.JButton();

        btnAbsenMasuk.setFont(new java.awt.Font("sansserif", 3, 12)); // NOI18N
        btnAbsenMasuk.setText("Absen Masuk");

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createTitledBorder(null, "Input", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("sansserif", 1, 12), new java.awt.Color(0, 153, 153)), "", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI", 0, 12), new java.awt.Color(0, 153, 153))); // NOI18N

        jLabel2.setText("ID Absensi ");

        jLabel3.setText("Nama Karyawan");

        jLabel4.setText("Tanggal");

        jLabel5.setText("Jam Masuk");

        jLabel6.setText("Jam Pulang ");

        jLabel7.setText("Status");

        jLabel8.setText("Keterangan");

        txtJamPulang.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtJamPulangActionPerformed(evt);
            }
        });

        cmbStatus.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Hadir", "Izin", "Sakit", "Alpha", "Cuti" }));

        cmbKaryawan.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3)
                            .addComponent(jLabel4)
                            .addComponent(jLabel5))
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(51, 51, 51)
                                .addComponent(txtIdAbsensi, javax.swing.GroupLayout.PREFERRED_SIZE, 174, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(txtJamMasuk, javax.swing.GroupLayout.DEFAULT_SIZE, 174, Short.MAX_VALUE)
                                        .addComponent(dateTanggal))
                                    .addComponent(cmbKaryawan, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 174, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel6)
                                    .addComponent(jLabel7))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel8)
                                .addGap(77, 77, 77)))
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(txtJamPulang)
                            .addComponent(cmbStatus, 0, 174, Short.MAX_VALUE)
                            .addComponent(txtKeterangan))))
                .addContainerGap(21, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(9, 9, 9)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(txtIdAbsensi, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(14, 14, 14)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(cmbKaryawan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(dateTanggal, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(txtJamMasuk, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(txtJamPulang, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(cmbStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(txtKeterangan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(18, Short.MAX_VALUE))
        );

        btnAbsenPulang.setFont(new java.awt.Font("sansserif", 3, 12)); // NOI18N
        btnAbsenPulang.setText("Absen Pulang");

        btnSimpan.setBackground(new java.awt.Color(0, 255, 204));
        btnSimpan.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnSimpan.setText("Simpan");

        btnUbah.setBackground(new java.awt.Color(51, 153, 255));
        btnUbah.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnUbah.setText("Ubah");

        btnRefresh.setFont(new java.awt.Font("sansserif", 1, 12)); // NOI18N
        btnRefresh.setText("Refresh");

        jLabel1.setFont(new java.awt.Font("Franklin Gothic Medium", 1, 16)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(0, 102, 153));
        jLabel1.setText("ABSENSI KARYAWAN");

        btnReset.setFont(new java.awt.Font("sansserif", 1, 12)); // NOI18N
        btnReset.setText("Reset");

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Data Absensi", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("sansserif", 3, 12), new java.awt.Color(0, 153, 153))); // NOI18N

        tblAbsensi.setModel(new javax.swing.table.DefaultTableModel(
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
        jScrollPane1.setViewportView(tblAbsensi);

        btnCari.setText("Cari");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 533, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(txtCari, javax.swing.GroupLayout.PREFERRED_SIZE, 199, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnCari)))
                .addContainerGap(26, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addGap(22, 22, 22)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtCari, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnCari))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 225, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(31, Short.MAX_VALUE))
        );

        btnHapus.setBackground(new java.awt.Color(255, 0, 0));
        btnHapus.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnHapus.setText("Hapus");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addGap(39, 39, 39)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(btnSimpan)
                                .addGap(32, 32, 32)
                                .addComponent(btnHapus)
                                .addGap(50, 50, 50)
                                .addComponent(btnUbah)
                                .addGap(41, 41, 41)
                                .addComponent(btnReset)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(btnRefresh)
                                .addGap(47, 47, 47))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(btnAbsenMasuk)
                                .addGap(116, 116, 116)
                                .addComponent(btnAbsenPulang)
                                .addGap(112, 112, 112))))))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addGap(404, 404, 404))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGap(17, 17, 17)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(25, 25, 25)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnAbsenPulang)
                            .addComponent(btnAbsenMasuk))
                        .addGap(24, 24, 24)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnSimpan)
                            .addComponent(btnHapus)
                            .addComponent(btnUbah)
                            .addComponent(btnRefresh)
                            .addComponent(btnReset))
                        .addGap(18, 18, 18)
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(16, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void txtJamPulangActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtJamPulangActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtJamPulangActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnAbsenMasuk;
    private javax.swing.JButton btnAbsenPulang;
    private javax.swing.JButton btnCari;
    private javax.swing.JButton btnHapus;
    private javax.swing.JButton btnRefresh;
    private javax.swing.JButton btnReset;
    private javax.swing.JButton btnSimpan;
    private javax.swing.JButton btnUbah;
    private javax.swing.JComboBox<String> cmbKaryawan;
    private javax.swing.JComboBox<String> cmbStatus;
    private javax.swing.JTextField dateTanggal;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable tblAbsensi;
    private javax.swing.JTextField txtCari;
    private javax.swing.JTextField txtIdAbsensi;
    private javax.swing.JTextField txtJamMasuk;
    private javax.swing.JTextField txtJamPulang;
    private javax.swing.JTextField txtKeterangan;
    // End of variables declaration//GEN-END:variables
}
