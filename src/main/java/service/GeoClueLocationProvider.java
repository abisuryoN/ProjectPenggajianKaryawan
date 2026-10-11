package service;

import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * Provider lokasi berbasis GeoClue2 (D-Bus) di Linux — implementasi NYATA,
 * tanpa koordinat palsu (Bagian D §15).
 *
 * <p>GeoClue2 dipanggil lewat {@code gdbus} (bagian dari paket GLib, tersedia
 * di Ubuntu). Bila GeoClue tidak aktif, geolokasi diblokir untuk user ini, atau
 * jawaban tidak diterima dalam timeout, provider mengembalikan
 * {@code tersedia = false} — dan absensi ditolak.
 *
 * <p>Keterbatasan yang perlu diketahui (dokumentasi jujur):
 * <ul>
 *   <li>Laptop tanpa GPS dan tanpa layanan geolokasi aktif tidak punya sumber
 *       koordinat sama sekali. Pada mesin uji proyek ini, geoclue.service
 *       berstatus {@code inactive (dead)} dan D-Bus membalas
 *       {@code Geolocation disabled for UID 1000}, sehingga lokasi selalu
 *       "belum tersedia". Itu bukan bug aplikasi.</li>
 *   <li>GeoClue berbasis WiFi/IP hanya akurat puluhan–ratusan meter; untuk
 *       radius 200 m nilai akurasi yang dilaporkan tetap diverifikasi
 *       ({@code LocationService#AKURASI_MAKS_METER}).</li>
 * </ul>
 */
public class GeoClueLocationProvider implements LocationProvider {

    /** Batas waktu total (ms) menunggu jawaban GeoClue. */
    private static final long TIMEOUT_MS = 20_000;

    /** Akurasi yang diminta: 4 = EXACT (WiFi/GPS bila tersedia). */
    private static final int ACCURACY_EXACT = 4;

    @Override
    public String nama() {
        return "GeoClue2 (D-Bus)";
    }

    @Override
    public HasilLokasiPengguna ambilLokasi() {
        String helper = "dbus-send --system --print-reply --dest=org.freedesktop.GeoClue2 "
                + "/org/freedesktop/GeoClue2/Manager org.freedesktop.GeoClue2.Manager.GetClient";
        try {
            // GeoClue2 "GetClient" mengembalikan object path client; client
            // hanya hidup di koneksi yang membuatnya, sehingga pendekatan CLI
            // per-perintah tidak dapat mempertahankan status. Karena itu kita
            // pakai skrip pendamping (gdbus satu sesi) lewat helper python3
            // bila tersedia; kalau tidak, laporkan sebagai tidak tersedia.
            return ambilViaHelper();
        } catch (Exception e) {
            return HasilLokasiPengguna.tidakAda(
                    "Lokasi belum tersedia (" + ringkas(e.getMessage()) + ").");
        }
    }

    /**
     * Memakai helper Python yang mempertahankan satu koneksi D-Bus selama
     * permintaan lokasi. Bila helper/python tidak tersedia, hasil = tidak ada.
     */
    private HasilLokasiPengguna ambilViaHelper() throws Exception {
        ProcessBuilder pb = new ProcessBuilder(
                "python3", "-c", SKRIP_HELPER, String.valueOf(ACCURACY_EXACT));
        pb.redirectErrorStream(true);
        Process p = pb.start();
        StringBuilder out = new StringBuilder();
        Thread pembaca = new Thread(() -> {
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
                String baris;
                while ((baris = r.readLine()) != null) {
                    synchronized (out) { out.append(baris).append('\n'); }
                }
            } catch (Exception ignored) {
            }
        });
        pembaca.setDaemon(true);
        pembaca.start();

        long tenggat = System.currentTimeMillis() + TIMEOUT_MS;
        while (System.currentTimeMillis() < tenggat) {
            synchronized (out) {
                String hasil = out.toString();
                if (hasil.contains("OK ")) {
                    return parse(hasil);
                }
                if (hasil.contains("GAGAL ")) {
                    return HasilLokasiPengguna.tidakAda(
                            "Lokasi belum tersedia (" + pesanGagal(hasil) + ").");
                }
            }
            if (!p.isAlive()) break;
            Thread.sleep(250);
        }
        p.destroyForcibly();
        return HasilLokasiPengguna.tidakAda(
                "Lokasi belum tersedia (layanan geolokasi tidak menjawab dalam "
                + (TIMEOUT_MS / 1000) + " detik).");
    }

    private HasilLokasiPengguna parse(String keluaran) {
        String baris = cari(keluaran, "OK ");
        // format: OK lat lon akurasi deskripsi
        String[] bagian = baris.trim().split("\\s+", 5);
        if (bagian.length < 4) {
            return HasilLokasiPengguna.tidakAda("Jawaban lokasi tidak lengkap.");
        }
        double lat = Double.parseDouble(bagian[1]);
        double lon = Double.parseDouble(bagian[2]);
        double akurasi = Double.parseDouble(bagian[3]);
        String ket = bagian.length >= 5 ? bagian[4] : "Lokasi diperoleh";
        return HasilLokasiPengguna.ada(lat, lon, akurasi, ket);
    }

    private String pesanGagal(String keluaran) {
        String baris = cari(keluaran, "GAGAL ");
        return baris.trim().replaceFirst("^GAGAL\\s*", "");
    }

    private static String cari(String teks, String penanda) {
        for (String baris : teks.split("\n")) {
            if (baris.contains(penanda)) return baris;
        }
        return "";
    }

    private static String ringkas(String pesan) {
        if (pesan == null) return "tidak diketahui";
        return pesan.length() > 120 ? pesan.substring(0, 120) + "…" : pesan;
    }

    /**
     * Helper Python: buat client GeoClue, minta lokasi, cetak satu baris
     * hasil lalu keluar. Sengaja tidak menyimpan apa pun ke disk.
     */
    private static final String SKRIP_HELPER =
            "import sys, time\n"
            + "try:\n"
            + "    import dbus\n"
            + "    bus = dbus.SystemBus()\n"
            + "    mgr = dbus.Interface(bus.get_object('org.freedesktop.GeoClue2',\n"
            + "        '/org/freedesktop/GeoClue2/Manager'),\n"
            + "        'org.freedesktop.GeoClue2.Manager')\n"
            + "    cp = mgr.GetClient()\n"
            + "    obj = bus.get_object('org.freedesktop.GeoClue2', cp)\n"
            + "    props = dbus.Interface(obj, 'org.freedesktop.DBus.Properties')\n"
            + "    props.Set('org.freedesktop.GeoClue2.Client', 'DesktopId', 'ProjectPenggajian')\n"
            + "    props.Set('org.freedesktop.GeoClue2.Client', 'RequestedAccuracyLevel',\n"
            + "        dbus.UInt32(int(sys.argv[1])))\n"
            + "    cli = dbus.Interface(obj, 'org.freedesktop.GeoClue2.Client')\n"
            + "    cli.Start()\n"
            + "    for _ in range(12):\n"
            + "        time.sleep(1.5)\n"
            + "        loc = str(props.Get('org.freedesktop.GeoClue2.Client', 'Location'))\n"
            + "        if loc != '/':\n"
            + "            lobj = bus.get_object('org.freedesktop.GeoClue2', loc)\n"
            + "            lp = dbus.Interface(lobj, 'org.freedesktop.DBus.Properties')\n"
            + "            def g(k):\n"
            + "                return lp.Get('org.freedesktop.GeoClue2.Location', k)\n"
            + "            print('OK %s %s %s %s' % (g('Latitude'), g('Longitude'),\n"
            + "                g('Accuracy'), str(g('Description')).replace('\\n', ' ')))\n"
            + "            cli.Stop()\n"
            + "            sys.exit(0)\n"
            + "    print('GAGAL tidak ada jawaban lokasi dari GeoClue')\n"
            + "except Exception as e:\n"
            + "    print('GAGAL %s' % str(e).replace('\\n', ' '))\n";
}
