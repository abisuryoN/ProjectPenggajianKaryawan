package model;

public class Tunjangan {
    private int idTunjangan;
    private String namaTunjangan;
    private double nominalTunjangan;
    private int idJabatan;
    private String namaJabatan;
    private String keterangan;

    public Tunjangan() {}

    public int getIdTunjangan() { return idTunjangan; }
    public void setIdTunjangan(int idTunjangan) { this.idTunjangan = idTunjangan; }

    public String getNamaTunjangan() { return namaTunjangan; }
    public void setNamaTunjangan(String namaTunjangan) { this.namaTunjangan = namaTunjangan; }

    public double getNominalTunjangan() { return nominalTunjangan; }
    public void setNominalTunjangan(double nominalTunjangan) { this.nominalTunjangan = nominalTunjangan; }

    // Alias untuk kode lama yang pakai nominal.
    public double getNominal() { return nominalTunjangan; }
    public void setNominal(double nominal) { this.nominalTunjangan = nominal; }

    public int getIdJabatan() { return idJabatan; }
    public void setIdJabatan(int idJabatan) { this.idJabatan = idJabatan; }

    public String getNamaJabatan() { return namaJabatan; }
    public void setNamaJabatan(String namaJabatan) { this.namaJabatan = namaJabatan; }

    public String getKeterangan() { return keterangan; }
    public void setKeterangan(String keterangan) { this.keterangan = keterangan; }
}
