package panel.transaksi;
import config.Koneksi;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import java.time.LocalDate;

/**
 * Panel kosong untuk diedit lewat NetBeans GUI Builder.
 * Buka tab Design, lalu drag komponen sesuai kebutuhan.
 */
public class PenggajianPanel extends javax.swing.JPanel {

    public PenggajianPanel() {
    initComponents();
    styleButton();
    loadKaryawan();
    loadBulan();
    tampilData();
    resetForm();
    util.DesignUtil.applyPage(this);

    tblPenggajian.addMouseListener(new java.awt.event.MouseAdapter() {
        public void mouseClicked(java.awt.event.MouseEvent evt) {
            ambilDataDariTabel();
        }
    });

    cmbKaryawan.addActionListener(e -> {
        if (cmbKaryawan.getSelectedIndex() > 0) {
            autoFillGaji();
        }
    });

    btnSimpan.addActionListener(e -> simpanData());
    btnUbah.addActionListener(e -> ubahData());
    btnHapus.addActionListener(e -> hapusData());
    btnReset.addActionListener(e -> resetForm());
    btnRefresh.addActionListener(e -> { tampilData(); resetForm(); });
    btnCetakSlip.addActionListener(e -> cetakSlip());
}
    
    private void styleButton() {
    styleFilledButton(btnHitung, new java.awt.Color(14, 165, 164), java.awt.Color.WHITE);
    styleFilledButton(btnSimpan, new java.awt.Color(20, 184, 166), java.awt.Color.WHITE);
    styleFilledButton(btnUbah, new java.awt.Color(37, 99, 235), java.awt.Color.WHITE);
    styleFilledButton(btnHapus, new java.awt.Color(239, 68, 68), java.awt.Color.WHITE);
    styleFilledButton(btnReset, new java.awt.Color(243, 244, 246), new java.awt.Color(17, 24, 39));
    styleFilledButton(btnRefresh, new java.awt.Color(243, 244, 246), new java.awt.Color(17, 24, 39));
    styleFilledButton(btnCari, new java.awt.Color(243, 244, 246), new java.awt.Color(17, 24, 39));
    styleFilledButton(btnCetakSlip, new java.awt.Color(79, 70, 229), java.awt.Color.WHITE);
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
    cmbKaryawan.addItem("-- Pilih Karyawan --");
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
    String[] bulan = {"Januari", "Februari", "Maret", "April", "Mei", "Juni",
                      "Juli", "Agustus", "September", "Oktober", "November", "Desember"};
    for (String b : bulan) cmbBulan.addItem(b);
    cmbBulan.setSelectedIndex(LocalDate.now().getMonthValue() - 1);
    txtTahun.setText(String.valueOf(LocalDate.now().getYear()));
}

private void autoFillGaji() {
    try {
        String selected = cmbKaryawan.getSelectedItem().toString();
        int idKaryawan = Integer.parseInt(selected.split(" - ")[0]);

        Connection conn = Koneksi.getConnection();

        // Ambil gaji pokok
        PreparedStatement pst = conn.prepareStatement(
            "SELECT j.gaji_pokok FROM karyawan k JOIN jabatan j ON k.id_jabatan = j.id_jabatan WHERE k.id_karyawan = ?"
        );
        pst.setInt(1, idKaryawan);
        ResultSet rs = pst.executeQuery();
        double gajiPokok = 0;
        if (rs.next()) gajiPokok = rs.getDouble("gaji_pokok");
        jTextField1.setText(String.format("%.0f", gajiPokok)); // <-- ubah ini

        // Ambil tunjangan
        PreparedStatement pst2 = conn.prepareStatement(
            "SELECT SUM(t.nominal) FROM tunjangan t JOIN jabatan j ON t.id_jabatan = j.id_jabatan "
            + "JOIN karyawan k ON k.id_jabatan = j.id_jabatan WHERE k.id_karyawan = ?"
        );
        pst2.setInt(1, idKaryawan);
        ResultSet rs2 = pst2.executeQuery();
        double tunjangan = 0;
        if (rs2.next()) tunjangan = rs2.getDouble(1);
        txtTunjangan.setText(String.format("%.0f", tunjangan)); // <-- ubah ini

        hitungTotal();
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal load gaji: " + e.getMessage());
    }
}

private void hitungTotal() {
    try {
        double gajiPokok = jTextField1.getText().isEmpty() ? 0 : Double.parseDouble(jTextField1.getText());
        double tunjangan = txtTunjangan.getText().isEmpty() ? 0 : Double.parseDouble(txtTunjangan.getText());
        double potongan = txtPotongan.getText().isEmpty() ? 0 : Double.parseDouble(txtPotongan.getText());
        double total = gajiPokok + tunjangan - potongan;
        txtTotalGaji.setText(String.format("%.0f", total)); // <-- ubah ini
    } catch (Exception e) {}
}

private void tampilData() {
    DefaultTableModel model = new DefaultTableModel();
    model.addColumn("ID Gaji");
    model.addColumn("Karyawan");
    model.addColumn("Bulan");
    model.addColumn("Tahun");
    model.addColumn("Gaji Pokok");
    model.addColumn("Tunjangan");
    model.addColumn("Potongan");
    model.addColumn("Total Gaji");
    model.addColumn("Keterangan");

    try {
        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement(
            "SELECT p.*, k.nama_karyawan FROM penggajian p "
            + "JOIN karyawan k ON p.id_karyawan = k.id_karyawan "
            + "ORDER BY p.id_penggajian DESC"
        );
        ResultSet rs = pst.executeQuery();
        while (rs.next()) {
            model.addRow(new Object[]{
                rs.getString("id_penggajian"),
                rs.getString("nama_karyawan"),
                rs.getString("periode").split("-")[1],
                rs.getString("periode").split("-")[0],
                formatRupiah(rs.getString("gaji_pokok")),
                formatRupiah(rs.getString("total_tunjangan")),
                formatRupiah(rs.getString("potongan")),
                formatRupiah(rs.getString("total_gaji")),
                ""
            });
        }
        tblPenggajian.setModel(model);
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal tampil data: " + e.getMessage());
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
        int bulan = cmbBulan.getSelectedIndex() + 1;
        String tahun = txtTahun.getText().trim();
        String periode = tahun + "-" + String.format("%02d", bulan);
        double gajiPokok = Double.parseDouble(jTextField1.getText().isEmpty() ? "0" : jTextField1.getText());
        double tunjangan = Double.parseDouble(txtTunjangan.getText().isEmpty() ? "0" : txtTunjangan.getText());
        double potongan = Double.parseDouble(txtPotongan.getText().isEmpty() ? "0" : txtPotongan.getText());
        double total = gajiPokok + tunjangan - potongan;

        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement(
            "INSERT INTO penggajian (id_karyawan, periode, gaji_pokok, total_tunjangan, potongan, total_gaji, tanggal_gaji) "
            + "VALUES (?, ?, ?, ?, ?, ?, CURDATE())"
        );
        pst.setInt(1, idKaryawan);
        pst.setString(2, periode);
        pst.setDouble(3, gajiPokok);
        pst.setDouble(4, tunjangan);
        pst.setDouble(5, potongan);
        pst.setDouble(6, total);
        pst.executeUpdate();

        JOptionPane.showMessageDialog(this, "Data penggajian berhasil disimpan.");
        tampilData();
        resetForm();
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal simpan: " + e.getMessage());
    }
}

private void ubahData() {
    String idGaji = txtIdGaji.getText().trim();
    if (idGaji.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Pilih data dari tabel dulu.");
        return;
    }
    try {
        double gajiPokok = Double.parseDouble(jTextField1.getText().isEmpty() ? "0" : jTextField1.getText());
        double tunjangan = Double.parseDouble(txtTunjangan.getText().isEmpty() ? "0" : txtTunjangan.getText());
        double potongan = Double.parseDouble(txtPotongan.getText().isEmpty() ? "0" : txtPotongan.getText());
        double total = gajiPokok + tunjangan - potongan;

        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement(
            "UPDATE penggajian SET gaji_pokok=?, total_tunjangan=?, potongan=?, total_gaji=? WHERE id_penggajian=?"
        );
        pst.setDouble(1, gajiPokok);
        pst.setDouble(2, tunjangan);
        pst.setDouble(3, potongan);
        pst.setDouble(4, total);
        pst.setString(5, idGaji);
        pst.executeUpdate();

        JOptionPane.showMessageDialog(this, "Data berhasil diubah.");
        tampilData();
        resetForm();
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal ubah: " + e.getMessage());
    }
}

private void hapusData() {
    String idGaji = txtIdGaji.getText().trim();
    if (idGaji.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Pilih data dari tabel dulu.");
        return;
    }
    int konfirmasi = JOptionPane.showConfirmDialog(this, "Yakin hapus data ini?", "Konfirmasi", JOptionPane.YES_NO_OPTION);
    if (konfirmasi == JOptionPane.YES_OPTION) {
        try {
            Connection conn = Koneksi.getConnection();
            PreparedStatement pst = conn.prepareStatement("DELETE FROM penggajian WHERE id_penggajian=?");
            pst.setString(1, idGaji);
            pst.executeUpdate();
            JOptionPane.showMessageDialog(this, "Data berhasil dihapus.");
            tampilData();
            resetForm();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal hapus: " + e.getMessage());
        }
    }
}

private void resetForm() {
    txtIdGaji.setText("");
    jTextField1.setText("");
    txtTunjangan.setText("");
    txtPotongan.setText("");
    txtTotalGaji.setText("");
    txtKeterangan.setText("");
    cmbKaryawan.setSelectedIndex(0);
    loadBulan();

    btnSimpan.setEnabled(true);
    btnUbah.setEnabled(false);
    btnHapus.setEnabled(false);
}

private void ambilDataDariTabel() {
    int baris = tblPenggajian.getSelectedRow();
    if (baris >= 0) {
        txtIdGaji.setText(tblPenggajian.getValueAt(baris, 0).toString());

        btnSimpan.setEnabled(false);
        btnUbah.setEnabled(true);
        btnHapus.setEnabled(true);
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

private void cetakSlip() {
    String idGaji = txtIdGaji.getText().trim();
    if (idGaji.isEmpty()) {
        JOptionPane.showMessageDialog(this, "Pilih data penggajian dari tabel dulu.");
        return;
    }

    try {
        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement(
            "SELECT p.*, k.nama_karyawan, k.nik, j.nama_jabatan, d.nama_divisi "
            + "FROM penggajian p "
            + "JOIN karyawan k ON p.id_karyawan = k.id_karyawan "
            + "LEFT JOIN jabatan j ON k.id_jabatan = j.id_jabatan "
            + "LEFT JOIN divisi d ON k.id_divisi = d.id_divisi "
            + "WHERE p.id_penggajian = ?"
        );
        pst.setString(1, idGaji);
        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            String namaFile = "slip_gaji_" + rs.getString("nik") + "_" + rs.getString("periode") + ".pdf";
            String desktopPath = System.getProperty("user.home") + "\\Downloads";
            java.io.File desktop = new java.io.File(desktopPath);
            if (!desktop.exists()) {
                desktop.mkdirs();
            }
            String path = desktopPath + "\\" + namaFile;

            com.itextpdf.text.Document doc = new com.itextpdf.text.Document();
            com.itextpdf.text.pdf.PdfWriter.getInstance(doc, new java.io.FileOutputStream(path));
            doc.open();

            // Font
            com.itextpdf.text.Font fontJudul = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 18, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Font fontSub = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 11, com.itextpdf.text.Font.NORMAL);
            com.itextpdf.text.Font fontBold = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 11, com.itextpdf.text.Font.BOLD);
            com.itextpdf.text.Font fontTotal = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 13, com.itextpdf.text.Font.BOLD,
                new com.itextpdf.text.BaseColor(0, 128, 0));
            com.itextpdf.text.Font fontPotongan = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 11, com.itextpdf.text.Font.NORMAL,
                new com.itextpdf.text.BaseColor(200, 0, 0));

            // Header
            com.itextpdf.text.Paragraph judul = new com.itextpdf.text.Paragraph("SLIP GAJI KARYAWAN", fontJudul);
            judul.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            doc.add(judul);

            com.itextpdf.text.Paragraph periode = new com.itextpdf.text.Paragraph(
                "Periode: " + rs.getString("periode"), fontSub);
            periode.setAlignment(com.itextpdf.text.Element.ALIGN_CENTER);
            doc.add(periode);
            doc.add(com.itextpdf.text.Chunk.NEWLINE);

            // Garis
            com.itextpdf.text.pdf.draw.LineSeparator line = new com.itextpdf.text.pdf.draw.LineSeparator();
            doc.add(new com.itextpdf.text.Chunk(line));
            doc.add(com.itextpdf.text.Chunk.NEWLINE);

            // Tabel info karyawan
            com.itextpdf.text.pdf.PdfPTable tabelInfo = new com.itextpdf.text.pdf.PdfPTable(2);
            tabelInfo.setWidthPercentage(100);
            tabelInfo.setWidths(new float[]{40, 60});
            tabelInfo.getDefaultCell().setBorder(com.itextpdf.text.pdf.PdfPCell.NO_BORDER);
            tabelInfo.getDefaultCell().setPadding(4);

            addRowInfo(tabelInfo, "Nama Karyawan", rs.getString("nama_karyawan"), fontSub, fontBold);
            addRowInfo(tabelInfo, "NIK", rs.getString("nik"), fontSub, fontBold);
            addRowInfo(tabelInfo, "Jabatan", rs.getString("nama_jabatan"), fontSub, fontBold);
            addRowInfo(tabelInfo, "Divisi", rs.getString("nama_divisi"), fontSub, fontBold);
            addRowInfo(tabelInfo, "Tanggal Gaji", rs.getString("tanggal_gaji"), fontSub, fontBold);
            doc.add(tabelInfo);
            doc.add(com.itextpdf.text.Chunk.NEWLINE);

            // Garis
            doc.add(new com.itextpdf.text.Chunk(line));
            doc.add(com.itextpdf.text.Chunk.NEWLINE);

            // Tabel gaji
            com.itextpdf.text.pdf.PdfPTable tabelGaji = new com.itextpdf.text.pdf.PdfPTable(2);
            tabelGaji.setWidthPercentage(100);
            tabelGaji.setWidths(new float[]{40, 60});
            tabelGaji.getDefaultCell().setBorder(com.itextpdf.text.pdf.PdfPCell.NO_BORDER);
            tabelGaji.getDefaultCell().setPadding(4);

            addRowInfo(tabelGaji, "Gaji Pokok", formatRupiah(rs.getString("gaji_pokok")), fontSub, fontBold);
            addRowInfo(tabelGaji, "Tunjangan", formatRupiah(rs.getString("total_tunjangan")), fontSub, fontBold);

            // Baris potongan warna merah
            com.itextpdf.text.pdf.PdfPCell cellLabelPot = new com.itextpdf.text.pdf.PdfPCell(
                new com.itextpdf.text.Phrase("Potongan", fontSub));
            cellLabelPot.setBorder(com.itextpdf.text.pdf.PdfPCell.NO_BORDER);
            cellLabelPot.setPadding(4);
            com.itextpdf.text.pdf.PdfPCell cellValPot = new com.itextpdf.text.pdf.PdfPCell(
                new com.itextpdf.text.Phrase("- " + formatRupiah(rs.getString("potongan")), fontPotongan));
            cellValPot.setBorder(com.itextpdf.text.pdf.PdfPCell.NO_BORDER);
            cellValPot.setPadding(4);
            tabelGaji.addCell(cellLabelPot);
            tabelGaji.addCell(cellValPot);

            doc.add(tabelGaji);
            doc.add(com.itextpdf.text.Chunk.NEWLINE);

            // Garis
            doc.add(new com.itextpdf.text.Chunk(line));
            doc.add(com.itextpdf.text.Chunk.NEWLINE);

            // Total gaji
            com.itextpdf.text.pdf.PdfPTable tabelTotal = new com.itextpdf.text.pdf.PdfPTable(2);
            tabelTotal.setWidthPercentage(100);
            tabelTotal.setWidths(new float[]{40, 60});
            tabelTotal.getDefaultCell().setBorder(com.itextpdf.text.pdf.PdfPCell.NO_BORDER);
            tabelTotal.getDefaultCell().setPadding(6);

            com.itextpdf.text.pdf.PdfPCell cellTotalLabel = new com.itextpdf.text.pdf.PdfPCell(
                new com.itextpdf.text.Phrase("TOTAL GAJI", fontTotal));
            cellTotalLabel.setBorder(com.itextpdf.text.pdf.PdfPCell.NO_BORDER);
            cellTotalLabel.setPadding(6);
            com.itextpdf.text.pdf.PdfPCell cellTotalVal = new com.itextpdf.text.pdf.PdfPCell(
                new com.itextpdf.text.Phrase(formatRupiah(rs.getString("total_gaji")), fontTotal));
            cellTotalVal.setBorder(com.itextpdf.text.pdf.PdfPCell.NO_BORDER);
            cellTotalVal.setPadding(6);
            tabelTotal.addCell(cellTotalLabel);
            tabelTotal.addCell(cellTotalVal);
            doc.add(tabelTotal);

            doc.add(com.itextpdf.text.Chunk.NEWLINE);
            com.itextpdf.text.Paragraph footer = new com.itextpdf.text.Paragraph(
                "Dicetak pada: " + java.time.LocalDate.now(), fontSub);
            footer.setAlignment(com.itextpdf.text.Element.ALIGN_RIGHT);
            doc.add(footer);

            doc.close();
            
            java.io.File filePdf = new java.io.File(path);
            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(filePdf);
            }

            JOptionPane.showMessageDialog(this, "Slip gaji berhasil disimpan di Desktop:\n" + namaFile);
        }
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal cetak slip: " + e.getMessage());
    }
}

private void addRowInfo(com.itextpdf.text.pdf.PdfPTable table, String label, String value,
        com.itextpdf.text.Font fontLabel, com.itextpdf.text.Font fontValue) {
    com.itextpdf.text.pdf.PdfPCell cellLabel = new com.itextpdf.text.pdf.PdfPCell(
        new com.itextpdf.text.Phrase(label, fontLabel));
    cellLabel.setBorder(com.itextpdf.text.pdf.PdfPCell.NO_BORDER);
    cellLabel.setPadding(4);

    com.itextpdf.text.pdf.PdfPCell cellValue = new com.itextpdf.text.pdf.PdfPCell(
        new com.itextpdf.text.Phrase(value != null ? value : "-", fontValue));
    cellValue.setBorder(com.itextpdf.text.pdf.PdfPCell.NO_BORDER);
    cellValue.setPadding(4);

    table.addCell(cellLabel);
    table.addCell(cellValue);
}

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code manually kalau masih mau pakai tab Design NetBeans.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        pnlHeader = new javax.swing.JPanel();
        pnlUtama = new javax.swing.JPanel();
        pnlForm = new javax.swing.JPanel();
        lblIdGaji = new javax.swing.JLabel();
        txtIdGaji = new javax.swing.JTextField();
        lblKaryawan = new javax.swing.JLabel();
        cmbKaryawan = new javax.swing.JComboBox<>();
        lblBulan = new javax.swing.JLabel();
        cmbBulan = new javax.swing.JComboBox<>();
        lblTahun = new javax.swing.JLabel();
        txtTahun = new javax.swing.JTextField();
        lblGajiPokok = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        lblTunjangan = new javax.swing.JLabel();
        txtTunjangan = new javax.swing.JTextField();
        lblPotongan = new javax.swing.JLabel();
        txtPotongan = new javax.swing.JTextField();
        lblTotalGaji = new javax.swing.JLabel();
        txtTotalGaji = new javax.swing.JTextField();
        lblKeterangan = new javax.swing.JLabel();
        txtKeterangan = new javax.swing.JTextField();
        lblCari = new javax.swing.JLabel();
        txtCari = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        btnHitung = new javax.swing.JButton();
        btnSimpan = new javax.swing.JButton();
        btnUbah = new javax.swing.JButton();
        btnHapus = new javax.swing.JButton();
        btnReset = new javax.swing.JButton();
        btnCari = new javax.swing.JButton();
        btnRefresh = new javax.swing.JButton();
        btnCetakSlip = new javax.swing.JButton();
        lblTitle = new javax.swing.JLabel();
        pnlTable = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblPenggajian = new javax.swing.JTable();

        setPreferredSize(new java.awt.Dimension(5437, 993));

        javax.swing.GroupLayout pnlHeaderLayout = new javax.swing.GroupLayout(pnlHeader);
        pnlHeader.setLayout(pnlHeaderLayout);
        pnlHeaderLayout.setHorizontalGroup(
            pnlHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1012, Short.MAX_VALUE)
        );
        pnlHeaderLayout.setVerticalGroup(
            pnlHeaderLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 662, Short.MAX_VALUE)
        );

        pnlUtama.setPreferredSize(new java.awt.Dimension(1000, 650));

        pnlForm.setPreferredSize(new java.awt.Dimension(300, 430));

        lblIdGaji.setText("ID Gaji");
        lblIdGaji.setAlignmentX(20.0F);
        lblIdGaji.setAlignmentY(20.0F);

        txtIdGaji.setActionCommand("<Not Set>");
        txtIdGaji.setAlignmentX(140.0F);
        txtIdGaji.setAlignmentY(20.0F);
        txtIdGaji.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtIdGajiActionPerformed(evt);
            }
        });

        lblKaryawan.setText("Karyawan");
        lblKaryawan.setAlignmentX(20.0F);
        lblKaryawan.setAlignmentY(60.0F);

        cmbKaryawan.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        cmbKaryawan.setAlignmentX(140.0F);
        cmbKaryawan.setAlignmentY(60.0F);

        lblBulan.setText("Bulan");
        lblBulan.setAlignmentX(20.0F);
        lblBulan.setAlignmentY(100.0F);

        cmbBulan.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        lblTahun.setText("Tahun");
        lblTahun.setAlignmentX(20.0F);
        lblTahun.setAlignmentY(140.0F);

        txtTahun.setAlignmentX(140.0F);
        txtTahun.setAlignmentY(140.0F);
        txtTahun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtTahunActionPerformed(evt);
            }
        });

        lblGajiPokok.setText("Gaji Pokok");
        lblGajiPokok.setAlignmentX(20.0F);
        lblGajiPokok.setAlignmentY(180.0F);

        jTextField1.setActionCommand(null);
        jTextField1.setAlignmentX(140.0F);
        jTextField1.setAlignmentY(180.0F);
        jTextField1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextField1ActionPerformed(evt);
            }
        });

        lblTunjangan.setText("Tunjangan");

        txtTunjangan.setAlignmentX(140.0F);
        txtTunjangan.setAlignmentY(220.0F);

        lblPotongan.setText("Potongan");

        txtPotongan.setAlignmentX(140.0F);
        txtPotongan.setAlignmentY(260.0F);

        lblTotalGaji.setText("Total Gaji");

        txtTotalGaji.setAlignmentX(140.0F);
        txtTotalGaji.setAlignmentY(300.0F);
        txtTotalGaji.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtTotalGajiActionPerformed(evt);
            }
        });

        lblKeterangan.setText("Keterangan");

        txtKeterangan.setAlignmentX(140.0F);
        txtKeterangan.setAlignmentY(340.0F);
        txtKeterangan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtKeteranganActionPerformed(evt);
            }
        });

        lblCari.setText("Cari");

        javax.swing.GroupLayout pnlFormLayout = new javax.swing.GroupLayout(pnlForm);
        pnlForm.setLayout(pnlFormLayout);
        pnlFormLayout.setHorizontalGroup(
            pnlFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlFormLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlFormLayout.createSequentialGroup()
                        .addGroup(pnlFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblKaryawan)
                            .addComponent(lblBulan)
                            .addComponent(lblTahun))
                        .addGroup(pnlFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(pnlFormLayout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addComponent(txtTahun))
                            .addGroup(pnlFormLayout.createSequentialGroup()
                                .addGap(18, 18, 18)
                                .addGroup(pnlFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(cmbBulan, javax.swing.GroupLayout.Alignment.TRAILING, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addGroup(pnlFormLayout.createSequentialGroup()
                                        .addGroup(pnlFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                            .addComponent(txtIdGaji)
                                            .addComponent(cmbKaryawan, 0, 129, Short.MAX_VALUE))
                                        .addContainerGap())))))
                    .addGroup(pnlFormLayout.createSequentialGroup()
                        .addComponent(lblGajiPokok)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jTextField1))
                    .addGroup(pnlFormLayout.createSequentialGroup()
                        .addGroup(pnlFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblIdGaji, javax.swing.GroupLayout.PREFERRED_SIZE, 45, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(pnlFormLayout.createSequentialGroup()
                                .addGroup(pnlFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(lblKeterangan)
                                    .addGroup(pnlFormLayout.createSequentialGroup()
                                        .addGap(9, 9, 9)
                                        .addComponent(lblCari, javax.swing.GroupLayout.PREFERRED_SIZE, 29, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(pnlFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(txtKeterangan, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(txtCari, javax.swing.GroupLayout.PREFERRED_SIZE, 110, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addGap(0, 28, Short.MAX_VALUE))
                    .addGroup(pnlFormLayout.createSequentialGroup()
                        .addComponent(lblTunjangan)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(txtTunjangan))
                    .addGroup(pnlFormLayout.createSequentialGroup()
                        .addComponent(lblPotongan)
                        .addGap(18, 18, 18)
                        .addComponent(txtPotongan))
                    .addGroup(pnlFormLayout.createSequentialGroup()
                        .addComponent(lblTotalGaji)
                        .addGap(18, 18, 18)
                        .addComponent(txtTotalGaji))))
        );
        pnlFormLayout.setVerticalGroup(
            pnlFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlFormLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(pnlFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblIdGaji)
                    .addComponent(txtIdGaji, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblKaryawan)
                    .addComponent(cmbKaryawan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cmbBulan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblBulan))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblTahun, javax.swing.GroupLayout.PREFERRED_SIZE, 22, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtTahun, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblGajiPokok)
                    .addComponent(jTextField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblTunjangan)
                    .addComponent(txtTunjangan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblPotongan)
                    .addComponent(txtPotongan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblTotalGaji)
                    .addComponent(txtTotalGaji, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnlFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(txtKeterangan, javax.swing.GroupLayout.PREFERRED_SIZE, 60, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblKeterangan))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblCari)
                    .addComponent(txtCari, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(24, Short.MAX_VALUE))
        );

        btnHitung.setText("HItung");
        btnHitung.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHitungActionPerformed(evt);
            }
        });

        btnSimpan.setText("Simpan");

        btnUbah.setText("Ubah");

        btnHapus.setText("Hapus");

        btnReset.setText("Reset");

        btnCari.setText("Cari");
        btnCari.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCariActionPerformed(evt);
            }
        });

        btnRefresh.setText("Refresh");

        btnCetakSlip.setText("Cetak Slip");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(btnHitung)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnSimpan)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnUbah))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(btnHapus)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnReset)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnRefresh))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(btnCari)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(btnCetakSlip)))
                .addContainerGap(71, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnHitung)
                    .addComponent(btnSimpan)
                    .addComponent(btnUbah))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnHapus)
                    .addComponent(btnReset)
                    .addComponent(btnRefresh))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnCari)
                    .addComponent(btnCetakSlip))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        lblTitle.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        lblTitle.setText("PENGGAJIAN KARYAWAN");

        tblPenggajian.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null},
                {null, null, null, null, null, null, null, null, null}
            },
            new String [] {
                "ID Gaji", "Karyawan", "Bulan", "Tahun ", "Gaji Pokok", "Tunjangan", "Potongan", "Total Gaji", "Keterangan"
            }
        ));
        jScrollPane1.setViewportView(tblPenggajian);

        javax.swing.GroupLayout pnlTableLayout = new javax.swing.GroupLayout(pnlTable);
        pnlTable.setLayout(pnlTableLayout);
        pnlTableLayout.setHorizontalGroup(
            pnlTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 871, Short.MAX_VALUE)
        );
        pnlTableLayout.setVerticalGroup(
            pnlTableLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlTableLayout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 433, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout pnlUtamaLayout = new javax.swing.GroupLayout(pnlUtama);
        pnlUtama.setLayout(pnlUtamaLayout);
        pnlUtamaLayout.setHorizontalGroup(
            pnlUtamaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlUtamaLayout.createSequentialGroup()
                .addGroup(pnlUtamaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlUtamaLayout.createSequentialGroup()
                        .addGap(41, 41, 41)
                        .addGroup(pnlUtamaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(pnlUtamaLayout.createSequentialGroup()
                                .addComponent(pnlForm, javax.swing.GroupLayout.PREFERRED_SIZE, 209, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(lblTitle, javax.swing.GroupLayout.PREFERRED_SIZE, 217, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(pnlUtamaLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(pnlTable, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(123, Short.MAX_VALUE))
        );
        pnlUtamaLayout.setVerticalGroup(
            pnlUtamaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlUtamaLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(lblTitle, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(pnlUtamaLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlUtamaLayout.createSequentialGroup()
                        .addComponent(pnlForm, javax.swing.GroupLayout.PREFERRED_SIZE, 348, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, pnlUtamaLayout.createSequentialGroup()
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(37, 37, 37)))
                .addComponent(pnlTable, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(31, 31, 31))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(159, 159, 159)
                        .addComponent(pnlHeader, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(67, 67, 67)
                        .addComponent(pnlUtama, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(4266, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(pnlUtama, javax.swing.GroupLayout.PREFERRED_SIZE, 827, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(pnlHeader, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    private void txtIdGajiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtIdGajiActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtIdGajiActionPerformed

    private void txtTahunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtTahunActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtTahunActionPerformed

    private void jTextField1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextField1ActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextField1ActionPerformed

    private void txtTotalGajiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtTotalGajiActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtTotalGajiActionPerformed

    private void txtKeteranganActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtKeteranganActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_txtKeteranganActionPerformed

    private void btnHitungActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHitungActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnHitungActionPerformed

    private void btnCariActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCariActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_btnCariActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCari;
    private javax.swing.JButton btnCetakSlip;
    private javax.swing.JButton btnHapus;
    private javax.swing.JButton btnHitung;
    private javax.swing.JButton btnRefresh;
    private javax.swing.JButton btnReset;
    private javax.swing.JButton btnSimpan;
    private javax.swing.JButton btnUbah;
    private javax.swing.JComboBox<String> cmbBulan;
    private javax.swing.JComboBox<String> cmbKaryawan;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JLabel lblBulan;
    private javax.swing.JLabel lblCari;
    private javax.swing.JLabel lblGajiPokok;
    private javax.swing.JLabel lblIdGaji;
    private javax.swing.JLabel lblKaryawan;
    private javax.swing.JLabel lblKeterangan;
    private javax.swing.JLabel lblPotongan;
    private javax.swing.JLabel lblTahun;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JLabel lblTotalGaji;
    private javax.swing.JLabel lblTunjangan;
    private javax.swing.JPanel pnlForm;
    private javax.swing.JPanel pnlHeader;
    private javax.swing.JPanel pnlTable;
    private javax.swing.JPanel pnlUtama;
    private javax.swing.JTable tblPenggajian;
    private javax.swing.JTextField txtCari;
    private javax.swing.JTextField txtIdGaji;
    private javax.swing.JTextField txtKeterangan;
    private javax.swing.JTextField txtPotongan;
    private javax.swing.JTextField txtTahun;
    private javax.swing.JTextField txtTotalGaji;
    private javax.swing.JTextField txtTunjangan;
    // End of variables declaration//GEN-END:variables
}
