package panel.absensi;

import util.DesignUtil;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.Border;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.RenderingHints;

/**
 * Komponen visual bersama untuk modul absensi (kode, bukan .form).
 *
 * <p>Semua warna/font modul absensi didefinisikan di sini supaya HRD dan
 * Karyawan memakai bahasa visual yang sama dan tidak tersebar sebagai
 * literal warna di banyak berkas.
 */
public final class AbsensiUI {

    private AbsensiUI() {}

    // ------------------------------------------------------------------ palet
    /** Latar halaman (terang, bersih). */
    public static final Color PAGE = new Color(244, 247, 250);
    /** Latar kartu/panel utama. */
    public static final Color CARD = Color.WHITE;
    /** Navy gelap untuk header/navigasi. */
    public static final Color NAVY = new Color(22, 48, 92);
    /** Aksen utama (teal). */
    public static final Color AKSEN = new Color(13, 148, 136);
    /** Aksen sekunder (biru). */
    public static final Color AKSEN_BIRU = new Color(37, 99, 235);
    /** Status berhasil. */
    public static final Color HIJAU = new Color(22, 163, 74);
    /** Status menunggu/proses. */
    public static final Color AMBER = new Color(217, 119, 6);
    /** Status gagal. */
    public static final Color MERAH = new Color(220, 38, 38);
    /** Teks utama. */
    public static final Color TEKS = new Color(17, 24, 39);
    /** Teks sekunder. */
    public static final Color TEKS_LEMBUT = new Color(100, 116, 139);
    /** Garis tepi halus. */
    public static final Color GARIS = new Color(226, 232, 240);
    /** Latar lembut (mis. chip netral). */
    public static final Color LEMBUT = new Color(241, 245, 249);

    public static Font font(int gaya, int ukuran) {
        return new Font(DesignUtil.UI_FONT, gaya, ukuran);
    }

    // --------------------------------------------------------------- pabrikan

    /** Judul halaman di dalam header navy. */
    public static JLabel judul(String teks) {
        JLabel l = new JLabel(teks);
        l.setFont(font(Font.BOLD, 20));
        l.setForeground(Color.WHITE);
        return l;
    }

    /** Teks kecil di dalam header navy. */
    public static JLabel subJudul(String teks) {
        JLabel l = new JLabel(teks);
        l.setFont(font(Font.PLAIN, 12));
        l.setForeground(new Color(203, 213, 225));
        return l;
    }

    /** Judul bagian di dalam kartu putih. */
    public static JLabel judulBagian(String teks) {
        JLabel l = new JLabel(teks);
        l.setFont(font(Font.BOLD, 14));
        l.setForeground(NAVY);
        return l;
    }

    /** Label biasa. */
    public static JLabel label(String teks) {
        JLabel l = new JLabel(teks);
        l.setFont(font(Font.PLAIN, 13));
        l.setForeground(TEKS);
        return l;
    }

    /** Baris "kunci: nilai" dengan kunci abu-abu dan nilai tebal. */
    public static JLabel baris(String kunci, String nilai) {
        JLabel l = new JLabel("<html><span style='color:#64748b'>" + escape(kunci)
                + "</span>&nbsp;&nbsp;<b>" + escape(nilai) + "</b></html>");
        l.setFont(font(Font.PLAIN, 13));
        return l;
    }

    /** Tombol utama (terisi warna). */
    public static JButton tombol(String teks, Color bg) {
        JButton b = new JButton(teks);
        DesignUtil.styleFilledButton(b, bg, Color.WHITE);
        b.setFont(font(Font.BOLD, 13));
        return b;
    }

    /** Tombol netral (abu terang). */
    public static JButton tombolNetral(String teks) {
        JButton b = new JButton(teks);
        DesignUtil.styleFilledButton(b, LEMBUT, TEKS);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GARIS),
                BorderFactory.createEmptyBorder(6, 15, 6, 15)));
        b.setFont(font(Font.BOLD, 13));
        return b;
    }

    /** Kartu putih dengan tepi halus dan sudut membulat. */
    public static RoundedPanel kartu() {
        RoundedPanel p = new RoundedPanel(14, CARD, GARIS);
        p.setLayout(new java.awt.BorderLayout(0, 10));
        p.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));
        return p;
    }

    /** Header navy dengan sudut membulat. */
    public static RoundedPanel header() {
        RoundedPanel p = new RoundedPanel(14, NAVY, NAVY);
        p.setLayout(new java.awt.BorderLayout(8, 0));
        p.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));
        return p;
    }

    /** Chip status kecil berwarna. */
    public static JLabel chip(String teks, Color warna) {
        JLabel l = new JLabel(teks, JLabel.CENTER);
        l.setFont(font(Font.BOLD, 12));
        l.setForeground(warna);
        l.setOpaque(true);
        l.setBackground(campur(warna, Color.WHITE, 0.88));
        l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(campur(warna, Color.WHITE, 0.62)),
                BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        return l;
    }

    /** Garis pemisah tipis. */
    public static JPanel garisPemisah() {
        JPanel p = new JPanel();
        p.setBackground(GARIS);
        p.setPreferredSize(new Dimension(10, 1));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return p;
    }

    public static Border tepiKartu() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GARIS),
                BorderFactory.createEmptyBorder(12, 14, 12, 14));
    }

    /** GridBagConstraints dengan nilai default yang nyaman. */
    public static GridBagConstraints gbc(int x, int y) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = x;
        g.gridy = y;
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1;
        return g;
    }

    /** GridBagConstraints untuk komponen yang tidak ikut melebar. */
    public static GridBagConstraints gbcTetap(int x, int y) {
        GridBagConstraints g = gbc(x, y);
        g.fill = GridBagConstraints.NONE;
        g.weightx = 0;
        return g;
    }

    /** Kolom teks putih bersih, gaya modern. */
    public static javax.swing.JTextField input(int kolom) {
        javax.swing.JTextField f = new javax.swing.JTextField(kolom);
        f.setFont(font(Font.PLAIN, 13));
        f.setBackground(Color.WHITE);
        f.setForeground(TEKS);
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(GARIS),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
        return f;
    }

    /** Chip kecil berwarna untuk status ringkas. */
    // ---------------------------------------------------------------- utilitas

    /** Campur dua warna; rasio 0 = warna a, 1 = warna b. */
    public static Color campur(Color a, Color b, double rasio) {
        double r = Math.max(0, Math.min(1, rasio));
        return new Color(
                (int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * r),
                (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * r),
                (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * r));
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    // ------------------------------------------------------------ panel bulat

    /** JPanel dengan sudut membulat dan latar opsional. */
    public static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color isi;
        private final Color tepi;

        public RoundedPanel(int radius, Color isi, Color tepi) {
            this.radius = radius;
            this.isi = isi;
            this.tepi = tepi;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            g2.setColor(isi);
            g2.fillRoundRect(0, 0, w - 1, h - 1, radius, radius);
            if (tepi != null) {
                g2.setColor(tepi);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(0, 0, w - 1, h - 1, radius, radius);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** Panel dengan latar putih dan tepi; dipakai sebagai kartu isi. */
    public static JPanel panelPutih() {
        JPanel p = new JPanel();
        p.setOpaque(true);
        p.setBackground(CARD);
        return p;
    }

    /** Menyeragamkan tinggi baris pada GridBagLayout. */
    public static Component sameHeight(Component c, int tinggi) {
        Dimension d = c.getPreferredSize();
        c.setPreferredSize(new Dimension(d.width, tinggi));
        return c;
    }
}
