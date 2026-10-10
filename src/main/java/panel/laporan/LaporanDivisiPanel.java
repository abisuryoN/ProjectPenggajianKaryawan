package panel.laporan;

import config.Koneksi;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

public class LaporanDivisiPanel extends javax.swing.JPanel {

    /** Tanggal cetak format Indonesia, mengikuti hari saat laporan dicetak. */
    private static String tanggalCetakIndonesia() {
        java.time.LocalDate t = java.time.LocalDate.now();
        String[] hari = {"", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu"};
        String[] bulan = {"", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"};
        return hari[t.getDayOfWeek().getValue()] + ", " + t.getDayOfMonth() + " "
                + bulan[t.getMonthValue()] + " " + t.getYear();
    }

    public LaporanDivisiPanel() {
        initComponents();
        styleButton();
        tampilData();
        util.DesignUtil.applyPage(this);
        tblDivisi.setDefaultEditor(Object.class, null);
        tblDivisi.getTableHeader().setReorderingAllowed(false);
    }

    private void styleButton() {
        util.DesignUtil.styleFilledButton(btnCari, util.DesignUtil.TOMBOL_UTAMA, java.awt.Color.WHITE);
        util.DesignUtil.styleFilledButton(btnCetak, util.DesignUtil.TOMBOL_UTAMA, java.awt.Color.WHITE);
        util.DesignUtil.styleFilledButton(btnExportPDF, util.DesignUtil.TOMBOL_SEKUNDER, java.awt.Color.WHITE);
        util.DesignUtil.styleFilledButton(btnRefresh, util.DesignUtil.TOMBOL_NETRAL, util.DesignUtil.TEKS_GELAP);
        util.DesignUtil.styleFilledButton(btnReset, util.DesignUtil.TOMBOL_BAHAYA, java.awt.Color.WHITE);
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

    private DefaultTableModel buatModel() {
        DefaultTableModel model = util.TableUtil.nonEditableModel();
        model.addColumn("No");
        model.addColumn("ID Divisi");
        model.addColumn("Nama Divisi");
        model.addColumn("Kepala Divisi");
        model.addColumn("Jumlah Karyawan");
        model.addColumn("Keterangan");
        return model;
    }

    private void tampilData() {
        DefaultTableModel model = buatModel();
        try {
            Connection conn = Koneksi.getConnection();
            PreparedStatement pst = conn.prepareStatement(
                    "SELECT d.id_divisi, d.nama_divisi, d.kepala_divisi, d.keterangan, "
                            + "COUNT(k.id_karyawan) AS jumlah_karyawan "
                            + "FROM divisi d "
                            + "LEFT JOIN karyawan k ON k.id_divisi = d.id_divisi "
                            + "GROUP BY d.id_divisi, d.nama_divisi, d.kepala_divisi, d.keterangan "
                            + "ORDER BY d.id_divisi ASC");
            ResultSet rs = pst.executeQuery();
            int no = 1;
            while (rs.next()) {
                model.addRow(new Object[] {
                        no++,
                        rs.getString("id_divisi"),
                        rs.getString("nama_divisi"),
                        rs.getString("kepala_divisi"),
                        rs.getString("jumlah_karyawan"),
                        rs.getString("keterangan")
                });
            }
            tblDivisi.setModel(model);
            util.DesignUtil.applyTable(tblDivisi);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal menampilkan laporan divisi: " + e.getMessage());
        }
    }

    private void cariData() {
        DefaultTableModel model = buatModel();
        String keyword = "%" + txtCari.getText().trim() + "%";
        try {
            Connection conn = Koneksi.getConnection();
            PreparedStatement pst = conn.prepareStatement(
                    "SELECT d.id_divisi, d.nama_divisi, d.kepala_divisi, d.keterangan, "
                            + "COUNT(k.id_karyawan) AS jumlah_karyawan "
                            + "FROM divisi d "
                            + "LEFT JOIN karyawan k ON k.id_divisi = d.id_divisi "
                            + "WHERE d.nama_divisi LIKE ? OR d.kepala_divisi LIKE ? OR d.keterangan LIKE ? "
                            + "GROUP BY d.id_divisi, d.nama_divisi, d.kepala_divisi, d.keterangan "
                            + "ORDER BY d.id_divisi ASC");
            pst.setString(1, keyword);
            pst.setString(2, keyword);
            pst.setString(3, keyword);
            ResultSet rs = pst.executeQuery();
            int no = 1;
            while (rs.next()) {
                model.addRow(new Object[] {
                        no++,
                        rs.getString("id_divisi"),
                        rs.getString("nama_divisi"),
                        rs.getString("kepala_divisi"),
                        rs.getString("jumlah_karyawan"),
                        rs.getString("keterangan")
                });
            }
            tblDivisi.setModel(model);
            util.DesignUtil.applyTable(tblDivisi);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal mencari laporan divisi: " + e.getMessage());
        }
    }

    private void cetakData() {
        try {
            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("tanggal_cetak", tanggalCetakIndonesia());
            util.ReportUtil.showReport("laporan_divisi", params);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal mencetak laporan divisi: " + e.getMessage());
        }
    }

    private void exportPDF() {
        cetakData();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code manually kalau masih mau pakai tab Design
     * NetBeans.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblTitle = new javax.swing.JLabel();
        jPanelFilter = new javax.swing.JPanel();
        lblCari = new javax.swing.JLabel();
        txtCari = new javax.swing.JTextField();
        pnlAksi = new javax.swing.JPanel();
        btnCari = new javax.swing.JButton();
        btnCetak = new javax.swing.JButton();
        btnExportPDF = new javax.swing.JButton();
        btnRefresh = new javax.swing.JButton();
        btnReset = new javax.swing.JButton();
        jPanelData = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblDivisi = new javax.swing.JTable();

        setBackground(new java.awt.Color(244, 247, 250));

        lblTitle.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        lblTitle.setForeground(new java.awt.Color(22, 48, 92));
        lblTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblTitle.setText("LAPORAN DATA DIVISI");

        jPanelFilter.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Filter Pencarian", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI", 1, 12), new java.awt.Color(22, 48, 92))); // NOI18N

        lblCari.setFont(new java.awt.Font("Segoe UI", 0, 13)); // NOI18N
        lblCari.setText("Cari Divisi");

        txtCari.setPreferredSize(new java.awt.Dimension(300, 34));

        javax.swing.GroupLayout jPanelFilterLayout = new javax.swing.GroupLayout(jPanelFilter);
        jPanelFilter.setLayout(jPanelFilterLayout);
        jPanelFilterLayout.setHorizontalGroup(
            jPanelFilterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelFilterLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblCari)
                .addGap(18, 18, 18)
                .addComponent(txtCari, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(16, Short.MAX_VALUE))
        );
        jPanelFilterLayout.setVerticalGroup(
            jPanelFilterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelFilterLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanelFilterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblCari)
                    .addComponent(txtCari, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pnlAksi.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Aksi", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI", 1, 12), new java.awt.Color(22, 48, 92))); // NOI18N

        btnCari.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnCari.setText("Cari");

        btnCetak.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnCetak.setText("Cetak");

        btnExportPDF.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnExportPDF.setText("Export PDF");

        btnRefresh.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnRefresh.setText("Refresh");

        btnReset.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnReset.setText("Reset");

        btnCari.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCariActionPerformed(evt);
            }
        });

        btnCetak.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCetakActionPerformed(evt);
            }
        });

        btnExportPDF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExportPDFActionPerformed(evt);
            }
        });

        btnRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRefreshActionPerformed(evt);
            }
        });

        btnReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnResetActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnlAksiLayout = new javax.swing.GroupLayout(pnlAksi);
        pnlAksi.setLayout(pnlAksiLayout);
        pnlAksiLayout.setHorizontalGroup(
            pnlAksiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlAksiLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(btnCari)
                .addGap(10, 10, 10)
                .addComponent(btnCetak)
                .addGap(10, 10, 10)
                .addComponent(btnExportPDF)
                .addGap(10, 10, 10)
                .addComponent(btnRefresh)
                .addGap(10, 10, 10)
                .addComponent(btnReset)
                .addContainerGap(16, Short.MAX_VALUE))
        );
        pnlAksiLayout.setVerticalGroup(
            pnlAksiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlAksiLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlAksiLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCari)
                    .addComponent(btnCetak)
                    .addComponent(btnExportPDF)
                    .addComponent(btnRefresh)
                    .addComponent(btnReset))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanelData.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Data Laporan Divisi", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI", 1, 12), new java.awt.Color(22, 48, 92))); // NOI18N

        tblDivisi.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "No", "ID Divisi", "Nama Divisi", "Kepala Divisi", "Jumlah Karyawan", "Keterangan"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        jScrollPane1.setViewportView(tblDivisi);
        tblDivisi.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        tblDivisi.setFillsViewportHeight(true);

        javax.swing.GroupLayout jPanelDataLayout = new javax.swing.GroupLayout(jPanelData);
        jPanelData.setLayout(jPanelDataLayout);
        jPanelDataLayout.setHorizontalGroup(
            jPanelDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelDataLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1)
                .addGap(8, 8, 8))
        );
        jPanelDataLayout.setVerticalGroup(
            jPanelDataLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelDataLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
                .addGap(8, 8, 8))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblTitle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jPanelFilter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGap(18, 18, 18)
                        .addComponent(pnlAksi, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanelData, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(26, 26, 26))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(lblTitle, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(10, 10, 10)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanelFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(pnlAksi, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12)
                .addComponent(jPanelData, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(14, 14, 14))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnCariActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnCariActionPerformed
        cariData();
    }// GEN-LAST:event_btnCariActionPerformed

    private void btnResetActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnResetActionPerformed
        txtCari.setText("");
        tampilData();
    }// GEN-LAST:event_btnResetActionPerformed

    private void btnCetakActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnCetakActionPerformed
        cetakData();
    }// GEN-LAST:event_btnCetakActionPerformed

    private void btnExportPDFActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnExportPDFActionPerformed
        exportPDF();
    }// GEN-LAST:event_btnExportPDFActionPerformed

    private void btnRefreshActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnRefreshActionPerformed
        tampilData();
    }// GEN-LAST:event_btnRefreshActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCari;
    private javax.swing.JButton btnCetak;
    private javax.swing.JButton btnExportPDF;
    private javax.swing.JButton btnRefresh;
    private javax.swing.JButton btnReset;
    private javax.swing.JPanel jPanelData;
    private javax.swing.JPanel jPanelFilter;
    private javax.swing.JPanel pnlAksi;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblCari;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JTable tblDivisi;
    private javax.swing.JTextField txtCari;
    // End of variables declaration//GEN-END:variables
}
