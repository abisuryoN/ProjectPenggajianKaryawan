package panel.transaksi;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.net.URI;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import model.LokasiKantor;
import service.LocationService;
import util.Session;

/**
 * Pengaturan Lokasi Absensi (HRD) — Bagian D §14.
 *
 * Batas radius maksimum 200 m ditegakkan di {@link LocationService}, bukan di
 * form ini. Form ini hanya mengumpulkan input dan menampilkan hasil validasi.
 */
public class LocationPanelForm extends JPanel {

    private static final Color BIRU_TUA = new Color(18, 40, 90);
    private static final Color ABU = new Color(90, 105, 125);
    private static final Color HIJAU = new Color(20, 184, 166);
    private static final Color MERAH = new Color(239, 68, 68);

    private final JTextField txtNama = new JTextField(24);
    private final JTextField txtLat = new JTextField(18);
    private final JTextField txtLon = new JTextField(18);
    private final JTextField txtRadius = new JTextField(10);
    private final JLabel lblStatus = new JLabel("Memuat konfigurasi...");
    private final JLabel lblKoordinat = new JLabel("-");

    private final LocationService lokasiService = new LocationService();

    public LocationPanelForm() {
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);
        setBorder(BorderFactory.createEmptyBorder(14, 6, 6, 6));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;

        int baris = 0;
        tambahBaris(form, g, baris++, "Nama lokasi", txtNama);
        tambahBaris(form, g, baris++, "Latitude kantor", txtLat);
        tambahBaris(form, g, baris++, "Longitude kantor", txtLon);
        tambahBaris(form, g, baris++, "Radius absensi (meter, maks "
                + LocationService.RADIUS_MAKS_METER + ")", txtRadius);

        g.gridx = 1; g.gridy = baris++;
        lblKoordinat.setForeground(ABU);
        form.add(lblKoordinat, g);

        JPanel tombol = new JPanel();
        tombol.setOpaque(false);
        JButton btnSimpan = tombol("Simpan", HIJAU, Color.WHITE);
        JButton btnMuat = tombol("Muat Ulang", new Color(243, 244, 246), Color.BLACK);
        JButton btnPeta = tombol("Buka di Peta", new Color(79, 70, 229), Color.WHITE);
        tombol.add(btnSimpan);
        tombol.add(btnMuat);
        tombol.add(btnPeta);

        g.gridx = 1; g.gridy = baris++;
        form.add(tombol, g);

        lblStatus.setForeground(ABU);
        g.gridx = 1; g.gridy = baris;
        form.add(lblStatus, g);

        JLabel catatan = new JLabel("<html><b>Catatan penting.</b> Desktop Ubuntu tanpa perangkat GPS "
                + "tidak menyediakan koordinat pengguna yang akurat. Bila layanan lokasi sistem tidak "
                + "tersedia, karyawan <b>tidak dapat</b> absen — aplikasi tidak memakai koordinat palsu "
                + "atau IP sebagai pengganti GPS.</html>");
        catatan.setForeground(ABU);
        catatan.setBorder(BorderFactory.createEmptyBorder(16, 6, 0, 6));

        JPanel atas = new JPanel(new BorderLayout());
        atas.setOpaque(false);
        atas.add(form, BorderLayout.NORTH);
        atas.add(catatan, BorderLayout.CENTER);
        add(atas, BorderLayout.NORTH);

        btnSimpan.addActionListener(e -> simpan());
        btnMuat.addActionListener(e -> muat());
        btnPeta.addActionListener(e -> bukaPeta());
    }

    private void tambahBaris(JPanel form, GridBagConstraints g, int baris,
                             String label, java.awt.Component input) {
        g.gridx = 0; g.gridy = baris; g.weightx = 0;
        JLabel l = new JLabel(label);
        l.setForeground(BIRU_TUA);
        form.add(l, g);
        g.gridx = 1; g.weightx = 1;
        form.add(input, g);
    }

    /** Muat konfigurasi terbaru dari database (bukan nilai tertanam di kode). */
    public void muat() {
        try {
            LokasiKantor k = lokasiService.konfigurasiAktif();
            if (k == null) {
                txtNama.setText("Kantor Pusat");
                txtLat.setText("");
                txtLon.setText("");
                txtRadius.setText(String.valueOf(LocationService.RADIUS_DEFAULT_METER));
                lblStatus.setText("Belum ada konfigurasi tersimpan. Isi koordinat lalu Simpan.");
                lblStatus.setForeground(MERAH);
                lblKoordinat.setText("Radius default " + LocationService.RADIUS_DEFAULT_METER + " m");
            } else {
                txtNama.setText(k.getNamaLokasi());
                txtLat.setText(String.valueOf(k.getLatitude()));
                txtLon.setText(String.valueOf(k.getLongitude()));
                txtRadius.setText(String.valueOf(k.getRadiusMeter()));
                lblStatus.setText("Konfigurasi aktif dimuat dari database.");
                lblStatus.setForeground(HIJAU);
                lblKoordinat.setText("Koordinat aktif: " + k.getLatitude()
                        + ", " + k.getLongitude() + "  (radius efektif "
                        + lokasiService.radiusEfektif(k) + " m)");
            }
        } catch (Exception e) {
            lblStatus.setText("Gagal memuat: " + e.getMessage());
            lblStatus.setForeground(MERAH);
        }
    }

    private void simpan() {
        try {
            double lat = Double.parseDouble(txtLat.getText().trim());
            double lon = Double.parseDouble(txtLon.getText().trim());
            int radius = Integer.parseInt(txtRadius.getText().trim());

            // Validasi + penegakan izin HRD ada di service (§14, §17)
            lokasiService.simpanKonfigurasi(Session.isHRD(), Session.getIdUser(),
                    txtNama.getText(), lat, lon, radius);

            JOptionPane.showMessageDialog(this, "Konfigurasi lokasi tersimpan. "
                    + "Pemeriksaan absensi berikutnya langsung memakai nilai ini.");
            muat();
        } catch (NumberFormatException nfe) {
            lblStatus.setText("Latitude/longitude harus angka, radius harus bilangan bulat.");
            lblStatus.setForeground(MERAH);
        } catch (SecurityException se) {
            lblStatus.setText("Ditolak: " + se.getMessage());
            lblStatus.setForeground(MERAH);
        } catch (IllegalArgumentException iae) {
            lblStatus.setText("Input tidak valid: " + iae.getMessage());
            lblStatus.setForeground(MERAH);
        } catch (Exception e) {
            lblStatus.setText("Gagal menyimpan: " + e.getMessage());
            lblStatus.setForeground(MERAH);
        }
    }

    private void bukaPeta() {
        try {
            String lat = txtLat.getText().trim();
            String lon = txtLon.getText().trim();
            if (lat.isEmpty() || lon.isEmpty()) {
                lblStatus.setText("Isi latitude dan longitude dulu untuk melihat peta.");
                return;
            }
            // Peta hanya alat bantu visual; keputusan absensi tetap dari hitungan koordinat.
            String url = "https://www.openstreetmap.org/?mlat=" + lat + "&mlon=" + lon
                    + "#map=18/" + lat + "/" + lon;
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                lblStatus.setText("Tautan peta: " + url);
            }
        } catch (Exception e) {
            lblStatus.setText("Tidak bisa membuka peta: " + e.getMessage());
        }
    }

    private static JButton tombol(String teks, Color bg, Color fg) {
        JButton b = new JButton(teks);
        b.setBackground(bg);
        b.setForeground(fg);
        b.setOpaque(true);
        b.setContentAreaFilled(true);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setFont(new Font(util.DesignUtil.UI_FONT, Font.BOLD, 13));
        return b;
    }
}
