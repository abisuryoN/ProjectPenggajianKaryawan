package panel.laporan;

import config.Koneksi;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

public class LaporanDivisiPanel extends javax.swing.JPanel {

    public LaporanDivisiPanel() {
        initComponents();
        styleButton();
        tampilData();
    }

    private void styleButton() {
        styleFilledButton(btnCari, new java.awt.Color(13, 71, 161), java.awt.Color.WHITE);
        styleFilledButton(btnCetak, new java.awt.Color(13, 71, 161), java.awt.Color.WHITE);
        styleFilledButton(btnExportPDF, new java.awt.Color(13, 71, 161), java.awt.Color.WHITE);
        styleFilledButton(btnRefresh, new java.awt.Color(13, 71, 161), java.awt.Color.WHITE);
        styleFilledButton(btnReset, new java.awt.Color(245, 245, 245), java.awt.Color.BLACK);
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
        DefaultTableModel model = new DefaultTableModel();
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
                    + "ORDER BY d.id_divisi ASC"
            );
            ResultSet rs = pst.executeQuery();
            int no = 1;
            while (rs.next()) {
                model.addRow(new Object[]{
                    no++,
                    rs.getString("id_divisi"),
                    rs.getString("nama_divisi"),
                    rs.getString("kepala_divisi"),
                    rs.getString("jumlah_karyawan"),
                    rs.getString("keterangan")
                });
            }
            tblDivisi.setModel(model);
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
                    + "ORDER BY d.id_divisi ASC"
            );
            pst.setString(1, keyword);
            pst.setString(2, keyword);
            pst.setString(3, keyword);
            ResultSet rs = pst.executeQuery();
            int no = 1;
            while (rs.next()) {
                model.addRow(new Object[]{
                    no++,
                    rs.getString("id_divisi"),
                    rs.getString("nama_divisi"),
                    rs.getString("kepala_divisi"),
                    rs.getString("jumlah_karyawan"),
                    rs.getString("keterangan")
                });
            }
            tblDivisi.setModel(model);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal mencari laporan divisi: " + e.getMessage());
        }
    }

    private void cetakData() {
        try {
            util.ReportUtil.showReport("laporan_divisi", new java.util.HashMap<String, Object>());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal mencetak laporan divisi: " + e.getMessage());
        }
    }

    private void exportPDF() {
        cetakData();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code manually kalau masih mau pakai tab Design NetBeans.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pnlHeader = new javax.swing.JPanel();
        lblTitle = new javax.swing.JLabel();
        lblSubtitle = new javax.swing.JLabel();
        lblFilter = new javax.swing.JLabel();
        lblCari = new javax.swing.JLabel();
        lblColon = new javax.swing.JLabel();
        txtCari = new javax.swing.JTextField();
        btnCari = new javax.swing.JButton();
        btnReset = new javax.swing.JButton();
        lblAksi = new javax.swing.JLabel();
        btnCetak = new javax.swing.JButton();
        btnExportPDF = new javax.swing.JButton();
        btnRefresh = new javax.swing.JButton();
        lblData = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblDivisi = new javax.swing.JTable();

        setBackground(new java.awt.Color(255, 255, 255));
        setPreferredSize(new java.awt.Dimension(900, 600));

        pnlHeader.setBackground(new java.awt.Color(13, 71, 161));

        lblTitle.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        lblTitle.setForeground(new java.awt.Color(255, 255, 255));
        lblTitle.setText("LAPORAN DATA DIVISI");

        lblSubtitle.setFont(new java.awt.Font("Segoe UI", 0, 16)); // NOI18N
        lblSubtitle.setForeground(new java.awt.Color(255, 255, 255));
        lblSubtitle.setText("Sistem Penggajian Karyawan");

        javax.swing.GroupLayout pnlHeaderLayout = new javax.swing.GroupLayout(pnlHeader);
        pnlHeader.setLayout(pnlHeaderLayout);
        pnlHeaderLayout.setHorizontalGroup(
            pnlHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlHeaderLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblTitle)
                    .addComponent(lblSubtitle))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        pnlHeaderLayout.setVerticalGroup(
            pnlHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlHeaderLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblTitle)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblSubtitle)
                .addContainerGap(16, Short.MAX_VALUE))
        );

        lblFilter.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lblFilter.setForeground(new java.awt.Color(13, 71, 161));
        lblFilter.setText("FILTER PENCARIAN");

        lblCari.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        lblCari.setText("Cari Divisi");

        lblColon.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        lblColon.setText(":");

        btnCari.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnCari.setText("Cari");
        btnCari.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCariActionPerformed(evt);
            }
        });

        btnReset.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnReset.setText("Reset");
        btnReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnResetActionPerformed(evt);
            }
        });

        lblAksi.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lblAksi.setForeground(new java.awt.Color(13, 71, 161));
        lblAksi.setText("AKSI");

        btnCetak.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnCetak.setText("CETAK");
        btnCetak.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCetakActionPerformed(evt);
            }
        });

        btnExportPDF.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnExportPDF.setText("Export PDF");
        btnExportPDF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExportPDFActionPerformed(evt);
            }
        });

        btnRefresh.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        btnRefresh.setText("Refresh");
        btnRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRefreshActionPerformed(evt);
            }
        });

        lblData.setFont(new java.awt.Font("Segoe UI", 1, 16)); // NOI18N
        lblData.setForeground(new java.awt.Color(13, 71, 161));
        lblData.setText("DATA LAPORAN DIVISI");

        tblDivisi.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null},
                {null, null, null, null, null, null}
            },
            new String [] {
                "No", "ID Divisi", "Nama Divisi", "Kepala Divisi", "Jumlah Karyawan", "Keterangan"
            }
        ));
        tblDivisi.setFillsViewportHeight(true);
        tblDivisi.setRowHeight(26);
        jScrollPane1.setViewportView(tblDivisi);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(pnlHeader, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblFilter)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblCari)
                        .addGap(38, 38, 38)
                        .addComponent(lblColon)
                        .addGap(18, 18, 18)
                        .addComponent(txtCari, javax.swing.GroupLayout.PREFERRED_SIZE, 226, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(56, 56, 56)
                        .addComponent(btnCari, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(52, 52, 52)
                        .addComponent(btnReset, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(lblAksi)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(38, 38, 38)
                        .addComponent(btnCetak, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(42, 42, 42)
                        .addComponent(btnExportPDF, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(70, 70, 70)
                        .addComponent(btnRefresh, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(lblData)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 793, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(99, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(pnlHeader, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(25, 25, 25)
                .addComponent(lblFilter)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblCari)
                    .addComponent(lblColon)
                    .addComponent(txtCari, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnCari, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnReset, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(16, 16, 16)
                .addComponent(lblAksi)
                .addGap(16, 16, 16)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCetak, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnExportPDF, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnRefresh, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(28, 28, 28)
                .addComponent(lblData)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 256, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(28, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void btnCariActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCariActionPerformed
        cariData();
    }//GEN-LAST:event_btnCariActionPerformed

    private void btnResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnResetActionPerformed
        txtCari.setText("");
        tampilData();
    }//GEN-LAST:event_btnResetActionPerformed

    private void btnCetakActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCetakActionPerformed
        cetakData();
    }//GEN-LAST:event_btnCetakActionPerformed

    private void btnExportPDFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExportPDFActionPerformed
        exportPDF();
    }//GEN-LAST:event_btnExportPDFActionPerformed

    private void btnRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshActionPerformed
        tampilData();
    }//GEN-LAST:event_btnRefreshActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCari;
    private javax.swing.JButton btnCetak;
    private javax.swing.JButton btnExportPDF;
    private javax.swing.JButton btnRefresh;
    private javax.swing.JButton btnReset;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel lblAksi;
    private javax.swing.JLabel lblCari;
    private javax.swing.JLabel lblColon;
    private javax.swing.JLabel lblData;
    private javax.swing.JLabel lblFilter;
    private javax.swing.JLabel lblSubtitle;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JPanel pnlHeader;
    private javax.swing.JTable tblDivisi;
    private javax.swing.JTextField txtCari;
    // End of variables declaration//GEN-END:variables
}
