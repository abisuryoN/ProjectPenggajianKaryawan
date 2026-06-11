package panel.laporan;
import config.Koneksi;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;

/**
 * Panel kosong untuk diedit lewat NetBeans GUI Builder.
 * Buka tab Design, lalu drag komponen sesuai kebutuhan.
 */
public class LaporanGajiPanel extends javax.swing.JPanel {

  public LaporanGajiPanel() {
    initComponents();
    styleButton();
    loadKaryawan();
    loadBulan();
    tampilData();
    txtTahun.setText(String.valueOf(java.time.LocalDate.now().getYear()));
    util.DesignUtil.applyPage(this);
}
  
  private void styleButton() {
    styleFilledButton(btnCetak, new java.awt.Color(34, 197, 94), java.awt.Color.WHITE);
    styleFilledButton(btnCetakSlip, new java.awt.Color(37, 99, 235), java.awt.Color.WHITE);
    styleFilledButton(btnExportPDF, new java.awt.Color(239, 68, 68), java.awt.Color.WHITE);
    styleFilledButton(btnRefresh, new java.awt.Color(249, 115, 22), java.awt.Color.WHITE);
    styleFilledButton(btnFilter, new java.awt.Color(14, 165, 164), java.awt.Color.WHITE);
    styleFilledButton(btnReset, new java.awt.Color(107, 114, 128), java.awt.Color.WHITE);
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

private void loadBulan() {
    cmbBulan.removeAllItems();
    cmbBulan.addItem("-- Semua Bulan --");
    String[] bulan = {"Januari", "Februari", "Maret", "April", "Mei", "Juni",
                      "Juli", "Agustus", "September", "Oktober", "November", "Desember"};
    for (String b : bulan) cmbBulan.addItem(b);
    cmbBulan.setSelectedIndex(java.time.LocalDate.now().getMonthValue());
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

private void tampilData() {
    DefaultTableModel model = new DefaultTableModel();
    model.addColumn("NO");
    model.addColumn("Nama Karyawan");
    model.addColumn("Bulan");
    model.addColumn("Tahun");
    model.addColumn("Gaji Pokok");
    model.addColumn("Tunjangan");
    model.addColumn("Potongan");
    model.addColumn("Total Gaji");

    try {
        String sql = "SELECT p.*, k.nama_karyawan FROM penggajian p "
                + "JOIN karyawan k ON p.id_karyawan = k.id_karyawan "
                + "ORDER BY p.id_penggajian DESC";

        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement(sql);
        ResultSet rs = pst.executeQuery();
        int no = 1;
        while (rs.next()) {
            String periode = rs.getString("periode");
            String[] parts = periode.split("-");
            String tahun = parts[0];
            String bulanAngka = parts.length > 1 ? parts[1] : "";
            String bulanNama = getNamaBulan(bulanAngka);

            model.addRow(new Object[]{
                no++,
                rs.getString("nama_karyawan"),
                bulanNama,
                tahun,
                formatRupiah(rs.getString("gaji_pokok")),
                formatRupiah(rs.getString("total_tunjangan")),
                formatRupiah(rs.getString("potongan")),
                formatRupiah(rs.getString("total_gaji"))
            });
        }
        tblLaporanGaji.setModel(model);
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal tampil data: " + e.getMessage());
    }
}

private String getNamaBulan(String angka) {
    String[] bulan = {"", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                      "Juli", "Agustus", "September", "Oktober", "November", "Desember"};
    try {
        int idx = Integer.parseInt(angka);
        if (idx >= 1 && idx <= 12) return bulan[idx];
    } catch (Exception e) {}
    return angka;
}

private void filterData() {
    DefaultTableModel model = new DefaultTableModel();
    model.addColumn("NO");
    model.addColumn("Nama Karyawan");
    model.addColumn("Bulan");
    model.addColumn("Tahun");
    model.addColumn("Gaji Pokok");
    model.addColumn("Tunjangan");
    model.addColumn("Potongan");
    model.addColumn("Total Gaji");

    try {
        StringBuilder sql = new StringBuilder(
            "SELECT p.*, k.nama_karyawan FROM penggajian p "
            + "JOIN karyawan k ON p.id_karyawan = k.id_karyawan WHERE 1=1 "
        );

        if (cmbKaryawan.getSelectedIndex() > 0) {
            String selected = cmbKaryawan.getSelectedItem().toString();
            int idKaryawan = Integer.parseInt(selected.split(" - ")[0]);
            sql.append("AND p.id_karyawan = ").append(idKaryawan).append(" ");
        }

        String tahun = txtTahun.getText().trim();
        if (!tahun.isEmpty()) {
            sql.append("AND p.periode LIKE '").append(tahun).append("%' ");
        }

        if (cmbBulan.getSelectedIndex() > 0) {
            String bulanIdx = String.format("%02d", cmbBulan.getSelectedIndex());
            sql.append("AND p.periode LIKE '%-").append(bulanIdx).append("' ");
        }

        String keyword = txtCari.getText().trim();
        if (!keyword.isEmpty()) {
            sql.append("AND k.nama_karyawan LIKE '%").append(keyword).append("%' ");
        }

        sql.append("ORDER BY p.id_penggajian DESC");

        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement(sql.toString());
        ResultSet rs = pst.executeQuery();
        int no = 1;
        while (rs.next()) {
            String periode = rs.getString("periode");
            String[] parts = periode.split("-");
            model.addRow(new Object[]{
                no++,
                rs.getString("nama_karyawan"),
                getNamaBulan(parts.length > 1 ? parts[1] : ""),
                parts[0],
                formatRupiah(rs.getString("gaji_pokok")),
                formatRupiah(rs.getString("total_tunjangan")),
                formatRupiah(rs.getString("potongan")),
                formatRupiah(rs.getString("total_gaji"))
            });
        }
        tblLaporanGaji.setModel(model);
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal filter: " + e.getMessage());
    }
}

private void exportPDF() {
    try {
        String namaFile = "laporan_gaji_" + java.time.LocalDate.now() + ".pdf";
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
            "LAPORAN GAJI KARYAWAN", fontJudul);
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
        table.setWidths(new float[]{4, 18, 10, 8, 14, 14, 12, 14});

        String[] headers = {"NO", "Nama Karyawan", "Bulan", "Tahun", "Gaji Pokok", "Tunjangan", "Potongan", "Total Gaji"};
        for (String h : headers) {
            com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(
                new com.itextpdf.text.Phrase(h, fontHeader));
            cell.setBackgroundColor(new com.itextpdf.text.BaseColor(22, 48, 92));
            cell.setHorizontalAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            cell.setPadding(6);
            table.addCell(cell);
        }

        DefaultTableModel mdl = (DefaultTableModel) tblLaporanGaji.getModel();
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
        Map<String, Object> params = new HashMap<>();
        String tahun = txtTahun.getText().trim();
        params.put("tahun", tahun.isEmpty() ? null : tahun);
        params.put("bulan", cmbBulan.getSelectedIndex() > 0
                ? String.format("%02d", cmbBulan.getSelectedIndex())
                : null);
        util.ReportUtil.showReport("laporan_penggajian", params);
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal membuka laporan penggajian: " + e.getMessage());
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
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jFormattedTextField1 = new javax.swing.JFormattedTextField();
        jPanel3 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        pnlUtama = new javax.swing.JPanel();
        pnlHeader = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        cmbKaryawan = new javax.swing.JComboBox<>();
        cmbBulan = new javax.swing.JComboBox<>();
        txtCari = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        txtTahun = new javax.swing.JTextField();
        btnFilter = new javax.swing.JButton();
        btnReset = new javax.swing.JButton();
        pnlFilter = new javax.swing.JPanel();
        btnCetak = new javax.swing.JButton();
        btnCetakSlip = new javax.swing.JButton();
        btnExportPDF = new javax.swing.JButton();
        btnRefresh = new javax.swing.JButton();
        jPanel8 = new javax.swing.JPanel();
        scrollLaporanGaji = new javax.swing.JScrollPane();
        tblLaporanGaji = new javax.swing.JTable();

        jPanel1.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Filter Data"));

        jLabel2.setText("Tanggal Awal");

        jLabel3.setText("Tanggal Akhir");

        jLabel4.setText("Karyawan");

        jFormattedTextField1.setFormatterFactory(new javax.swing.text.DefaultFormatterFactory(new javax.swing.text.DateFormatter(java.text.DateFormat.getDateInstance(java.text.DateFormat.FULL))));

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
                .addGap(84, 84, 84)
                .addComponent(jFormattedTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(505, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jFormattedTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jLabel3)
                .addGap(18, 18, 18)
                .addComponent(jLabel4)
                .addContainerGap(40, Short.MAX_VALUE))
        );

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("AKSI"));

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 28, Short.MAX_VALUE)
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("DATA LAPORAN ABSENSI"));

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 273, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel1.setText("LAPORAN GAJI KARYAWAN");

        pnlUtama.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        pnlHeader.setBorder(javax.swing.BorderFactory.createTitledBorder("FILTER DATA"));

        jLabel5.setText("Nama Karyawan");

        jLabel6.setText("Bulan");

        jLabel7.setText("Cari Data");

        cmbKaryawan.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Izzy jomok", "Hasan ", "Abi GAY", "Zaidan ", "Egi" }));

        cmbBulan.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Januari", "Februari ", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember" }));

        jLabel8.setText("Tahun");

        txtTahun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtTahunActionPerformed(evt);
            }
        });

        btnFilter.setBackground(new java.awt.Color(0, 255, 204));
        btnFilter.setForeground(new java.awt.Color(255, 255, 255));
        btnFilter.setText("🔽 Filter");
        btnFilter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFilterActionPerformed(evt);
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

        javax.swing.GroupLayout pnlHeaderLayout = new javax.swing.GroupLayout(pnlHeader);
        pnlHeader.setLayout(pnlHeaderLayout);
        pnlHeaderLayout.setHorizontalGroup(
            pnlHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlHeaderLayout.createSequentialGroup()
                .addGap(38, 38, 38)
                .addGroup(pnlHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jLabel5)
                    .addComponent(jLabel6, javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel7, javax.swing.GroupLayout.Alignment.LEADING))
                .addGap(52, 52, 52)
                .addGroup(pnlHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(pnlHeaderLayout.createSequentialGroup()
                        .addGroup(pnlHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(cmbKaryawan, javax.swing.GroupLayout.Alignment.LEADING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(cmbBulan, javax.swing.GroupLayout.Alignment.LEADING, 0, 254, Short.MAX_VALUE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel8))
                    .addComponent(txtCari, javax.swing.GroupLayout.PREFERRED_SIZE, 342, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGroup(pnlHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlHeaderLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnFilter)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnReset))
                    .addGroup(pnlHeaderLayout.createSequentialGroup()
                        .addGap(50, 50, 50)
                        .addComponent(txtTahun, javax.swing.GroupLayout.PREFERRED_SIZE, 75, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(139, Short.MAX_VALUE))
        );
        pnlHeaderLayout.setVerticalGroup(
            pnlHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlHeaderLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(cmbKaryawan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(pnlHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(cmbBulan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel8)
                    .addComponent(txtTahun, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(pnlHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(txtCari, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnFilter)
                    .addComponent(btnReset))
                .addContainerGap(28, Short.MAX_VALUE))
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

        btnCetakSlip.setBackground(new java.awt.Color(0, 153, 255));
        btnCetakSlip.setForeground(new java.awt.Color(255, 255, 255));
        btnCetakSlip.setText("📄 Cetak Slip");
        btnCetakSlip.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCetakSlipActionPerformed(evt);
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

        btnRefresh.setBackground(new java.awt.Color(255, 204, 0));
        btnRefresh.setForeground(new java.awt.Color(255, 255, 255));
        btnRefresh.setText("🔄 Refresh");
        btnRefresh.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnRefreshActionPerformed(evt);
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
                .addComponent(btnCetakSlip, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnExportPDF, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(btnRefresh, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        pnlFilterLayout.setVerticalGroup(
            pnlFilterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlFilterLayout.createSequentialGroup()
                .addGroup(pnlFilterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(btnRefresh, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 33, Short.MAX_VALUE)
                    .addComponent(btnCetak, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnCetakSlip, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnExportPDF, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        jPanel8.setBorder(javax.swing.BorderFactory.createTitledBorder("DATA LAPORAN GAJI"));

        scrollLaporanGaji.setBorder(null);
        scrollLaporanGaji.setToolTipText("");

        tblLaporanGaji.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null}
            },
            new String [] {
                "NO", "Nama Karyawan", "Bulan", "Tahun", "Gaji Pokok", "Tunjangan", "Potongan", "Total Gaji"
            }
        ));
        tblLaporanGaji.addAncestorListener(new javax.swing.event.AncestorListener() {
            public void ancestorAdded(javax.swing.event.AncestorEvent evt) {
                tblLaporanGajiAncestorAdded(evt);
            }
            public void ancestorMoved(javax.swing.event.AncestorEvent evt) {
            }
            public void ancestorRemoved(javax.swing.event.AncestorEvent evt) {
            }
        });
        scrollLaporanGaji.setViewportView(tblLaporanGaji);

        javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(scrollLaporanGaji)
                .addContainerGap())
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(scrollLaporanGaji, javax.swing.GroupLayout.DEFAULT_SIZE, 261, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout pnlUtamaLayout = new javax.swing.GroupLayout(pnlUtama);
        pnlUtama.setLayout(pnlUtamaLayout);
        pnlUtamaLayout.setHorizontalGroup(
            pnlUtamaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlUtamaLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlUtamaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(pnlHeader, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pnlFilter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        pnlUtamaLayout.setVerticalGroup(
            pnlUtamaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlUtamaLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(pnlHeader, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnlFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(26, 26, 26)
                .addComponent(pnlUtama, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(29, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addGap(320, 320, 320))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(pnlUtama, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(11, 11, 11))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void txtTahunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtTahunActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtTahunActionPerformed

    private void btnCetakActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCetakActionPerformed
       cetakLaporanJasper();
    }//GEN-LAST:event_btnCetakActionPerformed

    private void btnCetakSlipActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCetakSlipActionPerformed
         JOptionPane.showMessageDialog(this, "Untuk cetak slip per karyawan, gunakan menu Penggajian.");
    }//GEN-LAST:event_btnCetakSlipActionPerformed

    private void btnExportPDFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnExportPDFActionPerformed
        cetakLaporanJasper();
    }//GEN-LAST:event_btnExportPDFActionPerformed

    private void btnRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshActionPerformed
        tampilData();
    }//GEN-LAST:event_btnRefreshActionPerformed

    private void btnFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFilterActionPerformed
         filterData();
    }//GEN-LAST:event_btnFilterActionPerformed

    private void btnResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnResetActionPerformed
         cmbKaryawan.setSelectedIndex(0);
         cmbBulan.setSelectedIndex(0);
         txtCari.setText("");
         txtTahun.setText(String.valueOf(java.time.LocalDate.now().getYear()));
         tampilData();
    }//GEN-LAST:event_btnResetActionPerformed

    private void tblLaporanGajiAncestorAdded(javax.swing.event.AncestorEvent evt) {//GEN-FIRST:event_tblLaporanGajiAncestorAdded
        // TODO add your handling code here:
    }//GEN-LAST:event_tblLaporanGajiAncestorAdded


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCetak;
    private javax.swing.JButton btnCetakSlip;
    private javax.swing.JButton btnExportPDF;
    private javax.swing.JButton btnFilter;
    private javax.swing.JButton btnRefresh;
    private javax.swing.JButton btnReset;
    private javax.swing.JComboBox<String> cmbBulan;
    private javax.swing.JComboBox<String> cmbKaryawan;
    private javax.swing.JFormattedTextField jFormattedTextField1;
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
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel pnlFilter;
    private javax.swing.JPanel pnlHeader;
    private javax.swing.JPanel pnlUtama;
    private javax.swing.JScrollPane scrollLaporanGaji;
    private javax.swing.JTable tblLaporanGaji;
    private javax.swing.JTextField txtCari;
    private javax.swing.JTextField txtTahun;
    // End of variables declaration//GEN-END:variables
}
