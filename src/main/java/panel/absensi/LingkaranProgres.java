package panel.absensi;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;

/**
 * Indikator progres registrasi wajah dalam bentuk SATU lingkaran.
 *
 * <p>Satu arc melingkar yang terisi sesuai jumlah foto valid (0..3):
 * 0/3 kosong, 1/3 sepertiga, 2/3 dua pertiga, 3/3 penuh. Di tengah
 * lingkaran tampil teks "n/3". Digambar lewat {@code paintComponent()}
 * khusus komponen ini saja.
 */
public class LingkaranProgres extends JPanel {

    private int valid = 0;
    private int total = 3;

    public LingkaranProgres() {
        setOpaque(false);
        setPreferredSize(new java.awt.Dimension(150, 150));
    }

    /** Perbarui jumlah foto valid (0..total) dan gambar ulang. */
    public void setProgres(int valid, int total) {
        this.valid = Math.max(0, Math.min(total, valid));
        this.total = Math.max(1, total);
        repaint();
    }

    public int getValid() { return valid; }
    public int getTotal() { return total; }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int sisi = Math.min(getWidth(), getHeight());
        int cx = (getWidth() - sisi) / 2;
        int cy = (getHeight() - sisi) / 2;

        float tebal = Math.max(8f, sisi * 0.09f);
        int margin = (int) (tebal / 2f + 3);
        double diameter = sisi - margin * 2.0;

        // rel lingkaran penuh (belum terisi)
        g2.setColor(panel.absensi.AbsensiUI.GARIS);
        g2.setStroke(new BasicStroke(tebal, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(new Arc2D.Double(cx + margin, cy + margin, diameter, diameter,
                0, 360, Arc2D.OPEN));

        // arc progres
        if (valid > 0) {
            double sudut = 360.0 * valid / total;
            Color warna = (valid >= total)
                    ? panel.absensi.AbsensiUI.HIJAU
                    : panel.absensi.AbsensiUI.AKSEN;
            g2.setColor(warna);
            // Arc2D sudut positif = berlawanan arah jarum jam; mulai dari atas
            // (-90°) agar terisi searah jarum jam.
            g2.draw(new Arc2D.Double(cx + margin, cy + margin, diameter, diameter,
                    90, -sudut, Arc2D.OPEN));
        }

        // teks tengah "n/3"
        String teks = valid + "/" + total;
        g2.setColor(panel.absensi.AbsensiUI.NAVY);
        g2.setFont(new Font(panel.absensi.AbsensiUI.font(java.awt.Font.BOLD, 13).getFamily(),
                java.awt.Font.BOLD, Math.max(14, (int) (sisi * 0.16))));
        FontMetrics fm = g2.getFontMetrics();
        int tx = cx + (sisi - fm.stringWidth(teks)) / 2;
        int ty = cy + (sisi + fm.getAscent() - fm.getDescent()) / 2
                - (int) (sisi * 0.02);
        g2.drawString(teks, tx, ty);

        g2.dispose();
    }
}
