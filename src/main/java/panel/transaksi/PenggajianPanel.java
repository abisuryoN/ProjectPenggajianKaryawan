package panel.transaksi;

import config.Koneksi;
import java.awt.Color;
import java.awt.Component;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import service.PenggajianService;

/**
 * Panel Penggajian (JForm NetBeans — desain di PenggajianPanel.form).
 *
 * Perbaikan layout (bugfix):
 * - Form input DUA KOLOM (kiri: ID/Karyawan/Bulan/Tahun; kanan: Pokok/
 *   Tunjangan/Potongan/Total) — tidak lagi satu kolom panjang ke bawah.
 * - Dropdown karyawan berukuran terbatas (280..340 px) sehingga popup-nya
 *   tidak melebar memenuhi halaman; renderer merapikan tampilan "NIK | Nama".
 * - Tombol aksi satu baris (Hitung..Reset + Cetak Slip).
 * - Tabel lebar penuh di bawah, tinggi mengikuti sisa ruang.
 */
public class PenggajianPanel extends javax.swing.JPanel {

    private static final String[] BULAN = {
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    };

    private boolean sedangIsiForm = false;

    public PenggajianPanel() {
        initComponents();
        pasangGaya();
        pasangRendererTabel();
        loadKaryawan();
        setBulanTahunSekarang();
        tampilData();
        resetForm();
        util.DesignUtil.applyPage(this);
    }

    // ================================================================ gaya UI

    private void pasangGaya() {
        // warna tombol memakai palet bersama util.DesignUtil agar sama
        // dengan panel Master (Data Karyawan/Divisi/Jabatan/Tunjangan) dan Laporan.
        util.DesignUtil.styleFilledButton(btnHitung, util.DesignUtil.TOMBOL_SEKUNDER, java.awt.Color.WHITE);
        util.DesignUtil.styleFilledButton(btnSimpan, util.DesignUtil.TOMBOL_SEKUNDER, java.awt.Color.WHITE);
        util.DesignUtil.styleFilledButton(btnUbah, util.DesignUtil.TOMBOL_UTAMA, java.awt.Color.WHITE);
        util.DesignUtil.styleFilledButton(btnHapus, util.DesignUtil.TOMBOL_BAHAYA, java.awt.Color.WHITE);
        util.DesignUtil.styleFilledButton(btnResetAksi, util.DesignUtil.TOMBOL_NETRAL, util.DesignUtil.TEKS_GELAP);
        util.DesignUtil.styleFilledButton(btnCari, util.DesignUtil.TOMBOL_SEKUNDER, java.awt.Color.WHITE);
        util.DesignUtil.styleFilledButton(btnFilter, util.DesignUtil.TOMBOL_UTAMA, java.awt.Color.WHITE);
        util.DesignUtil.styleFilledButton(btnResetFilter, util.DesignUtil.TOMBOL_NETRAL, util.DesignUtil.TEKS_GELAP);
        util.DesignUtil.styleFilledButton(btnRefresh, util.DesignUtil.TOMBOL_NETRAL, util.DesignUtil.TEKS_GELAP);
        util.DesignUtil.styleFilledButton(btnCetakSlip, util.DesignUtil.TOMBOL_UTAMA, java.awt.Color.WHITE);

        txtTotalGaji.setFont(txtTotalGaji.getFont().deriveFont(java.awt.Font.BOLD));
        txtTotalGaji.setForeground(new Color(13, 148, 136));

        // Dropdown karyawan: popup mengikuti lebar combo (280..340 px),
        // bukan lebar halaman. Renderer merapikan "id - nik - nama".
        cmbKaryawan.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean selected, boolean focus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(
                        list, value, index, selected, focus);
                String s = String.valueOf(value);
                if (s.contains(" - ")) {
                    String[] bag = s.split(" - ", 3);
                    if (bag.length == 3) {
                        l.setText(bag[1] + "  |  " + bag[2]);
                    }
                }
                return l;
            }
        });
    }

    private void pasangRendererTabel() {
        DefaultTableCellRenderer kanan = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v,
                    boolean sel, boolean foc, int r, int c) {
                JLabel k = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, r, c);
                k.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
                return k;
            }
        };
        for (int kolom = 4; kolom <= 7; kolom++) {
            tblPenggajian.getColumnModel().getColumn(kolom).setCellRenderer(kanan);
        }
        int[] lebar = {70, 180, 90, 70, 130, 130, 130, 140, 160};
        for (int i = 0; i < lebar.length && i < tblPenggajian.getColumnCount(); i++) {
            tblPenggajian.getColumnModel().getColumn(i).setPreferredWidth(lebar[i]);
        }
        tblPenggajian.setRowHeight(26);
        tblPenggajian.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblPenggajian.getTableHeader().setReorderingAllowed(false);
    }

    private void setBulanTahunSekarang() {
        LocalDate kini = LocalDate.now();
        if (cmbBulan.getItemCount() >= 12) {
            cmbBulan.setSelectedIndex(kini.getMonthValue() - 1);
        }
        txtTahun.setText(String.valueOf(kini.getYear()));
        lblPeriode.setText("Periode aktif: " + BULAN[kini.getMonthValue() - 1]
                + " " + kini.getYear());
    }

    // ================================================================= data

    private void loadKaryawan() {
        sedangIsiForm = true;
        try {
            DefaultComboBoxModel<String> m = new DefaultComboBoxModel<>();
            m.addElement("-- Pilih Karyawan --");
            Connection conn = Koneksi.getConnection();
            PreparedStatement pst = conn.prepareStatement(
                "SELECT id_karyawan, nik, nama_karyawan FROM karyawan ORDER BY nama_karyawan");
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                m.addElement(rs.getInt("id_karyawan") + " - " + rs.getString("nik")
                        + " - " + rs.getString("nama_karyawan"));
            }
            cmbKaryawan.setModel(m);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal memuat karyawan: " + e.getMessage());
        } finally {
            sedangIsiForm = false;
        }
    }

    /** ID karyawan dari item combo "id - nik - nama". */
    private int idKaryawanTerpilih() {
        Object o = cmbKaryawan.getSelectedItem();
        if (o == null) return 0;
        String s = String.valueOf(o);
        int dash = s.indexOf(" - ");
        if (dash <= 0) return 0;
        try {
            return Integer.parseInt(s.substring(0, dash).trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String periodeTerpilih() {
        int bulan = cmbBulan.getSelectedIndex() + 1;
        String tahun = txtTahun.getText().trim();
        if (!tahun.matches("\\d{4}")) {
            throw new IllegalArgumentException("Tahun harus 4 digit angka.");
        }
        return tahun + "-" + String.format("%02d", bulan);
    }

    /**
     * Membersihkan teks nominal jadi angka murni siap di-parse:
     * membuang "Rp", spasi, titik ribuan, dan ",00" / ",-" di belakang
     * (yang muncul dari format mata uang Indonesia).
     */
    private String bersihkanNominal(String teks) {
        String t = teks == null ? "" : teks;
        t = t.replace("Rp", "").replace("rp", "").replace(" ", "").trim();
        t = t.replaceAll("[.,]00$", "");   // 100000,00 -> 100000
        t = t.replaceAll("[.,]-$", "");    // 100000,-  -> 100000
        t = t.replace(".", "");            // pemisah ribuan 100.000 -> 100000
        t = t.replace(",", ".");           // kalau masih ada koma desimal -> titik
        t = t.replaceAll("^[.,]+", "");
        return t;
    }

    private java.math.BigDecimal bacaNominal(javax.swing.JTextField f, String namaField) {
        String t = bersihkanNominal(f.getText());
        if (t.isEmpty()) {
            return java.math.BigDecimal.ZERO;
        }
        try {
            java.math.BigDecimal v = new java.math.BigDecimal(t);
            if (v.signum() < 0) {
                throw new IllegalArgumentException(namaField + " tidak boleh negatif.");
            }
            return v;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(namaField + " harus angka yang valid.");
        }
    }

    private void hitungTotal() {
        try {
            java.math.BigDecimal total = PenggajianService.hitungTotal(
                    bacaNominal(txtGajiPokok, "Gaji pokok"),
                    bacaNominal(txtTunjangan, "Tunjangan"),
                    bacaNominal(txtPotongan, "Potongan"));
            txtTotalGaji.setText(util.FormatRupiah.formatPlain(total.doubleValue()));
        } catch (IllegalArgumentException e) {
            txtTotalGaji.setText(e.getMessage());
        }
    }

    private void autoFillGaji() {
        int id = idKaryawanTerpilih();
        if (id <= 0) return;
        try {
            Connection conn = Koneksi.getConnection();
            PreparedStatement pst = conn.prepareStatement(
                "SELECT j.gaji_pokok FROM karyawan k JOIN jabatan j "
              + "ON k.id_jabatan = j.id_jabatan WHERE k.id_karyawan = ?");
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            double gajiPokok = 0;
            if (rs.next()) gajiPokok = rs.getDouble("gaji_pokok");
            txtGajiPokok.setText(String.valueOf((long) gajiPokok));

            PreparedStatement pst2 = conn.prepareStatement(
                "SELECT COALESCE(SUM(t.nominal),0) AS total FROM tunjangan t "
              + "JOIN karyawan k ON k.id_jabatan = t.id_jabatan WHERE k.id_karyawan = ?");
            pst2.setInt(1, id);
            ResultSet rs2 = pst2.executeQuery();
            double tunjangan = 0;
            if (rs2.next()) tunjangan = rs2.getDouble("total");
            txtTunjangan.setText(String.valueOf((long) tunjangan));
            hitungTotal();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal memuat data gaji: " + e.getMessage());
        }
    }

    private void tampilData() {
        filterData("", 0, "");
    }

    /**
     * Satu jalur tampil data + filter. keyword kosong = semua;
     * bulan 0 = semua bulan; tahun kosong = semua tahun.
     */
    private void filterData(String keyword, int bulan, String tahun) {
        DefaultTableModel model = util.TableUtil.nonEditableModel();
        for (String kolom : new String[]{"ID Gaji", "Karyawan", "Bulan", "Tahun",
                "Gaji Pokok", "Tunjangan", "Potongan", "Total Gaji", "Keterangan"}) {
            model.addColumn(kolom);
        }
        StringBuilder sql = new StringBuilder(
            "SELECT p.*, k.nama_karyawan FROM penggajian p "
          + "JOIN karyawan k ON p.id_karyawan = k.id_karyawan WHERE 1=1 ");
        boolean pakaiKeyword = keyword != null && !keyword.isEmpty();
        if (pakaiKeyword) {
            sql.append("AND (k.nama_karyawan LIKE ? OR k.nik LIKE ?) ");
        }
        if (bulan >= 1 && bulan <= 12) {
            sql.append("AND p.periode LIKE ? ");
        } else if (tahun != null && tahun.matches("\\d{4}")) {
            sql.append("AND p.periode LIKE ? ");
        }
        sql.append("ORDER BY p.id_penggajian DESC");
        try {
            Connection conn = Koneksi.getConnection();
            PreparedStatement pst = conn.prepareStatement(sql.toString());
            int idx = 1;
            if (pakaiKeyword) {
                pst.setString(idx++, "%" + keyword + "%");
                pst.setString(idx++, "%" + keyword + "%");
            }
            if (bulan >= 1 && bulan <= 12) {
                pst.setString(idx++, "%-" + String.format("%02d", bulan));
            } else if (tahun != null && tahun.matches("\\d{4}")) {
                pst.setString(idx++, tahun + "-%");
            }
            ResultSet rs = pst.executeQuery();
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getInt("id_penggajian"),
                    rs.getString("nama_karyawan"),
                    namaBulanDariPeriode(rs.getString("periode")),
                    tahunDariPeriode(rs.getString("periode")),
                    util.FormatRupiah.formatPlain(rs.getDouble("gaji_pokok")),
                    util.FormatRupiah.formatPlain(rs.getDouble("total_tunjangan")),
                    util.FormatRupiah.formatPlain(rs.getDouble("potongan")),
                    util.FormatRupiah.formatPlain(rs.getDouble("total_gaji")),
                    ""
                });
            }
            tblPenggajian.setModel(model);
            pasangRendererTabel();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal menampilkan data: " + e.getMessage());
        }
    }

    private String namaBulanDariPeriode(String periode) {
        if (periode == null || !periode.contains("-")) return "";
        try {
            int b = Integer.parseInt(periode.split("-")[1]);
            return (b >= 1 && b <= 12) ? BULAN[b - 1] : periode;
        } catch (Exception e) {
            return periode;
        }
    }

    private String tahunDariPeriode(String periode) {
        if (periode == null || !periode.contains("-")) return "";
        return periode.split("-")[0];
    }

    // ============================================================= CRUD

    private void simpanData() {
        try {
            int id = idKaryawanTerpilih();
            String periode = periodeTerpilih();
            java.math.BigDecimal pokok = bacaNominal(txtGajiPokok, "Gaji pokok");
            java.math.BigDecimal tunjangan = bacaNominal(txtTunjangan, "Tunjangan");
            java.math.BigDecimal potongan = bacaNominal(txtPotongan, "Potongan");
            java.math.BigDecimal total = PenggajianService.hitungTotal(pokok, tunjangan, potongan);

            PenggajianService.validasiSebelumSimpan(id, periode, pokok, tunjangan, potongan, true);

            Connection conn = Koneksi.getConnection();
            PreparedStatement pst = conn.prepareStatement(
                "INSERT INTO penggajian (id_karyawan, periode, gaji_pokok, total_tunjangan, "
              + "potongan, total_gaji, tanggal_gaji) VALUES (?, ?, ?, ?, ?, ?, CURDATE())");
            pst.setInt(1, id);
            pst.setString(2, periode);
            pst.setBigDecimal(3, pokok);
            pst.setBigDecimal(4, tunjangan);
            pst.setBigDecimal(5, potongan);
            pst.setBigDecimal(6, total);
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this, "Data penggajian berhasil disimpan.");
            tampilData();
            resetForm();
        } catch (IllegalArgumentException | IllegalStateException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Data belum lengkap",
                    JOptionPane.WARNING_MESSAGE);
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
            java.math.BigDecimal pokok = bacaNominal(txtGajiPokok, "Gaji pokok");
            java.math.BigDecimal tunjangan = bacaNominal(txtTunjangan, "Tunjangan");
            java.math.BigDecimal potongan = bacaNominal(txtPotongan, "Potongan");
            java.math.BigDecimal total = PenggajianService.hitungTotal(pokok, tunjangan, potongan);
            PenggajianService.validasiSebelumSimpan(1, "2026-01", pokok, tunjangan, potongan, false);

            Connection conn = Koneksi.getConnection();
            PreparedStatement pst = conn.prepareStatement(
                "UPDATE penggajian SET gaji_pokok=?, total_tunjangan=?, potongan=?, total_gaji=? "
              + "WHERE id_penggajian=?");
            pst.setBigDecimal(1, pokok);
            pst.setBigDecimal(2, tunjangan);
            pst.setBigDecimal(3, potongan);
            pst.setBigDecimal(4, total);
            pst.setString(5, idGaji);
            pst.executeUpdate();

            JOptionPane.showMessageDialog(this, "Data berhasil diubah.");
            tampilData();
            resetForm();
        } catch (IllegalArgumentException | IllegalStateException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Data belum valid",
                    JOptionPane.WARNING_MESSAGE);
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
        if (!util.Session.isHRD()) {
            JOptionPane.showMessageDialog(this,
                    "Hanya HRD yang boleh menghapus data penggajian.",
                    "Akses ditolak", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int konfirmasi = JOptionPane.showConfirmDialog(this,
                "Yakin hapus data penggajian ID " + idGaji + "?",
                "Konfirmasi", JOptionPane.YES_NO_OPTION);
        if (konfirmasi == JOptionPane.YES_OPTION) {
            try {
                Connection conn = Koneksi.getConnection();
                PreparedStatement pst = conn.prepareStatement(
                        "DELETE FROM penggajian WHERE id_penggajian=?");
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
        sedangIsiForm = true;
        txtIdGaji.setText("");
        txtGajiPokok.setText("");
        txtTunjangan.setText("");
        txtPotongan.setText("");
        txtTotalGaji.setText("");
        txtKeterangan.setText("");
        if (cmbKaryawan.getItemCount() > 0) cmbKaryawan.setSelectedIndex(0);
        sedangIsiForm = false;
        btnSimpan.setEnabled(true);
        btnUbah.setEnabled(false);
        btnHapus.setEnabled(false);
        setBulanTahunSekarang();
    }

    private void ambilDataDariTabel() {
        int baris = tblPenggajian.getSelectedRow();
        if (baris < 0) return;
        sedangIsiForm = true;
        try {
            txtIdGaji.setText(String.valueOf(tblPenggajian.getValueAt(baris, 0)));
            String nama = String.valueOf(tblPenggajian.getValueAt(baris, 1));
            for (int i = 1; i < cmbKaryawan.getItemCount(); i++) {
                if (String.valueOf(cmbKaryawan.getItemAt(i)).contains(" - " + nama)
                        || String.valueOf(cmbKaryawan.getItemAt(i)).endsWith("- " + nama)) {
                    cmbKaryawan.setSelectedIndex(i);
                    break;
                }
            }
            txtGajiPokok.setText(String.valueOf((long)
                    util.FormatRupiah.parse(safe(tblPenggajian.getValueAt(baris, 4)))));
            txtTunjangan.setText(String.valueOf((long)
                    util.FormatRupiah.parse(safe(tblPenggajian.getValueAt(baris, 5)))));
            txtPotongan.setText(String.valueOf((long)
                    util.FormatRupiah.parse(safe(tblPenggajian.getValueAt(baris, 6)))));
            txtTotalGaji.setText(String.valueOf((long)
                    util.FormatRupiah.parse(safe(tblPenggajian.getValueAt(baris, 7)))));
            btnSimpan.setEnabled(false);
            btnUbah.setEnabled(true);
            btnHapus.setEnabled(true);
        } finally {
            sedangIsiForm = false;
        }
    }

    private String safe(Object v) {
        return v == null ? "" : String.valueOf(v).trim();
    }

    private void cetakSlip() {
        String idGaji = txtIdGaji.getText().trim();
        if (idGaji.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Pilih data penggajian dari tabel dulu.");
            return;
        }
        try {
            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("id_gaji", Integer.parseInt(idGaji));
            util.ReportUtil.showReport("slip_gaji", params);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "ID gaji harus berupa angka.");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Gagal cetak slip: " + e.getMessage());
        }
    }

    // ==================================================== handler JForm (GEN)

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code manually kalau masih mau pakai tab Design NetBeans.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        lblTitle = new javax.swing.JLabel();
        lblSubjudul = new javax.swing.JLabel();
        pnlFilter = new javax.swing.JPanel();
        lblCari = new javax.swing.JLabel();
        txtCari = new javax.swing.JTextField();
        btnCari = new javax.swing.JButton();
        lblFilterBulan = new javax.swing.JLabel();
        cmbFilterBulan = new javax.swing.JComboBox<>();
        lblFilterTahun = new javax.swing.JLabel();
        txtFilterTahun = new javax.swing.JTextField();
        btnFilter = new javax.swing.JButton();
        btnResetFilter = new javax.swing.JButton();
        lblPeriode = new javax.swing.JLabel();
        btnRefresh = new javax.swing.JButton();
        pnlForm = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        lblIdGaji = new javax.swing.JLabel();
        txtIdGaji = new javax.swing.JTextField();
        lblKaryawan = new javax.swing.JLabel();
        cmbKaryawan = new javax.swing.JComboBox<>();
        lblBulan = new javax.swing.JLabel();
        cmbBulan = new javax.swing.JComboBox<>();
        lblTahun = new javax.swing.JLabel();
        txtTahun = new javax.swing.JTextField();
        lblGajiPokok = new javax.swing.JLabel();
        txtGajiPokok = new javax.swing.JTextField();
        lblTunjangan = new javax.swing.JLabel();
        txtTunjangan = new javax.swing.JTextField();
        lblPotongan = new javax.swing.JLabel();
        txtPotongan = new javax.swing.JTextField();
        lblTotalGaji = new javax.swing.JLabel();
        txtTotalGaji = new javax.swing.JTextField();
        lblKeterangan = new javax.swing.JLabel();
        txtKeterangan = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        btnHitung = new javax.swing.JButton();
        btnSimpan = new javax.swing.JButton();
        btnUbah = new javax.swing.JButton();
        btnHapus = new javax.swing.JButton();
        btnResetAksi = new javax.swing.JButton();
        btnCetakSlip = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jLabelJudulTabel = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        tblPenggajian = new javax.swing.JTable();

        lblTitle.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        lblTitle.setForeground(new java.awt.Color(22, 48, 92)); // NOI18N
        lblTitle.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblTitle.setText("PENGGAJIAN KARYAWAN");

        lblSubjudul.setForeground(new java.awt.Color(75, 85, 99)); // NOI18N
        lblSubjudul.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        lblSubjudul.setText("Kelola gaji pokok, tunjangan, potongan, dan slip gaji karyawan.");

        pnlFilter.setBackground(new java.awt.Color(255, 255, 255));
        pnlFilter.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 226, 235)));

        lblCari.setText("Cari (NIK / nama)");

        txtCari.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtCariActionPerformed(evt);
            }
        });

        btnCari.setText("Cari");
        btnCari.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCariActionPerformed(evt);
            }
        });

        lblFilterBulan.setText("Bulan");

        cmbFilterBulan.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Semua Bulan", "Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember" }));
        cmbFilterBulan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbFilterBulanActionPerformed(evt);
            }
        });

        lblFilterTahun.setText("Tahun");

        txtFilterTahun.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtFilterTahunActionPerformed(evt);
            }
        });

        btnFilter.setText("Filter");
        btnFilter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnFilterActionPerformed(evt);
            }
        });

        btnResetFilter.setText("Reset");
        btnResetFilter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnResetFilterActionPerformed(evt);
            }
        });

        lblPeriode.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        lblPeriode.setForeground(new java.awt.Color(22, 48, 92)); // NOI18N
        lblPeriode.setText("Periode aktif: -");

        btnRefresh.setText("Refresh");
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
                .addGap(16, 16, 16)
                .addGroup(pnlFilterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(pnlFilterLayout.createSequentialGroup()
                        .addComponent(lblCari)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtCari, javax.swing.GroupLayout.PREFERRED_SIZE, 220, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnCari)
                        .addGap(18, 18, 18)
                        .addComponent(lblFilterBulan)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmbFilterBulan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(lblFilterTahun)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtFilterTahun, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnFilter)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnResetFilter)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(pnlFilterLayout.createSequentialGroup()
                        .addComponent(lblPeriode)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btnRefresh)))
                .addGap(16, 16, 16))
        );
        pnlFilterLayout.setVerticalGroup(
            pnlFilterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlFilterLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addGroup(pnlFilterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblCari)
                    .addComponent(txtCari, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnCari)
                    .addComponent(lblFilterBulan)
                    .addComponent(cmbFilterBulan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblFilterTahun)
                    .addComponent(txtFilterTahun, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(btnFilter)
                    .addComponent(btnResetFilter))
                .addGap(8, 8, 8)
                .addGroup(pnlFilterLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblPeriode)
                    .addComponent(btnRefresh))
                .addGap(12, 12, 12))
        );

        pnlForm.setBackground(new java.awt.Color(255, 255, 255));
        pnlForm.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Data Penggajian", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI", 1, 13), new java.awt.Color(22, 48, 92))); // NOI18N

        lblIdGaji.setText("ID Gaji");

        txtIdGaji.setEditable(false);
        txtIdGaji.setBackground(new java.awt.Color(243, 244, 246));

        lblKaryawan.setText("Karyawan");

        cmbKaryawan.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "-- Pilih Karyawan --" }));
        cmbKaryawan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbKaryawanActionPerformed(evt);
            }
        });

        lblBulan.setText("Bulan (periode gaji)");

        cmbBulan.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember" }));
        cmbBulan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbBulanActionPerformed(evt);
            }
        });

        lblTahun.setText("Tahun");

        lblGajiPokok.setText("Gaji Pokok (Rp)");

        txtGajiPokok.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                txtGajiPokokActionPerformed(evt);
            }
        });

        lblTunjangan.setText("Tunjangan (Rp)");

        txtTunjangan.setEditable(false);
        txtTunjangan.setBackground(new java.awt.Color(243, 244, 246));

        lblPotongan.setText("Potongan (Rp)");

        lblTotalGaji.setText("Total Gaji Bersih (Rp)");

        txtTotalGaji.setEditable(false);
        txtTotalGaji.setBackground(new java.awt.Color(243, 244, 246));

        lblKeterangan.setText("Keterangan");

        javax.swing.GroupLayout jPanel5Layout = new javax.swing.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lblIdGaji, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblKaryawan, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblBulan, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblTahun, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtIdGaji, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cmbKaryawan, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(cmbBulan, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtTahun, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(32, 32, 32)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(lblGajiPokok, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblTunjangan, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblPotongan, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblTotalGaji, javax.swing.GroupLayout.PREFERRED_SIZE, 160, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(10, 10, 10)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(txtGajiPokok, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtTunjangan, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtPotongan, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(txtTotalGaji, javax.swing.GroupLayout.PREFERRED_SIZE, 240, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel5Layout.createSequentialGroup()
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblIdGaji)
                    .addComponent(txtIdGaji, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblGajiPokok)
                    .addComponent(txtGajiPokok, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblKaryawan)
                    .addComponent(cmbKaryawan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblTunjangan)
                    .addComponent(txtTunjangan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblBulan)
                    .addComponent(cmbBulan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblPotongan)
                    .addComponent(txtPotongan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel5Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblTahun)
                    .addComponent(txtTahun, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(lblTotalGaji)
                    .addComponent(txtTotalGaji, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );

        javax.swing.GroupLayout pnlFormLayout = new javax.swing.GroupLayout(pnlForm);
        pnlForm.setLayout(pnlFormLayout);
        pnlFormLayout.setHorizontalGroup(
            pnlFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlFormLayout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(pnlFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel5, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(pnlFormLayout.createSequentialGroup()
                        .addComponent(lblKeterangan)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(txtKeterangan)))
                .addGap(16, 16, 16))
        );
        pnlFormLayout.setVerticalGroup(
            pnlFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(pnlFormLayout.createSequentialGroup()
                .addGap(12, 12, 12)
                .addComponent(jPanel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(pnlFormLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(lblKeterangan)
                    .addComponent(txtKeterangan, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(12, 12, 12))
        );

        btnHitung.setText("Hitung");
        btnHitung.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHitungActionPerformed(evt);
            }
        });

        btnSimpan.setText("Simpan");
        btnSimpan.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnSimpanActionPerformed(evt);
            }
        });

        btnUbah.setText("Ubah");
        btnUbah.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnUbahActionPerformed(evt);
            }
        });

        btnHapus.setText("Hapus");
        btnHapus.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnHapusActionPerformed(evt);
            }
        });

        btnResetAksi.setText("Reset");
        btnResetAksi.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnResetAksiActionPerformed(evt);
            }
        });

        btnCetakSlip.setText("Cetak Slip Gaji");
        btnCetakSlip.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnCetakSlipActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(btnHitung)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnSimpan)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnUbah)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnHapus)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(btnResetAksi)
                .addGap(24, 24, 24)
                .addComponent(btnCetakSlip)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnHitung)
                    .addComponent(btnSimpan)
                    .addComponent(btnUbah)
                    .addComponent(btnHapus)
                    .addComponent(btnResetAksi)
                    .addComponent(btnCetakSlip))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3.setBackground(new java.awt.Color(255, 255, 255));
        jPanel3.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(220, 226, 235)));

        jLabelJudulTabel.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabelJudulTabel.setForeground(new java.awt.Color(22, 48, 92)); // NOI18N
        jLabelJudulTabel.setText("Daftar Penggajian");

        tblPenggajian.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID Gaji", "Karyawan", "Bulan", "Tahun", "Gaji Pokok", "Tunjangan", "Potongan", "Total Gaji", "Keterangan"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        tblPenggajian.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                tblPenggajianMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(tblPenggajian);

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabelJudulTabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 800, Short.MAX_VALUE))
                .addGap(16, 16, 16))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(jLabelJudulTabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 230, Short.MAX_VALUE)
                .addGap(16, 16, 16))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblTitle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(lblSubjudul, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pnlFilter, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(pnlForm, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addGap(18, 18, 18))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGap(16, 16, 16)
                .addComponent(lblTitle, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(2, 2, 2)
                .addComponent(lblSubjudul, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(pnlFilter, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(pnlForm, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(8, 8, 8)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(16, 16, 16))
        );
    }// </editor-fold>//GEN-END:initComponents


    private void txtGajiPokokActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtGajiPokokActionPerformed
        hitungTotal();
    }//GEN-LAST:event_txtGajiPokokActionPerformed

    private void btnHitungActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHitungActionPerformed
        hitungTotal();
    }//GEN-LAST:event_btnHitungActionPerformed

    private void btnSimpanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnSimpanActionPerformed
        simpanData();
    }//GEN-LAST:event_btnSimpanActionPerformed

    private void btnUbahActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnUbahActionPerformed
        ubahData();
    }//GEN-LAST:event_btnUbahActionPerformed

    private void btnHapusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnHapusActionPerformed
        hapusData();
    }//GEN-LAST:event_btnHapusActionPerformed

    private void btnResetFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnResetFilterActionPerformed
        txtCari.setText("");
        cmbFilterBulan.setSelectedIndex(0);
        txtFilterTahun.setText("");
        tampilData();
    }//GEN-LAST:event_btnResetFilterActionPerformed

    private void btnResetAksiActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnResetAksiActionPerformed
        resetForm();
    }//GEN-LAST:event_btnResetAksiActionPerformed

    private void btnCariActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCariActionPerformed
        terapkanFilterBulanTahun();
    }//GEN-LAST:event_btnCariActionPerformed

    private void txtCariActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtCariActionPerformed
        terapkanFilterBulanTahun();
    }//GEN-LAST:event_txtCariActionPerformed

    private void cmbFilterBulanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbFilterBulanActionPerformed
        if (!sedangIsiForm) terapkanFilterBulanTahun();
    }//GEN-LAST:event_cmbFilterBulanActionPerformed

    private void txtFilterTahunActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_txtFilterTahunActionPerformed
        terapkanFilterBulanTahun();
    }//GEN-LAST:event_txtFilterTahunActionPerformed

    private void btnFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnFilterActionPerformed
        terapkanFilterBulanTahun();
    }//GEN-LAST:event_btnFilterActionPerformed

    private void btnRefreshActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnRefreshActionPerformed
        txtCari.setText("");
        txtFilterTahun.setText("");
        cmbFilterBulan.setSelectedIndex(0);
        tampilData();
    }//GEN-LAST:event_btnRefreshActionPerformed

    private void cmbKaryawanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbKaryawanActionPerformed
        if (!sedangIsiForm && cmbKaryawan.getSelectedIndex() > 0) {
            autoFillGaji();
        }
    }//GEN-LAST:event_cmbKaryawanActionPerformed

    private void cmbBulanActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbBulanActionPerformed
        // periode berubah: cukup diperbarui saat simpan
    }//GEN-LAST:event_cmbBulanActionPerformed

    private void btnCetakSlipActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnCetakSlipActionPerformed
        cetakSlip();
    }//GEN-LAST:event_btnCetakSlipActionPerformed

    private void tblPenggajianMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tblPenggajianMouseClicked
        ambilDataDariTabel();
    }//GEN-LAST:event_tblPenggajianMouseClicked

    private void terapkanFilterBulanTahun() {
        String tahun = txtFilterTahun.getText().trim();
        if (!tahun.isEmpty() && !tahun.matches("\\d{4}")) {
            JOptionPane.showMessageDialog(this, "Tahun filter harus 4 digit angka.",
                    "Filter tidak valid", JOptionPane.WARNING_MESSAGE);
            return;
        }
        filterData(txtCari.getText().trim(), cmbFilterBulan.getSelectedIndex(), tahun);
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnCari;
    private javax.swing.JButton btnCetakSlip;
    private javax.swing.JButton btnFilter;
    private javax.swing.JButton btnHapus;
    private javax.swing.JButton btnHitung;
    private javax.swing.JButton btnRefresh;
    private javax.swing.JButton btnResetAksi;
    private javax.swing.JButton btnResetFilter;
    private javax.swing.JButton btnSimpan;
    private javax.swing.JButton btnUbah;
    private javax.swing.JComboBox<String> cmbBulan;
    private javax.swing.JComboBox<String> cmbFilterBulan;
    private javax.swing.JComboBox<String> cmbKaryawan;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JLabel jLabelJudulTabel;
    private javax.swing.JLabel lblBulan;
    private javax.swing.JLabel lblCari;
    private javax.swing.JLabel lblFilterBulan;
    private javax.swing.JLabel lblFilterTahun;
    private javax.swing.JLabel lblGajiPokok;
    private javax.swing.JLabel lblIdGaji;
    private javax.swing.JLabel lblKaryawan;
    private javax.swing.JLabel lblKeterangan;
    private javax.swing.JLabel lblPeriode;
    private javax.swing.JLabel lblPotongan;
    private javax.swing.JLabel lblSubjudul;
    private javax.swing.JLabel lblTahun;
    private javax.swing.JLabel lblTitle;
    private javax.swing.JLabel lblTotalGaji;
    private javax.swing.JLabel lblTunjangan;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JPanel pnlFilter;
    private javax.swing.JPanel pnlForm;
    private javax.swing.JTable tblPenggajian;
    private javax.swing.JTextField txtCari;
    private javax.swing.JTextField txtFilterTahun;
    private javax.swing.JTextField txtGajiPokok;
    private javax.swing.JTextField txtIdGaji;
    private javax.swing.JTextField txtKeterangan;
    private javax.swing.JTextField txtPotongan;
    private javax.swing.JTextField txtTahun;
    private javax.swing.JTextField txtTotalGaji;
    private javax.swing.JTextField txtTunjangan;
    // End of variables declaration//GEN-END:variables
}
