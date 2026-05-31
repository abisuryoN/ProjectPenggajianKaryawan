package model;

public class Absensi {
    private int idAbsensi;
    private int idKaryawan;
    private String namaKaryawan;
    private String tanggal;
    private String jamMasuk;
    private String jamPulang;
    private String status;
    private String keterangan;

    public Absensi() {}

    public int getIdAbsensi() { return idAbsensi; }
    public void setIdAbsensi(int idAbsensi) { this.idAbsensi = idAbsensi; }

    public int getIdKaryawan() { return idKaryawan; }
    public void setIdKaryawan(int idKaryawan) { this.idKaryawan = idKaryawan; }

    public String getNamaKaryawan() { return namaKaryawan; }
    public void setNamaKaryawan(String namaKaryawan) { this.namaKaryawan = namaKaryawan; }

    public String getTanggal() { return tanggal; }
    public void setTanggal(String tanggal) { this.tanggal = tanggal; }

    public String getJamMasuk() { return jamMasuk; }
    public void setJamMasuk(String jamMasuk) { this.jamMasuk = jamMasuk; }

    public String getJamPulang() { return jamPulang; }
    public void setJamPulang(String jamPulang) { this.jamPulang = jamPulang; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getKeterangan() { return keterangan; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }
}
