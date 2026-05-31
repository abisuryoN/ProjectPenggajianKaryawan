package model;

public class Divisi {
    private int idDivisi;
    private String namaDivisi;
    private String kepalaDivisi;
    private String keterangan;

    public Divisi() {}

    public int getIdDivisi() { return idDivisi; }
    public void setIdDivisi(int idDivisi) { this.idDivisi = idDivisi; }

    public String getNamaDivisi() { return namaDivisi; }
    public void setNamaDivisi(String namaDivisi) { this.namaDivisi = namaDivisi; }

    public String getKepalaDivisi() { return kepalaDivisi; }
    public void setKepalaDivisi(String kepalaDivisi) { this.kepalaDivisi = kepalaDivisi; }

    public String getKeterangan() { return keterangan; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }

    @Override
    public String toString() {
        return namaDivisi;
    }
}
