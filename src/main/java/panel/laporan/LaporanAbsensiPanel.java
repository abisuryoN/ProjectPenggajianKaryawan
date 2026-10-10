package panel.laporan;
import config.Koneksi;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;


/**
 * Panel kosong untuk diedit lewat NetBeans GUI Builder.
 * Buka tab Design, lalu drag komponen sesuai kebutuhan.
 */
public class LaporanAbsensiPanel extends javax.swing.JPanel {

    /** Tanggal cetak format Indonesia, mengikuti hari saat laporan dicetak. */
    private static String tanggalCetakIndonesia() {
        java.time.LocalDate t = java.time.LocalDate.now();
        String[] hari = {"", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu"};
        String[] bulan = {"", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"};
        return hari[t.getDayOfWeek().getValue()] + ", " + t.getDayOfMonth() + " "
                + bulan[t.getMonthValue()] + " " + t.getYear();
    }

    /** Tanggal lengkap format Indonesia, mis. "Senin, 12 Mei 2026". */
    private static String tanggalIndonesia(java.util.Date d) {
        if (d == null) return "";
        java.time.LocalDate t = new java.sql.Date(d.getTime()).toLocalDate();
        String[] hari = {"", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu"};
        String[] bulan = {"", "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"};
        return hari[t.getDayOfWeek().getValue()] + ", " + t.getDayOfMonth() + " "
                + bulan[t.getMonthValue()] + " " + t.getYear();
    }

    public LaporanAbsensiPanel() {
    initComponents();
    rebuildLayout();
    styleButton();
    loadKaryawan();
    loadStatus();
    setupDateChooser();
    tampilData();
    util.DesignUtil.applyPage(this);

    cmbKaryawan.setVisible(false);
    jLabel4.setVisible(false); // label "Karyawan"

    btnRefresh.addActionListener(e -> { txtCari.setText(""); filterData(); });
    btnReset.addActionListener(e -> {
        cmbStatus.setSelectedIndex(0);
        jDateChooser1.setDate(null);
        jDateChooser2.setDate(null);
        txtCari.setText("");
        tampilData();
    });
    // btnCari, btnFilter, btnExportPDF, btnCetak sudah punya handler dari
    // GUI Builder (.form); tidak dipasang lagi di sini supaya tidak jalan 2x.
    cmbStatus.addActionListener(e -> filterData());
    tblLaporanAbsensi.setDefaultEditor(Object.class, null);
    tblLaporanAbsensi.getTableHeader().setReorderingAllowed(false);
}

/** Susun ulang layout mengikuti gaya Laporan Karyawan: judul tengah + filter + tabel penuh. */
private void rebuildLayout() {
    removeAll();
    setLayout(new java.awt.BorderLayout(0, 14));

    javax.swing.JPanel titlePanel = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.CENTER, 0, 8));
    titlePanel.setOpaque(false);
    util.DesignUtil.styleJudulLaporan(jLabel1, judulLaporan());
    titlePanel.add(jLabel1);

    javax.swing.JPanel filter = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 6));
    filter.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Filter Data",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            new java.awt.Font(util.DesignUtil.UI_FONT, java.awt.Font.BOLD, 12),
            new java.awt.Color(22, 48, 92)));
    jLabel4.setVisible(false);
    cmbKaryawan.setVisible(false);
    jDateChooser1.setPreferredSize(new java.awt.Dimension(150, 32));
    jDateChooser2.setPreferredSize(new java.awt.Dimension(150, 32));
    cmbStatus.setPreferredSize(new java.awt.Dimension(150, 32));
    txtCari.setPreferredSize(new java.awt.Dimension(220, 32));
    filter.add(jLabel2); filter.add(jDateChooser1);
    filter.add(jLabel3); filter.add(jDateChooser2);
    filter.add(jLabel6); filter.add(cmbStatus);
    filter.add(jLabel7); filter.add(txtCari);

    // Label ringkasan tanggal terpilih: "Senin, 12 Mei 2026".
    // JDateChooser hanya menampilkan teks di dalam kotaknya, jadi ringkasan
    // hari/tanggal/bulan yang dipilih ditampilkan di sini.
    lblTanggalTerpilih = new javax.swing.JLabel();
    lblTanggalTerpilih.setFont(new java.awt.Font(util.DesignUtil.UI_FONT, java.awt.Font.BOLD, 12));
    lblTanggalTerpilih.setForeground(new java.awt.Color(22, 48, 92));
    filter.add(lblTanggalTerpilih);
    updateLabelTanggal();

    javax.swing.JPanel btnPanel = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, 10, 0));
    btnPanel.setOpaque(false);
    btnPanel.add(btnCari);
    btnPanel.add(btnFilter);
    btnPanel.add(btnExportPDF);
    btnPanel.add(btnCetak);
    btnPanel.add(btnReset);
    btnPanel.add(btnRefresh);

    javax.swing.JPanel topPanel = new javax.swing.JPanel(new java.awt.BorderLayout(16, 8));
    topPanel.setOpaque(false);
    topPanel.add(filter, java.awt.BorderLayout.CENTER);
    topPanel.add(btnPanel, java.awt.BorderLayout.SOUTH);

    javax.swing.JPanel tabelPanel = new javax.swing.JPanel(new java.awt.BorderLayout());
    tabelPanel.setOpaque(false);
    tabelPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Data Laporan Absensi",
            javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION,
            javax.swing.border.TitledBorder.DEFAULT_POSITION,
            new java.awt.Font(util.DesignUtil.UI_FONT, java.awt.Font.BOLD, 12),
            new java.awt.Color(22, 48, 92)));
    jScrollPane2.setHorizontalScrollBarPolicy(javax.swing.JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    tabelPanel.add(jScrollPane2, java.awt.BorderLayout.CENTER);

    javax.swing.JPanel content = new javax.swing.JPanel(new java.awt.BorderLayout(0, 12));
    content.setOpaque(false);
    content.add(topPanel, java.awt.BorderLayout.NORTH);
    content.add(tabelPanel, java.awt.BorderLayout.CENTER);

    add(titlePanel, java.awt.BorderLayout.NORTH);
    add(content, java.awt.BorderLayout.CENTER);
}

private String judulLaporan() {
    return "Laporan Absensi Karyawan";
}
    
    private void styleButton() {
    util.DesignUtil.styleFilledButton(btnCari, util.DesignUtil.TOMBOL_UTAMA, java.awt.Color.WHITE);
    util.DesignUtil.styleFilledButton(btnFilter, util.DesignUtil.TOMBOL_UTAMA, java.awt.Color.WHITE);
    util.DesignUtil.styleFilledButton(btnExportPDF, util.DesignUtil.TOMBOL_SEKUNDER, java.awt.Color.WHITE);
    util.DesignUtil.styleFilledButton(btnCetak, util.DesignUtil.TOMBOL_SEKUNDER, java.awt.Color.WHITE);
    util.DesignUtil.styleFilledButton(btnReset, util.DesignUtil.TOMBOL_BAHAYA, java.awt.Color.WHITE);
    util.DesignUtil.styleFilledButton(btnRefresh, util.DesignUtil.TOMBOL_NETRAL, util.DesignUtil.TEKS_GELAP);
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
    cmbStatus.addItem("Cuti");
}

private void tampilData() {
    loadAbsensiData(false);
}

private void setupDateChooser() {
    jDateChooser1.setDateFormatString("dd MMM yyyy");
    jDateChooser2.setDateFormatString("dd MMM yyyy");
    jDateChooser1.setDate(null);
    jDateChooser2.setDate(null);
    jDateChooser1.addPropertyChangeListener("date", e -> { updateLabelTanggal(); filterData(); });
    jDateChooser2.addPropertyChangeListener("date", e -> { updateLabelTanggal(); filterData(); });
}

/** Perbarui label ringkasan tanggal terpilih (hari + tanggal + bulan Indonesia). */
private void updateLabelTanggal() {
    java.util.Date awal = jDateChooser1.getDate();
    java.util.Date akhir = jDateChooser2.getDate();
    if (awal == null && akhir == null) {
        lblTanggalTerpilih.setText("");
    } else if (akhir == null) {
        lblTanggalTerpilih.setText("Dipilih: " + tanggalIndonesia(awal));
    } else if (awal == null) {
        lblTanggalTerpilih.setText("s.d. " + tanggalIndonesia(akhir));
    } else {
        lblTanggalTerpilih.setText(tanggalIndonesia(awal) + "  s.d.  " + tanggalIndonesia(akhir));
    }
}

private DefaultTableModel createTableModel() {
    DefaultTableModel model = util.TableUtil.nonEditableModel();
    model.addColumn("NO");
    model.addColumn("Nama Karyawan");
    model.addColumn("Tanggal");
    model.addColumn("Jam Masuk");
    model.addColumn("Jam Pulang");
    model.addColumn("Status");
    model.addColumn("Keterangan");
    return model;
}

private Date getTanggalAwal() {
    return jDateChooser1.getDate() == null ? null : new Date(jDateChooser1.getDate().getTime());
}

private Date getTanggalAkhir() {
    return jDateChooser2.getDate() == null ? null : new Date(jDateChooser2.getDate().getTime());
}

private Integer getSelectedIdKaryawan() {
    if (cmbKaryawan.getSelectedIndex() <= 0) {
        return null;
    }
    String selected = cmbKaryawan.getSelectedItem().toString();
    return Integer.parseInt(selected.split(" - ")[0]);
}

private String getSelectedStatus() {
    return cmbStatus.getSelectedIndex() > 0 ? cmbStatus.getSelectedItem().toString() : null;
}

private boolean isTanggalValid(Date tanggalAwal, Date tanggalAkhir) {
    if (tanggalAwal != null && tanggalAkhir != null && tanggalAwal.after(tanggalAkhir)) {
        JOptionPane.showMessageDialog(this, "Tanggal awal tidak boleh lebih besar dari tanggal akhir.");
        return false;
    }
    return true;
}

private void loadAbsensiData(boolean useFilter) {
    DefaultTableModel model = createTableModel();
    try {
        Date tanggalAwal = useFilter ? getTanggalAwal() : null;
        Date tanggalAkhir = useFilter ? getTanggalAkhir() : null;
        if (!isTanggalValid(tanggalAwal, tanggalAkhir)) return;

        boolean isHRD = util.Session.isHRD(); // ✅ cek role

        StringBuilder sql = new StringBuilder(
            "SELECT a.id_absensi, k.nama_karyawan, a.tanggal, "
            + "a.jam_masuk, a.jam_pulang, a.status, a.keterangan "
            + "FROM absensi a "
            + "JOIN karyawan k ON a.id_karyawan = k.id_karyawan WHERE 1=1 "
        );
        List<Object> params = new ArrayList<>();

        if (!isHRD) { // ✅ hanya filter by user kalau bukan HRD
            sql.append("AND a.id_karyawan = ? ");
            params.add(util.Session.getIdKaryawan());
        }

        if (useFilter) {
            String status = getSelectedStatus();
            String keyword = txtCari.getText().trim();
            if (tanggalAwal != null) { sql.append("AND a.tanggal >= ? "); params.add(tanggalAwal); }
            if (tanggalAkhir != null) { sql.append("AND a.tanggal <= ? "); params.add(tanggalAkhir); }
            if (status != null) { sql.append("AND a.status = ? "); params.add(status); }
            if (!keyword.isEmpty()) {
                sql.append("AND (k.nama_karyawan LIKE ? OR a.tanggal LIKE ? OR a.status LIKE ? OR COALESCE(a.keterangan,'') LIKE ?) ");
                String like = "%" + keyword + "%";
                params.add(like); params.add(like); params.add(like); params.add(like);
            }
        }

        sql.append("ORDER BY a.tanggal DESC");
        Connection conn = Koneksi.getConnection();
        PreparedStatement pst = conn.prepareStatement(sql.toString());
        for (int i = 0; i < params.size(); i++) pst.setObject(i + 1, params.get(i));
        ResultSet rs = pst.executeQuery();
        int no = 1;
        while (rs.next()) {
            model.addRow(new Object[]{
                no++, rs.getString("nama_karyawan"), rs.getString("tanggal"),
                rs.getString("jam_masuk"), rs.getString("jam_pulang"),
                rs.getString("status"),
                rs.getString("keterangan") != null ? rs.getString("keterangan") : ""
            });
        }
        tblLaporanAbsensi.setModel(model);
        util.DesignUtil.applyTable(tblLaporanAbsensi);
    } catch (Exception e) {
        JOptionPane.showMessageDialog(this, "Gagal tampil data: " + e.getMessage());
    }
}

private void filterData() {
    loadAbsensiData(true);
}

private void cariData() {
    filterData();
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
            "Dicetak: " + tanggalCetakIndonesia(),
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
        Date tanggalAwal = getTanggalAwal();
        Date tanggalAkhir = getTanggalAkhir();
        if (!isTanggalValid(tanggalAwal, tanggalAkhir)) {
            return;
        }
        params.put("tanggal_awal", tanggalAwal);
        params.put("tanggal_akhir", tanggalAkhir);
        params.put("id_karyawan", getSelectedIdKaryawan());
        params.put("status", getSelectedStatus());
        params.put("keyword", txtCari.getText().trim().isEmpty() ? null : txtCari.getText().trim());
        params.put("tanggal_cetak", tanggalCetakIndonesia());
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
        jCalendar1 = new com.toedter.calendar.JCalendar();
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
        jDateChooser1 = new com.toedter.calendar.JDateChooser();
        jDateChooser2 = new com.toedter.calendar.JDateChooser();
        jPanel4 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        tblLaporanAbsensi = new javax.swing.JTable();
        pnlFilter = new javax.swing.JPanel();
        btnCetak = new javax.swing.JButton();
        btnExportPDF = new javax.swing.JButton();
        btnRefresh = new javax.swing.JButton();
        btnReset = new javax.swing.JButton();

        jLabel5.setText("jLabel5");

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setText("Laporan Absensi Karyawan");

        jPanel1.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 226, 235)));

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Filter Data", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI", 1, 12), new java.awt.Color(13, 71, 161))); // NOI18N

        jLabel2.setText("Tanggal Awal");

        jLabel3.setText("Tanggal Akhir");

        jLabel4.setText("Karyawan");

        cmbKaryawan.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "-- Semua Karyawan --" }));

        jLabel6.setText("Status");

        jLabel7.setText("Cari Data");

        cmbStatus.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "-- Semua Status --", "Hadir", "Izin", "Sakit", "Alpha", "Cuti" }));

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
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(cmbKaryawan, javax.swing.GroupLayout.PREFERRED_SIZE, 215, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jDateChooser1, javax.swing.GroupLayout.DEFAULT_SIZE, 277, Short.MAX_VALUE)
                    .addComponent(jDateChooser2, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(39, 39, 39)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel7)
                    .addComponent(jLabel6))
                .addGap(88, 88, 88)
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
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel6)
                                .addComponent(cmbStatus, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(18, 18, 18)
                        .addComponent(txtCari, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(btnFilter)
                            .addComponent(btnCari)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(3, 3, 3)
                        .addComponent(jDateChooser1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(16, 16, 16)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel3)
                                .addComponent(jLabel7))
                            .addComponent(jDateChooser2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(24, 24, 24)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4)
                            .addComponent(cmbKaryawan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(12, Short.MAX_VALUE))
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Data Laporan Absensi", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI", 1, 12), new java.awt.Color(13, 71, 161))); // NOI18N

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
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 278, Short.MAX_VALUE)
                .addContainerGap())
        );

        pnlFilter.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Aksi", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI", 1, 12), new java.awt.Color(13, 71, 161))); // NOI18N

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
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(15, 15, 15)
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(318, 318, 318)
                        .addComponent(jLabel1)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(14, 14, 14)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void txtCariActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtCariActionPerformed
        cariData();
    }//GEN-LAST:event_txtCariActionPerformed

    private void btnFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFilterActionPerformed
        filterData();
    }//GEN-LAST:event_btnFilterActionPerformed

    private void btnCariActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCariActionPerformed
         cariData();
    }//GEN-LAST:event_btnCariActionPerformed

    private void btnRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshActionPerformed
         filterData();
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
        jDateChooser1.setDate(null);
        jDateChooser2.setDate(null);
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
    private com.toedter.calendar.JCalendar jCalendar1;
    private com.toedter.calendar.JDateChooser jDateChooser1;
    private com.toedter.calendar.JDateChooser jDateChooser2;
    private javax.swing.JLabel lblTanggalTerpilih;
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
