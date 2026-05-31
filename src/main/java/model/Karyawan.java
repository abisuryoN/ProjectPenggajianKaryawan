package model;

public class Karyawan {
    private int idKaryawan;
    private String nik;
    private String namaKaryawan;
    private String jenisKelamin;
    private String noHp;
    private String alamat;
    private int idJabatan;
    private String namaJabatan;
    private int idDivisi;
    private String namaDivisi;
    private String username;
    private String password;

    public Karyawan() {}

    public int getIdKaryawan() { return idKaryawan; }
    public void setIdKaryawan(int idKaryawan) { this.idKaryawan = idKaryawan; }

    public String getNik() { return nik; }
    public void setNik(String nik) { this.nik = nik; }

    public String getNamaKaryawan() { return namaKaryawan; }
    public void setNamaKaryawan(String namaKaryawan) { this.namaKaryawan = namaKaryawan; }

    // Alias biar tetap cocok kalau ada kode lama yang pakai getNama/setNama.
    public String getNama() { return namaKaryawan; }
    public void setNama(String nama) { this.namaKaryawan = nama; }

    public String getJenisKelamin() { return jenisKelamin; }
    public void setJenisKelamin(String jenisKelamin) { this.jenisKelamin = jenisKelamin; }

    public String getNoHp() { return noHp; }
    public void setNoHp(String noHp) { this.noHp = noHp; }

    public String getAlamat() { return alamat; }
    public void setAlamat(String alamat) { this.alamat = alamat; }

    public int getIdJabatan() { return idJabatan; }
    public void setIdJabatan(int idJabatan) { this.idJabatan = idJabatan; }

    public String getNamaJabatan() { return namaJabatan; }
    public void setNamaJabatan(String namaJabatan) { this.namaJabatan = namaJabatan; }

    public int getIdDivisi() { return idDivisi; }
    public void setIdDivisi(int idDivisi) { this.idDivisi = idDivisi; }

    public String getNamaDivisi() { return namaDivisi; }
    public void setNamaDivisi(String namaDivisi) { this.namaDivisi = namaDivisi; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
