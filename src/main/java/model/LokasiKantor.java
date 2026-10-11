package model;

/** Konfigurasi lokasi kantor untuk absensi (tabel pengaturan_lokasi). */
public class LokasiKantor {
    private int id;
    private String namaLokasi;
    private double latitude;
    private double longitude;
    private int radiusMeter;
    private boolean aktif;

    public LokasiKantor() {}

    public LokasiKantor(int id, String namaLokasi, double latitude,
                        double longitude, int radiusMeter, boolean aktif) {
        this.id = id;
        this.namaLokasi = namaLokasi;
        this.latitude = latitude;
        this.longitude = longitude;
        this.radiusMeter = radiusMeter;
        this.aktif = aktif;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNamaLokasi() { return namaLokasi; }
    public void setNamaLokasi(String namaLokasi) { this.namaLokasi = namaLokasi; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public int getRadiusMeter() { return radiusMeter; }
    public void setRadiusMeter(int radiusMeter) { this.radiusMeter = radiusMeter; }

    public boolean isAktif() { return aktif; }
    public void setAktif(boolean aktif) { this.aktif = aktif; }

    @Override
    public String toString() {
        return namaLokasi + " (" + latitude + ", " + longitude + "), radius " + radiusMeter + " m";
    }
}
