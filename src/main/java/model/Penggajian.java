package model;

public class Penggajian {
    private int idGaji;
    private int idPenggajian;
    private int idKaryawan;
    private String namaKaryawan;
    private String bulan;
    private String tahun;
    private double gajiPokok;
    private double tunjangan;
    private double potongan;
    private double totalGaji;
    private String keterangan;

    public Penggajian() {}

    public int getIdGaji() { return idGaji; }
    public void setIdGaji(int idGaji) { this.idGaji = idGaji; this.idPenggajian = idGaji; }

    // Alias untuk kode lama.
    public int getIdPenggajian() { return idPenggajian == 0 ? idGaji : idPenggajian; }
    public void setIdPenggajian(int idPenggajian) { this.idPenggajian = idPenggajian; this.idGaji = idPenggajian; }

    public int getIdKaryawan() { return idKaryawan; }
    public void setIdKaryawan(int idKaryawan) { this.idKaryawan = idKaryawan; }

    public String getNamaKaryawan() { return namaKaryawan; }
    public void setNamaKaryawan(String namaKaryawan) { this.namaKaryawan = namaKaryawan; }

    public String getBulan() { return bulan; }
    public void setBulan(String bulan) { this.bulan = bulan; }

    public String getTahun() { return tahun; }
    public void setTahun(String tahun) { this.tahun = tahun; }

    public String getPeriode() { return bulan + " " + tahun; }
    public void setPeriode(String periode) {
        if (periode == null) return;
        String[] parts = periode.trim().split(" ");
        if (parts.length >= 2) {
            bulan = parts[0];
            tahun = parts[1];
        } else {
            bulan = periode;
        }
    }

    public double getGajiPokok() { return gajiPokok; }
    public void setGajiPokok(double gajiPokok) { this.gajiPokok = gajiPokok; }

    public double getTunjangan() { return tunjangan; }
    public void setTunjangan(double tunjangan) { this.tunjangan = tunjangan; }

    public double getPotongan() { return potongan; }
    public void setPotongan(double potongan) { this.potongan = potongan; }

    public double getTotalGaji() { return totalGaji; }
    public void setTotalGaji(double totalGaji) { this.totalGaji = totalGaji; }

    public String getKeterangan() { return keterangan; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }
}
