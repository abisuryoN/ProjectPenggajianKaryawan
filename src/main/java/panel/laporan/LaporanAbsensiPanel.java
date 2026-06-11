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
public class LaporanAbsensiPanel extends javax.swing.JPanel {

    public LaporanAbsensiPanel() {
    initComponents();
    styleButton();
    loadKaryawan();
    loadStatus();
    tampilData();
    util.DesignUtil.applyPage(this);
    
    cmbKaryawan.addActionListener(e -> filterData());
    cmbStatus.addActionListener(e -> filterData());

}
    
    private void styleButton() {
    styleFilledButton(btnCetak, new java.awt.Color(34, 197, 94), java.awt.Color.WHITE);
    styleFilledButton(btnExportPDF, new java.awt.Color(239, 68, 68), java.awt.Color.WHITE);
    styleFilledButton(btnRefresh, new java.awt.Color(37, 99, 235), java.awt.Color.WHITE);
    styleFilledButton(btnReset, new java.awt.Color(107, 114, 128), java.awt.Color.WHITE);
    styleFilledButton(btnFilter, new java.awt.Color(107, 114, 128), java.awt.Color.WHITE);
    styleFilledButton(btnCari, new java.awt.Color(239, 68, 68), java.awt.Color.WHITE);
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
    
    private void loadKaryawan() {
    cmbKaryawan.removeAllItems();
    cmbKaryawan.addItem("-- Semua Karyawan --");
    try {
        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement(
            "SELECT id_karyawan, nama_karyawan FROM karyawan ORDER BY nama_karyawan"
        );
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            cmbKaryawan.addItem(rs.getString("id_karyawan") + " - " + rs.getString("nama_karyawan"));
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal load karyawan: " + e.getMessage());
    }
}

private void loadStatus() {
    cmbStatus.removeAllItems();
    cmbStatus.addItem("-- Semua Status --");
    cmbStatus.addItem("Hadir");
    cmbStatus.addItem("Izin");
    cmbStatus.addItem("Sakit");
    cmbStatus.addItem("Alpha");
}

private void tampilData() {
    DefaultTableModel model = new DefaultTableModel();
    model.addColumn("NO");
    model.addColumn("Nama Karyawan");
    model.addColumn("Tanggal");
    model.addColumn("Jam Masuk");
    model.addColumn("Jam Pulang");
    model.addColumn("Status");
    model.addColumn("Keterangan");

    try {
        String sql = "SELECT a.id_absensi, k.nama_karyawan, a.tanggal, "
                + "a.jam_masuk, a.jam_pulang, a.status, a.keterangan "
                + "FROM absensi a "
                + "JOIN karyawan k ON a.id_karyawan = k.id_karyawan "
                + "ORDER BY a.tanggal DESC";

        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement(sql);
        ResultSet rs = pst.executeQuery();
        int no = 1;
        while (rs.next()) {
            model.addRow(new Object[]{
                no++,
                rs.getString("nama_karyawan"),
                rs.getString("tanggal"),
                rs.getString("jam_masuk"),
                rs.getString("jam_pulang"),
                rs.getString("status"),
                rs.getString("keterangan") != null ? rs.getString("keterangan") : ""
            });
        }
        tblLaporanAbsensi.setModel(model);
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal tampil data: " + e.getMessage());
    }
}

private void filterData() {
    DefaultTableModel model = new DefaultTableModel();
    model.addColumn("NO");
    model.addColumn("Nama Karyawan");
    model.addColumn("Tanggal");
    model.addColumn("Jam Masuk");
    model.addColumn("Jam Pulang");
    model.addColumn("Status");
    model.addColumn("Keterangan");

    try {
       StringBuilder sql = new StringBuilder(
            "SELECT a.id_absensi, k.nama_karyawan, a.tanggal, "
            + "a.jam_masuk, a.jam_pulang, a.status, a.keterangan "
            + "FROM absensi a "
            + "JOIN karyawan k ON a.id_karyawan = k.id_karyawan "
            + "WHERE 1=1 "
        );

        if (cmbKaryawan.getSelectedIndex() > 0) {
            String selected = cmbKaryawan.getSelectedItem().toString();
            int idKaryawan = Integer.parseInt(selected.split(" - ")[0]);
            sql.append("AND a.id_karyawan = ").append(idKaryawan).append(" ");
        }

        if (cmbStatus.getSelectedIndex() > 0) {
            sql.append("AND a.status = '").append(cmbStatus.getSelectedItem()).append("' ");
        }

        sql.append("ORDER BY a.tanggal DESC");

        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement(sql.toString());
        ResultSet rs = pst.executeQuery();
        int no = 1;
        while (rs.next()) {
            model.addRow(new Object[]{
                no++,
                rs.getString("nama_karyawan"),
                rs.getString("tanggal"),
                rs.getString("jam_masuk"),
                rs.getString("jam_pulang"),
                rs.getString("status"),
                ""
            });
        }
        tblLaporanAbsensi.setModel(model);
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal filter data: " + e.getMessage());
    }
}

private void cariData() {
    String keyword = txtCari.getText().trim();
    DefaultTableModel model = new DefaultTableModel();
    model.addColumn("NO");
    model.addColumn("Nama Karyawan");
    model.addColumn("Tanggal");
    model.addColumn("Jam Masuk");
    model.addColumn("Jam Pulang");
    model.addColumn("Status");
    model.addColumn("Keterangan");

    try {
        String sql = "SELECT a.id_absensi, k.nama_karyawan, a.tanggal, "
            + "a.jam_masuk, a.jam_pulang, a.status, a.keterangan "
            + "FROM absensi a "
            + "JOIN karyawan k ON a.id_karyawan = k.id_karyawan "
            + "WHERE k.nama_karyawan LIKE ? OR a.tanggal LIKE ? OR a.status LIKE ? "
            + "ORDER BY a.tanggal DESC";

        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setString(1, "%" + keyword + "%");
        pst.setString(2, "%" + keyword + "%");
        pst.setString(3, "%" + keyword + "%");
        ResultSet rs = pst.executeQuery();
        int no = 1;
        while (rs.next()) {
            model.addRow(new Object[]{
                no++,
                rs.getString("nama_karyawan"),
                rs.getString("tanggal"),
                rs.getString("jam_masuk"),
                rs.getString("jam_pulang"),
                rs.getString("status"),
                ""
            });
        }
        tblLaporanAbsensi.setModel(model);
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal cari: " + e.getMessage());
    }
}

private void exportPDF() {
    try {
        String namaFile = "laporan_absensi_" + java.time.LocalDate.now() + ".pdf";
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
            "LAPORAN ABSENSI KARYAWAN", fontJudul);
        judul.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        doc.add(judul);

        com.itextpdf.text.Paragraph tgl = new com.itextpdf.text.Paragraph(
            "Dicetak: " + java.time.LocalDate.now(),
            new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10));
        tgl.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
        doc.add(tgl);
        doc.add(com.itextpdf.text.Chunk.NEWLINE);

        com.itextpdf.text.pdf.PdfPTable table = new com.itextpdf.text.pdf.PdfPTable(7);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{5, 20, 15, 12, 12, 12, 15});

        String[] headers = {"NO", "Nama Karyawan", "Tanggal", "Jam Masuk", "Jam Pulang", "Status", "Keterangan"};
        for (String h : headers) {
            com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(
                new com.itextpdf.text.Phrase(h, fontHeader));
            cell.setBackgroundColor(new com.itextpdf.text.BaseColor(33, 64, 154));
            cell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            cell.setPadding(6);
            table.addCell(cell);
        }

        DefaultTableModel model = (DefaultTableModel) tblLaporanAbsensi.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            for (int j = 0; j < model.getColumnCount(); j++) {
                String val = model.getValueAt(i, j) != null ? model.getValueAt(i, j).toString() : "";
                com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(
                    new com.itextpdf.text.Phrase(val, fontCell));
                cell.setPadding(5);
                if (i % 2 == 0) cell.setBackgroundColor(new com.itextpdf.text.BaseColor(240, 240, 240));
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
        java.util.Map<String, Object> params = new java.util.HashMap<>();
        params.put("id_karyawan", null);
        params.put("status", cmbStatus.getSelectedIndex() > 0 ? cmbStatus.getSelectedItem().toString() : null);
        params.put("keyword", txtCari.getText().trim().isEmpty() ? null : txtCari.getText().trim());
        if (cmbKaryawan.getSelectedIndex() > 0) {
            String selected = cmbKaryawan.getSelectedItem().toString();
            params.put("id_karyawan", Integer.parseInt(selected.split(" - ")[0]));
        }
        util.ReportUtil.showReport("laporan_absensi", params);
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal membuka laporan absensi: " + e.getMessage());
    }
}

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code manually kalau masih mau pakai tab Design NetBeans.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jLabel5 = new javax.swing.JLabel();
        jLabel1 = new javax.swing.JLabel();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        cmbKaryawan = new javax.swing.JComboBox<>();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        cmbStatus = new javax.swing.JComboBox<>();
        txtCari = new javax.swing.JTextField();
        btnFilter = new javax.swing.JButton();
        btnCari = new javax.swing.JButton();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblLaporanAbsensi = new javax.swing.JTable();
        pnlFilter = new javax.swing.JPanel();
        btnCetak = new javax.swing.JButton();
        btnExportPDF = new javax.swing.JButton();
        btnRefresh = new javax.swing.JButton();
        btnReset = new javax.swing.JButton();

        jLabel5.setText("jLabel5");

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setText("LAPORAN ABSENSI KARYAWAN");

        jPanel1.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Filter Data"));

        jLabel2.setText("Tanggal Awal");

        jLabel3.setText("Tanggal Akhir");

        jLabel4.setText("Karyawan");

        cmbKaryawan.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel6.setText("Status");

        jLabel7.setText("Cari Data");

        cmbStatus.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        txtCari.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtCariActionPerformed(evt);
            }
        });

        btnFilter.setBackground(new java.awt.Color(102, 102, 102));
        btnFilter.setForeground(new java.awt.Color(255, 255, 255));
        btnFilter.setText("🔽 Filter");
        btnFilter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFilterActionPerformed(evt);
            }
        });

        btnCari.setBackground(new java.awt.Color(255, 51, 51));
        btnCari.setForeground(new java.awt.Color(255, 255, 255));
        btnCari.setText("🔍 Cari");
        btnCari.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCariActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(38, 38, 38)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel4)
                    .addComponent(jLabel3)
                    .addComponent(jLabel2))
                .addGap(56, 56, 56)
                .addComponent(cmbKaryawan, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(101, 101, 101)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel6)
                    .addComponent(jLabel7))
                .addGap(64, 64, 64)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(btnCari)
                        .addGap(18, 18, 18)
                        .addComponent(btnFilter))
                    .addComponent(txtCari)
                    .addComponent(cmbStatus, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(109, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel6)
                        .addComponent(cmbStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(19, 19, 19)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addComponent(jLabel7))
                        .addGap(24, 24, 24)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4)
                            .addComponent(cmbKaryawan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addComponent(txtCari, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnFilter)
                            .addComponent(btnCari))))
                .addContainerGap(12, Short.MAX_VALUE))
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("DATA LAPORAN ABSENSI"));

        tblLaporanAbsensi.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null}
            },
            new String [] {
                "NO", "Nama Karyawan", "Tanggal", "Jam Masuk", "Jam Pulang", "Status", "Keterangan"
            }
        ));
        tblLaporanAbsensi.addAncestorListener(new javax.swing.event.AncestorListener() {
            public void ancestorAdded(javax.swing.event.AncestorEvent evt) {
                tblLaporanAbsensiAncestorAdded(evt);
            }
            public void ancestorMoved(javax.swing.event.AncestorEvent evt) {
            }
            public void ancestorRemoved(javax.swing.event.AncestorEvent evt) {
            }
        });
        jScrollPane2.setViewportView(tblLaporanAbsensi);

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2)
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 272, Short.MAX_VALUE)
                .addContainerGap())
        );

        pnlFilter.setBorder(javax.swing.BorderFactory.createTitledBorder("AKSI"));

        btnCetak.setBackground(new java.awt.Color(51, 204, 0));
        btnCetak.setForeground(new java.awt.Color(255, 255, 255));
        btnCetak.setText("🖨 Cetak");
        btnCetak.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCetakActionPerformed(evt);
            }
        });

        btnExportPDF.setBackground(new java.awt.Color(255, 0, 51));
        btnExportPDF.setForeground(new java.awt.Color(255, 255, 255));
        btnExportPDF.setText("📕 Export PDF");
        btnExportPDF.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnExportPDFActionPerformed(evt);
            }
        });

        btnRefresh.setBackground(new java.awt.Color(51, 153, 255));
        btnRefresh.setForeground(new java.awt.Color(255, 255, 255));
        btnRefresh.setText("🔄 Refresh");
        btnRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRefreshActionPerformed(evt);
            }
        });

        btnReset.setBackground(new java.awt.Color(153, 153, 153));
        btnReset.setForeground(new java.awt.Color(255, 255, 255));
        btnReset.setText("↩ Reset");
        btnReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnResetActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout pnlFilterLayout = new javax.swing.GroupLayout(pnlFilter);
        pnlFilter.setLayout(pnlFilterLayout);
        pnlFilterLayout.setHorizontalGroup(
            pnlFilterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlFilterLayout.createSequentialGroup()
                .addGap(21, 21, 21)
                .addComponent(btnCetak, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnExportPDF, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(24, 24, 24)
                .addComponent(btnRefresh, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnReset, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        pnlFilterLayout.setVerticalGroup(
            pnlFilterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlFilterLayout.createSequentialGroup()
                .addGroup(pnlFilterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlFilterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btnReset)
                        .addComponent(btnRefresh))
                    .addGroup(pnlFilterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(btnCetak, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(btnExportPDF, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pnlFilter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(pnlFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(15, 15, 15)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addGap(318, 318, 318))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void txtCariActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtCariActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtCariActionPerformed

    private void btnFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFilterActionPerformed
        filterData();
    }//GEN-LAST:event_btnFilterActionPerformed

    private void btnCariActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCariActionPerformed
         cariData();
    }//GEN-LAST:event_btnCariActionPerformed

    private void btnRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshActionPerformed
         tampilData();
    }//GEN-LAST:event_btnRefreshActionPerformed

    private void btnExportPDFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExportPDFActionPerformed
        cetakLaporanJasper();
    }//GEN-LAST:event_btnExportPDFActionPerformed

    private void btnCetakActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCetakActionPerformed
        cetakLaporanJasper();
    }//GEN-LAST:event_btnCetakActionPerformed

    private void btnResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnResetActionPerformed
        cmbKaryawan.setSelectedIndex(0);
        cmbStatus.setSelectedIndex(0);
        txtCari.setText("");
        tampilData();
    }//GEN-LAST:event_btnResetActionPerformed

    private void tblLaporanAbsensiAncestorAdded(javax.swing.event.AncestorEvent evt) {//GEN-FIRST:event_tblLaporanAbsensiAncestorAdded
        // TODO add your handling code here:
    }//GEN-LAST:event_tblLaporanAbsensiAncestorAdded


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCari;
    private javax.swing.JButton btnCetak;
    private javax.swing.JButton btnExportPDF;
    private javax.swing.JButton btnFilter;
    private javax.swing.JButton btnRefresh;
    private javax.swing.JButton btnReset;
    private javax.swing.JComboBox<String> cmbKaryawan;
    private javax.swing.JComboBox<String> cmbStatus;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JPanel pnlFilter;
    private javax.swing.JTable tblLaporanAbsensi;
    private javax.swing.JTextField txtCari;
    // End of variables declaration//GEN-END:variables
}
