package panel.laporan;
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
public class LaporanKaryawanPanel extends javax.swing.JPanel {

    public LaporanKaryawanPanel() {
    initComponents();
    styleButton();
    tampilData();
    util.DesignUtil.applyPage(this);
    
    btnCari.addActionListener(e -> cariData());
    btnRefresh.addActionListener(e -> tampilData());
    btnReset.addActionListener(e -> { txtCari.setText(""); tampilData(); });
    btnExportPDF.addActionListener(e -> cetakLaporanJasper());
    btncetak.addActionListener(e -> cetakLaporanJasper());
    
    // Biar bisa scroll kanan kiri
    tblAbsensi.setAutoResizeMode(javax.swing.JTable.AUTO_RESIZE_OFF);
}
    
    private void styleButton() {
    styleFilledButton(btnCari, new java.awt.Color(37, 99, 235), java.awt.Color.WHITE);
    styleFilledButton(btnExportPDF, new java.awt.Color(20, 184, 166), java.awt.Color.WHITE);
    styleFilledButton(btncetak, new java.awt.Color(37, 99, 235), java.awt.Color.WHITE);
    styleFilledButton(btnReset, new java.awt.Color(239, 68, 68), java.awt.Color.WHITE);
    styleFilledButton(btnRefresh, new java.awt.Color(243, 244, 246), new java.awt.Color(17, 24, 39));
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

private void tampilData() {
    DefaultTableModel model = new DefaultTableModel();
    model.addColumn("ID");
    model.addColumn("NIK");
    model.addColumn("Nama Karyawan");
    model.addColumn("Jenis Kelamin");
    model.addColumn("No. HP");
    model.addColumn("Jabatan");
    model.addColumn("Divisi");
    model.addColumn("Alamat");

    try {
        String sql = "SELECT k.id_karyawan, k.nik, k.nama_karyawan, k.jenis_kelamin, "
                + "k.no_hp, j.nama_jabatan, d.nama_divisi, k.alamat "
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
                rs.getString("nama_divisi"),
                rs.getString("alamat") != null ? rs.getString("alamat") : ""
            });
        }
        tblAbsensi.setModel(model);

        // Set lebar kolom
        tblAbsensi.getColumnModel().getColumn(0).setPreferredWidth(50);
        tblAbsensi.getColumnModel().getColumn(1).setPreferredWidth(80);
        tblAbsensi.getColumnModel().getColumn(2).setPreferredWidth(150);
        tblAbsensi.getColumnModel().getColumn(3).setPreferredWidth(100);
        tblAbsensi.getColumnModel().getColumn(4).setPreferredWidth(100);
        tblAbsensi.getColumnModel().getColumn(5).setPreferredWidth(100);
        tblAbsensi.getColumnModel().getColumn(6).setPreferredWidth(100);
        tblAbsensi.getColumnModel().getColumn(7).setPreferredWidth(200);

    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal tampil data: " + e.getMessage());
    }
}

private void cariData() {
    String keyword = txtCari.getText().trim();
    DefaultTableModel model = new DefaultTableModel();
    model.addColumn("ID");
    model.addColumn("NIK");
    model.addColumn("Nama Karyawan");
    model.addColumn("Jenis Kelamin");
    model.addColumn("No. HP");
    model.addColumn("Jabatan");
    model.addColumn("Divisi");
    model.addColumn("Alamat");

    try {
        String sql = "SELECT k.id_karyawan, k.nik, k.nama_karyawan, k.jenis_kelamin, "
                + "k.no_hp, j.nama_jabatan, d.nama_divisi, k.alamat "
                + "FROM karyawan k "
                + "LEFT JOIN jabatan j ON k.id_jabatan = j.id_jabatan "
                + "LEFT JOIN divisi d ON k.id_divisi = d.id_divisi "
                + "WHERE k.nama_karyawan LIKE ? OR k.nik LIKE ? OR d.nama_divisi LIKE ? "
                + "OR j.nama_jabatan LIKE ? "
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
                rs.getString("nama_divisi"),
                rs.getString("alamat") != null ? rs.getString("alamat") : ""
            });
        }
        tblAbsensi.setModel(model);

        tblAbsensi.getColumnModel().getColumn(0).setPreferredWidth(50);
        tblAbsensi.getColumnModel().getColumn(1).setPreferredWidth(80);
        tblAbsensi.getColumnModel().getColumn(2).setPreferredWidth(150);
        tblAbsensi.getColumnModel().getColumn(3).setPreferredWidth(100);
        tblAbsensi.getColumnModel().getColumn(4).setPreferredWidth(100);
        tblAbsensi.getColumnModel().getColumn(5).setPreferredWidth(100);
        tblAbsensi.getColumnModel().getColumn(6).setPreferredWidth(100);
        tblAbsensi.getColumnModel().getColumn(7).setPreferredWidth(200);

    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal cari: " + e.getMessage());
    }
}

private void exportPDF() {
    try {
        String namaFile = "laporan_karyawan_" + java.time.LocalDate.now() + ".pdf";
        String path = System.getProperty("user.home") + "\\Downloads\\" + namaFile;

        com.itextpdf.text.Document doc = new com.itextpdf.text.Document(
            com.itextpdf.text.PageSize.A4.rotate());
        com.itextpdf.text.pdf.PdfWriter.getInstance(doc, new java.io.FileOutputStream(path));
        doc.open();

        com.itextpdf.text.Font fontJudul = new com.itextpdf.text.Font(
            com.itextpdf.text.Font.FontFamily.HELVETICA, 16, com.itextpdf.text.Font.BOLD);
        com.itextpdf.text.Font fontHeader = new com.itextpdf.text.Font(
            com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.BOLD,
            com.itextpdf.text.BaseColor.WHITE);
        com.itextpdf.text.Font fontCell = new com.itextpdf.text.Font(
            com.itextpdf.text.Font.FontFamily.HELVETICA, 9);

        com.itextpdf.text.Paragraph judul = new com.itextpdf.text.Paragraph(
            "LAPORAN DATA KARYAWAN", fontJudul);
        judul.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        doc.add(judul);

        com.itextpdf.text.Paragraph tgl = new com.itextpdf.text.Paragraph(
            "Dicetak: " + java.time.LocalDate.now(),
            new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10));
        tgl.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        doc.add(tgl);
        doc.add(com.itextpdf.text.Chunk.NEWLINE);

        com.itextpdf.text.pdf.PdfPTable table = new com.itextpdf.text.pdf.PdfPTable(8);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{5, 10, 18, 12, 12, 12, 12, 18});

        String[] headers = {"ID", "NIK", "Nama Karyawan", "Jenis Kelamin", "No. HP", "Jabatan", "Divisi", "Alamat"};
        for (String h : headers) {
            com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(
                new com.itextpdf.text.Phrase(h, fontHeader));
            cell.setBackgroundColor(new com.itextpdf.text.BaseColor(22, 48, 92));
            cell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            cell.setPadding(6);
            table.addCell(cell);
        }

        DefaultTableModel mdl = (DefaultTableModel) tblAbsensi.getModel();
        for (int i = 0; i < mdl.getRowCount(); i++) {
            for (int j = 0; j < mdl.getColumnCount(); j++) {
                String val = mdl.getValueAt(i, j) != null ? mdl.getValueAt(i, j).toString() : "";
                com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(
                    new com.itextpdf.text.Phrase(val, fontCell));
                cell.setPadding(5);
                if (i % 2 == 0) cell.setBackgroundColor(new com.itextpdf.text.BaseColor(240, 245, 255));
                table.addCell(cell);
            }
        }

        doc.add(table);
        doc.close();

        java.awt.Desktop.getDesktop().open(new java.io.File(path));
        JOptionPane.showMessageDialog(this, "PDF berhasil diunduh:\n" + namaFile);

    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal export PDF: " + e.getMessage());
    }
}

private void cetakLaporanJasper() {
    try {
        util.ReportUtil.showReport("laporan_karyawan", new java.util.HashMap<String, Object>());
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal membuka laporan karyawan: " + e.getMessage());
    }
}

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code manually kalau masih mau pakai tab Design NetBeans.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        btnRefresh = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblAbsensi = new javax.swing.JTable();
        btnReset = new javax.swing.JButton();
        btnExportPDF = new javax.swing.JButton();
        btncetak = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jLabel9 = new javax.swing.JLabel();
        txtCari = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        btnCari = new javax.swing.JButton();

        btnRefresh.setFont(new java.awt.Font("sansserif", 1, 12)); // NOI18N
        btnRefresh.setText("Refresh");

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

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(25, 25, 25)
                .addComponent(jScrollPane1)
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel2Layout.createSequentialGroup()
                .addGap(69, 69, 69)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 225, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(29, Short.MAX_VALUE))
        );

        btnReset.setBackground(new java.awt.Color(255, 0, 0));
        btnReset.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnReset.setText("Reset");

        btnExportPDF.setBackground(new java.awt.Color(0, 255, 204));
        btnExportPDF.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btnExportPDF.setText("Export PDF");

        btncetak.setBackground(new java.awt.Color(51, 153, 255));
        btncetak.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        btncetak.setText("Cetak");

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createTitledBorder(null, "Input", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("sansserif", 1, 12), new java.awt.Color(0, 153, 153)), "", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Tahoma", 0, 16), new java.awt.Color(0, 153, 153))); // NOI18N

        jLabel9.setText("Cari Nama / NIK / Divisi");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel9)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 38, Short.MAX_VALUE)
                .addComponent(txtCari, javax.swing.GroupLayout.PREFERRED_SIZE, 199, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel9)
                    .addComponent(txtCari, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jLabel1.setFont(new java.awt.Font("Franklin Gothic Medium", 1, 24)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(0, 102, 153));
        jLabel1.setText("LAPORAN KARYAWAN");

        btnCari.setFont(new java.awt.Font("sansserif", 1, 12)); // NOI18N
        btnCari.setText("Cari");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel1)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(34, 34, 34)
                                .addComponent(btnCari, javax.swing.GroupLayout.PREFERRED_SIZE, 88, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(45, 45, 45)
                                .addComponent(btnExportPDF)))
                        .addGap(37, 37, 37)
                        .addComponent(btncetak)
                        .addGap(56, 56, 56)
                        .addComponent(btnReset)
                        .addGap(43, 43, 43)
                        .addComponent(btnRefresh)))
                .addContainerGap(58, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(27, 27, 27)
                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 42, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 65, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnExportPDF)
                            .addComponent(btnReset)
                            .addComponent(btnRefresh)
                            .addComponent(btncetak)
                            .addComponent(btnCari))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(9, 9, 9))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1138, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 0, Short.MAX_VALUE)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 600, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(0, 34, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGap(0, 34, Short.MAX_VALUE)))
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCari;
    private javax.swing.JButton btnExportPDF;
    private javax.swing.JButton btnRefresh;
    private javax.swing.JButton btnReset;
    private javax.swing.JButton btncetak;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable tblAbsensi;
    private javax.swing.JTextField txtCari;
    // End of variables declaration//GEN-END:variables
}
