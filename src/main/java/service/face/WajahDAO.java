package service.face;

import config.Koneksi;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Akses tabel {@code wajah_karyawan} (Bagian C §11).
 *
 * <p>Template = embedding SFace 128-D hasil RATA-RATA beberapa sampel
 * pendaftaran (variasi sudut/pencahayaan), disimpan sebagai BLOB biner
 * (float big-endian, 512 byte), BUKAN path/nama file. Satu baris per karyawan.
 */
public class WajahDAO {

    /** Template wajah milik satu karyawan. */
    public static final class TemplateWajah {
        public final int idKaryawan;
        public final String namaKaryawan;
        public final float[] embedding;

        TemplateWajah(int id, String nama, float[] embedding) {
            this.idKaryawan = id;
            this.namaKaryawan = nama;
            this.embedding = embedding;
        }
    }

    // ------------------------------------------------------------- serialisasi

    /** float[128] -> byte[512] (big-endian). */
    public static byte[] embeddingKeBytes(float[] v) {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(bos)) {
            dos.writeInt(v.length);
            for (float f : v) dos.writeFloat(f);
            dos.flush();
            return bos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Gagal mengemas embedding: " + e.getMessage(), e);
        }
    }

    /** byte[] -> float[]; null bila data rusak. */
    public static float[] bytesKeEmbedding(byte[] data) {
        if (data == null || data.length < 8) return null;
        try (DataInputStream dis = new DataInputStream(new ByteArrayInputStream(data))) {
            int n = dis.readInt();
            if (n <= 0 || n > 1024 || data.length < 4 + n * 4L) return null;
            float[] v = new float[n];
            for (int i = 0; i < n; i++) v[i] = dis.readFloat();
            return v;
        } catch (Exception e) {
            System.err.println("Template wajah rusak/tidak terbaca: " + e.getMessage());
            return null;
        }
    }

    /** Rata-rata + normalisasi L2 dari beberapa sampel pendaftaran. */
    public static float[] rataRata(List<float[]> sampel) {
        if (sampel == null || sampel.isEmpty()) {
            throw new IllegalArgumentException("Sampel wajah kosong.");
        }
        int n = sampel.get(0).length;
        float[] hasil = new float[n];
        for (float[] s : sampel) {
            if (s == null || s.length != n) {
                throw new IllegalArgumentException("Dimensi sampel tidak konsisten.");
            }
            for (int i = 0; i < n; i++) hasil[i] += s[i];
        }
        for (int i = 0; i < n; i++) hasil[i] /= sampel.size();
        double jumlah = 0;
        for (float f : hasil) jumlah += f * f;
        double norma = Math.sqrt(jumlah);
        if (norma > 0) {
            for (int i = 0; i < n; i++) hasil[i] = (float) (hasil[i] / norma);
        }
        return hasil;
    }

    // --------------------------------------------------------------------- DAO

    /** Muat template milik karyawan; null bila belum terdaftar. */
    public TemplateWajah muatTemplate(int idKaryawan) throws Exception {
        String sql = "SELECT w.template, k.nama_karyawan FROM wajah_karyawan w "
                + "JOIN karyawan k ON k.id_karyawan = w.id_karyawan "
                + "WHERE w.id_karyawan = ?";
        try (Connection c = Koneksi.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idKaryawan);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                float[] emb = bytesKeEmbedding(rs.getBytes("template"));
                if (emb == null) return null;
                return new TemplateWajah(idKaryawan, rs.getString("nama_karyawan"), emb);
            }
        }
    }

    /** True bila karyawan sudah punya template terdaftar. */
    public boolean sudahTerdaftar(int idKaryawan) throws Exception {
        String sql = "SELECT 1 FROM wajah_karyawan WHERE id_karyawan = ? LIMIT 1";
        try (Connection c = Koneksi.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idKaryawan);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Daftarkan/ganti template wajah karyawan.
     *
     * @param olehHRD wajib true — pendaftaran hanya oleh HRD (cek di service).
     * @param idUserHRD id users HRD pendaftar (audit), 0 bila tak diketahui.
     */
    public void simpanTemplate(boolean olehHRD, int idUserHRD, int idKaryawan,
                               List<float[]> sampel, Integer kualitas) throws Exception {
        if (!olehHRD) {
            throw new SecurityException("Pendaftaran wajah hanya boleh dilakukan oleh HRD.");
        }
        if (sampel == null || sampel.size() < 3) {
            throw new IllegalArgumentException(
                    "Butuh minimal 3 sampel wajah (variasi sudut/pencahayaan).");
        }
        float[] template = rataRata(sampel);
        byte[] blob = embeddingKeBytes(template);
        String sql = "INSERT INTO wajah_karyawan "
                + "(id_karyawan, template, algoritma, kualitas, didaftarkan_oleh) "
                + "VALUES (?, ?, 'LBPH-OpenCV', ?, ?) "
                + "ON DUPLICATE KEY UPDATE template = VALUES(template), "
                + "kualitas = VALUES(kualitas), didaftarkan_oleh = VALUES(didaftarkan_oleh), "
                + "didaftarkan_pada = CURRENT_TIMESTAMP";
        try (Connection c = Koneksi.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idKaryawan);
            ps.setBytes(2, blob);
            if (kualitas == null) {
                ps.setNull(3, java.sql.Types.INTEGER);
            } else {
                ps.setInt(3, kualitas);
            }
            if (idUserHRD > 0) {
                ps.setInt(4, idUserHRD);
            } else {
                ps.setNull(4, java.sql.Types.INTEGER);
            }
            ps.executeUpdate();
        }
    }

    /** Daftar karyawan + status pendaftaran wajah (untuk UI HRD). */
    public List<String[]> daftarStatusPendaftaran() throws Exception {
        List<String[]> hasil = new ArrayList<String[]>();
        String sql = "SELECT k.id_karyawan, k.nama_karyawan, "
                + "CASE WHEN w.id_wajah IS NULL THEN 'BELUM' ELSE 'TERDAFTAR' END AS status, "
                + "w.didaftarkan_pada FROM karyawan k "
                + "LEFT JOIN wajah_karyawan w ON w.id_karyawan = k.id_karyawan "
                + "ORDER BY k.nama_karyawan";
        try (Connection c = Koneksi.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                hasil.add(new String[]{
                        rs.getString("id_karyawan"),
                        rs.getString("nama_karyawan"),
                        rs.getString("status"),
                        rs.getString("didaftarkan_pada") == null
                                ? "-" : rs.getString("didaftarkan_pada")});
            }
        }
        return hasil;
    }
}
